package net.harimurti.tv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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
    private boolean doubleBackToExitPressedOnce;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View layoutSettings, layoutLoading;

    private static Playlist playlist, cachedPlaylist;
    private static boolean isFirst = true;
    private Preferences preferences;
    private StringRequest reqPlaylist;
    private RequestQueue request;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            setTheme(R.style.AppThemeTv);
        }
        setContentView(R.layout.activity_main);

        // get preferences
        preferences = new Preferences(this);
        cachedPlaylist = new JsonPlaylist(this).read();

        // define some view
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutSettings = findViewById(R.id.layout_settings);
        layoutSettings.setOnClickListener(view -> layoutSettings.setVisibility(View.GONE));
        SwitchCompat swLaunch = findViewById(R.id.launch_at_boot);
        swLaunch.setChecked(preferences.isLaunchAtBoot());
        swLaunch.setOnClickListener(view -> preferences.setLaunchAtBoot(swLaunch.isChecked()));
        SwitchCompat swOpenLast = findViewById(R.id.open_last_watched);
        swOpenLast.setChecked(preferences.isOpenLastWatched());
        swOpenLast.setOnClickListener(view -> preferences.setOpenLastWatched(swOpenLast.isChecked()));
        AppCompatButton btnReload = findViewById(R.id.reload_playlist);
        btnReload.setOnClickListener(view -> queueRequest(reqPlaylist));

        // volley library
        BaseHttpStack stack = new HurlStack();
        if (Build.VERSION.SDK_INT == VERSION_CODES.KITKAT) {
            try {
                stack = new HurlStack(null, new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                Log.e("Volley", "Could not create new stack for TLS v1.2!", e);
            }
        }
        request = Volley.newRequestQueue(this, stack);
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
                    else if (!Network.IsConnected(this)) {
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

        if (playlist == null) {
            queueRequest(reqPlaylist);
        }
        else {
            setPlaylistToViewPager();
        }
        if (!preferences.isCheckedUpdate()) {
            queueRequest(reqUpdate);
        }

        String streamUrl = preferences.getLastWatched();
        if (preferences.isOpenLastWatched() && !streamUrl.equals("") && PlayerActivity.isFirst) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("channel_url", streamUrl);
            this.startActivity(intent);
        }
    }

    private void queueRequest(StringRequest strReq) {
        request.getCache().clear();
        request.add(strReq);
    }

    private void setPlaylistToViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(this, playlist));
        new TabLayoutMediator(
                tabLayout, viewPager, (tab, i) -> tab.setText(playlist.categories.get(i).name)
        ).attach();
        layoutLoading.setVisibility(View.GONE);
        if (!isFirst) Toast.makeText(this, R.string.playlist_updated, Toast.LENGTH_SHORT).show();
        isFirst = false;
    }

    private void showAlertError(String error) {
        String message = error == null ? getString(R.string.something_went_wrong) : error;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_title_playlist_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_retry, (dialog, id) -> queueRequest(reqPlaylist));
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
        try {
            Uri uri = Uri.parse(url);
            DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(this, R.string.no_donnload_manager, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment())
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setVisibleInDownloadsUi(true);
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
            return;
        }

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            this.finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_back_twice_exit_app), Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
}
