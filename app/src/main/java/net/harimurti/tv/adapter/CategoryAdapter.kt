package net.harimurti.tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import net.harimurti.tv.BR
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemCategoryBinding
import net.harimurti.tv.extra.Preferences
import net.harimurti.tv.extra.addFavorite
import net.harimurti.tv.extra.isFavorite
import net.harimurti.tv.extra.sort
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Playlist
import kotlin.math.round

class CategoryAdapter (private val categories: ArrayList<Category>?) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    lateinit var context: Context

    class ViewHolder(var itemCatBinding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(itemCatBinding.root) {
        fun bind(obj: Any?) {
            itemCatBinding.setVariable(BR.catModel,obj)
            itemCatBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: ItemCategoryBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),R.layout.item_category,parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val category: Category? = categories?.get(position)
        val chCount = category?.channels?.size ?: 0
        val spanCount = when {
            chCount > 30 -> chCount.toDouble().div(20).let { round(it).toInt() }
            chCount > 20 -> 2
            else -> 1
        }

        val isFav = category.isFavorite(context) && position == 0
        viewHolder.itemCatBinding.chAdapter = ChannelAdapter(category?.channels, position, isFav)
        viewHolder.itemCatBinding.rvChannels.layoutManager =
                StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.HORIZONTAL)

        viewHolder.bind(category)
    }

    override fun getItemCount(): Int {
        return categories?.size ?: 0
    }

    fun clear() {
        val size = itemCount
        categories?.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun insertOrUpdateFavorite() {
        val fav = Playlist.favorites
        if (Preferences(context).sortFavorite) fav.sort()
        if (categories?.get(0)?.isFavorite(context) == false) {
            categories.addFavorite(context, fav.channels)
            notifyItemInserted(0)
        }
        else {
            categories?.get(0)?.channels = fav.channels
            notifyItemChanged(0)
        }
    }

    fun removeFavorite() {
        if (categories?.get(0)?.isFavorite(context) == true) {
            categories.removeAt(0)
            notifyItemRemoved(0)
        }
    }
}