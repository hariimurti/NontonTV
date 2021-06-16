package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import net.harimurti.tv.data.License;
import net.harimurti.tv.data.Playlist;
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.JsonPlaylist;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;

import java.util.Objects;

public class PlayerActivity extends AppCompatActivity {
    public static boolean isFirst = true;
    private boolean doubleBackToExitPressedOnce;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;
    private View layoutStatus, layoutSpin, layoutText;
    private TextView tvStatus, tvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        isFirst = false;
        Preferences preferences = new Preferences();

        // define some view
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        // get channel_url
        String url = getIntent().getStringExtra("channel_url");
        Uri uri = Uri.parse(url);

        // get drm license
        String drmLicense = "";
        Playlist playlist = new JsonPlaylist(this).read();
        try {
            for (License license: playlist.licenses) {
                if (license.domain.isEmpty() || Objects.requireNonNull(url).contains(license.domain)) {
                    drmLicense = license.drm_url;
                    break;
                }
            }
        }
        catch (Exception e) {
            Log.e("PLAYER", "Can't find a license.", e);
        }

        // prepare player user-agent
        String playerAgent = Util.getUserAgent(this, "ExoPlayer2");
        DataSource.Factory factory = new DefaultDataSourceFactory(this, playerAgent);
        MediaItem mediaItem = MediaItem.fromUri(uri);

        // define mediasource
        int contentType = Util.inferContentType(uri);
        if (contentType == C.TYPE_HLS) {
            mediaSource = new HlsMediaSource.Factory(factory).createMediaSource(mediaItem);
        }
        else if (contentType == C.TYPE_DASH) {
            if (!drmLicense.isEmpty()) {
                mediaItem = new MediaItem.Builder()
                        .setUri(uri)
                        .setDrmUuid(C.WIDEVINE_UUID)
                        .setDrmLicenseUri(drmLicense)
                        .setDrmMultiSession(true)
                        .build();
            }
            mediaSource = new DashMediaSource.Factory(factory).createMediaSource(mediaItem);
        }
        else if (contentType == C.TYPE_SS) {
            mediaSource = new SsMediaSource.Factory(factory).createMediaSource(mediaItem);
        }
        else {
            mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
        }

        // create player & set listener
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                int playbackState = player.getPlaybackState();
                if (isPlaying || playbackState == Player.STATE_READY) {
                    ShowLayoutMessage(View.GONE, false);
                    preferences.setLastWatched(url);
                }
                else {
                    if (playbackState == Player.STATE_BUFFERING) {
                        ShowLayoutMessage(View.VISIBLE, false);
                    } else {
                        ShowLayoutMessage(View.VISIBLE, true);
                        tvStatus.setText(R.string.source_offline);
                        tvRetry.setText(R.string.text_auto_retry);
                        RetryPlaying();
                    }
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                ShowLayoutMessage(View.VISIBLE, true);
                if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                    tvStatus.setText(R.string.source_offline);
                    tvRetry.setText(R.string.text_auto_retry);
                    tvRetry.setVisibility(View.VISIBLE);
                    RetryPlaying();
                } else {
                    tvStatus.setText(R.string.something_went_wrong);
                    tvRetry.setVisibility(View.GONE);
                }
            }
        });

        // set player view
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        // play mediasouce
        player.setMediaSource(mediaSource);
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

    private void RetryPlaying() {
        new AsyncSleep(this).task(new AsyncSleep.Task() {
            @Override
            public void onCountDown(int left) {
                if (!Network.IsConnected()) {
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
                if (Network.IsConnected()) {
                    player.setMediaSource(mediaSource);
                    player.prepare();
                }
                else {
                    RetryPlaying();
                }
            }
        }).start(5);
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

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            this.finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_back_to_exit), Toast.LENGTH_SHORT).show();

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