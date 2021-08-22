package net.harimurti.tv.extra

import android.os.Handler
import android.os.Looper
import net.harimurti.tv.App

class AsyncSleep {
    private val context = App.context
    private var task: Task? = null

    interface Task {
        fun onCountDown(count: Int) {}
        fun onFinish() {}
    }

    fun task(task: Task?): AsyncSleep {
        this.task = task
        return this
    }

    fun start(second: Int) {
        for (i in 1..second) {
            val left = second - i
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    task!!.onCountDown(left)
                    if (left == 0) {
                        task!!.onFinish()
                    }
                }
            }, (i * 1000).toLong())
        }
    }

    private fun runOnUiThread(task: Runnable) {
        Handler(context.mainLooper).post(task)
    }
}