package net.harimurti.tv.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemChannelBinding
import net.harimurti.tv.extra.ChannelClickListener
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData

class SearchAdapter (val channels: ArrayList<Channel>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>(), Filterable, ChannelClickListener {
    lateinit var context: Context
    var channelsFilter: ArrayList<Channel> = ArrayList()

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
        val channel = channelsFilter[position]
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
        return channelsFilter.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val r = FilterResults()
                if (constraint.isEmpty()) {
                    channelsFilter = channels
                } else {
                    val filterCh: ArrayList<Channel> = ArrayList()
                    for (ch in channels) {
                        if (ch.name?.lowercase()?.contains(constraint.toString().lowercase()) == true) {
                            filterCh.add(ch)
                        }
                        channelsFilter = filterCh
                    }
                }
                r.values = channelsFilter
                r.count = channelsFilter.size
                return r
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                channelsFilter  = objToArrayList(filterResults.values)
                notifyDataSetChanged()
            }

            private fun objToArrayList(obj: Any?): ArrayList<Channel> {
                return if (obj is ArrayList<*>) {
                    ArrayList(obj.filterIsInstance<Channel>())
                } else {
                    ArrayList()
                }
            }
        }
    }

    override fun channelClicked(ch: Channel?) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra(PlayData.VALUE, PlayData(ch?.cat_id!!, ch.ch_id!!))
        context.startActivity(intent)
    }
}