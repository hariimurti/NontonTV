package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData


class ChannelAdapter (private val channels: ArrayList<Channel>?, private val catId: Int) :
    RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {
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
        viewHolder.button.text = channel?.name
        viewHolder.button.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(PlayData.VALUE, PlayData(catId, position))
            context.startActivity(intent)
        }
        viewHolder.button.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.zoom_120)
                viewHolder.button.startAnimation(anim)
                anim.fillAfter = true
            } else {
                val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.zoom_100)
                viewHolder.button.startAnimation(anim)
                anim.fillAfter = true
            }
        }
    }

    override fun getItemCount(): Int {
        return channels!!.size
    }
}