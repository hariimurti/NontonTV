package net.harimurti.tv

import android.annotation.SuppressLint
import android.content.*
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import net.harimurti.tv.adapter.CategoryAdapter
import net.harimurti.tv.databinding.ActivityMainBinding
import net.harimurti.tv.dialog.SearchDialog
import net.harimurti.tv.dialog.SettingDialog
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.*

open class MainActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = UiMode().isTelevision()
    private val preferences = Preferences()
    private val helper = PlaylistHelper()
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CategoryAdapter

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.getStringExtra(MAIN_CALLBACK)){
                UPDATE_PLAYLIST -> updatePlaylist()
                INSERT_FAVORITE -> adapter.insertOrUpdateFavorite()
                REMOVE_FAVORITE -> adapter.removeFavorite()
            }
        }
    }

    companion object {
        const val MAIN_CALLBACK = "MAIN_CALLBACK"
        const val UPDATE_PLAYLIST = "UPDATE_PLAYLIST"
        const val INSERT_FAVORITE = "REFRESH_FAVORITE"
        const val REMOVE_FAVORITE = "REMOVE_FAVORITE"
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.rvCategory.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.swipeContainer.setOnRefreshListener {
            binding.swipeContainer.isRefreshing = false
            updatePlaylist()
        }

        if (isTelevision) {
            setTheme(R.style.AppThemeTv)
        }
        setContentView(binding.root)

        //search button
        binding.buttonSearch.setOnClickListener{
            openSearch()
        }
        //setting button
        binding.buttonSettings.setOnClickListener{
            openSettings()
        }

        // local broadcast receiver to update playlist
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(MAIN_CALLBACK))

        // set playlist
        if (!Playlist.cached.isCategoriesEmpty()) setPlaylistToAdapter(Playlist.cached)
        else showAlertPlaylistError(getString(R.string.null_playlist))

    }

    private fun setLoadingPlaylist(show: Boolean) {
        /* i don't why hideShimmer() leaves drawable visible */
        if (show) {
            binding.loading.startShimmer()
            binding.loading.visibility = View.VISIBLE
        }
        else {
            binding.loading.stopShimmer()
            binding.loading.visibility = View.GONE
        }
    }

    private fun setPlaylistToAdapter(playlistSet: Playlist) {
        // sort category by name
        if(preferences.sortCategory) playlistSet.sortCategories()
        // sort channels by name
        if(preferences.sortChannel) playlistSet.sortChannels()
        // remove channels with empty streamurl
        playlistSet.trimChannelWithEmptyStreamUrl()

        // favorites part
        val fav = helper.readFavorites()
            .trimNotExistFrom(playlistSet)
        if (preferences.sortFavorite) fav.sort()
        if (fav?.channels?.isNotEmpty() == true)
            playlistSet.insertFavorite(fav.channels)
        else playlistSet.removeFavorite()

        // set new playlist
        adapter = CategoryAdapter(playlistSet.categories)
        binding.catAdapter = adapter

        // write cache
        Playlist.cached = playlistSet
        helper.writeCache(playlistSet)

        // hide loading
        setLoadingPlaylist(false)
        Toast.makeText(applicationContext, R.string.playlist_updated, Toast.LENGTH_SHORT).show()

        // launch player if playlastwatched is true
        if (preferences.playLastWatched && PlayerActivity.isFirst) {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra(PlayData.VALUE, preferences.watched)
            this.startActivity(intent)
        }
    }

    private fun updatePlaylist() {
        // show loading
        setLoadingPlaylist(true)

        // clearing adapter
        binding.catAdapter?.clear()
        val playlistSet = Playlist()

        PlaylistHelper().task(preferences.sources,
            object: PlaylistHelper.TaskResponse {
                override fun onError(error: Exception, source: Source) {
                    val message = if (error.message.isNullOrBlank()) "Problem with $source" else error.message
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
                override fun onResponse(playlist: Playlist?) {
                    // merge into playlistset
                    if (playlist != null) playlistSet.mergeWith(playlist)
                    else Toast.makeText(applicationContext, getString(R.string.playlist_cant_be_parsed), Toast.LENGTH_SHORT).show()
                }
                override fun onFinish() {
                    if (playlistSet.isCategoriesEmpty()) showAlertPlaylistError(getString(R.string.null_playlist))
                    else setPlaylistToAdapter(playlistSet)
                }
        }).getResponse()
    }

    private fun showAlertPlaylistError(message: String?) {
        val alert = AlertDialog.Builder(this).apply {
            setTitle(R.string.alert_title_playlist_error)
            setMessage(message)
            setCancelable(false)
            setNeutralButton(R.string.settings) { _,_ -> openSettings() }
            setPositiveButton(R.string.dialog_retry) { _,_ -> updatePlaylist() }
        }
        val cache = helper.readCache()
        if (cache != null) {
            alert.setNegativeButton(R.string.dialog_cached) { _,_ -> setPlaylistToAdapter(cache) }
        }
        alert.create().show()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_MENU -> openSettings()
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
        Toast.makeText(this, getString(R.string.press_back_twice_exit_app), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun openSettings(){
        SettingDialog().show(supportFragmentManager.beginTransaction(),null)
    }

    private fun openSearch() {
        SearchDialog().show(supportFragmentManager.beginTransaction(),null)
    }
}