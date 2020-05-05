package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
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

import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.Preferences;

public class PlayerActivity extends AppCompatActivity {
    public static boolean isFirst = true;
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

        // hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // define some view
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        // get channel_url
        String url = getIntent().getStringExtra("channel_url");
        Uri uri = Uri.parse(url);

        // prepare player user-agent
        String playerAgent = Util.getUserAgent(this, "ExoPlayer2");
        DataSource.Factory factory = new DefaultDataSourceFactory(this, playerAgent);

        // define mediasource
        int contentType = Util.inferContentType(uri);
        mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(uri);
        if (contentType == C.TYPE_HLS)
            mediaSource = new HlsMediaSource.Factory(factory).createMediaSource(uri);
        if (contentType == C.TYPE_DASH)
            mediaSource = new DashMediaSource.Factory(factory).createMediaSource(uri);
        if (contentType == C.TYPE_SS)
            mediaSource = new SsMediaSource.Factory(factory).createMediaSource(uri);

        // create player & set listener
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    ShowLayoutMessage(View.GONE, false);
                    preferences.setLastWatched(url);
                }
                if (playbackState == Player.STATE_BUFFERING) {
                    ShowLayoutMessage(View.VISIBLE, false);
                }
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    ShowLayoutMessage(View.VISIBLE, true);
                    tvStatus.setText(R.string.source_offline);
                    tvRetry.setText(R.string.text_auto_retry);
                    RetryPlaying();
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
        player.prepare(mediaSource);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
                    player.prepare(mediaSource);
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
        super.onBackPressed();
        this.finish();
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