package net.harimurti.tv.extra

import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import net.harimurti.tv.App

class VolleyRequestQueue {
    companion object {
        fun create(): RequestQueue {
            var stack = HurlStack() as BaseHttpStack

            try {
                val factory = TLSSocketFactory()
                factory.trustAllHttps()
                stack = HurlStack(null, factory)
            } catch (e: Exception) {
                Log.e("Volley", "Could not trust all HTTPS connection!", e)
            }

            return Volley.newRequestQueue(App.context, stack)
        }
    }
}