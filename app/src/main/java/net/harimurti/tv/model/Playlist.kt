package net.harimurti.tv.model

class Playlist {
    var categories: ArrayList<Category>? = null
    var drm_licenses: ArrayList<DrmLicense>? = null

    companion object {
        var loaded: Playlist? = null
    }
}