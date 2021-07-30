package net.harimurti.tv.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.extra.PlaylistHelper
import net.harimurti.tv.extra.Preferences
import java.util.*

@SuppressLint("InflateParams")
class SettingsDialog(myContext: Context) : DialogFragment() {
    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()
    private val preferences = Preferences(myContext)

    companion object {
        var launchAtBoot = false
        var playLastWatched = false
        var useCustomPlaylist = false
        var mergePlaylist = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.SettingsDialogThemeOverlay)
        dialog.setTitle(R.string.settings)
        dialog.setCanceledOnTouchOutside(false)

        mFragmentList.add(StartupFragment())
        mFragmentList.add(PlaylistFragment())
        mFragmentTitleList.add("Startup")
        mFragmentTitleList.add("Playlist")
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val dialogView = inflater.inflate(R.layout.settings_dialog, container, false)
        val viewPager = dialogView.findViewById<ViewPager>(R.id.setting_view_pager).apply {
            adapter = FragmentAdapter(childFragmentManager)
        }
        dialogView.findViewById<TabLayout>(R.id.setting_tab_layout).apply {
            setupWithViewPager(viewPager)
        }
        // button cancel
        dialogView.findViewById<Button>(R.id.setting_cancel_button).apply {
            setOnClickListener { dismiss() }
        }
        // button ok
        dialogView.findViewById<Button>(R.id.setting_ok_button).apply {
            launchAtBoot = preferences.launchAtBoot
            playLastWatched = preferences.playLastWatched
            useCustomPlaylist = preferences.useCustomPlaylist
            mergePlaylist = if (preferences.playlistExternal.isNotEmpty()) false else preferences.mergePlaylist

            setOnClickListener {
                preferences.launchAtBoot = launchAtBoot
                preferences.playLastWatched = playLastWatched
                preferences.useCustomPlaylist = useCustomPlaylist
                preferences.mergePlaylist = mergePlaylist
                dismiss()
            }
        }
        return dialogView
    }

    private inner class FragmentAdapter(fragmentManager: FragmentManager?) :
        FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }

    class StartupFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
            val rootView = inflater.inflate(R.layout.settings_startup_fragment, container,false)
            val preferences = Preferences(rootView.context)

            rootView.findViewById<SwitchCompat>(R.id.launch_at_boot).apply {
                isChecked = preferences.launchAtBoot
                setOnClickListener {
                    launchAtBoot = isChecked
                }
            }

            rootView.findViewById<SwitchCompat>(R.id.open_last_watched).apply {
                isChecked = preferences.playLastWatched
                setOnClickListener {
                    playLastWatched = isChecked
                }
            }

            return rootView
        }

    }

    class PlaylistFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            val rootView = inflater.inflate(R.layout.settings_playlist_fragment, container,false)
            val preferences = Preferences(rootView.context)

            // layout custom playlist
            rootView.findViewById<LinearLayout>(R.id.layout_custom_playlist).apply {
                visibility = if (preferences.useCustomPlaylist) View.VISIBLE else View.GONE
            }
            // switch custom playlist
            rootView.findViewById<SwitchCompat>(R.id.use_custom_playlist).apply {
                isChecked = preferences.useCustomPlaylist
                setOnClickListener {
                    rootView.findViewById<LinearLayout>(R.id.layout_custom_playlist).visibility = if (isChecked) View.VISIBLE else View.GONE
                    useCustomPlaylist = isChecked
                    if (!isChecked)
                        rootView.findViewById<SwitchCompat>(R.id.merge_playlist).apply {
                            isChecked = false
                        }
                }
            }
            // switch merge playlist
            rootView.findViewById<SwitchCompat>(R.id.merge_playlist).apply {
                isChecked = preferences.mergePlaylist
                setOnClickListener {
                    mergePlaylist = isChecked
                }
            }
            // edittext custom playlist
            rootView.findViewById<AppCompatEditText>(R.id.custom_playlist).apply {
                setText(preferences.playlistExternal)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        preferences.playlistExternal = s.toString()
                        if(s.toString().isNotEmpty()) {
                            mergePlaylist = false
                            rootView.findViewById<SwitchCompat>(R.id.merge_playlist).apply {
                                isChecked = false
                            }
                        }else preferences.playlistExternal = PlaylistHelper.PLAYLIST_JSON

                    }
                })
            }
            // button reload playlist
            rootView.findViewById<AppCompatButton>(R.id.reload_playlist).setOnClickListener {
                preferences.useCustomPlaylist = useCustomPlaylist
                preferences.mergePlaylist = mergePlaylist
                LocalBroadcastManager.getInstance(rootView.context).sendBroadcast(
                    Intent(MainActivity.MAIN_CALLBACK)
                        .putExtra(MainActivity.MAIN_CALLBACK, MainActivity.UPDATE_PLAYLIST))
            }

            return rootView
        }
    }
}
