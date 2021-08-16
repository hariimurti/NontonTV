package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class Channel {
    var name: String? = null
    @SerializedName("stream_url")
    var streamUrl: String? = null
    @SerializedName("drm_name")
    var drmName: String? = null
    @SerializedName("cat_id")
    var catId: Int? = null
    @SerializedName("ch_id")
    var chId: Int? = null
}