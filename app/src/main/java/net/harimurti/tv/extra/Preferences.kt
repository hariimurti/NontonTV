package net.harimurti.tv.extra

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import net.harimurti.tv.model.PlayData
import java.util.*

class Preferences(context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var editor: SharedPreferences.Editor

    companion object {
        private const val LAST_CHECK_UPDATE = "LAST_CHECK_UPDATE"
        private const val LAST_WATCHED = "LAST_WATCHED"
        private const val OPEN_LAST_WATCHED = "OPEN_LAST_WATCHED"
        private const val LAUNCH_AT_BOOT = "LAUNCH_AT_BOOT"
        private const val USE_CUSTOM_PLAYLIST = "USE_CUSTOM_PLAYLIST"
        private const val MERGE_PLAYLIST = "MERGE_PLAYLIST"
        private const val PLAYLIST_EXTERNAL = "PLAYLIST_EXTERNAL"
        private const val LAST_VERSIONCODE = "LAST_VERSIONCODE"
        private const val TOTAL_CONTRIBUTORS = "TOTAL_CONTRIBUTORS"
        private const val SHOW_LESS_CONTRIBUTORS = "SHOW_LESS_CONTRIBUTORS"
        private const val RESIZE_MODE = "RESIZE_MODE"
    }

    fun setLastCheckUpdate() {
        val nextday = Calendar.getInstance()
        nextday.add(Calendar.DATE, 1)
        nextday[Calendar.HOUR_OF_DAY] = 0
        editor = preferences.edit()
        editor.putLong(LAST_CHECK_UPDATE, nextday.timeInMillis)
        editor.apply()
    }

    val isCheckedReleaseUpdate: Boolean
        get() = try {
            val last = Calendar.getInstance()
            last.timeInMillis = preferences.getLong(LAST_CHECK_UPDATE, 0)
            val dateLast = last.time
            val now = Calendar.getInstance()
            now[Calendar.HOUR_OF_DAY] = 0
            val dateNow = now.time
            dateLast.after(dateNow)
        } catch (ignore: Exception) {
            false
        }

    var launchAtBoot: Boolean
        get() = preferences.getBoolean(LAUNCH_AT_BOOT, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(LAUNCH_AT_BOOT, value)
            editor.apply()
        }

    var playLastWatched: Boolean
        get() = preferences.getBoolean(OPEN_LAST_WATCHED, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(OPEN_LAST_WATCHED, value)
            editor.apply()
        }

    var watched: PlayData
        get() = Gson().fromJson(preferences.getString(LAST_WATCHED, "{}").toString(), PlayData::class.java)
        set(value) {
            val json = Gson().toJson(value)
            editor = preferences.edit()
            editor.putString(LAST_WATCHED, json)
            editor.apply()
        }

    var useCustomPlaylist: Boolean
        get() = preferences.getBoolean(USE_CUSTOM_PLAYLIST, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(USE_CUSTOM_PLAYLIST, value)
            editor.apply()
        }

    var mergePlaylist: Boolean
        get() = preferences.getBoolean(MERGE_PLAYLIST, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(MERGE_PLAYLIST, value)
            editor.apply()
        }

    var playlistExternal: String
        get() = preferences.getString(PLAYLIST_EXTERNAL, "").toString()
        set(value) {
            editor = preferences.edit()
            editor.putString(PLAYLIST_EXTERNAL, value)
            editor.apply()
        }

    var lastVersionCode: Int
        get() = preferences.getInt(LAST_VERSIONCODE, 0)
        set(value) {
            editor = preferences.edit()
            editor.putInt(LAST_VERSIONCODE, value)
            editor.putInt(TOTAL_CONTRIBUTORS, 0)
            editor.apply()
        }

    fun showLessContributors() {
        editor = preferences.edit()
        editor.putBoolean(SHOW_LESS_CONTRIBUTORS, true)
        editor.apply()
    }

    val showLessContributors: Boolean
        get() = preferences.getBoolean(SHOW_LESS_CONTRIBUTORS, false)

    var totalContributors: Int
        get() = preferences.getInt(TOTAL_CONTRIBUTORS, 0)
        set(value) {
            editor = preferences.edit()
            editor.putInt(TOTAL_CONTRIBUTORS, value)
            editor.apply()
        }

    var resizeMode: Int
        get() = preferences.getInt(RESIZE_MODE, 0)
        set(value) {
            editor = preferences.edit()
            editor.putInt(RESIZE_MODE, value)
            editor.apply()
        }
}