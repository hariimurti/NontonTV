package net.harimurti.tv.extra

import net.harimurti.tv.App.Companion.runOnUiThread
import net.harimurti.tv.extension.*
import net.harimurti.tv.model.Source
import okhttp3.*
import java.io.IOException

class SourceChecker {
    private var source: Source? = null
    private var result: Result? = null

    interface Result {
        fun onCheckResult(result: Boolean) {}
    }

    fun set(source: Source, result: Result): SourceChecker {
        this.source = source
        this.result = result
        return this
    }

    fun run() {
        if (source?.path.isNullOrBlank()) {
            result?.onCheckResult(false); return
        }

        // local playlist
        if (source?.path?.isLinkUrl() == false) {
            result?.onCheckResult(source?.path.isPathExist())
            return
        }

        // internet playlist
        HttpClient(false)
            .create(source?.path!!.toRequest())
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { result?.onCheckResult(false) }
                }

                override fun onResponse(call: Call, response: Response) {
                    val content = response.content()
                    runOnUiThread {
                        if (response.isSuccessful && !content.isNullOrBlank()) {
                            if (!content.toPlaylist().isCategoriesEmpty())
                                result?.onCheckResult(true)
                            return@runOnUiThread
                        }
                        result?.onCheckResult(false)
                    }
                }
            })
    }
}