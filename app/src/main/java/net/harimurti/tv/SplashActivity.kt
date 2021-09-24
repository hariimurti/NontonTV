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
import com.google.gson.Gson
import net.harimurti.tv.databinding.ActivitySplashBinding
import net.harimurti.tv.extension.*
import net.harimurti.tv.extra.*
import net.harimurti.tv.model.GithubUser
import net.harimurti.tv.model.Playlist
import net.harimurti.tv.model.Release
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val preferences = Preferences()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PlayerActivity.isPipMode) {
            val intent = Intent(this, PlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            this.startActivity(intent)
            this.finish()
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textPresents.setOnClickListener {
            openWebsite(getString(R.string.telegram_group))
        }
        binding.textTitle.setOnClickListener {
            openWebsite(getString(R.string.website))
        }
        binding.textUsers.text = preferences.contributors

        // update constributors
        HttpClient(true)
            .create(getString(R.string.gh_contributors).toRequest())
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("HttpClient", "Could not get contributors!", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val content = response.content()
                        if (content.isNullOrBlank()) throw Exception("null content")
                        if (!response.isSuccessful) throw Exception(response.message())
                        val ghUsers = Gson().fromJson(content, Array<GithubUser>::class.java)
                        val users = ghUsers.toStringContributor()
                        preferences.contributors = users
                        setContributors(users)
                    } catch (e: Exception) {
                        Log.e("HttpClient", "Could not get contributors!", e)
                    }
                }
            })

        // first time alert
        if (preferences.isFirstTime) {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.app_name)
                setMessage(R.string.alert_first_time)
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { di,_ ->
                    preferences.isFirstTime = false
                    prepareWhatIsNeeded()
                    di.dismiss()
                }
                setNeutralButton(R.string.button_website) { _,_ ->
                    preferences.isFirstTime = false
                    openWebsite(getString(R.string.website))
                    finish()
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
        // start checking
        setStatus(R.string.status_checking_new_update)
        val request = getString(R.string.json_release).toRequest()
        HttpClient(true).create(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HttpClient", "Could not check new update!", e)
                lunchMainActivity()
            }

            override fun onResponse(call: Call, response: Response) {
                val content = response.content()
                if (!response.isSuccessful || content.isNullOrBlank()) {
                    Log.e("HttpClient", "Could not check new update! ${response.message()}")
                    return lunchMainActivity()
                }

                val release = Gson().fromJson(content, Release::class.java)
                if (release.versionCode <= BuildConfig.VERSION_CODE ||
                    release.versionCode <= preferences.ignoredVersion) {
                    return lunchMainActivity()
                }

                runOnUiThread {
                    showNewRelease(release)
                }
            }
        })
    }

    private fun showNewRelease(release: Release) {
        val message = StringBuilder(String.format(getString(R.string.message_update),
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
            release.versionName, release.versionCode))

        for (log in release.changelog) {
            message.append(String.format(getString(R.string.message_update_changelog), log))
        }
        if (release.changelog.isEmpty()) {
            message.append(getString(R.string.message_update_no_changelog))
        }

        val downloadUrl = if (release.downloadUrl.isBlank()) {
            String.format(getString(R.string.apk_release),
                release.versionName, release.versionName, release.versionCode)
        }
        else release.downloadUrl

        AlertDialog.Builder(this).apply {
            setTitle(R.string.alert_new_update); setMessage(message)
            setCancelable(false)
            setPositiveButton(R.string.dialog_download) { _,_ ->
                downloadFile(downloadUrl); lunchMainActivity()
            }
            setNegativeButton(R.string.dialog_ignore) { _, _ ->
                preferences.ignoredVersion = release.versionCode; lunchMainActivity()
            }
            setNeutralButton(R.string.button_website) { _,_ ->
                openWebsite(getString(R.string.website)); lunchMainActivity()
            }
            create(); show()
        }
    }

    private fun setContributors(users: String?) {
        runOnUiThread {
            binding.textUsers.text = users
        }
    }

    private fun setStatus(resid: Int) {
        runOnUiThread {
            binding.textStatus.setText(resid)
        }
    }

    private fun lunchMainActivity() {
        val playlistSet = Playlist()
        setStatus(R.string.status_preparing_playlist)
        SourcesReader().set(preferences.sources, object: SourcesReader.Result {
            override fun onError(source: String, error: String) {
                runOnUiThread {
                    val message = "Source: $source, Error: $error"
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
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
        }).process(true)
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