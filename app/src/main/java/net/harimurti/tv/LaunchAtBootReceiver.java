package net.harimurti.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.harimurti.tv.extra.Preferences;

public class LaunchAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences preferences = new Preferences();
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) && preferences.isLaunchAtBoot()) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
