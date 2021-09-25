package net.harimurti.tv.extension

import android.text.Html
import okhttp3.Request
import java.io.File

fun String?.isLinkUrl(): Boolean {
    if (this == null) return false
    return Regex("^https?://(?:[\\w.-]+\\.[a-zA-Z]{2,6}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?(?:/.*)?\$")
        .matches(this)
}

fun String?.isStreamUrl(): Boolean {
    if (this == null) return false
    return Regex("^(?:https?|rtmp)://(?:[\\w.-]+\\.[a-zA-Z]{2,6}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?(?:/.*)?\$")
        .matches(this)
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

@Suppress("DEPRECATION")
fun String?.normalize(): String? {
    if (this == null) return null
    val decoded = Html.fromHtml(this).toString()
    return Regex("([~@#\$%&<>{}();_=])(?:\\1{2,})").replace(decoded, "").trim()
}

fun String.toRequest(): Request {
    return Request.Builder().url(this).build()
}

fun String.toRequestBuilder(): Request.Builder {
    return Request.Builder().url(this)
}

fun String.decodeHex(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun String.toClearKey(): ByteArray {
    val keyId = this.substringBefore(":").decodeHex().toBase64Url()
    val keyValue = this.substringAfter(":").decodeHex().toBase64Url()
    return """{"keys":[{"kty":"oct","k":"$keyValue","kid":"$keyId"}],"type":"temporary"}""".toByteArray()
}