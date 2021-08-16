package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class DrmLicense {
    @SerializedName("drm_name")
    var name: String? = null
    @SerializedName("drm_url")
    var url: String? = null
}