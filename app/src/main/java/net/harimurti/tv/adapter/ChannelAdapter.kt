package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemChannelBinding
import net.harimurti.tv.extra.startAnimation
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData

interface ChannelClickListener {
    fun onClicked(ch: Channel, catId: Int, chId: Int)
}

class ChannelAdapter (val channels: ArrayList<Channel>?, private val catId: Int) :
    RecyclerView.Adapter<ChannelAdapter.ViewHolder>(), ChannelClickListener {
    lateinit var context: Context

    class ViewHolder(var itemChBinding: ItemChannelBinding) :
        RecyclerView.ViewHolder(itemChBinding.root) {
        fun bind(obj: Any?) {
            itemChBinding.setVariable(BR.modelChannel, obj)
            itemChBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: ItemChannelBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),R.layout.item_channel,parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val channel: Channel? = channels?.get(position)
        viewHolder.bind(channel)
        viewHolder.itemChBinding.catId = catId
        viewHolder.itemChBinding.chId = position
        viewHolder.itemChBinding.clickListener = this
        viewHolder.itemChBinding.btnPlay.apply {
            setOnFocusChangeListener { v, hasFocus ->
                v.startAnimation(context, hasFocus)
            }
        }
    }

    override fun getItemCount(): Int {
        return channels?.size ?: 0
    }

    override fun onClicked(ch: Channel, catId: Int, chId: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra(PlayData.VALUE, PlayData(catId, chId))
        context.startActivity(intent)
    }
}