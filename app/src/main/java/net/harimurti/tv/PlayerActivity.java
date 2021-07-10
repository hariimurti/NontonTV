package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.NonNullApi;

import net.harimurti.tv.data.License;
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;

public class PlayerActivity extends AppCompatActivity {
    public static boolean isFirst = true;
    private static boolean skipRetry = false;
    private boolean doubleBackToExitPressedOnce;
    private SimpleExoPlayer player;
    private MediaItem mediaItem;
    private View layoutStatus, layoutSpin, layoutText;
    private TextView tvStatus, tvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        isFirst = false;
        Preferences preferences = new Preferences(this);

        // define some view
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        // get channel_url
        String url = getIntent().getStringExtra("channel_url");
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.player_no_channel_url, Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        Uri uri = Uri.parse(url);

        // get drm license
        String drmLicense = "";
        for (License license: MainActivity.playlist.licenses) {
            if (license.drm_url.isEmpty()) continue;
            if (license.domain.isEmpty() || url.contains(license.domain)) {
                drmLicense = license.drm_url;
                break;
            }
        }

        // define mediasource
        if (!drmLicense.isEmpty()) {
            mediaItem = new MediaItem.Builder()
                    .setUri(uri)
                    .setDrmUuid(C.WIDEVINE_UUID)
                    .setDrmLicenseUri(drmLicense)
                    .setDrmMultiSession(true)
                    .build();
        }
        else {
            mediaItem = MediaItem.fromUri(uri);
        }

        // create player & set listener
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    showLayoutMessage(View.GONE, false);
                    preferences.setLastWatched(url);
                }
                else if (state == Player.STATE_BUFFERING) {
                    showLayoutMessage(View.VISIBLE, false);
                }
            }

            @Override @NonNullApi
            public void onPlayerError(ExoPlaybackException error) {
                if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                    tvStatus.setText(R.string.source_offline);
                } else {
                    tvStatus.setText(R.string.something_went_wrong);
                }
                tvRetry.setText(R.string.text_auto_retry);
                showLayoutMessage(View.VISIBLE, true);
                retryPlayback();
            }
        });

        // set player view
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        // play mediasouce
        player.setMediaItem(mediaItem);
        player.prepare();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) return;
            controller.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void retryPlayback() {
        if (skipRetry) return;
        skipRetry = true;
        new AsyncSleep(this).task(new AsyncSleep.Task() {
            @Override
            public void onCountDown(int left) {
                left--;
                if (!Network.IsConnected(getApplicationContext())) {
                    tvStatus.setText(R.string.no_network);
                }
                if (left <= 0) {
                    tvRetry.setText(R.string.text_auto_retry_now);
                }
                else {
                    tvRetry.setText(String.format(getString(R.string.text_auto_retry_second), left));
                }
            }
            @Override
            public void onFinish() {
                skipRetry = false;
                if (Network.IsConnected(getApplicationContext())) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                }
                else {
                    retryPlayback();
                }
            }
        }).start(6);
    }

    private void showLayoutMessage(int visibility, boolean isMessage) {
        layoutStatus.setVisibility(visibility);
        if (!isMessage) {
            layoutSpin.setVisibility(View.VISIBLE);
            layoutText.setVisibility(View.GONE);
        } else {
            layoutSpin.setVisibility(View.GONE);
            layoutText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            this.finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_back_twice_exit_player), Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.setPlayWhenReady(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.release();
    }
}