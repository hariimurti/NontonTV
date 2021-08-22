package net.harimurti.tv

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDexApplication

class App: MultiDexApplication() {
    companion object {
        private lateinit var current: Application
        val context: Context
            get() = current.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        current = this
    }
}