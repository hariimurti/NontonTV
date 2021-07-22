package net.harimurti.tv.extra

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class HttpsTrustManager : X509TrustManager {
    companion object {
        private val AcceptedIssuers = arrayOf<X509Certificate>()
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
    }

    fun isClientTrusted(chain: Array<X509Certificate?>?): Boolean {
        return true
    }

    fun isServerTrusted(chain: Array<X509Certificate?>?): Boolean {
        return true
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return AcceptedIssuers
    }
}