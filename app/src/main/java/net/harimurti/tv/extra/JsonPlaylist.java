package net.harimurti.tv.extra;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import net.harimurti.tv.data.Playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

public class JsonPlaylist {
    private static final String TAG = "JsonPlaylist";
    private static final String PLAYLIST_JSON = "playlist.json";
    private File file;

    public JsonPlaylist(Context context) {
        file = new File(context.getFilesDir(), PLAYLIST_JSON);
    }

    public boolean write(String content) {
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Could not write cache.", e);
            return false;
        }
    }

    public Playlist read() {
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
            Log.e(TAG, "Could not read cache.", e);
            return null;
        }
    }
}
