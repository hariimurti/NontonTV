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

fun String?.findPattern(pattern: String): String? {
    if (this == null) return null
    val option = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    val result = Regex(pattern, option).matchEntire(this)
    return result?.groups?.get(1)?.value
}