package net.harimurti.tv.extra

import java.io.File

fun String?.isWebsite(): Boolean {
    return Regex("(https?://.+?\\..+)", RegexOption.IGNORE_CASE).matches(this.toString())
}

fun String?.isPathExist(): Boolean {
    return File(this.toString()).exists()
}

fun String?.toFile(): File {
    return File(this.toString())
}