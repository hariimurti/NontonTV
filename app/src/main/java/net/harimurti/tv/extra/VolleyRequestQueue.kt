package net.harimurti.tv.extra

import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley

class VolleyRequestQueue {
    companion object {
        fun create(context: Context): RequestQueue {
            var stack = HurlStack() as BaseHttpStack

            try {
                val factory = TLSSocketFactory(context)
                factory.trustAllHttps()
                stack = HurlStack(null, factory)
            } catch (e: Exception) {
                Log.e("Volley", "Could not trust all HTTPS connection!", e)
            }

            return Volley.newRequestQueue(context, stack)
        }
    }
}