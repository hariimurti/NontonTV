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
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import net.harimurti.tv.databinding.ActivityPlayerBinding
import net.harimurti.tv.databinding.CustomControlBinding
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.Channel
import java.util.*

class PlayerActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = false
    private var skipRetry = false
    private lateinit var preferences: Preferences
    private lateinit var channelUrl: String
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var controlBinding: CustomControlBinding

    companion object {
        var isFirst = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFirst = false
        isTelevision = UiMode(this).isTelevision()
        preferences = Preferences(this)

        // define some view
        controlBinding = CustomControlBinding.bind(binding.root.findViewById(R.id.custom_control))
        controlBinding.playerSettings.setOnClickListener { showTrackSelector() }
        controlBinding.channelName.text = intent.getStringExtra(Channel.NAME)

        // get channel_url
        channelUrl = intent.getStringExtra(Channel.STREAMURL).toString()
        if (channelUrl.isEmpty()) {
            Toast.makeText(this, R.string.player_no_channel_url, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // define mediasource
        val drmLicense = intent.getStringExtra(Channel.DRMURL)
        mediaItem = if (drmLicense?.isNotEmpty() == true) {
            MediaItem.Builder()
                .setUri(Uri.parse(channelUrl))
                .setDrmUuid(C.WIDEVINE_UUID)
                .setDrmLicenseUri(drmLicense)
                .setDrmMultiSession(true)
                .build()
        } else {
            MediaItem.fromUri(Uri.parse(channelUrl))
        }

        // define User-Agent
        val userAgents = listOf(*resources.getStringArray(R.array.user_agent))
        var userAgent = userAgents.firstOrNull {
            channelUrl.contains(it.substring(0, it.indexOf("/")).lowercase(Locale.getDefault()))
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
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                showLayoutMessage(View.GONE, false)
                preferences.lastWatched = channelUrl
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
            if (trackGroups !== lastSeenTrackGroupArray) {
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_MENU) {
            showTrackSelector()
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
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
}