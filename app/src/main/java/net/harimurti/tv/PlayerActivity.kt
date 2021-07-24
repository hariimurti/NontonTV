package net.harimurti.tv

import android.net.Uri
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import net.harimurti.tv.databinding.ActivityPlayerBinding
import net.harimurti.tv.databinding.CustomControlBinding
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Playlist
import java.util.*
import kotlin.collections.ArrayList

class PlayerActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = false
    private var skipRetry = false
    private lateinit var preferences: Preferences
    private var playlist: Playlist? = null
    private var categoryId: Int = 0
    private var channels: ArrayList<Channel>? = null
    private var current: Channel? = null
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var controlBinding: CustomControlBinding

    companion object {
        var isFirst = true
        private const val CHANNEL_NEXT = 0
        private const val CHANNEL_PREVIOUS = 1
        private const val CATEGORY_UP = 2
        private const val CATEGORY_DOWN = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFirst = false
        isTelevision = UiMode(this).isTelevision()
        preferences = Preferences(this)

        // get playlist
        playlist = if (!preferences.playLastWatched) Playlist.loaded
            else PlaylistHelper(this).readCache()
        // set channels
        val parcel: PlayData? = intent.getParcelableExtra(PlayData.VALUE)
        val category = parcel.let { playlist?.categories?.get(it?.catId as Int) }
        categoryId = parcel?.catId as Int
        channels = category?.channels
        current = parcel.let { category?.channels?.get(it.chId) }

        // define some view
        controlBinding = CustomControlBinding.bind(binding.root.findViewById(R.id.custom_control))
        controlBinding.playerSettings.setOnClickListener { showTrackSelector() }

        // verify stream_url
        if (current == null) {
            Toast.makeText(this, R.string.player_no_channel, Toast.LENGTH_SHORT).show()
            finish()
        }
        else {
            playChannel()
        }
    }

    private fun playChannel() {
        // set channel name
        controlBinding.channelName.text = current?.name

        // define mediaitem
        val drmLicense = playlist?.drm_licenses?.firstOrNull {
            current?.drm_name?.equals(it.drm_name) == true
        }?.drm_url
        mediaItem = if (drmLicense?.isNotEmpty() == true) {
            MediaItem.Builder()
                .setUri(Uri.parse(current?.stream_url))
                .setDrmUuid(C.WIDEVINE_UUID)
                .setDrmLicenseUri(drmLicense)
                .setDrmMultiSession(true)
                .build()
        } else {
            MediaItem.fromUri(Uri.parse(current?.stream_url))
        }

        // define User-Agent
        val userAgents = listOf(*resources.getStringArray(R.array.user_agent))
        var userAgent = userAgents.firstOrNull {
            current?.stream_url?.contains(it.substring(0, it.indexOf("/")).lowercase(Locale.getDefault())) == true
        }
        if (userAgent.isNullOrEmpty()) {
            userAgent = userAgents[Random().nextInt(userAgents.size)]
        }

        // create player & set listener
        val httpDataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
        val dataSourceFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        trackSelector = DefaultTrackSelector(this)
        trackSelector.parameters = ParametersBuilder(this).build()
        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
        player.addListener(PlayerListener())

        // set player view
        binding.playerView.player = player
        binding.playerView.requestFocus()

        // play mediasouce
        player.playWhenReady = true
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    private fun switchChannel(mode: Int) {
        val chId = channels?.indexOf(current) as Int
        when(mode) {
            CATEGORY_UP -> {
                val back = categoryId - 1
                if (back > -1) {
                    categoryId = back
                    channels = playlist?.categories?.get(back)?.channels
                    current = channels!![0]
                }
            }
            CATEGORY_DOWN -> {
                val next = categoryId + 1
                if (next < playlist?.categories?.size ?: 0) {
                    categoryId = next
                    channels = playlist?.categories?.get(next)?.channels
                    current = channels!![0]
                }
            }
            CHANNEL_PREVIOUS -> {
                val back = chId - 1
                if (back > -1) {
                    current = channels!![back]
                }
                else {
                    switchChannel(CATEGORY_UP)
                    return
                }
            }
            CHANNEL_NEXT -> {
                val next = chId + 1
                if (next < channels?.size ?: 0) {
                    current = channels!![next]
                }
                else {
                    switchChannel(CATEGORY_DOWN)
                    return
                }
            }
        }

        player.playWhenReady = false
        player.release()
        playChannel()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                showLayoutMessage(View.GONE, false)
                preferences.watched = channels?.indexOf(current)?.let { PlayData(categoryId, it) } as PlayData
            } else if (state == Player.STATE_BUFFERING) {
                showLayoutMessage(View.VISIBLE, false)
            }
            controlBinding.playerSettings.isEnabled = TrackSelectionDialog.willHaveContent(trackSelector)
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                binding.textStatus.setText(R.string.source_offline)
            } else {
                binding.textStatus.setText(R.string.something_went_wrong)
            }
            binding.textRetry.setText(R.string.text_auto_retry)
            showLayoutMessage(View.VISIBLE, true)
            retryPlayback()
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            if (trackGroups == lastSeenTrackGroupArray) return
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(applicationContext, getString(R.string.error_unsupported_video), Toast.LENGTH_LONG).show()
                }
                if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(applicationContext, getString(R.string.error_unsupported_audio), Toast.LENGTH_LONG).show()
                }
            }
            lastSeenTrackGroupArray = trackGroups
        }
    }

    private fun retryPlayback() {
        if (skipRetry) return
        skipRetry = true
        val network = Network(applicationContext)
        AsyncSleep(this).task(object : AsyncSleep.Task {
            override fun onCountDown(count: Int) {
                val left = count - 1
                if (!network.isConnected()) {
                    binding.textStatus.setText(R.string.no_network)
                }
                if (left <= 0) {
                    binding.textRetry.setText(R.string.text_auto_retry_now)
                } else {
                    binding.textRetry.text = String.format(getString(R.string.text_auto_retry_second), left)
                }
            }

            override fun onFinish() {
                skipRetry = false
                if (network.isConnected()) {
                    player.setMediaItem(mediaItem)
                    player.prepare()
                } else {
                    retryPlayback()
                }
            }
        }).start(6)
    }

    private fun showTrackSelector() {
        TrackSelectionDialog.createForTrackSelector(trackSelector) { }
            .show(supportFragmentManager, null)
    }

    private fun showLayoutMessage(visibility: Int, isMessage: Boolean) {
        binding.layoutStatus.visibility = visibility
        if (!isMessage) {
            binding.layoutSpin.visibility = View.VISIBLE
            binding.layoutText.visibility = View.GONE
        } else {
            binding.layoutSpin.visibility = View.GONE
            binding.layoutText.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        player.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_MENU -> showTrackSelector()
            KeyEvent.KEYCODE_DPAD_UP -> switchChannel(CATEGORY_UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> switchChannel(CATEGORY_DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> switchChannel(CHANNEL_PREVIOUS)
            KeyEvent.KEYCODE_DPAD_RIGHT -> switchChannel(CHANNEL_NEXT)
            KeyEvent.KEYCODE_PAGE_UP -> switchChannel(CATEGORY_UP)
            KeyEvent.KEYCODE_PAGE_DOWN -> switchChannel(CATEGORY_DOWN)
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> switchChannel(CHANNEL_PREVIOUS)
            KeyEvent.KEYCODE_MEDIA_NEXT -> switchChannel(CHANNEL_NEXT)
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }

    override fun onBackPressed() {
        if (isTelevision || doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finish()
            return
        }
        doubleBackToExitPressedOnce = true
        Toast.makeText(this, getString(R.string.press_back_twice_exit_player), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }
}