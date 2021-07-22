package net.harimurti.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import net.harimurti.tv.adapter.ContentAdapter
import net.harimurti.tv.model.Channel

class GridViewFragment : Fragment() {
    private lateinit var channels: ArrayList<Channel>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            channels = requireArguments().getParcelableArrayList(CHANNELS)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gridview, container, false)
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val gridView = view.findViewById<GridView>(R.id.gridview)
        gridView.adapter = context?.let { ContentAdapter(it, channels) }
    }

    companion object {
        private const val CHANNELS = "channels"
        fun newFragment(arrayList: ArrayList<Channel>?): GridViewFragment {
            val args = Bundle()
            args.putParcelableArrayList(CHANNELS, arrayList)
            val fragment = GridViewFragment()
            fragment.arguments = args
            return fragment
        }
    }
}