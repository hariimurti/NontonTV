package net.harimurti.tv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;
import net.harimurti.tv.extra.TLSSocketFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private View layoutStatus, layoutSpin, layoutText;
    private TextView tvStatus, tvRetry;

    private StringRequest playlist;
    private RequestQueue volley;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Preferences preferences = new Preferences();

        // define some view
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        playlist = new StringRequest(Request.Method.GET,
                getString(R.string.json_playlist),
                response -> {
                    try {
                        Playlist playlist = new Gson().fromJson(response, Playlist.class);
                        viewPager.setAdapter(new ViewPagerAdapter(this, playlist));
                        new TabLayoutMediator(
                                tabLayout, viewPager, (tab, i) -> tab.setText(playlist.categories.get(i).name)
                        ).attach();
                        ShowLayoutMessage(View.GONE, false);
                    } catch (JsonSyntaxException error) {
                        ShowErrorMessage(error.getMessage(), false);
                    }
                },
                error -> ShowErrorMessage(error.getMessage(), true));

        StringRequest update = new StringRequest(Request.Method.GET,
                getString(R.string.json_release),
                response -> {
                    try {
                        Release release = new Gson().fromJson(response, Release.class);
                        preferences.setLastCheckUpdate();

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
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.alert_new_update);
                        builder.setMessage(message.toString())
                                .setPositiveButton(R.string.alert_download, (dialog, id) ->
                                        DownloadFile(release.downloadUrl))
                                .setNegativeButton(R.string.alert_close, (dialog, id) ->
                                        dialog.cancel());
                        builder.create().show();
                    } catch (Exception ignore) {}
                }, null);

        BaseHttpStack stack = new HurlStack();
        if (Build.VERSION.SDK_INT == VERSION_CODES.KITKAT) {
            try {
                stack = new HurlStack(null, new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                Log.e("HttpStack", "Could not create new stack for TLS v1.2");
            }
        }
        volley = Volley.newRequestQueue(this, stack);
        volley.add(playlist);
        if (!preferences.isCheckedUpdate()) {
            volley.add(update);
        }
    }

    private void ShowLayoutMessage(int visibility, boolean isMessage) {
        layoutStatus.setVisibility(visibility);
        if (!isMessage) {
            layoutSpin.setVisibility(View.VISIBLE);
            layoutText.setVisibility(View.GONE);
        } else {
            layoutSpin.setVisibility(View.GONE);
            layoutText.setVisibility(View.VISIBLE);
        }
    }

    private void ShowErrorMessage(String error, boolean retry) {
        tvStatus.setText(error);
        tvRetry.setText(R.string.text_auto_retry);
        ShowLayoutMessage(View.VISIBLE, true);

        if (!retry) return;

        Network network = new Network(this);
        new AsyncSleep(this).task(new AsyncSleep.Task() {
            @Override
            public void onCountDown(int left) {
                if (!network.IsConnected()) {
                    tvStatus.setText(R.string.no_network);
                }
                if (left == 0) {
                    tvRetry.setText(R.string.text_auto_retry_now);
                }
                else {
                    tvRetry.setText(String.format(getString(R.string.text_auto_retry_second), left));
                }
            }
            @Override
            public void onFinish() {
                if (network.IsConnected()) {
                    volley.add(playlist);
                }
                else {
                    ShowErrorMessage(getString(R.string.no_network), true);
                }
            }
        }).start(5);
    }

    private void DownloadFile(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager manager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        this.finish();
    }
}
