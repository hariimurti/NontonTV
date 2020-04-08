package net.harimurti.tv.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.harimurti.tv.MainFragment;
import net.harimurti.tv.data.Category;
import net.harimurti.tv.data.Channel;
import net.harimurti.tv.data.Playlist;

import java.util.ArrayList;

@SuppressWarnings("all")
public class ViewPagerAdapter extends FragmentStateAdapter {
    private static ArrayList<Category> categories;
    private static ArrayList<Channel> channels;

    public ViewPagerAdapter(FragmentActivity activity, Playlist playlist) {
        super(activity);
        this.categories = playlist.categories;
        this.channels = playlist.channels;
    }

    @Override
    public Fragment createFragment(int position) {
        int cid = categories.get(position).id;
        ArrayList<Channel> contents = new ArrayList<>();
        for (Channel channel : channels) {
            if (channel.cid == cid)
                contents.add(channel);
        }
        return MainFragment.newInstance(contents);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}
