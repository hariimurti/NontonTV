package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.MainActivity
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemChannelBinding
import net.harimurti.tv.extra.insert
import net.harimurti.tv.extra.remove
import net.harimurti.tv.extra.save
import net.harimurti.tv.extra.startAnimation
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData
import net.harimurti.tv.model.Playlist

interface ChannelClickListener {
    fun onClicked(ch: Channel, catId: Int, chId: Int)
    fun onLongClicked(ch: Channel, catId: Int, chId: Int): Boolean
}

class ChannelAdapter (val channels: ArrayList<Channel>?, private val catId: Int, private val isFav: Boolean) :
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
                v.startAnimation(hasFocus)
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

    override fun onLongClicked(ch: Channel, catId: Int, chId: Int): Boolean {
        val fav = Playlist.favorites
        if (isFav) {
            channels?.remove(ch)
            fav.remove(ch)

            // notifyupdate
            if (itemCount != 0) {
                notifyItemRemoved(chId)
                notifyItemRangeChanged(0, itemCount)
            } else sendBroadcast(false)

            Toast.makeText(context,
                String.format(context.getString(R.string.removed_from_favorite), ch.name),
                Toast.LENGTH_SHORT).show()
        }
        else {
            val result = fav.insert(ch)

            // notifyupdate
            if (result) sendBroadcast(true)

            val message = if (result) String.format(context.getString(R.string.added_into_favorite), ch.name)
            else String.format(context.getString(R.string.already_in_favorite), ch.name)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        fav.save()
        return true
    }

    private fun sendBroadcast(isInserted: Boolean) {
        val callback = if (isInserted) MainActivity.INSERT_FAVORITE else MainActivity.REMOVE_FAVORITE
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(MainActivity.MAIN_CALLBACK)
                .putExtra(MainActivity.MAIN_CALLBACK, callback))
    }
}