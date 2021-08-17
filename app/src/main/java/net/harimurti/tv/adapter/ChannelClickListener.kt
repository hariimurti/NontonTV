package net.harimurti.tv.adapter

import net.harimurti.tv.model.Channel

interface ChannelClickListener {
    fun onClicked(ch: Channel?)
}