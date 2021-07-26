package net.harimurti.tv.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import net.harimurti.tv.R

@SuppressLint("InflateParams")
class ProgressDialog(val context: Context) {
    private var dialog: AlertDialog? = null

    fun show(message: String): ProgressDialog {
        // set text message
        val loadView: View = LayoutInflater.from(context).inflate(R.layout.progress_dialog_message, null)
        loadView.findViewById<TextView>(R.id.tv_message).text = message
        // create dialog
        if (dialog == null) {
            dialog = AlertDialog.Builder(context, R.style.DialogMessage).create()
        }
        dialog?.setView(loadView, 0, 0, 0, 0)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.show()
        return this
    }

    fun dismiss() {
        if (dialog == null) return
        if (dialog?.isShowing == true) {
            try { dialog?.dismiss() }
            catch (e: Exception) { }
        }
    }
}
