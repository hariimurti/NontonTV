package net.harimurti.tv.extra

import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import net.harimurti.tv.R
import net.harimurti.tv.model.*
import java.io.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.NetworkResponse
import com.android.volley.Response
import net.harimurti.tv.App

class PlaylistHelper {
    private val context = App.context
    private val cache: File = File(context.cacheDir, "NontonTV.json")
    private val favorite: File = File(context.cacheDir, "Favorite.json")
    private var taskResponse: TaskResponse? = null
    private var taskChecker: TaskChecker? = null
    private var sources: ArrayList<Source> = ArrayList()
    private var checkSource: Source? = null
    private val volley = VolleyRequestQueue.create()

    companion object {
        private const val TAG = "PlaylistHelper"
    }

    interface TaskResponse {
        fun onResponse(playlist: Playlist?) {}
        fun onError(error: Exception, source: Source) {}
        fun onFinish() {}
    }

    interface TaskChecker {
        fun onCheckResult(result: Boolean) {}
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

    private fun readFile(file: File): Playlist? {
        return try {
            file.readText(Charsets.UTF_8).toPlaylist()
        } catch (e: Exception) {
            if (file == cache) e.printStackTrace()
            else Log.e(TAG, String.format("Could not read from %s", file), e)
            null
        }
    }

    fun readCache(): Playlist? {
        return readFile(cache)
    }

    fun readFavorites(): Favorites {
        val fav = try {
            val content = favorite.readText(Charsets.UTF_8)
            Gson().fromJson(content, Favorites::class.java)
        } catch (e: Exception) {
            if (e == FileNotFoundException()) e.printStackTrace()
            else Log.e(TAG, "Could not read from ${favorite.name}", e)
            Favorites()
        }
        Playlist.favorites = fav
        return fav
    }

    fun task(sources: ArrayList<Source>?, taskResponse: TaskResponse?): PlaylistHelper {
        sources?.let { this.sources.addAll(it) }
        this.taskResponse = taskResponse
        return this
    }

    fun task(source: Source, taskChecker: TaskChecker?): PlaylistHelper {
        checkSource = source
        this.taskChecker = taskChecker
        return this
    }

    fun getResponse() {
        // send onFinish when sources is empty
        if (sources.isEmpty()) {
            taskResponse?.onFinish()
            return
        }

        // get first source
        val source = sources.first()
        sources.remove(source)
        if (!source.active) {
            getResponse()
            return
        }

        // local playlist
        if (!source.path.isLinkUrl()) {
            val playlist = readFile(source.path.toFile())
            taskResponse?.onResponse(playlist)
            getResponse()
            return
        }

        // online playlist
        val stringRequest = object: StringRequest(Method.GET, source.path,
            { content ->
                taskResponse?.onResponse(content.toPlaylist())
                getResponse()
            },
            { error ->
                var message = "[UNKNOWN] : ${source.path}"
                if (error.networkResponse != null) {
                    val errorcode = error.networkResponse.statusCode
                    message = "[HTTP_$errorcode] : ${source.path}"
                } else if (!Network().isConnected()) {
                    message = context.getString(R.string.no_network)
                }
                Log.e(TAG, "Source : ${source.path}", error)
                taskResponse?.onError(Exception(message), source)
                getResponse()
            }) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<String?>? {
                // Volley's default charset is "ISO-8859-1".
                // If no charset is specified, we want to default to UTF-8.
                val charset = HttpHeaderParser.parseCharset(response.headers, null)
                if (null == charset) {
                    var contentType = response.headers!!["Content-Type"]
                    contentType = if (null != contentType) "$contentType;charset=UTF-8" else "charset=UTF-8"
                    response.headers!!["Content-Type"] = contentType
                }
                return super.parseNetworkResponse(response)
            }
        }
        volley.cache.clear()
        volley.add(stringRequest)
    }

    fun checkResult() {
        var result = false
        if (checkSource?.path?.isLinkUrl() == false) {
            result = checkSource?.path.isPathExist()
            taskChecker?.onCheckResult(result)
        }

        val stringRequest = StringRequest(
            Request.Method.GET, checkSource?.path.toString(),
            { content ->
                val pls = content.toPlaylist()
                result = pls != null && pls.categories.isNotEmpty()
                taskChecker?.onCheckResult(result)
            },
            { error ->
                var message = "[UNKNOWN] : ${checkSource?.path}"
                if (error.networkResponse != null) {
                    val errorcode = error.networkResponse.statusCode
                    message = "[HTTP_$errorcode] : ${checkSource?.path}"
                } else if (!Network().isConnected()) {
                    message = context.getString(R.string.no_network)
                }
                Log.e(TAG, message, error)
                taskChecker?.onCheckResult(result)
            })
        volley.cache.clear()
        volley.add(stringRequest)
    }
}

