package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class Playlist {
    var categories: ArrayList<Category>? = null
    @SerializedName("drm_licenses")
    var drmLicenses: ArrayList<DrmLicense>? = null

    companion object {
        var loaded: Playlist? = null
    }
}