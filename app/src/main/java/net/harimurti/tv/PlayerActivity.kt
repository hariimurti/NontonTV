package net.harimurti.tv

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.*
import android.content.res.Configuration
import android.media.MediaDrm
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import net.harimurti.tv.databinding.ActivityPlayerBinding
import net.harimurti.tv.databinding.CustomControlBinding
import net.harimurti.tv.dialog.TrackSelectionDialog
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Playlist
import java.net.URLDecoder
import java.util.*

class PlayerActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = UiMode().isTelevision()
    private val preferences = Preferences()
    private val network = Network()
    private var category: Category? = null
    private var current: Channel? = null
    private var player: SimpleExoPlayer? = null
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
            }
        }
    }

    companion object {
        var isFirst = true
        var isPipMode = false
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
        bindingControl = CustomControlBinding.bind(bindingRoot.root.findViewById(R.id.custom_control))
        setContentView(bindingRoot.root)

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
        bindingRoot.playerView.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeDown() { switchChannel(CATEGORY_UP) }
            override fun onSwipeUp() { switchChannel(CATEGORY_DOWN) }
            override fun onSwipeLeft() { switchChannel(CHANNEL_NEXT) }
            override fun onSwipeRight() { switchChannel(CHANNEL_PREVIOUS) }
        })
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.buttonExit.setOnClickListener { finish() }
        bindingControl.buttonPrevious.setOnClickListener { switchChannel(CHANNEL_PREVIOUS) }
        bindingControl.buttonRewind.setOnClickListener { player?.seekBack() }
        bindingControl.buttonForward.setOnClickListener { player?.seekForward() }
        bindingControl.buttonNext.setOnClickListener { switchChannel(CHANNEL_NEXT) }
        bindingControl.screenMode.setOnClickListener { showScreenMenu(it) }
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.buttonLock.visibility = if (isTelevision) View.GONE else View.VISIBLE
        bindingControl.buttonLock.setOnClickListener {
            if (!isLocked) {
                (it as ImageButton).setImageResource(R.drawable.ic_lock)
                lockControl(true)
            }
        }
        bindingControl.buttonLock.setOnLongClickListener {
            (it as ImageButton).setImageResource(
                if (isLocked) R.drawable.ic_lock_open else R.drawable.ic_lock
            )
            lockControl(!isLocked)
            true
        }
    }

    private fun lockControl(setLocked: Boolean) {
        this.isLocked = setLocked
        val visibility = if (setLocked) View.INVISIBLE else View.VISIBLE
        bindingControl.categoryName.visibility = visibility
        bindingControl.channelName.visibility = visibility
        bindingControl.buttonExit.visibility = visibility
        bindingControl.layoutControl.visibility = visibility
        bindingControl.screenMode.visibility = visibility
        bindingControl.trackSelection.visibility = visibility
        switchLiveOrVideo()
    }

    private fun switchLiveOrVideo() { switchLiveOrVideo(false) }
    private fun switchLiveOrVideo(reset: Boolean) {
        var visibility = when {
            reset -> View.GONE
            isLocked -> View.INVISIBLE
            player?.isCurrentWindowLive == true -> View.GONE
            else -> View.VISIBLE
        }
        bindingControl.layoutSeekbar.visibility = visibility
        // override visibility if not seekable
        if (player?.isCurrentWindowSeekable == false) visibility = View.GONE
        bindingControl.buttonRewind.visibility = visibility
        bindingControl.buttonForward.visibility = visibility
    }

    private fun isDrmWidevineSupported(): Boolean {
        if (MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID)) return true
        AlertDialog.Builder(this).apply {
            setTitle(R.string.player_playback_error)
            setMessage(R.string.device_not_support_widevine)
            setCancelable(false)
            setPositiveButton(getString(R.string.btn_next_channel)) { _,_ -> switchChannel(CHANNEL_NEXT) }
            setNegativeButton(R.string.btn_close) { _,_ -> finish() }
            create()
            show()
        }
        return false
    }

    private fun playChannel() {
        // reset view
        switchLiveOrVideo(true)

        // set category & channel name
        bindingControl.categoryName.text = category?.name
        bindingControl.channelName.text = current?.name

        // split streamurl with referer, user-agent
        var streamUrl = URLDecoder.decode(current?.streamUrl, "utf-8")
        var userAgent = streamUrl.findPattern(".*user-agent=(.+?)(\\|.*)?")
        val referer = streamUrl.findPattern(".*referer=(.+?)(\\|.*)?")

        // clean streamurl
        streamUrl = streamUrl.findPattern("(.+?)(\\|.*)?") ?: streamUrl

        // if null set User-Agent with existing resources
        if (userAgent == null) {
            val userAgents = listOf(*resources.getStringArray(R.array.user_agent))
            userAgent = userAgents.firstOrNull {
                current?.streamUrl?.contains(
                    it.substring(0, it.indexOf("/")).lowercase(Locale.getDefault())
                ) == true
            }
            if (userAgent.isNullOrEmpty()) {
                userAgent = userAgents[Random().nextInt(userAgents.size)]
            }
        }

        // define mediaitem
        val drmLicense = Playlist.cached.drmLicenses.firstOrNull {
            current?.drmName?.equals(it.name) == true
        }?.url
        mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
        if (drmLicense?.isNotEmpty() == true) {
            mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setDrmUuid(C.WIDEVINE_UUID)
                .setDrmLicenseUri(drmLicense)
                .setDrmMultiSession(true)
                .build()
            if (!isDrmWidevineSupported()) return
        }

        // create factory
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
        if (referer != null) httpDataSourceFactory.setDefaultRequestProperties(mapOf(Pair("referer", referer)))
        val dataSourceFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // create trackselector
        trackSelector = DefaultTrackSelector(this).apply {
            parameters = ParametersBuilder(applicationContext).build()
        }

        // create player & set listener
        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
        player?.addListener(PlayerListener())

        // set player view
        bindingRoot.playerView.player = player
        bindingRoot.playerView.resizeMode = preferences.resizeMode
        bindingRoot.playerView.requestFocus()

        // play mediasouce
        player?.playWhenReady = true
        player?.setMediaItem(mediaItem)
        player?.prepare()
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
        player?.playWhenReady = false
        player?.release()
        playChannel()
    }

    private fun retryPlayback(force: Boolean) {
        if (force) {
            player?.playWhenReady = true
            player?.setMediaItem(mediaItem)
            player?.prepare()
            return
        }

        AsyncSleep().task(object : AsyncSleep.Task {
            override fun onFinish() {
                retryPlayback(true)
            }
        }).start(1)
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val trackHaveContent = TrackSelectionDialog.willHaveContent(trackSelector)
            bindingControl.trackSelection.visibility =
                if (trackHaveContent) View.VISIBLE else View.GONE
            when (state) {
                Player.STATE_READY -> {
                    errorCounter = 0
                    val catId = Playlist.cached.categories.indexOf(category)
                    val chId = category?.channels?.indexOf(current) as Int
                    preferences.watched = PlayData(catId, chId)
                    switchLiveOrVideo()
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
                val message = String.format(getString(R.string.error_unsupported), problem)
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
        AsyncSleep().task(object : AsyncSleep.Task{
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

    private fun showScreenMenu(view: View) {
        val menu = PopupMenu(this, view).apply {
            inflate(R.menu.screen_resize_mode)
            setOnMenuItemClickListener { m: MenuItem ->
                val mode = when(m.itemId) {
                    R.id.mode_fixed_width -> 1
                    R.id.mode_fixed_height -> 2
                    R.id.mode_fill -> 3
                    R.id.mode_zoom -> 4
                    else -> 0
                }
                if (bindingRoot.playerView.resizeMode != mode) {
                    bindingRoot.playerView.resizeMode = mode
                    preferences.resizeMode = mode
                }
                true
            }
            show()
        }
        val timeout = bindingRoot.playerView.controllerShowTimeoutMs.toLong() - 500
        Handler(Looper.getMainLooper()).postDelayed({ menu.dismiss() }, timeout)
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    @Suppress("DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == false) return
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
        player?.playWhenReady = true
        isPipMode = pip
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
        if (!bindingRoot.playerView.isControllerVisible && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            bindingRoot.playerView.showController()
            return true
        }
        if (isLocked) return true
        when(keyCode) {
            KeyEvent.KEYCODE_MENU -> return showTrackSelector()
            KeyEvent.KEYCODE_PAGE_UP -> return switchChannel(CATEGORY_UP)
            KeyEvent.KEYCODE_PAGE_DOWN -> return switchChannel(CATEGORY_DOWN)
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> return switchChannel(CHANNEL_PREVIOUS)
            KeyEvent.KEYCODE_MEDIA_NEXT -> return switchChannel(CHANNEL_NEXT)
        }
        if (bindingRoot.playerView.isControllerVisible) return super.onKeyUp(keyCode, event)
        if (!preferences.reverseNavigation) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> return switchChannel(CATEGORY_UP)
                KeyEvent.KEYCODE_DPAD_DOWN -> return switchChannel(CATEGORY_DOWN)
                KeyEvent.KEYCODE_DPAD_LEFT -> return switchChannel(CHANNEL_PREVIOUS)
                KeyEvent.KEYCODE_DPAD_RIGHT -> return switchChannel(CHANNEL_NEXT)
            }
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> return switchChannel(CHANNEL_NEXT)
                KeyEvent.KEYCODE_DPAD_DOWN -> return switchChannel(CHANNEL_PREVIOUS)
                KeyEvent.KEYCODE_DPAD_LEFT -> return switchChannel(CATEGORY_UP)
                KeyEvent.KEYCODE_DPAD_RIGHT -> return switchChannel(CATEGORY_DOWN)
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
        player?.release()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}
