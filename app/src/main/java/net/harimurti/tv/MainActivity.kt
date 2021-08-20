package net.harimurti.tv

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import net.harimurti.tv.adapter.CategoryAdapter
import net.harimurti.tv.databinding.ActivityMainBinding
import net.harimurti.tv.dialog.SearchDialog
import net.harimurti.tv.dialog.SettingsDialog
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.*
import java.util.*

open class MainActivity : AppCompatActivity() {
    private var doubleBackToExitPressedOnce = false
    private var isTelevision = false
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: Preferences
    private lateinit var helper: PlaylistHelper
    private lateinit var request: RequestQueue

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.getStringExtra(MAIN_CALLBACK)){
                UPDATE_PLAYLIST -> updatePlaylist()
                OPEN_SETTINGS -> openSettings()
            }
        }
    }

    companion object {
        const val MAIN_CALLBACK = "MAIN_CALLBACK"
        const val UPDATE_PLAYLIST = "UPDATE_PLAYLIST"
        const val OPEN_SETTINGS = "OPEN_SETTINGS"
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

        preferences = Preferences(this)
        helper = PlaylistHelper(this)
        request = VolleyRequestQueue.create(this)

        isTelevision = UiMode(this).isTelevision()
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

        // set adapter with existed playlist
        if (savedInstanceState != null) {
            if (Playlist.cached.isCategoriesEmpty()) updatePlaylist()
            else setPlaylistToAdapter(Playlist.cached)
            return
        }

        // ask all premissions need
        askPermissions()

        // playlist update
        updatePlaylist()

        // check new release
        if (!preferences.isCheckedReleaseUpdate) {
            checkNewRelease()
        }

        // get contributors
        if (preferences.lastVersionCode != BuildConfig.VERSION_CODE || !preferences.showLessContributors) {
            preferences.lastVersionCode = BuildConfig.VERSION_CODE
            getContributors()
        }
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
        //sort category by name
        if(preferences.sortCategory) playlistSet.sortCategories()
        //sort channels by name
        if(preferences.sortChannel) playlistSet.sortChannels()
        //remove channels with empty streamurl
        playlistSet.trimChannelWithEmptyStreamUrl()

        // set new playlist
        binding.catAdapter = CategoryAdapter(playlistSet.categories)

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

        PlaylistHelper(this).task(preferences.sources,
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
                    if (playlistSet.categories.size == 0) showAlertPlaylistError(null)
                    else setPlaylistToAdapter(playlistSet)
                }
        }).getResponse()
    }

    private fun showAlertPlaylistError(error: String?) {
        val message = error ?: getString(R.string.null_playlist)
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

    private fun checkNewRelease() {
        val stringRequest = StringRequest(Request.Method.GET,
            getString(R.string.json_release),
            Response.Listener { response: String? ->
                try {
                    val release = Gson().fromJson(response, Release::class.java)
                    if (release.versionCode <= BuildConfig.VERSION_CODE) return@Listener
                    val message = StringBuilder(
                        String.format(
                            getString(R.string.message_update),
                            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                            release.versionName, release.versionCode
                        )
                    )
                    for (log in release.changelog) {
                        message.append(String.format(getString(R.string.message_update_changelog), log))
                    }
                    if (release.changelog.isEmpty()) {
                        message.append(getString(R.string.message_update_no_changelog))
                    }
                    showAlertUpdate(message.toString(), release.downloadUrl)
                } catch (e: Exception) {
                    Log.e("Volley", "Could not check new update!", e)
                }
            }, null
        )
        request.add(stringRequest)
    }

    private fun showAlertUpdate(message: String, fileUrl: String) {
        askPermissions()
        val alert = AlertDialog.Builder(this).apply {
            setTitle(R.string.alert_new_update)
            setMessage(message)
            setPositiveButton(R.string.dialog_download) { _,_ -> downloadFile(fileUrl) }
            setNegativeButton(R.string.dialog_skip) { _,_ -> preferences.setLastCheckUpdate() }
            create()
        }
        alert.show()
    }

    private fun getContributors() {
        val stringRequest = StringRequest(Request.Method.GET,
            getString(R.string.gh_contributors),
            { response: String? ->
                try {
                    val users = Gson().fromJson(response, Array<GithubUser>::class.java)
                    val message = StringBuilder(getString(R.string.message_thanks_to))
                    for (user in users) {
                        message.append(user.login).append(", ")
                    }
                    if (users.isNotEmpty() && preferences.totalContributors < users.size) {
                        preferences.totalContributors = users.size
                        showAlertContributors(message.substring(0, message.length - 2))
                    }
                } catch (e: Exception) {
                    Log.e("Volley", "Could not get contributors!", e)
                }
            }, null
        )
        request.add(stringRequest)
    }

    private fun showAlertContributors(message: String) {
        val positiveBtn = if (preferences.showLessContributors) R.string.dialog_close else R.string.dialog_show_less
        val alert = AlertDialog.Builder(this).apply {
            setTitle(R.string.alert_title_contributors)
            setMessage(message)
            setNeutralButton(R.string.dialog_telegram) { _,_ -> openWebsite(getString(R.string.telegram_group)) }
            setNegativeButton(R.string.dialog_website) { _,_ -> openWebsite(getString(R.string.website)) }
            setPositiveButton(positiveBtn) { _,_ -> preferences.showLessContributors() }
        }
        val dialog = alert.create()
        alert.show()
        Handler(Looper.getMainLooper())
            .postDelayed({ try { dialog.dismiss() } catch (e: Exception) { }}, 10000)
    }

    private fun openWebsite(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(link)))
    }

    private fun downloadFile(url: String) {
        try {
            val uri = Uri.parse(url)
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.lastPathSegment)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            dm.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    @TargetApi(23)
    protected fun askPermissions() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.M) return
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        for (perm in permissions) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 260621)
                break
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 260621) return
        if (!grantResults.contains(PackageManager.PERMISSION_DENIED)) return
        Toast.makeText(this, getString(R.string.must_allow_permissions), Toast.LENGTH_LONG).show()
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
        SettingsDialog().show(supportFragmentManager.beginTransaction(),null)
    }

    private fun openSearch() {
        SearchDialog().show(supportFragmentManager.beginTransaction(),null)
    }
}