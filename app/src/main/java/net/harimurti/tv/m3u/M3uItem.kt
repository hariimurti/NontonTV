package net.harimurti.tv.m3u

class M3uItem {
    var channelName: String? = null

    var streamURL: String? = null

    var category: String? = null

    var drmURL: String? = null

    var drmName: String? = null

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("\n{")
        if (channelName != null) {
            sb.append("\n"+
                    "\t\"name\": $channelName")
        }
        if (streamURL != null) {
            sb.append("\n"+
                    "\t\"stream_url\": $streamURL")
        }
        if (category != null) {
            sb.append("\n"+
                    "\t\"category\": $category")
        }
        if (drmURL != null) {
            sb.append("\n"+
                    "\t\"drm_url\": $drmURL")
        }
        if (drmName != null) {
            sb.append("\n"+
                    "\t\"drm_name\": $drmName")
        }
        sb.append("\n}")
        return sb.toString()
    }
}