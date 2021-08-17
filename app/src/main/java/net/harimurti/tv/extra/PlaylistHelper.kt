package net.harimurti.tv.extra

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import net.harimurti.tv.R
import net.harimurti.tv.model.*
import java.io.*

class PlaylistHelper(val context: Context) {
    private val cache: File = File(context.cacheDir, "NontonTV.json")
    private var taskListener: TaskListener? = null
    private var sources: ArrayList<Source> = ArrayList()
    private val volley = VolleyRequestQueue.create(context)

    companion object {
        private const val TAG = "PlaylistHelper"
    }

    interface TaskListener {
        fun onResponse(playlist: Playlist?) {}
        fun onError(error: Exception, source: Source) {}
        fun onFinish() {}
    }

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

    fun readFile(file: File): Playlist? {
        return try {
            if (!file.exists()) throw FileNotFoundException()
            FileReader(file.absoluteFile).readText().toPlaylist()
        } catch (e: Exception) {
            if (file == cache) e.printStackTrace()
            else Log.e(TAG, String.format("Could not read %s", file.name), e)
            null
        }
    }

    fun readCache(): Playlist? {
        return readFile(cache)
    }

    fun request(sources: ArrayList<Source>?, taskListener: TaskListener?): PlaylistHelper {
        sources?.let { this.sources.addAll(it) }
        this.taskListener = taskListener
        return this
    }

    fun get() {
        // throw onFinish when sources is empty
        if (sources.isEmpty()) {
            taskListener?.onFinish()
            return
        }

        // get first source
        val source = sources.first()
        sources.remove(source)
        if (!source.active) {
            get()
            return
        }

        // local playlist
        if (source.path?.startsWith("http", ignoreCase = true) == false) {
            val playlist = readFile(File(source.path.toString()))
            taskListener?.onResponse(playlist)
            get()
            return
        }

        // online playlist
        val stringRequest = StringRequest(
            Request.Method.GET, source.path,
            { content ->
                taskListener?.onResponse(content.toPlaylist())
                get()
            },
            { error ->
                var message = "[UNKNOWN] : ${source.path}"
                if (error.networkResponse != null) {
                    val errorcode = error.networkResponse.statusCode
                    message = "[$errorcode] : ${source.path}"
                } else if (!Network(context).isConnected()) {
                    message = context.getString(R.string.no_network)
                }
                Log.e(TAG, "Source : ${source.path}", error)
                taskListener?.onError(Exception(message), source)
                get()
            })
        volley.add(stringRequest)
    }
}

