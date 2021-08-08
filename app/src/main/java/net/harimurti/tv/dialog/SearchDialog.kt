package net.harimurti.tv.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import net.harimurti.tv.R
import net.harimurti.tv.adapter.SearchAdapter
import net.harimurti.tv.databinding.SearchDialogBinding
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.Playlist

@Suppress("DEPRECATION")
class SearchDialog : DialogFragment() {
    private var _binding : SearchDialogBinding? = null
    private val binding get() = _binding!!
    lateinit var searchAdapter: SearchAdapter
    private val channels: ArrayList<Channel> = ArrayList()
    private var channelsId: ArrayList<String> = ArrayList()

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            //full screen
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.SettingsDialogThemeOverlay)
        dialog.setTitle(R.string.search_channel)
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SearchDialogBinding.inflate(inflater,container, false)
        val dialogView = binding.root

        setAdapter()

        //edittext
        binding.searchInput.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    searchAdapter.filter.filter(s)
                    binding.searchList.visibility = if(s.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.searchReset.visibility = if(s.isNotEmpty()) View.VISIBLE else View.GONE

                }
            })
        }

        //button cleartext
        binding.searchReset.apply {
            setOnClickListener {
                binding.searchInput.setText("")
            }
        }

        //RecyclerView
        binding.searchList.apply {
            adapter = searchAdapter
            layoutManager = GridLayoutManager(context,spanColumn())
        }

        //button close
        binding.searchClose.apply {
            setOnClickListener {
                dismiss()
            }
        }
        return dialogView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setAdapter(){
        val playlist = Playlist.loaded
        for (catId in playlist?.categories?.indices!!) {
            for ((chId, ch) in playlist.categories!![catId].channels!!.withIndex()) {
                channels.add(ch)
                channelsId.add("$catId/$chId/${ch.name}")
            }
        }

        searchAdapter = SearchAdapter(channels,channelsId)
    }

    private fun spanColumn(): Int {
        val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        return (screenWidthDp / 130F + 0.5).toInt()
    }
}
