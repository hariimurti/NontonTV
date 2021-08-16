package net.harimurti.tv.extra

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import net.harimurti.tv.R
import net.harimurti.tv.model.*
import java.io.*

class PlaylistHelper(val context: Context) {
    private val preferences: Preferences = Preferences(context)
    private val cache: File = File(context.cacheDir, PLAYLIST_JSON)
    private var local: File = File(context.getExternalFilesDir(null)?.absolutePath
        ?.substringBefore("/Android"), PLAYLIST_JSON)

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
        get() = if (mode() == MODE_CUSTOM) preferences.playlistExternal
        else context.getString(R.string.json_playlist)

    fun writeCache(playlist: Playlist) {
        val content = Gson().toJson(playlist)
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
            if (!file.exists()) throw FileNotFoundException()
            FileReader(file.absoluteFile).readText().toPlaylist()
        } catch (e: Exception) {
            if (file == cache) e.printStackTrace()
            else Log.e(TAG, String.format("Could not read %s", file.name), e)
            null
        }
    }

    private fun readM3U(file: File): Playlist? {
        return try {
            if (!file.exists()) throw FileNotFoundException()
            val fr = FileReader(file.absoluteFile)
            val content = fr.readText()
            if (!content.contains(M3U.EXTINF)) throw M3U.ParsingException("${file.name} is malformed")
            return M3uTool.parse(content).toPlaylist()
        } catch (e: Exception) {
            Log.e(TAG, String.format("Could not read %s", file.name), e)
            null
        }
    }

    fun readCache(): Playlist? {
        return read(cache)
    }

    fun readLocal(): Playlist? {
        return read(local)
    }

    fun readSelect(path: String): Playlist? {
        val select = File(path)
        return if(path.endsWith(".json", ignoreCase = true)) read(select)
        else readM3U(select)
    }
}

