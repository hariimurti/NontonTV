package net.harimurti.tv.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.harimurti.tv.GridViewFragment
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Playlist

class ViewPagerAdapter(activity: FragmentActivity?, playlist: Playlist) : FragmentStateAdapter(activity!!) {
    private val categories: ArrayList<Category>? = playlist.categories

    override fun createFragment(position: Int): Fragment {
        return GridViewFragment.newFragment(categories?.get(position)?.channels)
    }

    override fun getItemCount(): Int {
        return categories!!.size
    }
}