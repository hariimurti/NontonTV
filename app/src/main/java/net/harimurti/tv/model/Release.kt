package net.harimurti.tv.model

class Release {
    var versionCode = 0
    lateinit var versionName: String
    lateinit var changelog: List<String>
    lateinit var downloadUrl: String
}