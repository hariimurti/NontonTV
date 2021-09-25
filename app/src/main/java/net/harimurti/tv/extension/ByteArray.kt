package net.harimurti.tv.extension

import android.util.Base64

fun ByteArray.toBase64Url(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE)
            .trim().trimEnd('=')
}