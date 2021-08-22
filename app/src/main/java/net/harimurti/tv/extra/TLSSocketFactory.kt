package net.harimurti.tv.extra

import android.os.Build
import com.google.android.gms.security.ProviderInstaller
import net.harimurti.tv.App
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.*

class TLSSocketFactory : SSLSocketFactory() {
    private val context = App.context
    private val factory: SSLSocketFactory

    @get:Throws(NoSuchAlgorithmException::class)
    private val sSLContext: SSLContext
        get() {
            if (Build.VERSION.SDK_INT < 22) {
                try {
                    return SSLContext.getInstance("TLSv1.2")
                } catch (e: NoSuchAlgorithmException) {
                    // fallback to TLS
                }
            }
            return SSLContext.getInstance("TLS")
        }

    fun trustAllHttps() {
        HttpsURLConnection.setDefaultHostnameVerifier { _: String?, _: SSLSession? -> true }
        HttpsURLConnection.setDefaultSSLSocketFactory(factory)
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return factory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return factory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return enableTLSOnSocket(factory.createSocket())
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return enableTLSOnSocket(factory.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return enableTLSOnSocket(factory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return enableTLSOnSocket(factory.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        return enableTLSOnSocket(factory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return enableTLSOnSocket(factory.createSocket(address, port, localAddress, localPort))
    }

    private fun enableTLSOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
        }
        return socket
    }

    companion object {
        var trustManagers: Array<TrustManager>? = null
    }

    init {
        if (trustManagers == null) {
            trustManagers = arrayOf(HttpsTrustManager())
        }

        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sslContext = sSLContext
        sslContext.init(null, trustManagers, SecureRandom())
        factory = sslContext.socketFactory
    }
}