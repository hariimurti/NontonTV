package net.harimurti.tv.adapter

//import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
//import com.bumptech.glide.Glide
//import net.harimurti.tv.R
import net.harimurti.tv.model.Channel
import kotlin.math.round

@BindingAdapter(value = ["channels","position", "isFav"], requireAll = false)
fun bindChAdapter(recyclerView: RecyclerView, channels: ArrayList<Channel>?,position: Int, isFav: Boolean) {
    val chAdapter = ChannelAdapter(channels,position,isFav)
    recyclerView.adapter = chAdapter
}

@BindingAdapter("spanCountBind")
fun entries(recyclerView: RecyclerView?, chCount: Int) {
    val spanCount = when {
        chCount > 30 -> chCount.toDouble().div(20).let { round(it).toInt() }
        chCount > 20 -> 2
        else -> 1
    }

    recyclerView?.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.HORIZONTAL)
}

/*@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .placeholder(R.mipmap.ic_banner)
            .into(view)
    }else{
        view.setImageResource(R.mipmap.ic_banner)
    }
}*/


