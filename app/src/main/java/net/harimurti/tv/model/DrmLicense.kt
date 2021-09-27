package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class DrmLicense {
    @SerializedName("drm_id")
    var id: String? = null
    @SerializedName("drm_type")
    lateinit var type: String
    @SerializedName("drm_key")
    lateinit var key: String
}