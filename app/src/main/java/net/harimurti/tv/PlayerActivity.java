package net.harimurti.tv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import net.harimurti.tv.data.License;
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity {
    public static boolean isFirst = true;
    private static boolean skipRetry = false;
    private boolean doubleBackToExitPressedOnce;
    private Preferences preferences;
    private String channelUrl;
    private SimpleExoPlayer player;
    private MediaItem mediaItem;
    private DefaultTrackSelector trackSelector;
    private TrackGroupArray lastSeenTrackGroupArray;
    private View layoutStatus, layoutSpin, layoutText, trackButton;
    private TextView tvStatus, tvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        isFirst = false;
        preferences = new Preferences(this);

        // define some view
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);
        trackButton = findViewById(R.id.player_settings);
        trackButton.setOnClickListener(view -> showTrackSelector());

        // get channel_url
        channelUrl = getIntent().getStringExtra("channel_url");
        if (channelUrl.isEmpty()) {
            Toast.makeText(this, R.string.player_no_channel_url, Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        // get drm license
        String drmLicense = "";
        for (License license: MainActivity.playlist.licenses) {
            if (license.drm_url.isEmpty()) continue;
            if (license.domain.isEmpty() || channelUrl.contains(license.domain)) {
                drmLicense = license.drm_url;
                break;
            }
        }

        // define mediasource
        if (!drmLicense.isEmpty()) {
            mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(channelUrl))
                    .setDrmUuid(C.WIDEVINE_UUID)
                    .setDrmLicenseUri(drmLicense)
                    .setDrmMultiSession(true)
                    .build();
        }
        else {
            mediaItem = MediaItem.fromUri(Uri.parse(channelUrl));
        }

        // define User-Agent
        List<String> userAgents = Arrays.asList(getResources().getStringArray(R.array.user_agent));
        String userAgent = userAgents.get(new Random().nextInt(userAgents.size()));
        for (String ua: userAgents) {
            if (channelUrl.contains(ua.substring(0, ua.indexOf("/")).toLowerCase()))
                userAgent = ua;
        }
        // create player & set listener
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(userAgent);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, httpDataSourceFactory);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(new DefaultTrackSelector.ParametersBuilder(this).build());
        player = new SimpleExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector)
                .build();
        player.addListener(new playerListener());

        // set player view
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playerView.requestFocus();

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

    private class playerListener implements Player.Listener {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
                showLayoutMessage(View.GONE, false);
                preferences.setLastWatched(channelUrl);
            }
            else if (state == Player.STATE_BUFFERING) {
                showLayoutMessage(View.VISIBLE, false);
            }
            trackButton.setEnabled(TrackSelectionDialog.willHaveContent(trackSelector));
        }

        @Override
        public void onPlayerError(@NonNull ExoPlaybackException error) {
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                tvStatus.setText(R.string.source_offline);
            } else {
                tvStatus.setText(R.string.something_went_wrong);
            }
            tvRetry.setText(R.string.text_auto_retry);
            showLayoutMessage(View.VISIBLE, true);
            retryPlayback();
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {
            if (trackGroups != lastSeenTrackGroupArray) {
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_unsupported_video), Toast.LENGTH_LONG).show();
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_unsupported_audio), Toast.LENGTH_LONG).show();
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
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

    private void showTrackSelector() {
        TrackSelectionDialog.createForTrackSelector(trackSelector, dismissedDialog -> {})
                .show(getSupportFragmentManager(), null);
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showTrackSelector();
            return true;
        }
        else {
            return super.onKeyUp(keyCode, event);
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