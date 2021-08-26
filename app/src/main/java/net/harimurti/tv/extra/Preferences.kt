package net.harimurti.tv.extra

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import net.harimurti.tv.App
import net.harimurti.tv.R
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Source
import java.util.*
import kotlin.collections.ArrayList

class Preferences {
    private val context = App.context
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var editor: SharedPreferences.Editor

    companion object {
        private const val FIRST_TIME = "FIRST_TIME"
        private const val LAST_CHECK_UPDATE = "LAST_CHECK_UPDATE"
        private const val LAST_WATCHED = "LAST_WATCHED"
        private const val OPEN_LAST_WATCHED = "OPEN_LAST_WATCHED"
        private const val LAUNCH_AT_BOOT = "LAUNCH_AT_BOOT"
        private const val SORT_FAVORITE = "SORT_FAVORITE"
        private const val SORT_CATEGORY = "SORT_CATEGORY"
        private const val SORT_CHANNEL = "SORT_CHANNEL"
        private const val REVERSE_NAVIGATION = "REVERSE_NAVIGATION"
        private const val CONTRIBUTORS = "CONTRIBUTORS"
        private const val RESIZE_MODE = "RESIZE_MODE"
        private const val SOURCES_PLAYLIST = "SOURCES_PLAYLIST"
    }

    var isFirstTime: Boolean
        get() = preferences.getBoolean(FIRST_TIME, true)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(FIRST_TIME, value)
            editor.apply()
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

    var sortFavorite: Boolean
        get() = preferences.getBoolean(SORT_FAVORITE, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(SORT_FAVORITE, value)
            editor.apply()
        }

    var sortCategory: Boolean
        get() = preferences.getBoolean(SORT_CATEGORY, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(SORT_CATEGORY, value)
            editor.apply()
        }

    var sortChannel: Boolean
        get() = preferences.getBoolean(SORT_CHANNEL, true)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(SORT_CHANNEL, value)
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

    var reverseNavigation: Boolean
        get() = preferences.getBoolean(REVERSE_NAVIGATION, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(REVERSE_NAVIGATION, value)
            editor.apply()
        }

    var sources: ArrayList<Source>?
        get() {
            val default = Source().apply {
                path = context.getString(R.string.json_playlist)
                active = true
            }
            return try {
                val result = ArrayList<Source>()
                val json = preferences.getString(SOURCES_PLAYLIST, "").toString()
                if (json.isBlank()) {
                    result.add(default)
                }
                else {
                    val list = Gson().fromJson(json, Array<Source>::class.java)
                    val active = list.filter { s -> s.active }
                    if (list[0].path != default.path) result.add(default)
                    if (active.size == 1 && active[0].path == default.path) list[0].active = true
                    result.addAll(list)
                }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        set(value) {
            val json = Gson().toJson(value)
            editor = preferences.edit()
            editor.putString(SOURCES_PLAYLIST, json)
            editor.apply()
        }

    var contributors: String?
        get() = preferences.getString(CONTRIBUTORS, context.getString(R.string.main_contributors))
        set(value) {
            editor = preferences.edit()
            editor.putString(CONTRIBUTORS, value)
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