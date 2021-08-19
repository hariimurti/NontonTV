package net.harimurti.tv.extra

import java.io.File

fun String?.isLinkUrl(): Boolean {
    return this?.startsWith("http", true) == true
}

fun String?.isStreamUrl(): Boolean {
    return this?.isLinkUrl() == true || this?.startsWith("rtmp", true) == true
}

fun String?.isPathExist(): Boolean {
    return File(this.toString()).exists()
}

fun String?.toFile(): File {
    return File(this.toString())
}