package net.harimurti.tv.extra

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.harimurti.tv.R
import net.harimurti.tv.databinding.SettingsDialogBinding

@SuppressLint("InflateParams")
class SettingsDialog(val context: Context) {
    private val preferences = Preferences(context)

    fun show() {
        val dialog = AlertDialog.Builder(context, R.style.DialogMessage).create()
        val binding = SettingsDialogBinding.inflate(LayoutInflater.from(context))

        // switch launch at boot
        binding.launchAtBoot.apply {
            isChecked = preferences.launchAtBoot
            setOnClickListener {
                preferences.launchAtBoot = isChecked
            }
        }
        // switch play last watched
        binding.openLastWatched.apply {
            isChecked = preferences.playLastWatched
            setOnClickListener {
                preferences.playLastWatched = isChecked
            }
        }
        // layout custom playlist
        binding.layoutCustomPlaylist.apply {
            visibility = if (preferences.useCustomPlaylist) View.VISIBLE else View.GONE
        }
        // switch custom playlist
        binding.useCustomPlaylist.apply {
            isChecked = preferences.useCustomPlaylist
            setOnClickListener {
                binding.layoutCustomPlaylist.visibility = if (isChecked) View.VISIBLE else View.GONE
                preferences.useCustomPlaylist = isChecked
            }
        }
        // edittext custom playlist
        binding.customPlaylist.apply {
            setText(preferences.playlistExternal)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    preferences.playlistExternal = s.toString()
                }
            })
        }
        // button reload playlist
        binding.reloadPlaylist.setOnClickListener {
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("RELOAD_MAIN_PLAYLIST"))
            dialog.dismiss()
        }

        dialog?.setView(binding.root, 0, 0, 0, 0)
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.show()
    }
}
