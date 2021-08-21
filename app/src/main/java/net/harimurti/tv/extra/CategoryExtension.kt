package net.harimurti.tv.extra

import android.content.Context
import net.harimurti.tv.R
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel

fun Category?.isFavorite(context: Context): Boolean {
    return this?.name == context.getString(R.string.favorite_channel)
}

fun ArrayList<Category>?.addFavorite(context: Context, channels: ArrayList<Channel>) {
    val title = context.getString(R.string.favorite_channel)
    this?.add(0, Category().apply {
        this.name = title
        this.channels = channels
    })
}