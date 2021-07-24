package net.harimurti.tv.model

class Channel {
    var name: String? = null
    var stream_url: String? = null
    var drm_name: String? = null

    companion object {
        const val NAME: String = "CH_NAME"
        const val STREAMURL: String = "CH_STREAM_URL"
        const val DRMURL: String = "CH_DRM_URL"
    }
}