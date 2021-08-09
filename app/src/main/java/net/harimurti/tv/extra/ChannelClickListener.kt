package net.harimurti.tv.extra

import net.harimurti.tv.model.Channel

interface ChannelClickListener {
    fun channelClicked(ch: Channel?)
}