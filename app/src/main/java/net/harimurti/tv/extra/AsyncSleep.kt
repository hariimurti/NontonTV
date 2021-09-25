package net.harimurti.tv.extra

import android.os.Handler
import android.os.Looper
import net.harimurti.tv.App

class AsyncSleep {
    private val context = App.context
    private var task: Task? = null
    private var handler: Handler? = null

    interface Task {
        fun onCountDown(count: Int) {}
        fun onFinish() {}
    }

    fun task(task: Task?): AsyncSleep {
        this.task = task
        return this
    }

    fun start(second: Int) {
        handler = Handler(Looper.getMainLooper())
        for (i in 1..second) {
            val left = second - i
            handler?.postDelayed({
                runOnUiThread {
                    task!!.onCountDown(left)
                    if (left == 0) {
                        task!!.onFinish()
                    }
                }
            }, (i * 1000).toLong())
        }
    }

    fun stop() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    private fun runOnUiThread(task: Runnable) {
        Handler(context.mainLooper).post(task)
    }
}