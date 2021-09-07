package net.harimurti.tv

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.multidex.MultiDexApplication
import net.harimurti.tv.extra.TLSSocketFactory

class App: MultiDexApplication() {
    companion object {
        private lateinit var current: Application

        val context: Context
            get() = current.applicationContext

        val sslSocketFactory: TLSSocketFactory
            get() = TLSSocketFactory().trustAllHttps()

        fun runOnUiThread(task: Runnable) {
            Handler(Looper.getMainLooper()).post(task)
        }
    }

    override fun onCreate() {
        super.onCreate()
        current = this
    }
}