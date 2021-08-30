package net.harimurti.tv.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.harimurti.tv.databinding.SettingAppFragmentBinding

class SettingAppFragment : Fragment() {
    companion object {
        var isLandscape = false
        var launchAtBoot = false
        var playLastWatched = false
        var sortFavorite = false
        var sortCategory = false
        var sortChannel = true
        var reverseNavigation = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SettingAppFragmentBinding.inflate(inflater, container, false)

        binding.forceLandscape.apply {
            isChecked = isLandscape
            setOnClickListener {
                isLandscape = isChecked
            }
        }

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
            }
        }

        binding.sortCategory.apply {
            isChecked = sortCategory
            setOnClickListener {
                sortCategory = isChecked
            }
        }

        binding.sortChannel.apply {
            isChecked = sortChannel
            setOnClickListener {
                sortChannel = isChecked
            }
        }

        binding.reverseNavigation.apply {
            isChecked = reverseNavigation
            setOnClickListener {
                reverseNavigation = isChecked
            }
        }

        return binding.root
    }
}