package net.harimurti.tv.extension

import net.harimurti.tv.App
import net.harimurti.tv.R
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel

fun Category?.isFavorite(): Boolean {
    return this?.name == App.context.getString(R.string.favorite_channel)
}

fun ArrayList<Category>?.addFavorite(channels: ArrayList<Channel>) {
    val title = App.context.getString(R.string.favorite_channel)
    this?.add(0, Category().apply {
        this.name = title
        this.channels = channels
    })
}