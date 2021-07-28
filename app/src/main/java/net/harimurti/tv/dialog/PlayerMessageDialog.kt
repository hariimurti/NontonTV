package net.harimurti.tv.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.PlayerDialogMessageBinding
import net.harimurti.tv.extra.AsyncSleep

class PlayerMessageDialog(val context: Context) {
    private val broadcast = LocalBroadcastManager.getInstance(context)
    private var dialog: AlertDialog? = null
    private var binding: PlayerDialogMessageBinding? = null
    private lateinit var message: String

    fun show(message: String) {
        this.message = message

        if (dialog == null || binding == null) {
            dialog = AlertDialog.Builder(context, R.style.PlayerMessage)
                .setOnKeyListener { _, _, keyEvent ->
                    if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        sendClosePlayer()
                    }
                    return@setOnKeyListener true
                }
                .create()

            binding = PlayerDialogMessageBinding.inflate(LayoutInflater.from(context))
            binding?.btnRetry?.setOnClickListener {
                sendRetryPlayback()
            }
            binding?.btnClose?.setOnClickListener {
                sendClosePlayer()
            }
        }

        binding?.textMessage?.text = message
        binding?.btnClose?.requestFocus()

        dialog?.setView(binding?.root, 0, 0, 0, 0)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        dialog?.show()

        retryCoundown()
    }

    private fun retryCoundown() {
        val waitInSecond = 30
        binding?.btnRetry?.text = String.format(context.getString(R.string.btn_retry_count), waitInSecond)
        AsyncSleep(context).task(object : AsyncSleep.Task{
            override fun onCountDown(count: Int) {
                binding?.btnRetry?.text = if (count <= 0) context.getString(R.string.btn_retry)
                else String.format(context.getString(R.string.btn_retry_count), count)
            }

            override fun onFinish() {
                sendRetryPlayback()
            }
        }).start(waitInSecond)
    }

    private fun sendRetryPlayback() {
        broadcast.sendBroadcast(
            Intent(PlayerActivity.PLAYER_CALLBACK)
                .putExtra(PlayerActivity.PLAYER_CALLBACK, PlayerActivity.RETRY_PLAYBACK)
        )
    }

    private fun sendClosePlayer() {
        broadcast.sendBroadcast(
            Intent(PlayerActivity.PLAYER_CALLBACK)
                .putExtra(PlayerActivity.PLAYER_CALLBACK, PlayerActivity.CLOSE_PLAYER)
        )
    }

    fun dismiss() {
        if (dialog == null) return
        if (dialog?.isShowing == true) {
            try { dialog?.dismiss() }
            catch (e: Exception) { }
        }
    }
}