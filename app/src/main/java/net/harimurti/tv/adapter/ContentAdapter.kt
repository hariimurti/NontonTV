package net.harimurti.tv.adapter

import android.content.*
import android.view.*
import android.view.View.OnFocusChangeListener
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.TextView
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.model.Channel
import java.util.*

class ContentAdapter(context: Context, dataSet: ArrayList<Channel>) :
    ArrayAdapter<Channel>(context, R.layout.fragment_content, dataSet as List<Channel>) {

    private inner class ContentHolder(v: View) {
        var layout: View = v.findViewById(R.id.layout)
        var textView: TextView = v.findViewById(R.id.textview)
    }

    override fun getView(i: Int, v: View?, parent: ViewGroup): View {
        val data = getItem(i)
        val view: View
        val viewHolder: ContentHolder
        if (v == null) {
            view = LayoutInflater.from(context).inflate(R.layout.fragment_content, parent, false)
            viewHolder = ContentHolder(view)
            view.tag = viewHolder
        } else {
            view = v
            viewHolder = v.tag as ContentHolder
        }
        viewHolder.textView.text = data?.name
        viewHolder.layout.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("channel_url", data?.stream_url)
            context.startActivity(intent)
        }
        viewHolder.layout.onFocusChangeListener = OnFocusChangeListener { _, b ->
            if (!b) return@OnFocusChangeListener
            val gridView = parent as GridView
            val columns = gridView.numColumns
            val first = gridView.firstVisiblePosition
            val last = gridView.lastVisiblePosition
            if (first <= i && i <= first + columns + 1) {
                var position = first - columns
                if (position < 0) position = 0
                gridView.smoothScrollToPosition(position)
            }
            if (last - columns + 1 <= i && i <= last) {
                val position = if (last + columns > last) last else last + columns
                gridView.smoothScrollToPosition(position)
            }
        }
        return view
    }
}