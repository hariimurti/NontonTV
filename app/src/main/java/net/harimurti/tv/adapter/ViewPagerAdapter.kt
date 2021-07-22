package net.harimurti.tv.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.harimurti.tv.GridViewFragment
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.Playlist
import java.util.*

class ViewPagerAdapter(activity: FragmentActivity?, playlist: Playlist) :
    FragmentStateAdapter(activity!!) {
    private val categories: ArrayList<Category>? = playlist.categories
    private val channels: ArrayList<Channel>? = playlist.channels
    override fun createFragment(position: Int): Fragment {
        val contents = ArrayList<Channel?>()
        for (channel in channels!!) {
            if (channel.cid == categories!![position].id) contents.add(channel)
        }
        return GridViewFragment.newFragment(contents)
    }

    override fun getItemCount(): Int {
        return categories!!.size
    }
}