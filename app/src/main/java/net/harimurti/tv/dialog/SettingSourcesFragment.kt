package net.harimurti.tv.dialog

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import net.harimurti.tv.R
import net.harimurti.tv.adapter.SourcesAdapter
import net.harimurti.tv.databinding.SettingSourcesFragmentBinding
import net.harimurti.tv.extension.isLinkUrl
import net.harimurti.tv.extra.PlaylistHelper
import net.harimurti.tv.model.Source
import java.io.File

class SettingSourcesFragment: Fragment() {
    companion object {
        var sources: ArrayList<Source>? = null
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SettingSourcesFragmentBinding.inflate(inflater, container, false)

        val adapter = SourcesAdapter(sources)
        binding.sourcesAdapter = adapter
        binding.rvSources.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        val properties = DialogProperties().apply {
            extensions = arrayOf("json","m3u")
            selection_mode = DialogConfigs.MULTI_MODE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                root = File(Environment.getExternalStorageDirectory().path)
                error_dir = File(Environment.getExternalStorageDirectory().path)
                offset  = File(Environment.getExternalStorageDirectory().path)
            } else {
                root = File("/")
                offset  = File("/mnt/sdcard:/storage")
            }
        }

        val filePicker = FilePickerDialog(requireContext()).apply {
            setTitle(getString(R.string.title_select_file_json))
            setProperties(properties)
            setDialogSelectionListener {
                for (path in it){
                    adapter.addItem(Source().apply {
                        this.path = path
                        active = true
                    })
                }
                sources = adapter.getItems()
            }
        }

        binding.btnPick.setOnClickListener {
            filePicker.show()
        }

        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        binding.inputSource.apply {
            setText(if (clipText.isLinkUrl()) clipText.trim() else "")
            setOnEditorActionListener { _, i, k ->
                if (i == EditorInfo.IME_ACTION_DONE || k.keyCode == KeyEvent.KEYCODE_ENTER) {
                    binding.btnAdd.performClick(); true
                }
                else false
            }
        }

        binding.btnAdd.setOnClickListener {
            val inputSource = binding.inputSource
            var input = inputSource.text.toString()
            if (input.isBlank()) {
                clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
                if (clipText.isLinkUrl()) input = clipText
                else return@setOnClickListener
            }
            else if (!input.isLinkUrl()) return@setOnClickListener

            it.isEnabled = false
            inputSource.isEnabled = false
            inputSource.setText(R.string.checking_url)

            val source = Source().apply {
                path = input
                active = true
            }

            PlaylistHelper().task(source, object: PlaylistHelper.TaskChecker{
                override fun onCheckResult(result: Boolean) {
                    it.isEnabled = true
                    inputSource.text?.clear()
                    inputSource.isEnabled =true
                    if (result) {
                        adapter.addItem(source)
                        sources = adapter.getItems()
                    }
                    else {
                        inputSource.setText(input)
                        Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show()
                    }
                }
            }).checkResult()
        }

        return binding.root
    }
}