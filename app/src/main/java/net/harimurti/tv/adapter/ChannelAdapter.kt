package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemChannelBinding
import net.harimurti.tv.extra.ChannelClickListener
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData


class ChannelAdapter (private val channels: ArrayList<Channel>?) :
    RecyclerView.Adapter<ChannelAdapter.ViewHolder>(), ChannelClickListener {
    lateinit var context: Context

    class ViewHolder(var itemChBinding: ItemChannelBinding) :
        RecyclerView.ViewHolder(itemChBinding.root) {
        fun bind(obj: Any?) {
            itemChBinding.setVariable(BR.chModel,obj)
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
        viewHolder.itemChBinding.chClickListener = this
        viewHolder.itemChBinding.btnPlay.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.zoom_120)
                    viewHolder.itemChBinding.btnPlay.startAnimation(anim)
                    anim.fillAfter = true
                } else {
                    val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.zoom_100)
                    viewHolder.itemChBinding.btnPlay.startAnimation(anim)
                    anim.fillAfter = true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return channels?.size ?: 0
    }

    override fun channelClicked(ch: Channel?) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra(PlayData.VALUE, PlayData(ch?.catId!!, ch.chId!!))
        context.startActivity(intent)
    }
}