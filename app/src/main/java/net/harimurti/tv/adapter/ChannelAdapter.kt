package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.Playlist


class ChannelAdapter (private val channels: ArrayList<Channel>?) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {
    lateinit var context: Context

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.btn_play)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelAdapter.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_channel, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: ChannelAdapter.ViewHolder, position: Int) {
        val channel: Channel? = channels?.get(position)
        val drmUrl: String? = Playlist.loaded?.drm_licenses?.firstOrNull {
            channel?.drm_name?.equals(it.drm_name) == true
        }?.drm_url

        val button = viewHolder.button
        button.text = channel?.name
        button.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(Channel.NAME, channel?.name)
            intent.putExtra(Channel.STREAMURL, channel?.stream_url)
            intent.putExtra(Channel.DRMURL, drmUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return channels!!.size
    }
}