package net.harimurti.tv.extension

import android.text.Html
import com.google.android.exoplayer2.C
import okhttp3.Request
import java.io.File
import java.net.URLDecoder
import java.util.*
import java.util.zip.CRC32

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
    val result = Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(this)
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

fun String.decodeUrl(): String {
    return URLDecoder.decode(this, "utf-8")
}

fun String.decodeHex(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun String.toClearKey(): ByteArray {
    val keyId = this.substringBefore(":").decodeHex().toBase64Url()
    val keyValue = this.substringAfter(":").decodeHex().toBase64Url()
    return """{"keys":[{"kty":"oct","k":"$keyValue","kid":"$keyId"}],"type":"temporary"}""".toByteArray()
}

fun String.toCRC32(): String {
    val bytes = this.toByteArray()
    return CRC32().apply { update(bytes) }.value.toString()
}

fun String.toUUID(): UUID {
    return when {
        this.contains("clearkey") -> C.CLEARKEY_UUID
        this.contains("widevine") -> C.WIDEVINE_UUID
        this.contains("playready") -> C.PLAYREADY_UUID
        else -> C.UUID_NIL
    }
}