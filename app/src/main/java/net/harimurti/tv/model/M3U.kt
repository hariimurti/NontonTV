package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName
import java.lang.Exception
import java.lang.RuntimeException

class M3U {
    var groupName: String? = null
    @SerializedName("name")
    var channelName: String? = null
    @SerializedName("stream_url")
    var streamUrl: ArrayList<String>? = ArrayList()
    @SerializedName("drm_url")
    var licenseKey: String? = null
    @SerializedName("drm_name")
    var licenseName: String? = null

    companion object {
        const val KODIPROP = "#KODIPROP"
        const val EXTINF = "#EXTINF"
        const val EXTGRP = "#EXTGRP"
        const val EXTVLCOPT = "#EXTVLCOPT"
    }

    class ParsingException : RuntimeException {
        private var line: Int

        constructor(line: Int, message: String) : super("$message at line $line") {
            this.line = line
        }

        constructor(line: Int, message: String, cause: Exception?) : super("$message at line $line", cause) {
            this.line = line
        }

        constructor(message: String) : super(message) {
            this.line = 0
        }
    }
}