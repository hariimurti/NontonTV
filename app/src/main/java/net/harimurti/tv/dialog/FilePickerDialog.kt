package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.widget.AppCompatCheckBox
import com.developer.filepicker.controller.DialogSelectionListener
import com.developer.filepicker.controller.adapters.FileListAdapter
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.model.FileListItem
import com.developer.filepicker.model.MarkedItemList
import com.developer.filepicker.utils.ExtensionFilter
import com.developer.filepicker.utils.Utility
import com.developer.filepicker.widget.MaterialCheckbox
import net.harimurti.tv.R
import java.io.File
import java.util.*

@Suppress("DEPRECATION")
class FilePickerDialog(context: Context) : Dialog(context), OnItemClickListener {
    private lateinit var listView: ListView
    private var dname: TextView? = null
    private var dirPath: TextView? = null
    private var title: TextView? = null
    private var properties: DialogProperties
    private var callbacks: DialogSelectionListener? = null
    private var internalList: ArrayList<FileListItem>
    private var filter: ExtensionFilter
    private var mFileListAdapter: FileListAdapter? = null
    private lateinit var select: Button
    private var titleStr: String? = null
    private var positiveBtnNameStr: String? = null
    private var negativeBtnNameStr: String? = null
    private lateinit var showHide: AppCompatCheckBox
    private lateinit var imageView: ImageView
    private lateinit var offset1: File
    private lateinit var offset2: File

