package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.SettingDialogBinding
import net.harimurti.tv.extra.Preferences

class SettingDialog : DialogFragment() {
    private val tabFragment = arrayOf(SettingSourcesFragment(), SettingAppFragment(), SettingAboutFragment())
    private val tabTitle = arrayOf(R.string.tab_sources, R.string.tab_app, R.string.tab_about)

    @Suppress("DEPRECATION")
    inner class FragmentAdapter(fragmentManager: FragmentManager?) :
        FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return tabFragment[position]
        }

        override fun getCount(): Int {
            return tabFragment.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return getString(tabTitle[position])
        }
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
        SettingSourcesFragment.isChanged = false

        // view pager
        binding.settingViewPager.adapter = FragmentAdapter(childFragmentManager)
        // tab layout
        binding.settingTabLayout.setupWithViewPager(binding.settingViewPager)
        // button cancel
        binding.settingCancelButton.apply {
            setOnClickListener { dismiss() }
        }
        // button ok
        binding.settingOkButton.apply {
            setOnClickListener {
                // playlist sources
                val sources = SettingSourcesFragment.sources
                if (sources?.filter { s -> s.active }?.size == 0) {
                    sources[0].active = true
                    Toast.makeText(context, R.string.warning_none_source_active, Toast.LENGTH_SHORT).show()
                }
                preferences.sources = sources
                if (SettingSourcesFragment.isChanged) sendUpdatePlaylist(rootView.context)
                // setting app
                preferences.launchAtBoot = SettingAppFragment.launchAtBoot
                preferences.playLastWatched = SettingAppFragment.playLastWatched
                preferences.sortFavorite = SettingAppFragment.sortFavorite
                preferences.sortCategory = SettingAppFragment.sortCategory
                preferences.sortChannel = SettingAppFragment.sortChannel
                preferences.reverseNavigation = SettingAppFragment.reverseNavigation
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
