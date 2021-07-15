package net.harimurti.tv.extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;

public class Preferences {
    private static final String LAST_CHECK_UPDATE = "LAST_CHECK_UPDATE";
    private static final String LAST_WATCHED = "LAST_WATCHED";
    private static final String OPEN_LAST_WATCHED = "OPEN_LAST_WATCHED";
    private static final String LAUNCH_AT_BOOT = "LAUNCH_AT_BOOT";
    private static final String USE_CUSTOM_PLAYLIST = "USE_CUSTOM_PLAYLIST";
    private static final String PLAYLIST_EXTERNAL = "PLAYLIST_EXTERNAL";
    private static final String LAST_VERSIONCODE = "LAST_VERSIONCODE";
    private static final String TOTAL_CONTRIBUTORS = "TOTAL_CONTRIBUTORS";
    private static final String SHOW_LESS_CONTRIBUTORS = "SHOW_LESS_CONTRIBUTORS";

    private final SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public Preferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setLastCheckUpdate() {
        Calendar nextday = Calendar.getInstance();
        nextday.add(Calendar.DATE, 1);
        nextday.set(Calendar.HOUR_OF_DAY, 0);

        editor = preferences.edit();
        editor.putLong(LAST_CHECK_UPDATE, nextday.getTimeInMillis());
        editor.apply();
    }

    public boolean isCheckedReleaseUpdate() {
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

    public void setUseCustomPlaylist(boolean value) {
        editor = preferences.edit();
        editor.putBoolean(USE_CUSTOM_PLAYLIST, value);
        editor.apply();
    }

    public boolean useCustomPlaylist() {
        return preferences.getBoolean(USE_CUSTOM_PLAYLIST, false);
    }

    public void setPlaylistExternal(String value) {
        editor = preferences.edit();
        editor.putString(PLAYLIST_EXTERNAL, value);
        editor.apply();
    }

    public String getPlaylistExternal() {
        return  preferences.getString(PLAYLIST_EXTERNAL, "");
    }

    public void setLastVersionCode(int value) {
        editor = preferences.edit();
        editor.putInt(LAST_VERSIONCODE, value);
        editor.putInt(TOTAL_CONTRIBUTORS, 0);
        editor.apply();
    }

    public int getLastVersionCode() {
        return preferences.getInt(LAST_VERSIONCODE, 0);
    }

    public void setShowLessContributors() {
        editor = preferences.edit();
        editor.putBoolean(SHOW_LESS_CONTRIBUTORS, true);
        editor.apply();
    }

    public boolean isShowLessContributors() {
        return preferences.getBoolean(SHOW_LESS_CONTRIBUTORS, false);
    }

    public void setTotalContributors(int value) {
        editor = preferences.edit();
        editor.putInt(TOTAL_CONTRIBUTORS, value);
        editor.apply();
    }

    public int getTotalContributors() {
        return preferences.getInt(TOTAL_CONTRIBUTORS, 0);
    }
}
