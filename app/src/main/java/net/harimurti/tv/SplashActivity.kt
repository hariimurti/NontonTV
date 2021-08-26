package net.harimurti.tv

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import net.harimurti.tv.databinding.ActivitySplashBinding
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.GithubUser
import net.harimurti.tv.model.Playlist
import net.harimurti.tv.model.Release
import net.harimurti.tv.model.Source

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val preferences = Preferences()
    private val request = VolleyRequestQueue.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PlayerActivity.isPipMode) {
            val intent = Intent(this, PlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            this.startActivity(intent)
            this.finish()
            return
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textPresents.setOnClickListener {
            openWebsite(getString(R.string.telegram_group))
        }
        binding.textTitle.setOnClickListener {
            openWebsite(getString(R.string.website))
        }
        binding.textUsers.text = preferences.contributors

        // lock screen orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        // update constributors
        getContributors()

        // first time alert
        if (preferences.isFirstTime) {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.app_name)
                setMessage(R.string.alert_first_time)
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { _,_ ->
                    preferences.isFirstTime = false
                    prepareWhatIsNeeded()
                }
                create()
                show()
            }
        }
        else prepareWhatIsNeeded()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // check new release
        checkNewRelease()

        if (requestCode != 260621) return
        if (!grantResults.contains(PackageManager.PERMISSION_DENIED)) return
        Toast.makeText(this, getString(R.string.must_allow_permissions), Toast.LENGTH_LONG).show()
    }

    private fun prepareWhatIsNeeded() {
        // ask to grant all permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setStatus(R.string.status_checking_permission)
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            var passes = true
            for (perm in permissions) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, 260621)
                    passes = false
                    break
                }
            }
            if (!passes) return
        }

        // check new release
        checkNewRelease()
    }

    private fun checkNewRelease() {
        // skip if already done
        if (preferences.isCheckedReleaseUpdate) {
            return lunchMainActivity()
        }
        // start checking
        setStatus(R.string.status_checking_new_update)
        val stringRequest = StringRequest(
            Request.Method.GET,
            getString(R.string.json_release),
            Response.Listener { response: String? ->
                try {
                    val release = Gson().fromJson(response, Release::class.java)
                    if (release.versionCode <= BuildConfig.VERSION_CODE)
                        return@Listener lunchMainActivity()

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

                    AlertDialog.Builder(this).apply {
                        setTitle(R.string.alert_new_update)
                        setMessage(message)
                        setCancelable(false)
                        setPositiveButton(R.string.dialog_download) { _,_ ->
                            downloadFile(release.downloadUrl)
                            lunchMainActivity()
                        }
                        setNegativeButton(R.string.dialog_skip) { _,_ ->
                            preferences.setLastCheckUpdate()
                            lunchMainActivity()
                        }
                        create()
                        show()
                    }
                } catch (e: Exception) {
                    Log.e("Volley", "Could not check new update!", e)
                    lunchMainActivity()
                }
            }, { lunchMainActivity() }
        )
        request.add(stringRequest)
    }

    private fun getContributors() {
        val stringRequest = StringRequest(
            Request.Method.GET,
            getString(R.string.gh_contributors),
            { response: String? ->
                try {
                    val ghUsers = Gson().fromJson(response, Array<GithubUser>::class.java)
                    val users = ghUsers.toStringContributor()
                    preferences.contributors = users
                    binding.textUsers.text = users
                } catch (e: Exception) {
                    Log.e("Volley", "Could not get contributors!", e)
                }
            }, null
        )
        request.add(stringRequest)
    }

    private fun setStatus(resid: Int) {
        binding.textStatus.setText(resid)
    }

    private fun lunchMainActivity() {
        val playlistSet = Playlist()
        setStatus(R.string.status_preparing_playlist)
        PlaylistHelper().task(preferences.sources,
            object: PlaylistHelper.TaskResponse {
                override fun onError(error: Exception, source: Source) {
                    val message = if (error.message.isNullOrBlank()) "Problem with $source" else error.message
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
                override fun onResponse(playlist: Playlist?) {
                    if (playlist != null) playlistSet.mergeWith(playlist)
                }
                override fun onFinish() {
                    Playlist.cached = playlistSet
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 2000)
                }
            }).getResponse()
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
}