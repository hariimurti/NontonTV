package net.harimurti.tv.extra;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import net.harimurti.tv.R;
import net.harimurti.tv.data.Playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

public class PlaylistHelper {
    private static final String TAG = "PlaylistHelper";
    private static final String PLAYLIST_JSON = "NontonTV.json";

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_CUSTOM = 1;
    public static final int MODE_LOCAL = 2;

    private final Context context;
    private final Preferences preferences;
    private final File cache, local;

    public PlaylistHelper(Context context) {
        this.context = context;
        preferences = new Preferences(context);
        cache = new File(context.getCacheDir(), PLAYLIST_JSON);
        local = new File(Environment.getExternalStorageDirectory(), PLAYLIST_JSON);
    }

    public int mode() {
        if (!preferences.useCustomPlaylist()) return MODE_DEFAULT;
        if (!preferences.getPlaylistExternal().isEmpty()) return MODE_CUSTOM;
        else return MODE_LOCAL;
    }

    public String getUrlPath() {
        if (mode() == MODE_CUSTOM) return preferences.getPlaylistExternal();
        else return context.getString(R.string.json_playlist);
    }

    public void writeCache(String content) {
        try {
            FileWriter fw = new FileWriter(cache.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        }
        catch (Exception e) {
            Log.e(TAG, String.format("Could not write %s", cache.getName()), e);
        }
    }

    private Playlist read(File file) {
        try {
            if (!file.exists()) throw new FileNotFoundException();

            FileReader fr = new FileReader(file.getAbsoluteFile());
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            return new Gson().fromJson(sb.toString(), Playlist.class);
        }
        catch (Exception e) {
            Log.e(TAG, String.format("Could not read %s", file.getName()), e);
            return null;
        }
    }

    public Playlist readCache() {
        return read(cache);
    }

    public Playlist readLocal() {
        return read(local);
    }
}
