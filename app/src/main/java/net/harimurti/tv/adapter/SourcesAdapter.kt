package net.harimurti.tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemSourceBinding
import net.harimurti.tv.model.Source

interface SourceClickListener {
    fun buttonClicked(source: Source?)
    fun checkBoxClicked(view: View, source: Source?)
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

    override fun buttonClicked(source: Source?) {
        val position = sources?.indexOf(source) ?: 0
        sources?.remove(source)
        notifyItemRemoved(position)
        if (sources?.size == 1) {
            sources[0].active = true
            notifyItemChanged(0)
        }
    }

    override fun checkBoxClicked(view: View, source: Source?) {
        val position = sources?.indexOf(source) ?: 0
        val checkBox = view as AppCompatCheckBox
        sources?.get(position)?.active = checkBox.isChecked
        if (!checkBox.isChecked && sources?.filter { s -> s.active }?.size == 0 && !sources[0].active) {
            sources[0].active = true
            notifyItemChanged(0)
            Toast.makeText(context, "Can't disable the default, you're only have one active playlist!", Toast.LENGTH_SHORT).show()
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