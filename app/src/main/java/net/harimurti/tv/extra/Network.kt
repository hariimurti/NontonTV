package net.harimurti.tv.extra

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import net.harimurti.tv.App

@Suppress("DEPRECATION")
class Network {
    private val context = App.context
    fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.getNetworkCapabilities(cm.activeNetwork) != null
        } else {
            cm.activeNetworkInfo?.isConnected ?: false
        }
    }
}