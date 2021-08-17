package net.harimurti.tv

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.*
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
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
import net.harimurti.tv.dialog.TrackSelectionDialog
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
    private var category: Category? = null
    private var current: Channel? = null
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var bindingRoot: ActivityPlayerBinding
    private lateinit var bindingControl: CustomControlBinding
    private var errorCounter = 0
    private var isLocked = false

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.getStringExtra(PLAYER_CALLBACK)) {
                RETRY_PLAYBACK -> retryPlayback(true)
                CLOSE_PLAYER -> finish()
                CHANGE_SCREEN_MODE -> changeScreenMode(intent.getIntExtra(SCREEN_MODE, 0))
            }
        }
    }

    companion object {
        var isFirst = true
        const val PLAYER_CALLBACK = "PLAYER_CALLBACK"
        const val RETRY_PLAYBACK = "RETRY_PLAYBACK"
        const val CLOSE_PLAYER = "CLOSE_PLAYER"
        const val CHANGE_SCREEN_MODE = "CHANGE_SCREEN_MODE"
        const val SCREEN_MODE = "SCREEN_MODE"
        private const val CHANNEL_NEXT = 0
        private const val CHANNEL_PREVIOUS = 1
        private const val CATEGORY_UP = 2
        private const val CATEGORY_DOWN = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingRoot = ActivityPlayerBinding.inflate(layoutInflater)
        bindingControl = CustomControlBinding.bind(bindingRoot.root.findViewById(R.id.custom_control))
        setContentView(bindingRoot.root)

        isTelevision = UiMode(this).isTelevision()
        preferences = Preferences(this)
        network = Network(this)

        // set this is not first time
        isFirst = false

        // verify playlist
        if (Playlist.cached.isCategoriesEmpty()) {
            Log.e("PLAYER", getString(R.string.player_no_playlist))
            Toast.makeText(this, R.string.player_no_playlist, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        // get categories & channel to play
        try {
            val parcel: PlayData? = intent.getParcelableExtra(PlayData.VALUE)
            category = parcel.let { Playlist.cached.categories[it?.catId as Int] }
            current = parcel.let { category?.channels?.get(it?.chId as Int) }
        }
        catch (e: Exception) {
            Log.e("PLAYER", getString(R.string.player_playdata_error))
            Toast.makeText(this, R.string.player_playdata_error, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        // verify
        if (category == null || current == null) {
            Log.e("PLAYER", getString(R.string.player_no_channel))
            Toast.makeText(this, R.string.player_no_channel, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        // set listener
        bindingListener()

        // play the channel
        playChannel()

        // local broadcast receiver to update playlist
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(PLAYER_CALLBACK))
    }

    private fun bindingListener() {
        bindingRoot.playerView.setOnTouchListener(object : OnSwipeTouchListener(baseContext) {
            override fun onSwipeDown() { switchChannel(CATEGORY_UP) }
            override fun onSwipeUp() { switchChannel(CATEGORY_DOWN) }
            override fun onSwipeLeft() { switchChannel(CHANNEL_NEXT) }
            override fun onSwipeRight() { switchChannel(CHANNEL_PREVIOUS) }
        })
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.screenMode.setOnClickListener {
            var mode = bindingRoot.playerView.resizeMode + 1
            if (mode > 4) mode = 0
            changeScreenMode(mode)
        }
        bindingControl.buttonBack.setOnClickListener { finish() }
        bindingControl.buttonLock.visibility = if (isTelevision) View.GONE else View.VISIBLE
        bindingControl.buttonLock.setOnClickListener {
            if (!isLocked) {
                (it as ImageButton).setImageResource(R.drawable.ic_lock)
                setControlVisibility(true)
            }
        }
        bindingControl.buttonLock.setOnLongClickListener {
            if (isLocked) {
                (it as ImageButton).setImageResource(R.drawable.ic_lock_open)
                setControlVisibility(false)
            }
            true
        }
    }

    private fun setControlVisibility(locked: Boolean) {
        val visibility = if (locked) View.INVISIBLE else View.VISIBLE
        bindingControl.categoryName.visibility = visibility
        bindingControl.channelName.visibility = visibility
        bindingControl.buttonBack.visibility = visibility
        bindingControl.screenMode.visibility = visibility
        bindingControl.trackSelection.visibility = visibility
        isLocked = locked
    }

    private fun playChannel() {
        // set category & channel name
        bindingControl.categoryName.text = category?.name
        bindingControl.channelName.text = current?.name

        // define mediaitem
        val drmLicense = Playlist.cached.drmLicenses.firstOrNull {
            current?.drmName?.equals(it.name) == true
        }?.url
        mediaItem = if (drmLicense?.isNotEmpty() == true) {
            MediaItem.Builder()
                .setUri(Uri.parse(current?.streamUrl))
                .setDrmUuid(C.WIDEVINE_UUID)
                .setDrmLicenseUri(drmLicense)
                .setDrmMultiSession(true)
                .build()
        } else {
            MediaItem.fromUri(Uri.parse(current?.streamUrl))
        }

        // define User-Agent
        val userAgents = listOf(*resources.getStringArray(R.array.user_agent))
        var userAgent = userAgents.firstOrNull {
            current?.streamUrl?.contains(it.substring(0, it.indexOf("/")).lowercase(Locale.getDefault())) == true
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

    private fun switchChannel(mode: Int): Boolean {
        if (isLocked) return true
        switchChannel(mode, false)
        bindingRoot.playerView.hideController()
        return true
    }

    private fun switchChannel(mode: Int, lastCh: Boolean) {
        bindingRoot.playerView.showController()
        val catId = Playlist.cached.categories.indexOf(category)
        val chId = category?.channels?.indexOf(current) as Int
        when(mode) {
            CATEGORY_UP -> {
                val previous = catId - 1
                if (previous > -1) {
                    category = Playlist.cached.categories[previous]
                    current = if (lastCh) category?.channels?.get(category?.channels?.size?.minus(1) ?: 0)
                    else category?.channels?.get(0)
                }
                else {
                    Toast.makeText(this, R.string.top_category, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            CATEGORY_DOWN -> {
                val next = catId + 1
                if (next < Playlist.cached.categories.size) {
                    category = Playlist.cached.categories[next]
                    current = category?.channels?.get(0)
                }
                else {
                    Toast.makeText(this, R.string.bottom_category, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            CHANNEL_PREVIOUS -> {
                val previous = chId - 1
                if (previous > -1) {
                    current = category?.channels?.get(previous)
                }
                else {
                    switchChannel(CATEGORY_UP, true)
                    return
                }
            }
            CHANNEL_NEXT -> {
                val next = chId + 1
                if (next < category?.channels?.size ?: 0) {
                    current = category?.channels?.get(next)
                }
                else {
                    switchChannel(CATEGORY_DOWN)
                    return
                }
            }
        }

        // reset player & play
        player.playWhenReady = false
        player.release()
        playChannel()
    }

    private fun retryPlayback(force: Boolean) {
        if (force) {
            player.playWhenReady = true
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
                Player.STATE_READY -> {
                    errorCounter = 0
                    val catId = Playlist.cached.categories.indexOf(category)
                    val chId = category?.channels?.indexOf(current) as Int
                    preferences.watched = PlayData(catId, chId)
                }
                Player.STATE_ENDED -> retryPlayback(true)
                else -> { }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // if error more than 5 times, then show message dialog
            if (errorCounter < 5 && network.isConnected()) {
                errorCounter++
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
                retryPlayback(false)
            }
            else {
                showMessage(
                    String.format(getString(R.string.player_error_message),
                        error.errorCode, error.errorCodeName, error.message), true
                )
            }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            if (trackGroups == lastSeenTrackGroupArray) return
            else lastSeenTrackGroupArray = trackGroups

            val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
            val isVideoProblem = mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
            val isAudioProblem = mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
            if (isVideoProblem || isAudioProblem) {
                val problem = when {
                    isVideoProblem && isAudioProblem -> "video & audio"
                    isVideoProblem -> "video"
                    else -> "audio"
                }
                val codec = CodecInfo.getDecoder(problem)
                val message = String.format(getString(R.string.error_unsupported), problem, codec)
                showMessage(message, false)
            }
        }
    }

    private fun showMessage(message: String, autoretry: Boolean) {
        val waitInSecond = 30
        val btnRetryText = if (autoretry) String.format(getString(R.string.btn_retry_count), waitInSecond) else getString(R.string.btn_retry)
        val builder = AlertDialog.Builder(this).apply {
            setTitle(R.string.player_playback_error)
            setMessage(message)
            setCancelable(false)
            setNegativeButton(getString(R.string.btn_next_channel)) { _,_ -> switchChannel(CHANNEL_NEXT) }
            setPositiveButton(btnRetryText) { _,_ -> retryPlayback(true) }
            setNeutralButton(R.string.btn_close) { _,_ -> finish() }
            create()
        }
        val dialog = builder.show()

        if (!autoretry) return
        AsyncSleep(this).task(object : AsyncSleep.Task{
            override fun onCountDown(count: Int) {
                val text = if (count <= 0) getString(R.string.btn_retry)
                else String.format(getString(R.string.btn_retry_count), count)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).text = text
            }

            override fun onFinish() {
                dialog.dismiss()
                retryPlayback(true)
            }
        }).start(waitInSecond)
    }

    private fun showTrackSelector(): Boolean {
        TrackSelectionDialog.createForTrackSelector(trackSelector) { }
            .show(supportFragmentManager, null)
        return true
    }

    private fun changeScreenMode(mode: Int) {
        if (bindingRoot.playerView.resizeMode == mode) return

        bindingRoot.playerView.resizeMode = mode
        preferences.resizeMode = mode

        val text = when (mode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> getString(R.string.mode_fixed_width)
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> getString(R.string.mode_fixed_height)
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> getString(R.string.mode_fill)
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> getString(R.string.mode_zoom)
            else -> getString(R.string.mode_fit)
        }
        Toast.makeText(applicationContext, String.format(getString(R.string.toast_screen_mode), text), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        player.playWhenReady = false
    }

    @Suppress("DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isTelevision) return
        if (!player.isPlaying) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            }
            else {
                enterPictureInPictureMode()
            }
        }
    }

    override fun onPictureInPictureModeChanged(pip: Boolean, config: Configuration) {
        super.onPictureInPictureModeChanged(pip, config)
        bindingRoot.playerView.useController = !pip
        player.playWhenReady = true
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
            KeyEvent.KEYCODE_MENU -> return showTrackSelector()
            KeyEvent.KEYCODE_PAGE_UP -> return switchChannel(CATEGORY_UP)
            KeyEvent.KEYCODE_PAGE_DOWN -> return switchChannel(CATEGORY_DOWN)
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> return switchChannel(CHANNEL_PREVIOUS)
            KeyEvent.KEYCODE_MEDIA_NEXT -> return switchChannel(CHANNEL_NEXT)
        }
        if (!bindingRoot.playerView.isControllerVisible) {
            when(keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> return switchChannel(CATEGORY_UP)
                KeyEvent.KEYCODE_DPAD_DOWN -> return switchChannel(CATEGORY_DOWN)
                KeyEvent.KEYCODE_DPAD_LEFT -> return switchChannel(CHANNEL_PREVIOUS)
                KeyEvent.KEYCODE_DPAD_RIGHT -> return switchChannel(CHANNEL_NEXT)
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    bindingRoot.playerView.showController()
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
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
        player.release()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}
