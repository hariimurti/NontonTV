package net.harimurti.tv.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.PlayData

class SearchAdapter (val channels: ArrayList<Channel>, private var channelsId: ArrayList<String>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>(), Filterable {
    lateinit var context: Context
    var channelsFilter: ArrayList<Channel> = ArrayList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.btn_play)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchAdapter.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_channel, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: SearchAdapter.ViewHolder, position: Int) {
        val channel = channelsFilter[position]
        val chName = channel.name!!

        viewHolder.button.apply {
            text = chName
            setOnClickListener {
                for (cId in channelsId){
                    if (cId.split("/")[2].lowercase() == chName.lowercase()) {
                        val catId:Int = cId.split("/")[0].toInt()
                        val chId:Int = cId.split("/")[1].toInt()

                        val intent = Intent(context, PlayerActivity::class.java)
                        intent.putExtra(PlayData.VALUE, PlayData(catId, chId))
                        context.startActivity(intent)
                    }
                }
            }
            setOnFocusChangeListener { _, hasFocus ->
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
}