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
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Playlist
import java.util.*

class PlayerActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = false
    private var skipRetry = false
    private lateinit var preferences: Preferences
    private var playlist: Playlist? = null
    private var category: Category? = null
    private var current: Channel? = null
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var bindingRoot: ActivityPlayerBinding
    private lateinit var bindingControl: CustomControlBinding

    companion object {
        var isFirst = true
        private const val CHANNEL_NEXT = 0
        private const val CHANNEL_PREVIOUS = 1
        private const val CATEGORY_UP = 2
        private const val CATEGORY_DOWN = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingRoot = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(bindingRoot.root)

        isFirst = false
        isTelevision = UiMode(this).isTelevision()
        preferences = Preferences(this)

        // get playlist
        playlist = if (!preferences.playLastWatched) Playlist.loaded
            else PlaylistHelper(this).readCache()
        // set channels
        val parcel: PlayData? = intent.getParcelableExtra(PlayData.VALUE)
        category = parcel.let { playlist?.categories?.get(it?.catId as Int) }
        current = parcel.let { category?.channels?.get(it?.chId as Int) }

        // define some view
        bindingControl = CustomControlBinding.bind(bindingRoot.root.findViewById(R.id.custom_control))
        bindingControl.playerSettings.setOnClickListener { showTrackSelector() }

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
        // set category & channel name
        bindingControl.categoryName.text = category?.name
        bindingControl.channelName.text = current?.name

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
        bindingRoot.playerView.player = player
        bindingRoot.playerView.requestFocus()

        // play mediasouce
        player.playWhenReady = true
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    private fun switchChannel(mode: Int) {
        val catId = playlist?.categories?.indexOf(category) as Int
        val chId = category?.channels?.indexOf(current) as Int
        when(mode) {
            CATEGORY_UP -> {
                val previous = catId - 1
                if (previous > -1) {
                    category = playlist?.categories?.get(previous)
                    current = category?.channels!![0]
                }
            }
            CATEGORY_DOWN -> {
                val next = catId + 1
                if (next < playlist?.categories?.size ?: 0) {
                    category = playlist?.categories?.get(next)
                    current = category?.channels!![0]
                }
            }
            CHANNEL_PREVIOUS -> {
                val previous = chId - 1
                if (previous > -1) {
                    current = category?.channels!![previous]
                }
                else {
                    switchChannel(CATEGORY_UP)
                    return
                }
            }
            CHANNEL_NEXT -> {
                val next = chId + 1
                if (next < category?.channels?.size ?: 0) {
                    current = category?.channels!![next]
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
            bindingControl.playerSettings.isEnabled = TrackSelectionDialog.willHaveContent(trackSelector)
            when (state) {
                Player.STATE_IDLE -> { }
                Player.STATE_BUFFERING -> hideCardMessage()
                Player.STATE_READY -> {
                    hideCardMessage()
                    val catId = playlist?.categories?.indexOf(category) as Int
                    val chId = category?.channels?.indexOf(current) as Int
                    preferences.watched = PlayData(catId, chId)
                }
                Player.STATE_ENDED -> {
                    showCardMessage(getString(R.string.stream_has_ended),
                        getString(R.string.text_auto_retry))
                    retryPlayback()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                showCardMessage(getString(R.string.stream_source_offline),
                    getString(R.string.text_auto_retry))
            } else {
                showCardMessage(getString(R.string.something_went_wrong),
                    getString(R.string.text_auto_retry))
            }
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
                    showCardMessage(getString(R.string.no_network),
                        bindingControl.textMessage.text.toString())
                }
                if (left <= 0) {
                    showCardMessage(bindingControl.textTitle.text.toString(),
                        getString(R.string.text_auto_retry_now))
                } else {
                    showCardMessage(bindingControl.textTitle.text.toString(),
                        String.format(getString(R.string.text_auto_retry_second), left))
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

    private fun hideCardMessage() {
        bindingControl.layoutMessage.visibility = View.INVISIBLE
    }

    private fun showCardMessage(title: String, message: String) {
        bindingControl.textTitle.text = title
        bindingControl.textMessage.text = message
        bindingControl.layoutMessage.visibility = View.VISIBLE
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
            KeyEvent.KEYCODE_DPAD_CENTER -> showTrackSelector()
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