    companion object {
        const val EXTERNAL_READ_PERMISSION_GRANT = 112
    }
    init {
        properties = DialogProperties()
        filter = ExtensionFilter(properties)
        internalList = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.picker_dialog)
        listView = findViewById(R.id.fileList)
        select = findViewById(R.id.select)
        showHide = findViewById(R.id.show_hide)
        imageView = findViewById(R.id.imageView)
        val size = MarkedItemList.getFileCount()
        if (size == 0) {
            select.isEnabled = false
            val color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.resources.getColor(R.color.color_text, context.theme)
            } else {
                context.resources.getColor(R.color.color_text)
            }
            select.setTextColor(
                Color.argb( 128, Color.red(color), Color.green(color), Color.blue(color))
            )
        }
        dname = findViewById(R.id.dname)
        title = findViewById(R.id.title)
        dirPath = findViewById(R.id.dir_path)
        val cancel = findViewById<Button>(R.id.cancel)
        if (negativeBtnNameStr != null) {
            cancel.text = negativeBtnNameStr
        }
        select.setOnClickListener {
            val paths = MarkedItemList.getSelectedPaths()
            if (callbacks != null) {
                callbacks!!.onSelectedFilePaths(paths)
            }
            dismiss()
        }
        cancel.setOnClickListener { cancel() }
        mFileListAdapter = FileListAdapter(internalList, context, properties)
        mFileListAdapter!!.setNotifyItemCheckedListener {
            positiveBtnNameStr =
                if (positiveBtnNameStr == null) context.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
            val size1 = MarkedItemList.getFileCount()
            if (size1 == 0) {
                select.isEnabled = false
                val color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.resources.getColor(R.color.color_text, context.theme)
                } else {
                    context.resources.getColor(R.color.color_text)
                }
                select.setTextColor(
                    Color.argb( 128, Color.red(color), Color.green(color), Color.blue(color))
                )
                select.text = positiveBtnNameStr
            } else {
                select.isEnabled = true
                val color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.resources.getColor(R.color.color_text, context.theme)
                } else {
                    context.resources.getColor(R.color.color_text)
                }
                select.setTextColor(color)
                val buttonLabel = "$positiveBtnNameStr ($size1) "
                select.text = buttonLabel
            }
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                mFileListAdapter!!.notifyDataSetChanged()
            }
        }
        listView.adapter = mFileListAdapter

        setTitle()
        if(properties.offset.absolutePath.toString().contains(":")){
            splitOffset()
            imageView.setOnClickListener {
                if(properties.offset == offset1)
                    properties.offset = offset2
                else properties.offset = offset1
                prepareDialog()
            }
        }

    }
    private fun splitOffset() {
        val split = properties.offset.absolutePath.toString().split(":".toRegex()).toTypedArray()
        offset1 = File(split[0])
        offset2 = File(split[1])
        properties.offset = offset1
    }

    private fun setTitle() {
        if (title == null || dname == null) {
            return
        }
        if (titleStr != null) {
            if (title!!.visibility == View.INVISIBLE) {
                title!!.visibility = View.VISIBLE
            }
            title!!.text = titleStr
            if (dname!!.visibility == View.VISIBLE) {
                dname!!.visibility = View.INVISIBLE
            }
        } else {
            if (title!!.visibility == View.VISIBLE) {
                title!!.visibility = View.INVISIBLE
            }
            if (dname!!.visibility == View.INVISIBLE) {
                dname!!.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        positiveBtnNameStr =
            if (positiveBtnNameStr == null) context.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
        select.text = positiveBtnNameStr
        prepareDialog()
        listView.onItemClickListener = this

        showHide.setOnCheckedChangeListener {_,ischeck ->
            properties.show_hidden_files = ischeck
            prepareDialog()
        }
    }

    private fun prepareDialog() {
        val currLoc: File
        internalList.clear()
        if (properties.offset.isDirectory && validateOffsetPath()) {
            currLoc = File(properties.offset.path)
            val parent = FileListItem()
            parent.filename = context.getString(R.string.label_parent_dir)
            parent.isDirectory = true
            parent.location = currLoc.parentFile?.path
            parent.time = currLoc.lastModified()
            internalList.add(parent)
        } else if (properties.root.exists() && properties.root.isDirectory) {
            currLoc = File(properties.root.path)
        } else {
            currLoc = File(properties.error_dir.path)
        }
        dname!!.text = currLoc.name
        dirPath!!.text = currLoc.path
        setTitle()
        internalList = Utility.prepareFileListEntries(
            internalList,
            currLoc,
            filter,
            properties.show_hidden_files
        )
        mFileListAdapter!!.notifyDataSetChanged()
    }

    private fun validateOffsetPath(): Boolean {
        val offsetPath = properties.offset.path
        val rootPath = properties.root.path
        return offsetPath != rootPath && offsetPath.contains(rootPath)
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
        if (internalList.size > i) {
            val fitem = internalList[i]
            if (fitem.isDirectory) {
                if (File(fitem.location).canRead()) {
                    val currLoc = File(fitem.location)
                    dname!!.text = currLoc.name
                    setTitle()
                    dirPath!!.text = currLoc.path
                    internalList.clear()
                    if (currLoc.name != properties.root.name) {
                        val parent = FileListItem()
                        parent.filename = context.getString(R.string.label_parent_dir)
                        parent.isDirectory = true
                        parent.location = currLoc.parentFile?.path
                        parent.time = currLoc.lastModified()
                        internalList.add(parent)
                    }
                    internalList = Utility.prepareFileListEntries(
                        internalList,
                        currLoc,
                        filter,
                        properties.show_hidden_files
                    )
                    mFileListAdapter!!.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, R.string.error_dir_access, Toast.LENGTH_SHORT).show()
                }
            } else {
                val fmark: MaterialCheckbox = view.findViewById(R.id.file_mark)
                fmark.performClick()
            }
        }
    }

    fun setProperties(properties: DialogProperties) {
        this.properties = properties
        filter = ExtensionFilter(properties)
    }

    fun setDialogSelectionListener(callbacks: DialogSelectionListener?) {
        this.callbacks = callbacks
    }

    override fun setTitle(titleStr: CharSequence?) {
        if (titleStr != null) {
            this.titleStr = titleStr.toString()
        } else {
            this.titleStr = null
        }
        setTitle()
    }

    /*fun setPositiveBtnName(positiveBtnNameStr: CharSequence?) {
        if (positiveBtnNameStr != null) {
            this.positiveBtnNameStr = positiveBtnNameStr.toString()
        } else {
            this.positiveBtnNameStr = null
        }
    }

    fun setNegativeBtnName(negativeBtnNameStr: CharSequence?) {
        if (negativeBtnNameStr != null) {
            this.negativeBtnNameStr = negativeBtnNameStr.toString()
        } else {
            this.negativeBtnNameStr = null
        }
    }*/

    //mark file when open
    /*fun markFiles(paths: List<String?>?) {
        if (paths != null && paths.size > 0) {
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                val temp = File(paths[0])
                when (properties.selection_type) {
                    DialogConfigs.DIR_SELECT -> if (temp.exists() && temp.isDirectory) {
                        val item = FileListItem()
                        item.filename = temp.name
                        item.isDirectory = temp.isDirectory
                        item.isMarked = true
                        item.time = temp.lastModified()
                        item.location = temp.path
                        MarkedItemList.addSelectedItem(item)
                    }
                    DialogConfigs.FILE_SELECT -> if (temp.exists() && temp.isFile) {
                        val item = FileListItem()
                        item.filename = temp.name
                        item.isDirectory = temp.isDirectory
                        item.isMarked = true
                        item.time = temp.lastModified()
                        item.location = temp.path
                        MarkedItemList.addSelectedItem(item)
                    }
                    DialogConfigs.FILE_AND_DIR_SELECT -> if (temp.exists()) {
                        val item = FileListItem()
                        item.filename = temp.name
                        item.isDirectory = temp.isDirectory
                        item.isMarked = true
                        item.time = temp.lastModified()
                        item.location = temp.path
                        MarkedItemList.addSelectedItem(item)
                    }
                }
            } else {
                for (path in paths) {
                    when (properties.selection_type) {
                        DialogConfigs.DIR_SELECT -> {
                            val temp = File(path)
                            if (temp.exists() && temp.isDirectory) {
                                val item = FileListItem()
                                item.filename = temp.name
                                item.isDirectory = temp.isDirectory
                                item.isMarked = true
                                item.time = temp.lastModified()
                                item.location = temp.path
                                MarkedItemList.addSelectedItem(item)
                            }
                        }
                        DialogConfigs.FILE_SELECT -> {
                            val temp = File(path)
                            if (temp.exists() && temp.isFile) {
                                val item = FileListItem()
                                item.filename = temp.name
                                item.isDirectory = temp.isDirectory
                                item.isMarked = true
                                item.time = temp.lastModified()
                                item.location = temp.path
                                MarkedItemList.addSelectedItem(item)
                            }
                        }
                        DialogConfigs.FILE_AND_DIR_SELECT -> {
                            val temp = File(path)
                            if (temp.exists() && (temp.isFile || temp.isDirectory)) {
                                val item = FileListItem()
                                item.filename = temp.name
                                item.isDirectory = temp.isDirectory
                                item.isMarked = true
                                item.time = temp.lastModified()
                                item.location = temp.path
                                MarkedItemList.addSelectedItem(item)
                            }
                        }
                    }
                }
            }
        }
    }*/

    override fun show() {
        super.show()
        positiveBtnNameStr =
            if (positiveBtnNameStr == null) context.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
        select.text = positiveBtnNameStr
        val size = MarkedItemList.getFileCount()
        if (size == 0) {
            select.text = positiveBtnNameStr
        } else {
            val buttonLabel = "$positiveBtnNameStr ($size) "
            select.text = buttonLabel
        }
    }

    override fun onBackPressed() {
        //currentDirName is dependent on dname
        val currentDirName = dname!!.text.toString()
        if (internalList.size > 0) {
            val fitem = internalList[0]
            val currLoc = File(fitem.location)
            if (currentDirName == properties.root.name ||
                !currLoc.canRead()
            ) {
                super.onBackPressed()
            } else {
                dname!!.text = currLoc.name
                dirPath!!.text = currLoc.path
                internalList.clear()
                if (currLoc.name != properties.root.name) {
                    val parent = FileListItem()
                    parent.filename = context.getString(R.string.label_parent_dir)
                    parent.isDirectory = true
                    parent.location = currLoc.parentFile?.path
                    parent.time = currLoc.lastModified()
                    internalList.add(parent)
                }
                internalList = Utility.prepareFileListEntries(
                    internalList,
                    currLoc,
                    filter,
                    properties.show_hidden_files
                )
                mFileListAdapter!!.notifyDataSetChanged()
            }
            setTitle()
        } else {
            super.onBackPressed()
        }
    }

    override fun dismiss() {
        MarkedItemList.clearSelectionList()
        internalList.clear()
        super.dismiss()
    }
}