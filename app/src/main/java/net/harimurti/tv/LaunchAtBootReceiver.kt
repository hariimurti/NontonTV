package net.harimurti.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.harimurti.tv.extra.Preferences

class LaunchAtBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = Preferences(context)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && preferences.launchAtBoot) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}