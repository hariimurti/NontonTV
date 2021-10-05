package net.harimurti.tv.extra

import net.harimurti.tv.App.Companion.runOnUiThread
import net.harimurti.tv.extension.content
import net.harimurti.tv.extension.toRequest
import net.harimurti.tv.model.ProxyList
import net.harimurti.tv.model.ProxySource
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException


class ProxyReader {
    private var sources: ArrayList<ProxySource.ListUrl> = ArrayList()
    private var result: Result? = null

    interface Result {
        fun onProgress(source: String) {}
        fun onResponse(proxyList: ProxyList?) {}
        fun onError(source: String, error: String) {}
        fun onFinish() {}
    }

    fun set(sources: ArrayList<ProxySource.ListUrl>?, result: Result): ProxyReader {
        sources?.let { this.sources.addAll(it) }
        this.result = result
        return this
    }

    fun process() {
        // send onFinish when sources is empty
        if (sources.isNullOrEmpty()) {
            result?.onFinish(); return
        }

        // get first source & remove it
        val source = sources.first()
        sources.remove(source)

        // report progress
        result?.onProgress(source.path)

        // get proxy from internet
        HttpClient(false)
            .create(source.path.toRequest())
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        result?.onError(source.path, e.message.toString())
                        process()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val content = response.content()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            if (!content.isNullOrBlank()) {
                                val playlist = if(source.path.contains("fate0"))
                                    content.toProxyList2()
                                else content.toProxyList(source.type)
                                result?.onResponse(playlist)
                            } else result?.onError(source.path, "null content")
                        } else result?.onError(source.path, response.message())
                        // repeat until sources is empty
                        process()
                    }
                }
            })
    }

    fun String?.toProxyList(type: String): ProxyList? {
        try { return ProxyTool().parse(this,type) }
        catch (e: Exception) { e.printStackTrace() }
        // content cant be parsed
        return null
    }

    fun String?.toProxyList2(): ProxyList? {
        try { return ProxyTool().parse2(this) }
        catch (e: Exception) { e.printStackTrace() }
        // content cant be parsed
        return null
    }
}
