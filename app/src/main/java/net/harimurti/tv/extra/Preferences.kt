package net.harimurti.tv.extra

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import net.harimurti.tv.App
import net.harimurti.tv.R
import net.harimurti.tv.extension.isLinkUrl
import net.harimurti.tv.extension.isPathExist
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
        private const val IGNORED_VERSION = "IGNORED_VERSION"
        private const val LAST_WATCHED = "LAST_WATCHED"
        private const val OPEN_LAST_WATCHED = "OPEN_LAST_WATCHED"
        private const val LAUNCH_AT_BOOT = "LAUNCH_AT_BOOT"
        private const val SORT_FAVORITE = "SORT_FAVORITE"
        private const val SORT_CATEGORY = "SORT_CATEGORY"
        private const val SORT_CHANNEL = "SORT_CHANNEL"
        private const val OPTIMIZE_PREBUFFER = "OPTIMIZE_PREBUFFER"
        private const val REVERSE_NAVIGATION = "REVERSE_NAVIGATION"
        private const val CONTRIBUTORS = "CONTRIBUTORS"
        private const val RESIZE_MODE = "RESIZE_MODE"
        private const val SPEED_MODE = "SPEED_MODE"
        private const val VOLUME_CONTROL = "VOLUME_CONTROL"
        private const val SOURCES_PLAYLIST = "SOURCES_PLAYLIST"
        private const val COUNTRY_ID = "COUNTRY_ID"
    }

    var isFirstTime: Boolean
        get() = preferences.getBoolean(FIRST_TIME, true)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(FIRST_TIME, value)
            editor.apply()
        }

    var ignoredVersion: Int
        get() = preferences.getInt(IGNORED_VERSION, 0)
        set(value) {
            editor = preferences.edit()
            editor.putInt(IGNORED_VERSION, value)
            editor.apply()
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

    var optimizePrebuffer: Boolean
        get() = preferences.getBoolean(OPTIMIZE_PREBUFFER, true)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(OPTIMIZE_PREBUFFER, value)
            editor.apply()
        }

    var reverseNavigation: Boolean
        get() = preferences.getBoolean(REVERSE_NAVIGATION, false)
        set(value) {
            editor = preferences.edit()
            editor.putBoolean(REVERSE_NAVIGATION, value)
            editor.apply()
        }

    var countryId: String
        get() = preferences.getString(COUNTRY_ID, "id").toString()
    set(value) {
        editor = preferences.edit()
        editor.putString(COUNTRY_ID, value)
        editor.apply()
    }

    var sources: ArrayList<Source>?
        get() {
            val result = ArrayList<Source>()
            val default = Source().apply {
                path = String.format(context.getString(R.string.iptv_playlist), countryId)
                active = true
            }
            try {
                val json = preferences.getString(SOURCES_PLAYLIST, "").toString()
                if (json.isBlank()) throw Exception("no playlist sources in preference")
                val list = Gson().fromJson(json, Array<Source>::class.java)
                if (list == null || list.isEmpty()) throw Exception("cannot parse sources?")
                list.first().path = default.path
                list.forEach {
                    if (it.path.isLinkUrl()) result.add(it)
                    else if (it.path.isPathExist()) result.add(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (result.isEmpty()) result.add(default)
            val active = result.filter { s -> s.active }
            if (active.isEmpty()) result.first().active = true

            return result
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

    var speedMode: Float
        get() = preferences.getFloat(SPEED_MODE, 1F)
        set(value) {
            editor = preferences.edit()
            editor.putFloat(SPEED_MODE, value)
            editor.apply()
        }

    var volume: Float
        get() = preferences.getFloat(VOLUME_CONTROL, 1F)
        set(value) {
            editor = preferences.edit()
            editor.putFloat(VOLUME_CONTROL, value)
            editor.apply()
        }
}