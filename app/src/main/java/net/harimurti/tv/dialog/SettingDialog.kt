package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.SettingDialogBinding
import net.harimurti.tv.extra.Preferences

class SettingDialog : DialogFragment() {
    private val tabFragment = arrayOf(SettingSourcesFragment(), SettingAppFragment(), SettingAboutFragment())
    private val tabTitle = arrayOf(R.string.tab_sources, R.string.tab_app, R.string.tab_about)

    inner class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = tabFragment.size
        override fun createFragment(position: Int): Fragment = tabFragment[position]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(activity, R.style.SettingsDialogThemeOverlay).apply {
            setTitle(R.string.settings)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SettingDialogBinding.inflate(inflater, container, false)
        val preferences = Preferences()

        // init
        SettingAppFragment.launchAtBoot = preferences.launchAtBoot
        SettingAppFragment.playLastWatched = preferences.playLastWatched
        SettingAppFragment.sortCategory = preferences.sortCategory
        SettingAppFragment.sortChannel = preferences.sortChannel
        SettingSourcesFragment.sources = preferences.sources

        // view pager
        binding.settingViewPager.adapter = PagerAdapter(this.requireActivity())
        // tab layout
        TabLayoutMediator(binding.settingTabLayout, binding.settingViewPager) {
            tab, position -> tab.text = getString(tabTitle[position])
        }.attach()
        // button cancel
        binding.settingCancelButton.apply {
            setOnClickListener { dismiss() }
        }
        // button ok
        binding.settingOkButton.apply {
            setOnClickListener {
                //update sources
                preferences.sources = SettingSourcesFragment.sources
                //save tab 2
                preferences.launchAtBoot = SettingAppFragment.launchAtBoot
                preferences.playLastWatched = SettingAppFragment.playLastWatched
                preferences.sortFavorite = SettingAppFragment.sortFavorite
                preferences.sortCategory = SettingAppFragment.sortCategory
                preferences.sortChannel = SettingAppFragment.sortChannel
                preferences.reverseNavigation = SettingAppFragment.reverseNavigation
                sendUpdatePlaylist(rootView.context)
                dismiss()
            }
        }

        return binding.root
    }

    private fun sendUpdatePlaylist(context: Context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(MainActivity.MAIN_CALLBACK)
                .putExtra(MainActivity.MAIN_CALLBACK, MainActivity.UPDATE_PLAYLIST))
    }
}
