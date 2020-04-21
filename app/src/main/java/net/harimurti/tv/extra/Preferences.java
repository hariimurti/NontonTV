package net.harimurti.tv.extra;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.harimurti.tv.App;

import java.util.Calendar;
import java.util.Date;

public class Preferences {
    private static final String LAST_CHECK_UPDATE = "LAST_CHECK_UPDATE";
    private static final String LAST_WATCHED = "LAST_WATCHED";
    private static final String OPEN_LAST_WATCHED = "OPEN_LAST_WATCHED";
    private static final String LAUNCH_AT_BOOT = "LAUNCH_AT_BOOT";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public Preferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
    }

    public void setLastCheckUpdate() {
        Calendar nextday = Calendar.getInstance();
        nextday.add(Calendar.DATE, 1);
        nextday.set(Calendar.HOUR_OF_DAY, 0);

        editor = preferences.edit();
        editor.putLong(LAST_CHECK_UPDATE, nextday.getTimeInMillis());
        editor.apply();
    }

    public boolean isCheckedUpdate() {
        try {
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(preferences.getLong(LAST_CHECK_UPDATE, 0));
            Date dateLast = last.getTime();

            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            Date dateNow = now.getTime();

            return dateLast.after(dateNow);
        }
        catch (Exception ignore) {
            return false;
        }
    }

    public void setLaunchAtBoot(boolean value) {
        editor = preferences.edit();
        editor.putBoolean(LAUNCH_AT_BOOT, value);
        editor.apply();
    }

    public boolean isLaunchAtBoot() {
        return preferences.getBoolean(LAUNCH_AT_BOOT, false);
    }

    public void setOpenLastWatched(boolean value) {
        editor = preferences.edit();
        editor.putBoolean(OPEN_LAST_WATCHED, value);
        editor.apply();
    }

    public boolean isOpenLastWatched() {
        return preferences.getBoolean(OPEN_LAST_WATCHED, false);
    }

    public void setLastWatched(String value) {
        editor = preferences.edit();
        editor.putString(LAST_WATCHED, value);
        editor.apply();
    }

    public String getLastWatched() {
        return  preferences.getString(LAST_WATCHED, "");
    }
}
