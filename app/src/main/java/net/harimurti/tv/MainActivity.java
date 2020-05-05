package net.harimurti.tv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.harimurti.tv.adapter.ViewPagerAdapter;
import net.harimurti.tv.data.Playlist;
import net.harimurti.tv.data.Release;
import net.harimurti.tv.extra.JsonPlaylist;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;
import net.harimurti.tv.extra.TLSSocketFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View layoutSettings, layoutLoading;

    private static Playlist playlist, cachedPlaylist;
    private Preferences preferences;
    private StringRequest reqPlaylist;
    private RequestQueue request;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get preferences
        preferences = new Preferences();
        cachedPlaylist = new JsonPlaylist(this).read();

        // define some view
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutSettings = findViewById(R.id.layout_settings);
        layoutSettings.setOnClickListener(view -> layoutSettings.setVisibility(View.GONE));
        Switch swLaunch = findViewById(R.id.launch_at_boot);
        swLaunch.setChecked(preferences.isLaunchAtBoot());
        swLaunch.setOnClickListener(view -> preferences.setLaunchAtBoot(swLaunch.isChecked()));
        Switch swOpenLast = findViewById(R.id.open_last_watched);
        swOpenLast.setChecked(preferences.isOpenLastWatched());
        swOpenLast.setOnClickListener(view -> preferences.setOpenLastWatched(swOpenLast.isChecked()));

        reqPlaylist = new StringRequest(Request.Method.GET,
                getString(R.string.json_playlist),
                response -> {
                    try {
                        playlist = new Gson().fromJson(response, Playlist.class);
                        setPlaylistToViewPager();
                        new JsonPlaylist(this).write(response);
                    } catch (JsonSyntaxException error) {
                        showAlertError(error.getMessage());
                    }
                },
                error -> {
                    String message = getString(R.string.something_went_wrong);
                    if (error.networkResponse != null) {
                        int errorcode = error.networkResponse.statusCode;
                        if (400 <= errorcode && errorcode < 500)
                            message = String.format(getString(R.string.error_4xx), errorcode);
                        if (500 <= errorcode && errorcode < 600)
                            message = String.format(getString(R.string.error_5xx), errorcode);
                    }
                    else if (!Network.IsConnected()) {
                        message = getString(R.string.no_network);
                    }
                    showAlertError(message);
                });

        StringRequest reqUpdate = new StringRequest(Request.Method.GET,
                getString(R.string.json_release),
                response -> {
                    try {
                        Release release = new Gson().fromJson(response, Release.class);

                        if (release.versionCode <= BuildConfig.VERSION_CODE) return;

                        StringBuilder message = new StringBuilder(
                                String.format(getString(R.string.message_update),
                                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                                        release.versionName, release.versionCode));
                        for (String log : release.changelog) {
                            message.append(String.format(getString(R.string.message_update_changelog), log));
                        }
                        if (release.changelog.size() == 0) {
                            message.append(getString(R.string.message_update_no_changelog));
                        }
                        showAlertUpdate(message.toString(), release.downloadUrl);
                    } catch (Exception e) { Log.e("Volley", "Could not check new update!", e); }
                }, null);

        BaseHttpStack stack = new HurlStack();
        if (Build.VERSION.SDK_INT == VERSION_CODES.KITKAT) {
            try {
                stack = new HurlStack(null, new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                Log.e("Volley", "Could not create new stack for TLS v1.2!", e);
            }
        }

        request = Volley.newRequestQueue(this, stack);
        if (playlist == null) {
            request.add(reqPlaylist);
        }
        else {
            setPlaylistToViewPager();
        }
        if (!preferences.isCheckedUpdate()) {
            request.add(reqUpdate);
        }

        String streamUrl = preferences.getLastWatched();
        if (preferences.isOpenLastWatched() && !streamUrl.equals("") && PlayerActivity.isFirst) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("channel_url", streamUrl);
            this.startActivity(intent);
        }
    }

    private void setPlaylistToViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(this, playlist));
        new TabLayoutMediator(
                tabLayout, viewPager, (tab, i) -> tab.setText(playlist.categories.get(i).name)
        ).attach();
        layoutLoading.setVisibility(View.GONE);
    }

    private void showAlertError(String error) {
        String message = error == null ? getString(R.string.something_went_wrong) : error;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_title_playlist_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_retry, (dialog, id) -> request.add(reqPlaylist));
        if (cachedPlaylist != null) {
            alert.setNegativeButton(R.string.dialog_cached, (dialog, id) -> {
                playlist = cachedPlaylist;
                setPlaylistToViewPager();
            });
        }
        alert.create().show();
    }

    private void showAlertUpdate(String message, String fileUrl) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },1000);
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_new_update)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_download, (dialog, id) -> downloadFile(fileUrl))
                .setNegativeButton(R.string.dialog_skip, (dialog, id) -> preferences.setLastCheckUpdate());
        alert.create().show();
    }

    private void downloadFile(String url) {
        DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        if (dm == null) return;

        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment())
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setVisibleInDownloadsUi(true);

        try {
            dm.enqueue(request);
        }
        catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            layoutSettings.setVisibility(View.VISIBLE);
            return true;
        }
        else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutSettings.getVisibility() == View.VISIBLE) {
            layoutSettings.setVisibility(View.GONE);
        }
        else {
            super.onBackPressed();
            this.finish();
        }
    }
}
