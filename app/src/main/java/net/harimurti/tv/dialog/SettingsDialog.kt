package net.harimurti.tv.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.developer.filepicker.model.DialogProperties
import com.google.android.material.textfield.TextInputLayout
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.SettingsDialogBinding
import net.harimurti.tv.databinding.SettingsPlaylistFragmentBinding
import net.harimurti.tv.databinding.SettingsStartupFragmentBinding
import net.harimurti.tv.extra.PlaylistHelper
import net.harimurti.tv.extra.Preferences
import java.io.File


@Suppress("DEPRECATION")
class SettingsDialog : DialogFragment() {
    private var _binding : SettingsDialogBinding? = null
    private val binding get() = _binding!!
    private val tabFragment = arrayOf(
        StartupFragment(),
        PlaylistFragment()
    )
    private val tabTitle = arrayOf(
        R.string.tab_1,
        R.string.tab_2
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.SettingsDialogThemeOverlay)
        dialog.setTitle(R.string.settings)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsDialogBinding.inflate(inflater,container, false)
        val dialogView = binding.root
        val preferences = Preferences(dialogView.context)

        // init
        StartupFragment.launchAtBoot = preferences.launchAtBoot
        StartupFragment.playLastWatched = preferences.playLastWatched
        val useCustomPlaylist = preferences.useCustomPlaylist
        PlaylistFragment.useCustomPlaylist = useCustomPlaylist
        val mergePlaylist = preferences.mergePlaylist
        PlaylistFragment.mergePlaylist = mergePlaylist
        val radioPlaylist = preferences.radioPlaylist
        PlaylistFragment.radioPlaylist = radioPlaylist
        val playlistSelect = preferences.playlistSelect
        PlaylistFragment.playlistSelect = playlistSelect
        val playlistExternal = preferences.playlistExternal
        PlaylistFragment.playlistExternal = playlistExternal

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
            setOnClickListener {
                //revert tab 2 only
                preferences.useCustomPlaylist = useCustomPlaylist
                preferences.mergePlaylist = mergePlaylist
                preferences.radioPlaylist = radioPlaylist
                preferences.playlistSelect = playlistSelect
                preferences.playlistExternal = playlistExternal
                //update playlist
                sendUpdatePlaylist(rootView.context)
                dismiss()
            }
        }
        // button ok
        binding.settingOkButton.apply {
            setOnClickListener {
                //save tab 1
                preferences.launchAtBoot = StartupFragment.launchAtBoot
                preferences.playLastWatched = StartupFragment.playLastWatched
                //save tab 2
                preferences.useCustomPlaylist = PlaylistFragment.useCustomPlaylist
                preferences.mergePlaylist = PlaylistFragment.mergePlaylist
                preferences.radioPlaylist = PlaylistFragment.radioPlaylist
                preferences.playlistSelect = PlaylistFragment.playlistSelect
                preferences.playlistExternal = PlaylistFragment.playlistExternal
                //update playlist
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

    class StartupFragment : Fragment() {
        private var _binding: SettingsStartupFragmentBinding? = null
        private val binding get() = _binding!!

        companion object {
            var launchAtBoot = false
            var playLastWatched = false
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
            _binding = SettingsStartupFragmentBinding.inflate(inflater, container, false)
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

            return rootView
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }

    class PlaylistFragment : Fragment() {
        private var _binding: SettingsPlaylistFragmentBinding? = null
        private val binding get() = _binding!!
        private lateinit var dialog : FilePickerDialog

        companion object {
            var useCustomPlaylist = false
            var mergePlaylist = false
            var radioPlaylist = 0
            var playlistSelect = ""
            var playlistExternal = ""
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
            _binding = SettingsPlaylistFragmentBinding.inflate(inflater, container, false)
            val rootView = binding.root
            val preferences = Preferences(rootView.context)

            //init
            updateLayout(radioPlaylist)
            binding.customSelected.apply {
                check(binding.customSelected.getChildAt(radioPlaylist).id)
            }
            if(!useCustomPlaylist) binding.pickButton.visibility = View.GONE
            // layout custom playlist
            binding.layoutCustomPlaylist.apply {
                visibility = if (useCustomPlaylist) View.VISIBLE else View.GONE
            }
            // switch custom playlist
            binding.useCustomPlaylist.apply {
                isChecked = useCustomPlaylist
                setOnClickListener {
                    binding.layoutCustomPlaylist.visibility = if (isChecked) View.VISIBLE else View.GONE
                    useCustomPlaylist = isChecked
                    updateLayout(radioPlaylist)
                    if (!isChecked) {
                        binding.pickButton.visibility = View.GONE
                        mergePlaylist = false
                    }
                }
            }

            // RadioButton localPlaylist
            binding.localPlaylist.apply {
                setOnClickListener{
                    radioPlaylist = 0
                    updateLayout(radioPlaylist)
                    binding.reloadPlaylist.requestFocus()
                }
            }
            // RadioButton pickPlaylist
            binding.pickPlaylist.apply {
                setOnClickListener{
                    radioPlaylist = 1
                    updateLayout(radioPlaylist)
                    binding.pickButton.requestFocus()
                }
            }
            // RadioButton urlPlaylist
            binding.urlPlaylist.apply {
                setOnClickListener{
                    radioPlaylist = 2
                    updateLayout(radioPlaylist)
                    binding.customPlaylist.requestFocus()
                }
            }
            // button pick playlist
            binding.pickButton.apply {
                setOnClickListener {
                    openFilePicker()
                }
            }
            // edittext custom playlist
            binding.customPlaylist.apply {
                //setText(preferences.playlistExternal)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        val text = s.toString()
                        when (radioPlaylist) {
                            1 -> {
                                playlistSelect = text
                            }
                            2 -> {
                                playlistExternal = text
                            }
                        }
                    }
                })
            }
            // switch merge playlist
            binding.mergePlaylist.apply {
                isChecked = mergePlaylist
                setOnClickListener {
                    mergePlaylist = isChecked
                }
            }
            // button reload playlist
            binding.reloadPlaylist.apply {
                setOnClickListener {
                    //update preferences before load
                    preferences.useCustomPlaylist = useCustomPlaylist
                    preferences.mergePlaylist = mergePlaylist
                    preferences.radioPlaylist = radioPlaylist
                    preferences.playlistSelect = playlistSelect
                    preferences.playlistExternal = playlistExternal
                    //load playlist
                    LocalBroadcastManager.getInstance(rootView.context).sendBroadcast(
                        Intent(MainActivity.MAIN_CALLBACK)
                            .putExtra(MainActivity.MAIN_CALLBACK, MainActivity.UPDATE_PLAYLIST))
                }
            }

            val properties = DialogProperties().apply {
                extensions = arrayOf("json","m3u")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    root = File(Environment.getExternalStorageDirectory().path)
                    error_dir = File(Environment.getExternalStorageDirectory().path)
                    offset  = File(Environment.getExternalStorageDirectory().path)
                } else {
                    root = File("/")
                    offset  = File("/mnt/sdcard:/storage")
                }
            }
            dialog = FilePickerDialog(rootView.context).apply {
                setTitle(getString(R.string.title_select_file_json))
                setProperties(properties)
                setDialogSelectionListener {
                    for (path in it) {
                        playlistSelect = File(path).path
                        binding.customPlaylist.setText(playlistSelect)
                    }
                }
            }
            return rootView
        }

        @SuppressLint("SetTextI18n")
        private fun updateLayout(radioPlaylist: Int) {
            when (radioPlaylist) {
                0 -> {
                    binding.pickButton.visibility = View.GONE
                    binding.mergePlaylist.visibility = View.VISIBLE
                    binding.customPlaylist.isEnabled = false
                    binding.customPlaylist.setText("InternalStorage/${PlaylistHelper.PLAYLIST_JSON}")
                    binding.customTextField.setHint(R.string.hint_custom_playlist_path)
                    binding.customTextField.endIconMode = TextInputLayout.END_ICON_NONE
                }
                1 -> {
                    binding.pickButton.visibility = View.VISIBLE
                    binding.mergePlaylist.visibility = View.VISIBLE
                    binding.customPlaylist.isEnabled = false
                    binding.customPlaylist.setText(playlistSelect)
                    binding.customTextField.setHint(R.string.hint_custom_playlist_path)
                    binding.customTextField.endIconMode = TextInputLayout.END_ICON_NONE
                }
                else -> {
                    binding.pickButton.visibility = View.GONE
                    binding.mergePlaylist.visibility = View.GONE
                    binding.customPlaylist.isEnabled = true
                    binding.customPlaylist.setText(playlistExternal)
                    binding.customTextField.setHint(R.string.hint_custom_playlist_url)
                    binding.customTextField.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    mergePlaylist = false
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        private fun openFilePicker() {
            //set focus to reload button
            binding.reloadPlaylist.requestFocus()
            //show dialog
            dialog.show()
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
    }
}
