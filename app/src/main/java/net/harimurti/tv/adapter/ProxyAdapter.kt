package net.harimurti.tv.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.harimurti.tv.BR
import net.harimurti.tv.R
import net.harimurti.tv.databinding.ItemProxyBinding
import net.harimurti.tv.extra.Preferences
import net.harimurti.tv.model.*

interface OnProxyClickedListener {
    fun onProxyClicked()
}

class ProxyAdapter(private val proxies: ArrayList<ProxyData>) :
    RecyclerView.Adapter<ProxyAdapter.ViewHolder>(), Filterable {

    lateinit var context: Context
    var listProxy: ArrayList<ProxyData> = ArrayList()
    lateinit var mCallback: OnProxyClickedListener

    class ViewHolder(var itemBinding: ItemProxyBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(obj: Any?) {
            itemBinding.setVariable(BR.modelProxy, obj)
            itemBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: ItemProxyBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.item_proxy, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val proxy = listProxy[position]
        viewHolder.bind(proxy)
        viewHolder.itemBinding.proxy.setOnClickListener {
            val proxyData = ProxyData()
            proxyData.type = proxy.type
            proxyData.ip = proxy.ip
            proxyData.port = proxy.port
            Preferences().proxy = proxyData
            mCallback.onProxyClicked()
        }
    }

    override fun getItemCount(): Int {
        return listProxy.size
    }

    fun clear() {
        val size = itemCount
        proxies.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                listProxy = if (constraint.isEmpty()) {
                    proxies
                } else {
                    val listPr: ArrayList<ProxyData> = ArrayList()
                    for (id in proxies.indices) {
                        if (proxies[id].country.equals(constraint.toString(), true)) {
                            listPr.add(proxies[id])
                        }
                    }
                    listPr
                }
                return FilterResults().apply {
                    values = listProxy
                    count = listProxy.size
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                listProxy = objToArrayList(filterResults.values)
                notifyDataSetChanged()
            }

            private fun objToArrayList(obj: Any?): ArrayList<ProxyData> {
                return if (obj is ArrayList<*>) ArrayList(obj.filterIsInstance<ProxyData>())
                else ArrayList()
            }
        }
    }

    fun setOnProxyClickedListener(mCallback: OnProxyClickedListener) {
        this.mCallback = mCallback
    }
}