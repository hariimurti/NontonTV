package net.harimurti.tv.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.harimurti.tv.GridViewFragment;
import net.harimurti.tv.data.Category;
import net.harimurti.tv.data.Channel;
import net.harimurti.tv.data.Playlist;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private ArrayList<Category> categories;
    private ArrayList<Channel> channels;

    public ViewPagerAdapter(FragmentActivity activity, Playlist playlist) {
        super(activity);
        this.categories = playlist.categories;
        this.channels = playlist.channels;
    }

    @SuppressWarnings("all")
    @Override
    public Fragment createFragment(int position) {
        ArrayList<Channel> contents = new ArrayList<>();
        for (Channel channel : channels) {
            if (channel.cid == categories.get(position).id)
                contents.add(channel);
        }
        return GridViewFragment.newFragment(contents);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}
