package net.harimurti.tv.extension

import net.harimurti.tv.extra.PlaylistHelper
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.Favorites
import net.harimurti.tv.model.Playlist

fun Favorites?.insert(channel: Channel): Boolean {
    if (this == null) return false
    val filter = this.channels.lastOrNull {
            c -> c.name == channel.name &&
            c.streamUrl == channel.streamUrl &&
            c.drmId == channel.drmId
    }
    return if (filter == null) {
        this.channels.add(channel)
        Playlist.favorites = this
        true
    }
    else false
}

fun Favorites?.remove(channel: Channel): Boolean {
    if (this == null) return false
    val filter = this.channels.filter {
            c -> c.name == channel.name &&
            c.streamUrl == channel.streamUrl &&
            c.drmId == channel.drmId
    }
    return if (!filter.isNullOrEmpty()) {
        this.channels.removeAll(filter)
        true
    }
    else false
}

fun Favorites?.sort(): Favorites? {
    this?.channels?.sortBy { channel -> channel.name?.lowercase() }
    return this
}

fun Favorites?.trimNotExistFrom(playlist: Playlist): Favorites? {
    if (this == null) return this
    val verified = ArrayList<Channel>()
    for (item in this.channels) {
        val filter = playlist.categories.lastOrNull { cat ->
            cat.channels?.lastOrNull { ch ->
                ch.name == item.name &&
                        ch.streamUrl == item.streamUrl &&
                        ch.drmId == item.drmId
            } != null
        }
        if (filter != null) verified.add(item)
    }
    this.channels = verified
    return this
}

fun Favorites?.save() {
    PlaylistHelper().writeFavorites(this ?: Favorites())
}