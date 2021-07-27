/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.harimurti.tv.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.ui.TrackSelectionView
import com.google.android.exoplayer2.ui.TrackSelectionView.TrackSelectionListener
import com.google.android.exoplayer2.util.Assertions
import com.google.android.material.tabs.TabLayout
import net.harimurti.tv.PlayerActivity
import net.harimurti.tv.R
import net.harimurti.tv.extra.Preferences
import java.util.*

/** Dialog to select tracks.  */
@Suppress("DEPRECATION")
class TrackSelectionDialog : DialogFragment() {
    private val tabFragments: SparseArray<TrackSelectionViewFragment> = SparseArray()
    private val tabTrackTypes: ArrayList<Int> = ArrayList()
    private var titleId = 0
    private lateinit var onClickListener: DialogInterface.OnClickListener
    private lateinit var onDismissListener: DialogInterface.OnDismissListener

    private fun init(titleId: Int, mappedTrackInfo: MappedTrackInfo, initialParameters: DefaultTrackSelector.Parameters, allowAdaptiveSelections: Boolean, allowMultipleOverrides: Boolean, onClickListener: DialogInterface.OnClickListener, onDismissListener: DialogInterface.OnDismissListener) {
        this.titleId = titleId
        this.onClickListener = onClickListener
        this.onDismissListener = onDismissListener
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (showTabForRenderer(mappedTrackInfo, i)) {
                val trackType = mappedTrackInfo.getRendererType(i)
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                val tabFragment = TrackSelectionViewFragment()
                tabFragment.init(mappedTrackInfo, i, initialParameters.getRendererDisabled(i), initialParameters.getSelectionOverride(i, trackGroupArray), allowAdaptiveSelections, allowMultipleOverrides)
                tabFragments.put(i, tabFragment)
                tabTrackTypes.add(trackType)
            }
        }
    }

    /**
     * Returns whether a renderer is disabled.
     *
     * @param rendererIndex Renderer index.
     * @return Whether the renderer is disabled.
     */
    fun getIsDisabled(rendererIndex: Int): Boolean {
        val rendererView = tabFragments[rendererIndex]
        return rendererView != null && rendererView.isDisabled
    }

    /**
     * Returns the list of selected track selection overrides for the specified renderer. There will
     * be at most one override for each track group.
     *
     * @param rendererIndex Renderer index.
     * @return The list of track selection overrides for this renderer.
     */
    fun getOverrides(rendererIndex: Int): List<SelectionOverride> {
        val rendererView = tabFragments[rendererIndex]
        return if (rendererView == null) emptyList() else rendererView.overrides!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // We need to own the view to let tab layout work correctly on all API levels. We can't use
        // AlertDialog because it owns the view itself, so we use AppCompatDialog instead, themed using
        // the AlertDialog theme overlay with force-enabled title.
        val dialog = AppCompatDialog(activity, R.style.TrackSelectionDialogThemeOverlay)
        dialog.setTitle(titleId)
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener.onDismiss(dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false)
        val viewPager = dialogView.findViewById<ViewPager>(R.id.track_selection_dialog_view_pager).apply {
            adapter = FragmentAdapter(childFragmentManager)
        }
        dialogView.findViewById<TabLayout>(R.id.track_selection_dialog_tab_layout).apply {
            setupWithViewPager(viewPager)
            visibility = if (tabFragments.size() > 1) View.VISIBLE else View.GONE
        }
        // dropdown screen mode
        val screenMode = dialogView.findViewById<AppCompatSpinner>(R.id.dropdown_screen_mode).apply {
            val arrayAdapter = ArrayAdapter.createFromResource(context as Context, R.array.resize_mode, android.R.layout.simple_spinner_item)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = arrayAdapter
            setSelection(Preferences(context).resizeMode)
        }
        // button cancel
        dialogView.findViewById<Button>(R.id.track_selection_dialog_cancel_button).apply {
            setOnClickListener { dismiss() }
        }
        // button ok
        dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button).apply {
            setOnClickListener {
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                    Intent(PlayerActivity.PLAYER_CALLBACK).apply {
                        putExtra(PlayerActivity.PLAYER_CALLBACK, PlayerActivity.CHANGE_SCREEN_MODE)
                        putExtra(PlayerActivity.SCREEN_MODE, screenMode.selectedItemPosition)
                })
                onClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                dismiss()
            }
        }
        return dialogView
    }

    private inner class FragmentAdapter(fragmentManager: FragmentManager?) :
        FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return tabFragments.valueAt(position)
        }

        override fun getCount(): Int {
            return tabFragments.size()
        }

        override fun getPageTitle(position: Int): CharSequence {
            return getTrackTypeString(resources, tabTrackTypes[position])
        }
    }

    /** Fragment to show a track selection in tab of the track selection dialog.  */
    class TrackSelectionViewFragment : Fragment(), TrackSelectionListener {
        private var mappedTrackInfo: MappedTrackInfo? = null
        private var rendererIndex = 0
        private var allowAdaptiveSelections = false
        private var allowMultipleOverrides = false

        /* package */
        var isDisabled = false

        /* package */
        var overrides: List<SelectionOverride>? = null
        fun init(mappedTrackInfo: MappedTrackInfo?, rendererIndex: Int, initialIsDisabled: Boolean, initialOverride: SelectionOverride?, allowAdaptiveSelections: Boolean, allowMultipleOverrides: Boolean) {
            this.mappedTrackInfo = mappedTrackInfo
            this.rendererIndex = rendererIndex
            isDisabled = initialIsDisabled
            overrides = initialOverride?.let { listOf(it) } ?: emptyList()
            this.allowAdaptiveSelections = allowAdaptiveSelections
            this.allowMultipleOverrides = allowMultipleOverrides
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.exo_track_selection_dialog, container,false)
            val trackSelectionView: TrackSelectionView = rootView.findViewById(R.id.exo_track_selection_view)
            trackSelectionView.setShowDisableOption(true)
            trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides)
            trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections)
            trackSelectionView.init(mappedTrackInfo!!, rendererIndex, isDisabled, overrides!!, null, this)
            return rootView
        }

        override fun onTrackSelectionChanged(isDisabled: Boolean, overrides: List<SelectionOverride>) {
            this.isDisabled = isDisabled
            this.overrides = overrides
        }

        init {
            // Retain instance across activity re-creation to prevent losing access to init data.
            retainInstance = true
        }
    }

    companion object {
        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified [DefaultTrackSelector] in its current state.
         */
        fun willHaveContent(trackSelector: DefaultTrackSelector?): Boolean {
            val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
            return mappedTrackInfo != null && willHaveContent(mappedTrackInfo)
        }

        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified [MappedTrackInfo].
         */
        private fun willHaveContent(mappedTrackInfo: MappedTrackInfo): Boolean {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                if (showTabForRenderer(mappedTrackInfo, i)) {
                    return true
                }
            }
            return false
        }

        /**
         * Creates a dialog for a given [DefaultTrackSelector], whose parameters will be
         * automatically updated when tracks are selected.
         *
         * @param trackSelector The [DefaultTrackSelector].
         * @param onDismissListener A [DialogInterface.OnDismissListener] to call when the dialog is
         * dismissed.
         */
        fun createForTrackSelector(trackSelector: DefaultTrackSelector?, onDismissListener: DialogInterface.OnDismissListener): TrackSelectionDialog {
            val mappedTrackInfo = Assertions.checkNotNull(trackSelector!!.currentMappedTrackInfo)
            val trackSelectionDialog = TrackSelectionDialog()
            val parameters = trackSelector.parameters
            trackSelectionDialog.init(R.string.track_selection_title, mappedTrackInfo, parameters, allowAdaptiveSelections = true, allowMultipleOverrides = false, onClickListener = {
                    _: DialogInterface?, _: Int ->
                    val builder = parameters.buildUpon()
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        builder.clearSelectionOverrides(i).setRendererDisabled(i, trackSelectionDialog.getIsDisabled(i))
                        val overrides = trackSelectionDialog.getOverrides(i)
                        if (overrides.isNotEmpty()) {
                            builder.setSelectionOverride(i, mappedTrackInfo.getTrackGroups(i), overrides[0])
                        }
                    }
                    trackSelector.setParameters(builder)
                },
                onDismissListener = onDismissListener
            )
            return trackSelectionDialog
        }

        /**
         * Creates a dialog for given [MappedTrackInfo] and [DefaultTrackSelector.Parameters].
         *
         * @param titleId The resource id of the dialog title.
         * @param mappedTrackInfo The [MappedTrackInfo] to display.
         * @param initialParameters The [DefaultTrackSelector.Parameters] describing the initial
         * track selection.
         * @param allowAdaptiveSelections Whether adaptive selections (consisting of more than one track)
         * can be made.
         * @param allowMultipleOverrides Whether tracks from multiple track groups can be selected.
         * @param onClickListener [DialogInterface.OnClickListener] called when tracks are selected.
         * @param onDismissListener [DialogInterface.OnDismissListener] called when the dialog is
         * dismissed.
         */
        fun createForMappedTrackInfoAndParameters(titleId: Int, mappedTrackInfo: MappedTrackInfo, initialParameters: DefaultTrackSelector.Parameters, allowAdaptiveSelections: Boolean, allowMultipleOverrides: Boolean, onClickListener: DialogInterface.OnClickListener, onDismissListener: DialogInterface.OnDismissListener): TrackSelectionDialog {
            val trackSelectionDialog = TrackSelectionDialog()
            trackSelectionDialog.init(titleId, mappedTrackInfo, initialParameters, allowAdaptiveSelections, allowMultipleOverrides, onClickListener, onDismissListener)
            return trackSelectionDialog
        }

        private fun showTabForRenderer(mappedTrackInfo: MappedTrackInfo, rendererIndex: Int): Boolean {
            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
            if (trackGroupArray.length == 0) {
                return false
            }
            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
            return isSupportedTrackType(trackType)
        }

        private fun isSupportedTrackType(trackType: Int): Boolean {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT -> true
                else -> false
            }
        }

        private fun getTrackTypeString(resources: Resources, trackType: Int): String {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> resources.getString(R.string.exo_track_selection_title_video)
                C.TRACK_TYPE_AUDIO -> resources.getString(R.string.exo_track_selection_title_audio)
                C.TRACK_TYPE_TEXT -> resources.getString(R.string.exo_track_selection_title_text)
                else -> throw IllegalArgumentException()
            }
        }
    }

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }
}