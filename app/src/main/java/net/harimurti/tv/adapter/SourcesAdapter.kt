package net.harimurti.tv.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemSourceBinding
import net.harimurti.tv.dialog.SettingDialog
import net.harimurti.tv.extra.Preferences
import net.harimurti.tv.model.Source

interface SourceClickListener {
    fun onClicked(position: Int)
    fun onCheckChanged(view: View, checked: Boolean, position: Int)
    fun onlongClicked(view: View, source: Source?, position: Int): Boolean
}

class SourcesAdapter(private val sources: ArrayList<Source>?):
    RecyclerView.Adapter<SourcesAdapter.ViewHolder>(), SourceClickListener {
    lateinit var context: Context
    val preferences = Preferences()

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
        viewHolder.itemBinding.position = position
        viewHolder.itemBinding.clickListener = this
        if (position == 0) {
            val name = String.format(context.getString(R.string.default_playlist), preferences.countryId.uppercase())
            viewHolder.itemBinding.swSource.text = name
            viewHolder.itemBinding.btnRemove.visibility = View.GONE
        }
    }

    override fun onViewRecycled(viewHolder: ViewHolder) {
        super.onViewRecycled(viewHolder)
        viewHolder.itemBinding.clickListener = null
    }

    override fun onClicked(position: Int) {
        sources?.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(0, itemCount)
        if (itemCount == 1) {
            sources?.get(0)?.active = true
            notifyItemChanged(0)
        }
        SettingDialog.isSourcesChanged = true
    }

    override fun onlongClicked(view: View, source: Source?, position: Int): Boolean {
        if (position == 0) {
            PopupMenu(context, view, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0).apply {
                inflate(R.menu.country_list)
                setOnMenuItemClickListener { m: MenuItem ->
                    val mode = when(m.itemId) {
                        R.id.japan -> "jp"
                        R.id.malaysia -> "my"
                        R.id.singapore -> "sg"
                        R.id.southkorea -> "kr"
                        else -> "id"
                    }
                    preferences.countryId = mode
                    SettingDialog.isSourcesChanged = true
                    notifyItemChanged(0)
                    true
                }
                show()
            }
        }
        else {
            val clipData = ClipData.newPlainText("url", source?.path)
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        return true
    }

    override fun onCheckChanged(view: View, checked: Boolean, position: Int) {
        sources?.get(position)?.active = checked
        SettingDialog.isSourcesChanged = true
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    fun getItems(): ArrayList<Source>? {
        return sources
    }

    fun addItem(source: Source) {
        val position = itemCount
        sources?.add(source)
        notifyItemInserted(position)
        SettingDialog.isSourcesChanged = true
    }
}