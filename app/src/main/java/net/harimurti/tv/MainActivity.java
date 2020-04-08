package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.harimurti.tv.adapter.ViewPagerAdapter;
import net.harimurti.tv.data.Playlist;
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.RestClient;
import net.harimurti.tv.extra.RestClient.OnClientResult;

public class MainActivity extends AppCompatActivity {
    private RestClient client;
    private View layoutStatus, layoutSpin, layoutText;
    private TextView tvStatus, tvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FragmentActivity fa = this;

        // define some view
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        // create client & set listener -> getchannels
        client = new RestClient(this);
        client.setOnClientResult(new OnClientResult() {
            @Override
            public void onFailure(String status) {
                runOnUiThread(() -> {
                    ShowLayoutMessage(View.VISIBLE, true);
                    tvStatus.setText(status);

                    // try fetching later
                    RetryGetChannels();
                });
            }

            @Override
            public void onProgress(boolean status) {
                runOnUiThread(() -> {
                    ShowLayoutMessage(status ? View.VISIBLE : View.GONE, false);
                });
            }

            @Override
            public void onSuccess(Playlist playlist) {
                runOnUiThread(() -> {
                    viewPager.setAdapter(new ViewPagerAdapter(fa, playlist));
                    new TabLayoutMediator(
                            tabLayout, viewPager, (tab, i) -> tab.setText(playlist.categories.get(i).name)
                    ).attach();
                });
            }
        });
        client.GetChannels();
    }

    private void RetryGetChannels() {
        Network network = new Network(this);
        new AsyncSleep(this).task(new AsyncSleep.Task() {
            @Override
            public void onCountDown(int left) {
                if (!network.IsConnected())
                    tvStatus.setText(getString(R.string.no_network));

                tvRetry.setText(String.format(getString(R.string.retry_time), left));
            }
            @Override
            public void onFinish() {
                if (network.IsConnected())
                    client.GetChannels();
                else
                    RetryGetChannels();
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
        //super.onBackPressed();
        this.finish();
    }
}
