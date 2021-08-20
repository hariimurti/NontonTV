package net.harimurti.tv.extra

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import net.harimurti.tv.R

fun View?.startAnimation(context: Context, hasFocus: Boolean) {
    val animation = AnimationUtils.loadAnimation(context,
        if (hasFocus) R.anim.zoom_120 else R.anim.zoom_100)
    this?.startAnimation(animation)
    animation.fillAfter = true
}