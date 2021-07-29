package net.harimurti.tv.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.MainActivity
import net.harimurti.tv.R
import net.harimurti.tv.model.Category

class CategoryAdapter (private val categories: ArrayList<Category>?) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    lateinit var context: Context

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text_category)
        val buttonSetting: ImageButton = itemView.findViewById(R.id.main_settings)
        val recyclerView: RecyclerView = itemView.findViewById(R.id.rv_channels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryAdapter.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_category, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: CategoryAdapter.ViewHolder, position: Int) {
        val category: Category? = categories?.get(position)
        viewHolder.textView.text = category?.name
        viewHolder.buttonSetting.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
        viewHolder.buttonSetting.setOnClickListener {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(MainActivity.MAIN_CALLBACK)
                .putExtra(MainActivity.MAIN_CALLBACK, MainActivity.OPEN_SETTINGS))
        }
        viewHolder.recyclerView.adapter = ChannelAdapter(category?.channels, position)
    }

    override fun getItemCount(): Int {
        return categories!!.size
    }

    fun change(list: ArrayList<Category>?) {
        categories?.clear()
        list?.let { categories?.addAll(it) }
        notifyDataSetChanged()
    }
}