package net.harimurti.tv.extra;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.harimurti.tv.App;

import java.util.Calendar;
import java.util.Date;

public class Preferences {
    private static final String LAST_CHECK_UPDATE = "LAST_CHECK_UPDATE";

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
}
