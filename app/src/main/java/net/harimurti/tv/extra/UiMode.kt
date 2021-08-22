package net.harimurti.tv.extra

import android.app.UiModeManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import net.harimurti.tv.App

class UiMode {
    private val context = App.context
    fun isTelevision() : Boolean {
        val manager = context.getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
        return manager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}