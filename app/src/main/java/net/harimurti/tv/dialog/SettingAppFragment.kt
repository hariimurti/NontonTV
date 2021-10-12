package net.harimurti.tv.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.harimurti.tv.databinding.SettingAppFragmentBinding
import net.harimurti.tv.extra.Preferences

class SettingAppFragment : Fragment() {
    companion object {
        var launchAtBoot = false
        var playLastWatched = false
        var sortFavorite = false
        var sortCategory = false
        var sortChannel = true
        var optimizePrebuffer = true
        var reverseNavigation = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SettingAppFragmentBinding.inflate(inflater, container, false)

        binding.launchAtBoot.apply {
            isChecked = launchAtBoot
            setOnClickListener {
                launchAtBoot = isChecked
            }
        }

        binding.openLastWatched.apply {
            isChecked = playLastWatched
            setOnClickListener {
                playLastWatched = isChecked
            }
        }

        binding.sortFavorite.apply {
            isChecked = sortFavorite
            setOnClickListener {
                sortFavorite = isChecked
                SettingDialog.isSourcesChanged = true
            }
        }

        binding.sortCategory.apply {
            isChecked = sortCategory
            setOnClickListener {
                sortCategory = isChecked
                SettingDialog.isSourcesChanged = true
            }
        }

        binding.sortChannel.apply {
            isChecked = sortChannel
            setOnClickListener {
                sortChannel = isChecked
                SettingDialog.isSourcesChanged = true
            }
        }

        binding.optimizePrebuffer.apply {
            isChecked = optimizePrebuffer
            setOnClickListener {
                optimizePrebuffer = isChecked
            }
        }

        binding.reverseNavigation.apply {
            isChecked = reverseNavigation
            setOnClickListener {
                reverseNavigation = isChecked
            }
        }

        binding.useProxy.apply {
            isChecked = Preferences().useProxy
            setOnClickListener {
                Preferences().useProxy = isChecked
                if(this.isChecked)
                ProxyDialog().show(childFragmentManager.beginTransaction(),null)
            }
        }

        return binding.root
    }
}