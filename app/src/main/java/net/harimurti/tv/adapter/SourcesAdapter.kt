package net.harimurti.tv.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemSourceBinding
import net.harimurti.tv.model.Source

interface SourceClickListener {
    fun onClicked(source: Source?)
    fun onCheckChanged(view: View, checked: Boolean, source: Source?)
    fun onlongClicked(view: View, source: Source?): Boolean
}

class SourcesAdapter(private val sources: ArrayList<Source>?):
    RecyclerView.Adapter<SourcesAdapter.ViewHolder>(), SourceClickListener {
    lateinit var context: Context

    class ViewHolder(var itemBinding: ItemSourceBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(obj: Any?) {
            itemBinding.setVariable(BR.modelSource, obj)
            itemBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: ItemSourceBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.item_source, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val source = sources?.get(position)
        viewHolder.bind(source)
        viewHolder.itemBinding.clickListener = this
        if (source?.path?.equals(context.getString(R.string.json_playlist)) == true) {
            viewHolder.itemBinding.swSource.setText(R.string.default_playlist)
            viewHolder.itemBinding.btnRemove.visibility = View.GONE
        }
    }

    override fun onClicked(source: Source?) {
        val position = sources?.indexOf(source) ?: 0
        sources?.remove(source)
        notifyItemRemoved(position)
        if (sources?.size == 1) {
            sources[0].active = true
            notifyItemChanged(0)
        }
    }

    override fun onlongClicked(view: View, source: Source?): Boolean {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("url", source?.path)

        clipboard.setPrimaryClip(clipData)
        Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show()

        return true
    }

    override fun onCheckChanged(view: View, checked: Boolean, source: Source?) {
        val position = sources?.indexOf(source) ?: 0
        sources?.get(position)?.active = checked
        if (!checked && sources?.filter { s -> s.active }?.size == 0 && !sources[0].active) {
            sources[0].active = true
            notifyItemChanged(0)
            Toast.makeText(context, R.string.warning_none_source_active, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    fun getItems(): ArrayList<Source>? {
        return sources
    }

    fun addItem(source: Source) {
        sources?.add(source)
        notifyItemChanged(sources?.indexOf(source) ?: 0)
    }
}