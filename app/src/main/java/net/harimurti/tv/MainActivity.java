package net.harimurti.tv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
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
import net.harimurti.tv.data.GithubUser;
import net.harimurti.tv.data.Playlist;
import net.harimurti.tv.data.Release;
import net.harimurti.tv.extra.PlaylistHelper;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;
import net.harimurti.tv.extra.TLSSocketFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private boolean doubleBackToExitPressedOnce;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View layoutSettings, layoutLoading, layoutCustom;
    private SwitchCompat swCustomPlaylist;

    public static Playlist playlist;
    private Preferences preferences;
    private PlaylistHelper playlistHelper;
    private RequestQueue volley;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            setTheme(R.style.AppThemeTv);
        }
        setContentView(R.layout.activity_main);
        askPermissions();

        preferences = new Preferences(this);
        playlistHelper = new PlaylistHelper(this);

        // define some view
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        layoutLoading = findViewById(R.id.layout_loading);
        // layout settings
        layoutSettings = findViewById(R.id.layout_settings);
        layoutSettings.setOnClickListener(view -> layoutSettings.setVisibility(View.GONE));
        // switch launch at boot
        SwitchCompat swLaunch = findViewById(R.id.launch_at_boot);
        swLaunch.setChecked(preferences.isLaunchAtBoot());
        swLaunch.setOnClickListener(view -> preferences.setLaunchAtBoot(swLaunch.isChecked()));
        // switch play last watched
        SwitchCompat swOpenLast = findViewById(R.id.open_last_watched);
        swOpenLast.setChecked(preferences.isOpenLastWatched());
        swOpenLast.setOnClickListener(view -> preferences.setOpenLastWatched(swOpenLast.isChecked()));
        // layout custom playlist
        layoutCustom = findViewById(R.id.layout_custom_playlist);
        layoutCustom.setVisibility(preferences.useCustomPlaylist() ? View.VISIBLE : View.GONE);
        // switch custom playlist
        swCustomPlaylist = findViewById(R.id.use_custom_playlist);
        swCustomPlaylist.setChecked(preferences.useCustomPlaylist());
        swCustomPlaylist.setOnClickListener(view -> {
            layoutCustom.setVisibility(swCustomPlaylist.isChecked() ? View.VISIBLE : View.GONE);
            preferences.setUseCustomPlaylist(swCustomPlaylist.isChecked());
        });
        // edittext custom playlist
        EditText txtCustom = findViewById(R.id.custom_playlist);
        txtCustom.setText(preferences.getPlaylistExternal());
        txtCustom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                preferences.setPlaylistExternal(s.toString());
            }
        });
        // button reload playlist
        findViewById(R.id.reload_playlist).setOnClickListener(view -> updatePlaylist());

        // volley library
        BaseHttpStack stack = new HurlStack();
        try {
            TLSSocketFactory factory = new TLSSocketFactory();
            factory.trustAllHttps();
            stack = new HurlStack(null, factory);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Log.e("MainApp", "Could not trust all HTTPS connection!", e);
        } finally {
            volley = Volley.newRequestQueue(this, stack);
        }

        // playlist update
        if (playlist == null) {
            updatePlaylist();
        }
        else {
            setPlaylistToViewPager(playlist);
        }

        // check new release
        if (!preferences.isCheckedReleaseUpdate()) {
            checkNewRelease();
        }

        // get contributors
        if (preferences.getLastVersionCode() != BuildConfig.VERSION_CODE || !preferences.isShowLessContributors()) {
            preferences.setLastVersionCode(BuildConfig.VERSION_CODE);
            getContributors();
        }

        // launch player if openlastwatched is true
        String streamUrl = preferences.getLastWatched();
        if (preferences.isOpenLastWatched() && !streamUrl.equals("") && PlayerActivity.isFirst) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("channel_url", streamUrl);
            this.startActivity(intent);
        }
    }

    private void checkNewRelease() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
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
        volley.getCache().clear();
        volley.add(stringRequest);
    }

    private void getContributors() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                getString(R.string.gh_contributors),
                response -> {
                    try {
                        GithubUser[] users = new Gson().fromJson(response, GithubUser[].class);
                        StringBuilder message = new StringBuilder(getString(R.string.message_thanks_to));
                        for (GithubUser user : users) {
                            message.append(user.login).append(", ");
                        }
                        if (preferences.getTotalContributors() < users.length && 0 < users.length) {
                            preferences.setTotalContributors(users.length);
                            showAlertContributors(message.substring(0, message.length() - 2));
                        }
                    } catch (Exception e) { Log.e("Volley", "Could not get contributors!", e); }
                }, null);
        volley.getCache().clear();
        volley.add(stringRequest);
    }

    private void updatePlaylist() {
        // from local storage
        if (playlistHelper.mode() == PlaylistHelper.MODE_LOCAL) {
            Playlist local = playlistHelper.readLocal();
            if (local == null) {
                showAlertLocalError();
                return;
            }
            setPlaylistToViewPager(local);
            return;
        }

        // from internet
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                playlistHelper.getUrlPath(),
                response -> {
                    try {
                        Playlist newPls = new Gson().fromJson(response, Playlist.class);
                        playlistHelper.writeCache(response);
                        setPlaylistToViewPager(newPls);
                    } catch (JsonSyntaxException error) {
                        showAlertPlaylistError(error.getMessage());
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
                    showAlertPlaylistError(message);
                });

        volley.getCache().clear();
        volley.add(stringRequest);
    }

    private void setPlaylistToViewPager(Playlist newPls) {
        viewPager.setAdapter(new ViewPagerAdapter(this, newPls));
        new TabLayoutMediator(
                tabLayout, viewPager, (tab, i) -> tab.setText(newPls.categories.get(i).name)
        ).attach();
        layoutLoading.setVisibility(View.GONE);
        if (playlist != newPls) Toast.makeText(this, R.string.playlist_updated, Toast.LENGTH_SHORT).show();
        playlist = newPls;
    }

    private void showAlertLocalError() {
        askPermissions();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_title_playlist_error)
                .setMessage(R.string.local_playlist_read_error)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_retry, (dialog, id) -> updatePlaylist())
                .setNegativeButton(getString(R.string.dialog_default), (dialog, id) -> {
                    preferences.setUseCustomPlaylist(false);
                    swCustomPlaylist.setChecked(false);
                    layoutCustom.setVisibility(View.GONE);
                    updatePlaylist();
                });
        alert.create().show();
    }

    private void showAlertPlaylistError(String error) {
        String message = error == null ? getString(R.string.something_went_wrong) : error;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_title_playlist_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_retry, (dialog, id) -> updatePlaylist());

        Playlist cache = playlistHelper.readCache();
        if (cache != null) {
            alert.setNegativeButton(R.string.dialog_cached, (dialog, id) -> setPlaylistToViewPager(cache));
        }
        alert.create().show();
    }

    private void showAlertUpdate(String message, String fileUrl) {
        askPermissions();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_new_update)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_download, (dialog, id) -> downloadFile(fileUrl))
                .setNegativeButton(R.string.dialog_skip, (dialog, id) -> preferences.setLastCheckUpdate());
        alert.create().show();
    }

    private void showAlertContributors(String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.alert_title_contributors)
                .setMessage(message)
                .setNeutralButton(R.string.dialog_telegram, (dialog, id) -> openWebsite(getString(R.string.telegram_group)))
                .setNegativeButton(R.string.dialog_website, (dialog, id) -> openWebsite(getString(R.string.website)))
                .setPositiveButton(preferences.isShowLessContributors() ? R.string.dialog_close : R.string.dialog_show_less,
                        (dialog, id) -> preferences.setShowLessContributors());
        AlertDialog dialog = alert.create();
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 10000);
    }

    private void openWebsite(String link) {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(link)));
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

    @TargetApi(23)
    protected void askPermissions() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.M) return;
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
            },1000);
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
