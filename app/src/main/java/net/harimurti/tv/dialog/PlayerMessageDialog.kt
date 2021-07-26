package net.harimurti.tv.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.PlayerDialogMessageBinding

class PlayerMessageDialog(val context: Context) {
    private val broadcast = LocalBroadcastManager.getInstance(context)
    private var dialog: AlertDialog? = null
    private var binding: PlayerDialogMessageBinding? = null
    private lateinit var message: String

    fun show(message: String) {
        this.message = message

        if (dialog == null || binding == null) {
            dialog = AlertDialog.Builder(context, R.style.PlayerMessage).create()
            binding = PlayerDialogMessageBinding.inflate(LayoutInflater.from(context))
            binding?.btnRetry?.setOnClickListener {
                broadcast.sendBroadcast(
                    Intent(PlayerActivity.PLAYER_CALLBACK)
                        .putExtra(PlayerActivity.PLAYER_CALLBACK, PlayerActivity.RETRY_PLAYBACK)
                )
            }
            binding?.btnClose?.setOnClickListener {
                broadcast.sendBroadcast(
                    Intent(PlayerActivity.PLAYER_CALLBACK)
                        .putExtra(PlayerActivity.PLAYER_CALLBACK, PlayerActivity.CLOSE_PLAYER)
                )
            }
        }

        binding?.textMessage?.text = message
        binding?.btnClose?.requestFocus()

        dialog?.setView(binding?.root, 0, 0, 0, 0)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        dialog?.show()
    }

    fun dismiss() {
        if (dialog == null) return
        if (dialog?.isShowing == true) {
            try { dialog?.dismiss() }
            catch (e: Exception) { }
        }
    }
}