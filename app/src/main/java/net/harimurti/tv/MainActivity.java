package net.harimurti.tv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.harimurti.tv.adapter.ViewPagerAdapter;
import net.harimurti.tv.data.Playlist;
import net.harimurti.tv.extra.RestClient;
import net.harimurti.tv.extra.RestClient.OnClientResult;

public class MainActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FragmentActivity fa = this;

        // define some view
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        RelativeLayout layoutMessage = findViewById(R.id.layout_status);
        SpinKitView spinKit = findViewById(R.id.spin_kit);
        TextView textMessage = findViewById(R.id.text_status);

        // create client & set listener -> getchannels
        RestClient client = new RestClient(this);
        client.setOnClientResult(new OnClientResult() {
            @Override
            public void onFailure(String status) {
                layoutMessage.setVisibility(View.VISIBLE);
                spinKit.setVisibility(View.INVISIBLE);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(status);
            }

            @Override
            public void onProgress(boolean status) {
                layoutMessage.setVisibility(status ? View.VISIBLE : View.GONE);
                spinKit.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);
            }

            @Override
            public void onSuccess(Playlist playlist) {
                viewPager.setAdapter(new ViewPagerAdapter(fa, playlist));
                new TabLayoutMediator(
                        tabLayout, viewPager, (tab, i) -> tab.setText(playlist.categories.get(i).name)
                ).attach();
            }
        }).GetChannels();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        this.finish();
    }
}
