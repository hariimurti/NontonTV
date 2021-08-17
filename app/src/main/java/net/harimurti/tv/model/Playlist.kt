package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class Playlist {
    var categories: ArrayList<Category> = ArrayList()
    @SerializedName("drm_licenses")
    var drmLicenses: ArrayList<DrmLicense> = ArrayList()

    companion object {
        var loaded: Playlist? = null
    }
}