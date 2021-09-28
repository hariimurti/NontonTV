package net.harimurti.tv

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaDrm
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import net.harimurti.tv.databinding.ActivityPlayerBinding
import net.harimurti.tv.databinding.CustomControlBinding
import net.harimurti.tv.dialog.TrackSelectionDialog
import net.harimurti.tv.extension.*
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Playlist
import java.util.*
import kotlin.math.ceil
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.source.MediaSource

class PlayerActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = UiMode().isTelevision()
    private val preferences = Preferences()
    private val network = Network()
    private var category: Category? = null
    private var current: Channel? = null
    private var player: SimpleExoPlayer? = null
    private lateinit var mediaSource: MediaSource
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private lateinit var bindingRoot: ActivityPlayerBinding
    private lateinit var bindingControl: CustomControlBinding
    private var handlerInfo: Handler? = null
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

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
        bindingRoot.playerView.apply {
            setOnTouchListener(object : OnSwipeTouchListener(this@apply) {
                override fun onSwipeDown() { switchChannel(CATEGORY_UP) }
                override fun onSwipeUp() { switchChannel(CATEGORY_DOWN) }
                override fun onSwipeLeft() { switchChannel(CHANNEL_NEXT) }
                override fun onSwipeRight() { switchChannel(CHANNEL_PREVIOUS) }
                override fun onTapDoubleLeft(click: Int) { doubleTapLeft(click) }
                override fun onTapDoubleRight(click: Int) { doubleTapRight(click) }
                override fun onTapDoubleFinish(click: Int,isLeft: Boolean) {
                    doubleTapFinish(click,isLeft)
                }
            })
            setControllerVisibilityListener {
                setChannelInformation (it == View.VISIBLE)
            }
        }
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.buttonExit.apply {
            visibility = if (isTelevision) View.GONE else View.VISIBLE
            setOnClickListener { finish() }
        }
        bindingControl.buttonPrevious.setOnClickListener { switchChannel(CHANNEL_PREVIOUS) }
        bindingControl.buttonRewind.setOnClickListener { player?.seekBack() }
        bindingControl.buttonForward.setOnClickListener { player?.seekForward() }
        bindingControl.buttonNext.setOnClickListener { switchChannel(CHANNEL_NEXT) }
        bindingControl.screenMode.setOnClickListener { showMenu(it) }
        bindingControl.trackSelection.setOnClickListener { showTrackSelector() }
        bindingControl.buttonLock.apply {
            visibility = if (isTelevision) View.GONE else View.VISIBLE
            setOnClickListener {
                if (!isLocked) {
                    (it as ImageButton).setImageResource(R.drawable.ic_lock)
                    lockControl(true)
                }
            }
            setOnLongClickListener {
                val resId = if (isLocked) R.drawable.ic_lock_open else R.drawable.ic_lock
                (it as ImageButton).setImageResource(resId)
                lockControl(!isLocked); true
            }
        }
        bindingControl.buttonVolume.setOnClickListener { showVolumeMenu() }
        isMute(bindingControl.buttonVolume)
    }

    @SuppressLint("SetTextI18n")
    private fun doubleTapLeft(clicks: Int) {
        if(player?.isCurrentWindowLive == false) {
            bindingRoot.seekBack.text = "- ${timeToString((clicks * 10).toDouble())}"
            bindingRoot.seekBack.alpha = 1f
            val seekAnimation = AlphaAnimation(0f, 1f)
            seekAnimation.fillAfter = true
            bindingRoot.seekBack.startAnimation(seekAnimation)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun doubleTapRight(clicks: Int) {
        if(player?.isCurrentWindowLive == false) {
            bindingRoot.seekForward.text = "+ ${timeToString((clicks * 10).toDouble())}"
            bindingRoot.seekForward.alpha = 1f
            val seekAnimation = AlphaAnimation(0f, 1f)
            seekAnimation.fillAfter = true
            bindingRoot.seekForward.startAnimation(seekAnimation)
        }
    }

    private fun doubleTapFinish(clicks: Int, isLeft:Boolean) {
        if(player?.isCurrentWindowLive == false) {
            val click = if (isLeft) clicks * -1 else clicks
            val seekAnimation = AlphaAnimation(1f, 0f)
            seekAnimation.duration = 1800
            seekAnimation.fillAfter = true
            if (isLeft) bindingRoot.seekBack.startAnimation(seekAnimation)
            else bindingRoot.seekForward.startAnimation(seekAnimation)
            seekTime((click * 10000).toLong())
        }
    }

    private fun seekTime(time: Long) {
        player?.seekTo(maxOf(minOf(player?.currentPosition?.plus(time)!!,
            player?.duration!!), 0))
    }

    private fun timeToString(time: Double): String {
        val second = time.toInt()
        val rsec = second % 60
        val minute = ceil((second - rsec) / 60.0).toInt()
        val rmin = minute % 60
        val hour = ceil((minute - rmin) / 60.0).toInt()
        return (if (hour > 0) forceTwoDigit(hour) + ":" else "") +
                (if (rmin >= 0 || hour >= 0) forceTwoDigit(rmin) + ":" else "") +
                forceTwoDigit(rsec)
    }

    private fun forceTwoDigit(inp: Int, length: Int = 2): String {
        val added: Int = length - inp.toString().length
        return if (added > 0) {
            "0".repeat(added) + inp.toString()
        } else {
            inp.toString()
        }
    }

    private fun setChannelInformation(visible: Boolean) {
        if (isLocked) return
        bindingRoot.layoutInfo.visibility =
            if (visible && !isPipMode) View.VISIBLE else View.INVISIBLE
        bindingControl.volumeLayout.visibility =
            if (visible || isPipMode) View.INVISIBLE else View.VISIBLE

        if (isPipMode) return
        if (visible == bindingRoot.playerView.isControllerVisible) return
        if (visible) bindingRoot.playerView.clearFocus()
        else return

        if (handlerInfo == null)
            handlerInfo = Handler(Looper.getMainLooper())

        handlerInfo?.removeCallbacksAndMessages(null)
        handlerInfo?.postDelayed({
                if (bindingRoot.playerView.isControllerVisible) return@postDelayed
                bindingRoot.layoutInfo.visibility = View.INVISIBLE
            },
            bindingRoot.playerView.controllerShowTimeoutMs.toLong()
        )
    }

    private fun lockControl(setLocked: Boolean) {
        isLocked = setLocked
        val visibility = if (setLocked) View.INVISIBLE else View.VISIBLE
        bindingRoot.layoutInfo.visibility = visibility
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
        bindingControl.spacerControl.visibility = visibility
        // override visibility if not seekable
        if (player?.isCurrentWindowSeekable == false) visibility = View.GONE
        bindingControl.buttonRewind.visibility = visibility
        bindingControl.buttonForward.visibility = visibility
    }

    private fun isDeviceSupportDrm(type: String): Boolean {
        val message = String.format(getString(R.string.device_not_support_drm), type.uppercase())
        if (MediaDrm.isCryptoSchemeSupported(type.toUUID())) return true
        AlertDialog.Builder(this).apply {
            setTitle(R.string.player_playback_error)
            setMessage(message)
            setCancelable(false)
            setPositiveButton(getString(R.string.btn_next_channel)) { _,_ -> switchChannel(CHANNEL_NEXT) }
            setNegativeButton(R.string.btn_close) { _,_ -> finish() }
            create()
            show()
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun playChannel() {
        // reset view
        switchLiveOrVideo(true)

        // set category & channel name
        bindingRoot.categoryName.text = category?.name?.trim()
        bindingRoot.channelName.text = current?.name?.trim()

        // create mediaitem
        val userAgent = current?.userAgent ?: "NontonTV/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})"
        val referer = current?.referer.toString()
        val streamUrl = current?.streamUrl?.decodeUrl()
        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))

        // create some factory
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
        if (current?.referer != null)
            httpDataSourceFactory.setDefaultRequestProperties(mapOf(Pair("referer", referer)))
        val dataSourceFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val drmLicense = Playlist.cached.drmLicenses.firstOrNull {
            current?.drmId?.equals(it.id) == true
        }
        
        // create mediaSource with/without drm factory
        if (drmLicense != null) {
            val uuid = drmLicense.type.toUUID()
            val drmCallback = if (uuid != C.CLEARKEY_UUID) HttpMediaDrmCallback(drmLicense.key, httpDataSourceFactory)
                    else LocalMediaDrmCallback(drmLicense.key.toClearKey())
            val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(uuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .setMultiSession(uuid != C.CLEARKEY_UUID)
                    .build(drmCallback)
            mediaSource = mediaSourceFactory.setDrmSessionManager(drmSessionManager)
                    .createMediaSource(mediaItem)

            if (!isDeviceSupportDrm(drmLicense.type)) return
        }
        else mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        // create trackselector
        trackSelector = DefaultTrackSelector(this).apply {
            parameters = ParametersBuilder(applicationContext).build()
        }

        // optimize prebuffer
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                32 * 1024,
                64 * 1024,
                1024,
                1024)
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true).build()

        // enable extension renderer
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        // set player builder
        val playerBuilder = SimpleExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
        if (preferences.optimizePrebuffer)
            playerBuilder.setLoadControl(loadControl)

        // create player & set listener
        player = playerBuilder.build()
        player?.addListener(PlayerListener())

        // set player view
        bindingRoot.playerView.player = player
        bindingRoot.playerView.resizeMode = preferences.resizeMode
        bindingRoot.playerView.requestFocus()

        // play mediasouce
        player?.playWhenReady = true
        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.playbackParameters = PlaybackParameters(preferences.speedMode)
        player?.volume = preferences.volume
    }

    private fun switchChannel(mode: Int): Boolean {
        if (isLocked) return true
        switchChannel(mode, false)
        bindingRoot.playerView.hideController()
        return true
    }

    private fun switchChannel(mode: Int, lastCh: Boolean) {
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
        errorCounter = 0
        player?.playWhenReady = false
        player?.release()
        playChannel()
    }

    private fun retryPlayback(force: Boolean) {
        if (force) {
            player?.playWhenReady = true
            player?.setMediaSource(mediaSource)
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

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) setChannelInformation(true)
        }

        override fun onPlayerError(error: PlaybackException) {
            // if error more than 5 times, then show message dialog
            if (errorCounter < 5 && network.isConnected()) {
                errorCounter++
                Toast.makeText(applicationContext, error.errorCodeName, Toast.LENGTH_SHORT).show()
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

            val problem = when {
                isVideoProblem && isAudioProblem -> "video & audio"
                isVideoProblem -> "video"
                else -> "audio"
            }
            val message = String.format(getString(R.string.error_unsupported), problem)
            if (isVideoProblem) showMessage(message, false)
            else if (isAudioProblem) Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showMessage(message: String, autoretry: Boolean) {
        val countdown = AsyncSleep()
        val waitInSecond = 30
        val btnRetryText = if (autoretry) String.format(getString(R.string.btn_retry_count), waitInSecond) else getString(R.string.btn_retry)
        val builder = AlertDialog.Builder(this).apply {
            setTitle(R.string.player_playback_error)
            setMessage(message)
            setCancelable(false)
            setNegativeButton(getString(R.string.btn_next_channel)) { di,_ ->
                switchChannel(CHANNEL_NEXT)
                di.dismiss()
            }
            setPositiveButton(btnRetryText) { di,_ ->
                retryPlayback(true)
                di.dismiss()
            }
            setNeutralButton(R.string.btn_close) { di,_ ->
                di.dismiss()
                finish()
            }
            setOnDismissListener { countdown.stop() }
            create()
        }
        val dialog = builder.show()

        if (!autoretry) return
        countdown.task(object : AsyncSleep.Task{
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

    private fun showMenu(view: View) {
        PopupMenu(this, view).apply {
            inflate(R.menu.setting_mode)
            setOnMenuItemClickListener { m: MenuItem ->
                when(m.itemId) {
                    R.id.speed_mode -> showSpeedMenu(view)
                    else -> showScreenMenu(view)
                }
                true
            }
            show()
        }
    }

    private fun showScreenMenu(view: View) {
        val timeout = bindingRoot.playerView.controllerShowTimeoutMs
        bindingRoot.playerView.controllerShowTimeoutMs = 0
        val popupMenu = PopupMenu(this, view).apply {
            inflate(R.menu.screen_resize_mode)
            setOnMenuItemClickListener { m: MenuItem ->
                val mode = when(m.itemId) {
                    R.id.mode_fit -> 0
                    R.id.mode_fixed_width -> 1
                    R.id.mode_fixed_height -> 2
                    R.id.mode_fill -> 3
                    R.id.mode_zoom -> 4
                    else -> 5
                }
                if (bindingRoot.playerView.resizeMode != mode && mode != 5) {
                    bindingRoot.playerView.resizeMode = mode
                    preferences.resizeMode = mode
                }
                if(m.itemId == R.id.mode_back) showMenu(view) else showScreenMenu(view)
                true
            }
            //set check preference
            when(preferences.resizeMode) {
                0 -> menu.findItem(R.id.mode_fit).isChecked = true
                1 -> menu.findItem(R.id.mode_fixed_width).isChecked = true
                2 -> menu.findItem(R.id.mode_fixed_height).isChecked = true
                3 -> menu.findItem(R.id.mode_fill).isChecked = true
                4 -> menu.findItem(R.id.mode_zoom).isChecked = true
            }
            setOnDismissListener {
                bindingRoot.playerView.controllerShowTimeoutMs = timeout
            }
        }
        //force show icon
        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon",Boolean::class.java)
                .invoke(menu,true)
        }catch (e:Exception){
            e.printStackTrace()
        } finally {
            popupMenu.show()
        }
    }

    private fun showSpeedMenu(view: View) {
        val timeout = bindingRoot.playerView.controllerShowTimeoutMs
        bindingRoot.playerView.controllerShowTimeoutMs = 0
        val popupMenu = PopupMenu(this, view).apply {
            inflate(R.menu.playback_speed_mode)
            setOnMenuItemClickListener { m: MenuItem ->
                val speed = when(m.itemId) {
                    R.id.speed_0_25 -> 0.25F
                    R.id.speed_0_50 -> 0.5F
                    R.id.speed_0_75 -> 0.75F
                    R.id.speed_1_00 -> 1F
                    R.id.speed_1_25 -> 1.25F
                    R.id.speed_1_50 -> 1.5F
                    R.id.speed_1_75 -> 1.75F
                    R.id.speed_2_00 -> 2F
                    else -> 0F
                }
                if(preferences.speedMode != speed && speed != 0F) {
                    player?.playbackParameters = PlaybackParameters(speed)
                    preferences.speedMode = speed
                    showSpeedMenu(view)
                }

                if(m.itemId == R.id.speed_back) showMenu(view)// else showSpeedMenu(view)
                true
            }
            //set check preference
            when(preferences.speedMode) {
                0.25F -> menu.findItem(R.id.speed_0_25).isChecked = true
                0.5F -> menu.findItem(R.id.speed_0_50).isChecked = true
                0.75F -> menu.findItem(R.id.speed_0_75).isChecked = true
                1F -> menu.findItem(R.id.speed_1_00).isChecked = true
                1.25F -> menu.findItem(R.id.speed_1_25).isChecked = true
                1.5F -> menu.findItem(R.id.speed_1_50).isChecked = true
                1.75F -> menu.findItem(R.id.speed_1_75).isChecked = true
                2F -> menu.findItem(R.id.speed_2_00).isChecked = true
            }
            setOnDismissListener {
                bindingRoot.playerView.controllerShowTimeoutMs = timeout
            }
        }
        //force show icon
        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon",Boolean::class.java)
                .invoke(menu,true)
        }catch (e:Exception){
            e.printStackTrace()
        } finally {
            popupMenu.show()
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showVolumeMenu() {
        bindingControl.volumeLayout.visibility = View.VISIBLE
        bindingControl.volumeSeek.apply {
            progress = (preferences.volume * 100).toInt()
            setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    preferences.volume = i.toFloat() / 100
                    player?.volume = preferences.volume
                    isMute(bindingControl.buttonVolume)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
    }

    private fun isMute(v: ImageButton){
        when (preferences.volume) {
            0F -> v.setImageResource(R.drawable.ic_volume_off)
            else -> v.setImageResource(R.drawable.ic_volume_on)
        }
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
        isPipMode = pip
        setChannelInformation(!pip)
        bindingRoot.playerView.useController = !pip
        player?.playWhenReady = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.setFullScreenFlags()
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
            KeyEvent.KEYCODE_MEDIA_PLAY -> { player?.play(); return true; }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { player?.pause(); return true; }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player?.isPlaying == false) player?.play() else player?.pause()
                return true
            }
        }
        if (player?.isCurrentWindowLive == false) {
            when(keyCode) {
                KeyEvent.KEYCODE_MEDIA_REWIND -> { player?.seekBack(); return true }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { player?.seekForward(); return true }
            }
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
        if (isLocked) return
        if (isTelevision || doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finish(); return
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
