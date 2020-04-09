package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

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
import net.harimurti.tv.extra.AsyncSleep;
import net.harimurti.tv.extra.Network;
import net.harimurti.tv.extra.TLSSocketFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private View layoutStatus, layoutSpin, layoutText;
    private TextView tvStatus, tvRetry;

    private StringRequest request;
    private RequestQueue volley;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // define some view
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        layoutStatus = findViewById(R.id.layout_status);
        layoutSpin = findViewById(R.id.layout_spin);
        layoutText = findViewById(R.id.layout_text);
        tvStatus = findViewById(R.id.text_status);
        tvRetry = findViewById(R.id.text_retry);

        request = new StringRequest(Request.Method.GET,
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

        BaseHttpStack stack = new HurlStack();
        if (Build.VERSION.SDK_INT == VERSION_CODES.KITKAT) {
            try {
                stack = new HurlStack(null, new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                Log.e("Volley", "Could not create new stack for TLS v1.2");
            }
        }
        volley = Volley.newRequestQueue(this, stack);
        volley.add(request);
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
                    volley.add(request);
                }
                else {
                    ShowErrorMessage(getString(R.string.no_network), true);
                }
            }
        }).start(5);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        this.finish();
    }
}
