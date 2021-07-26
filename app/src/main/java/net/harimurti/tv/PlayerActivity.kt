package net.harimurti.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
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
    private lateinit var preferences: Preferences
    private lateinit var network: Network
    private var playlist: Playlist? = null
    private var category: Category? = null
    private var current: Channel? = null
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var bindingRoot: ActivityPlayerBinding
    private lateinit var bindingControl: CustomControlBinding
    private lateinit var dialogMessage: PlayerDialogMessage
    private var errorCounter = 0

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.getStringExtra(PLAYER_CALLBACK)) {
                RETRY_PLAYBACK -> retryPlayback(true)
                CLOSE_PLAYER -> finish()
            }
        }
    }

    companion object {
        var isFirst = true
        const val PLAYER_CALLBACK = "PLAYER_CALLBACK"
        const val RETRY_PLAYBACK = "RETRY_PLAYBACK"
        const val CLOSE_PLAYER = "CLOSE_PLAYER"
        private const val CHANNEL_NEXT = 0
        private const val CHANNEL_PREVIOUS = 1
        private const val CATEGORY_UP = 2
        private const val CATEGORY_DOWN = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingRoot = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(bindingRoot.root)

        isTelevision = UiMode(this).isTelevision()
        preferences = Preferences(this)
        network = Network(this)

        // get playlist
        playlist = if (preferences.playLastWatched && isFirst) PlaylistHelper(this).readCache() else Playlist.loaded
        isFirst = false

        // verify playlist
        if (playlist == null) {
            Toast.makeText(this, R.string.player_no_playlist, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        // set channels
        val parcel: PlayData? = intent.getParcelableExtra(PlayData.VALUE)
        category = parcel.let { playlist?.categories?.get(it?.catId as Int) }
        current = parcel.let { category?.channels?.get(it?.chId as Int) }

        // define some view
        dialogMessage = PlayerDialogMessage(this)
        bindingControl = CustomControlBinding.bind(bindingRoot.root.findViewById(R.id.custom_control))
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.screenMode.setOnClickListener {
            var ratio = bindingRoot.playerView.resizeMode + 1
            if (ratio > 4) ratio = 0

            bindingRoot.playerView.resizeMode = ratio
            preferences.resizeMode = ratio

            val mode = when (ratio) {
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> getString(R.string.mode_fixed_width)
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> getString(R.string.mode_fixed_height)
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> getString(R.string.mode_fill)
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> getString(R.string.mode_zoom)
                else -> getString(R.string.mode_fit)
            }
            Toast.makeText(applicationContext, String.format(getString(R.string.toast_screen_mode), mode), Toast.LENGTH_SHORT).show()
        }

        // verify stream_url
        if (current == null) {
            Toast.makeText(this, R.string.player_no_channel, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }
        else {
            playChannel()
        }

        // local broadcast receiver to update playlist
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(PLAYER_CALLBACK))
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
        bindingRoot.playerView.resizeMode = preferences.resizeMode
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

    fun retryPlayback(force: Boolean) {
        if (force) {
            player.setMediaItem(mediaItem)
            player.prepare()
            return
        }

        AsyncSleep(this).task(object : AsyncSleep.Task {
            override fun onFinish() {
                retryPlayback(true)
            }
        }).start(1)
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            bindingControl.trackSelection.isEnabled =
                TrackSelectionDialog.willHaveContent(trackSelector)
            when (state) {
                Player.STATE_IDLE -> { }
                Player.STATE_BUFFERING -> dialogMessage.dismiss()
                Player.STATE_READY -> {
                    errorCounter = 0
                    dialogMessage.dismiss()
                    val catId = playlist?.categories?.indexOf(category) as Int
                    val chId = category?.channels?.indexOf(current) as Int
                    preferences.watched = PlayData(catId, chId)
                }
                Player.STATE_ENDED -> retryPlayback(true)
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            val message = if (error.type == ExoPlaybackException.TYPE_SOURCE) getString(R.string.stream_source_offline)
            else if (!network.isConnected()) getString(R.string.no_network) else getString(R.string.something_went_wrong)

            // if error more than 5 times, then show message dialog
            if (errorCounter < 5 && network.isConnected()) {
                errorCounter++
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                retryPlayback(false)
            }
            else {
                dialogMessage.show(message)
            }
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

    private fun showTrackSelector() {
        TrackSelectionDialog.createForTrackSelector(trackSelector) { }
            .show(supportFragmentManager, null)
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

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}