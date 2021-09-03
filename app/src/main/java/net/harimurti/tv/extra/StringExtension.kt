package net.harimurti.tv.extra

import java.io.File

fun String?.isLinkUrl(): Boolean {
    if (this == null) return false
    return Regex("^https?://[a-zA-Z0-9-.]+\\.[a-zA-Z]{2,6}(/.*)?\$").matches(this)
}

fun String?.isStreamUrl(): Boolean {
    if (this == null) return false
    return Regex("^(?:https?|rtmp)://[a-zA-Z0-9-.]+\\.[a-zA-Z]{2,6}(/.*)?\$").matches(this)
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