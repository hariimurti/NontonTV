package net.harimurti.tv.extra

import android.util.Log
import com.google.gson.Gson
import net.harimurti.tv.App
import net.harimurti.tv.model.*
import java.io.*
import net.harimurti.tv.extension.*

class PlaylistHelper {
    private val context = App.context
    private val cache: File = File(context.cacheDir, "NontonTV.json")
    private val favorite: File = File(context.filesDir, "Favorite.json")

    companion object {
        private const val TAG = "PlaylistHelper"
    }

    fun writeCache(playlist: Playlist) {
        try {
            val content = Gson().toJson(playlist)
            File(cache.absolutePath).writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, String.format("Could not write to %s", cache.name), e)
        }
    }

    fun writeFavorites(fav: Favorites) {
        try {
            Playlist.favorites = fav
            val content = Gson().toJson(fav)
            File(favorite.absolutePath).writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to ${favorite.name}", e)
        }
    }

    fun readFile(file: File): Playlist? {
        return try {
            file.readText(Charsets.UTF_8).toPlaylist()
        } catch (e: Exception) {
            if (file != cache) Log.e(TAG, String.format("Could not read from %s", file), e)
            null
        }
    }

    fun readCache(): Playlist? {
        return readFile(cache)
    }

    fun readFavorites(): Favorites {
        val newFav = Favorites()
        val fav = try {
            if (favorite.exists()) {
                val content = favorite.readText(Charsets.UTF_8)
                Gson().fromJson(content, Favorites::class.java)
            }
            else {
                writeFavorites(newFav)
                newFav
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read from ${favorite.name}", e)
            newFav
        }
        Playlist.favorites = fav
        return fav
    }
}

