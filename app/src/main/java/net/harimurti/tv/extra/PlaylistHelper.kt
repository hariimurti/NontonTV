package net.harimurti.tv.extra

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import net.harimurti.tv.R
import net.harimurti.tv.model.Playlist
import java.io.*

class PlaylistHelper(val context: Context) {
    private val preferences: Preferences = Preferences(context)
    private val cache: File = File(context.cacheDir, PLAYLIST_JSON)
    private var local: File = File(context.getExternalFilesDir(null)?.absolutePath?.substringBefore("/Android"), PLAYLIST_JSON)
    private val select: File = File(preferences.playlistSelect)

    companion object {
        private const val TAG = "PlaylistHelper"
        const val PLAYLIST_JSON = "NontonTV.json"
        const val MODE_DEFAULT = 0
        const val MODE_CUSTOM = 1
        const val MODE_LOCAL = 2
        const val MODE_SELECT = 3
    }

    fun mode(): Int {
        return if (!preferences.useCustomPlaylist) MODE_DEFAULT
        else when (preferences.radioPlaylist) {
            0 -> MODE_LOCAL
            1 -> MODE_SELECT
            else -> MODE_CUSTOM
        }
    }

    val urlPath: String
        get() = if (mode() == MODE_CUSTOM) preferences.playlistExternal else context.getString(
            R.string.json_playlist
        )

    fun writeCache(content: String?) {
        try {
            val fw = FileWriter(cache.absoluteFile)
            val bw = BufferedWriter(fw)
            bw.write(content)
            bw.close()
        } catch (e: Exception) {
            Log.e(TAG, String.format("Could not write %s", cache.name), e)
        }
    }

    private fun read(file: File): Playlist? {
        return try {
            Log.e(TAG, file.exists().toString())
            Log.e(TAG, file.name)
            if (!file.exists()) throw FileNotFoundException()
            val fr = FileReader(file.absoluteFile)
            val br = BufferedReader(fr)
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
            br.close()
            Log.e(TAG, file.name)
            Gson().fromJson(sb.toString(), Playlist::class.java)
        } catch (e: Exception) {
            if (file == cache) e.printStackTrace()
            else Log.e(TAG, String.format("Could not read %s", file.name), e)
            null
        }
    }

    fun readCache(): Playlist? {
        return read(cache)
    }

    fun readLocal(): Playlist? {
        return read(local)
    }

    fun readSelect(): Playlist? {
        return read(select)
    }
}