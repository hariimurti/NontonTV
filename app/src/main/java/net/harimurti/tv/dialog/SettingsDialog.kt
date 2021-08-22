package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.adapter.SourcesAdapter
import net.harimurti.tv.databinding.SettingsApplicationFragmentBinding
import net.harimurti.tv.databinding.SettingsDialogBinding
import net.harimurti.tv.databinding.SettingsSourcesFragmentBinding
import net.harimurti.tv.extra.PlaylistHelper
import net.harimurti.tv.extra.Preferences
import net.harimurti.tv.extra.isLinkUrl
import net.harimurti.tv.model.Source
import java.io.File

@Suppress("DEPRECATION")
class SettingsDialog : DialogFragment() {
    private var _binding : SettingsDialogBinding? = null
    private val binding get() = _binding!!
    private val tabFragment = arrayOf(SourcesFragment(), ApplicationFragment())
    private val tabTitle = arrayOf(R.string.tab_1, R.string.tab_2)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(activity, R.style.SettingsDialogThemeOverlay).apply {
            setTitle(R.string.settings)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsDialogBinding.inflate(inflater,container, false)
        val dialogView = binding.root
        val preferences = Preferences()

        // init
        ApplicationFragment.launchAtBoot = preferences.launchAtBoot
        ApplicationFragment.playLastWatched = preferences.playLastWatched
        ApplicationFragment.sortCategory = preferences.sortCategory
        ApplicationFragment.sortChannel = preferences.sortChannel
        SourcesFragment.sources = preferences.sources

        // view pager
        binding.settingViewPager.apply {
            adapter = FragmentAdapter(childFragmentManager)
        }
        // tab layout
        binding.settingTabLayout.apply {
            setupWithViewPager(binding.settingViewPager)
        }
        // button cancel
        binding.settingCancelButton.apply {
            setOnClickListener { dismiss() }
        }
        // button ok
        binding.settingOkButton.apply {
            setOnClickListener {
                //update sources
                preferences.sources = SourcesFragment.sources
                //save tab 2
                preferences.launchAtBoot = ApplicationFragment.launchAtBoot
                preferences.playLastWatched = ApplicationFragment.playLastWatched
                preferences.sortFavorite = ApplicationFragment.sortFavorite
                preferences.sortCategory = ApplicationFragment.sortCategory
                preferences.sortChannel = ApplicationFragment.sortChannel
                preferences.reverseNavigation = ApplicationFragment.reverseNavigation
                sendUpdatePlaylist(rootView.context)
                dismiss()
            }
        }
        return dialogView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun sendUpdatePlaylist(context: Context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(MainActivity.MAIN_CALLBACK)
                .putExtra(MainActivity.MAIN_CALLBACK, MainActivity.UPDATE_PLAYLIST))
    }

    private inner class FragmentAdapter(fragmentManager: FragmentManager?) :
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

    class ApplicationFragment : Fragment() {
        private var _binding: SettingsApplicationFragmentBinding? = null
        private val binding get() = _binding!!

        companion object {
            var launchAtBoot = false
            var playLastWatched = false
            var sortFavorite = false
            var sortCategory = false
            var sortChannel = true
            var reverseNavigation = false
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
            _binding = SettingsApplicationFragmentBinding.inflate(inflater, container, false)
            val rootView = binding.root

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

            return rootView
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }

    class SourcesFragment: Fragment() {
        private var _binding: SettingsSourcesFragmentBinding? = null
        private val binding get() = _binding!!
        private lateinit var filePicker: FilePickerDialog

        companion object {
            var sources: ArrayList<Source>? = null
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
            _binding = SettingsSourcesFragmentBinding.inflate(inflater, container, false)
            val rootView = binding.root

            val adapter = SourcesAdapter(sources)
            binding.sourcesAdapter = adapter
            binding.rvSources.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            val properties = DialogProperties().apply {
                extensions = arrayOf("json","m3u")
                selection_mode = DialogConfigs.MULTI_MODE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    root = File(Environment.getExternalStorageDirectory().path)
                    error_dir = File(Environment.getExternalStorageDirectory().path)
                    offset  = File(Environment.getExternalStorageDirectory().path)
                } else {
                    root = File("/")
                    offset  = File("/mnt/sdcard:/storage")
                }
            }

            filePicker = FilePickerDialog(rootView.context).apply {
                setTitle(getString(R.string.title_select_file_json))
                setProperties(properties)
                setDialogSelectionListener {
                    for (path in it){
                        adapter.addItem(Source().apply {
                            this.path = path
                            active = true
                        })
                    }
                    sources = adapter.getItems()
                }
            }

            binding.inputSource.setOnEditorActionListener { _, i, k ->
                if (i == EditorInfo.IME_ACTION_DONE || k.keyCode == KeyEvent.KEYCODE_ENTER) {
                    addLinkUrl()
                    true
                }
                else false
            }

            binding.btnAdd.setOnClickListener {
                addLinkUrl()
            }

            binding.btnPick.setOnClickListener {
                openFilePicker()
            }

            return rootView
        }

        private fun addLinkUrl() {
            val input = binding.inputSource.text.toString()
            if (input.isBlank()) return
            if (!input.isLinkUrl()) return

            binding.btnAdd.isEnabled = false
            binding.inputSource.isEnabled = false
            binding.inputSource.setText(R.string.checking_url)

            val adapter = binding.sourcesAdapter
            val source = Source().apply {
                path = input
                active = true
            }

            PlaylistHelper().task(source, object: PlaylistHelper.TaskChecker{
                override fun onCheckResult(result: Boolean) {
                    binding.btnAdd.isEnabled = true
                    binding.inputSource.text?.clear()
                    binding.inputSource.isEnabled =true
                    if (result) {
                        adapter?.addItem(source)
                        sources = adapter?.getItems()
                    }
                    else {
                        binding.inputSource.setText(input)
                        Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show()
                    }
                }
            }).checkResult()
        }

        private fun openFilePicker() {
            filePicker.show()
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray ) {
            when (requestCode) {
                FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        openFilePicker()
                    } else {
                        //Permission has not been granted. Notify the user.
                        Toast.makeText(context, getString(R.string.must_allow_permissions), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
}
