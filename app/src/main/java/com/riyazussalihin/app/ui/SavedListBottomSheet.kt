package com.riyazussalihin.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.riyazussalihin.app.R
import com.riyazussalihin.app.model.ThemePalette

data class SavedDisplayItem(
    val hadithId: Int,
    val heading: String,
    val preview: String
)

class SavedListBottomSheet(
    private val title: String,
    private val emptyMessage: String,
    private val initialItems: List<SavedDisplayItem>,
    private val palette: ThemePalette,
    private val onSelectHadith: (Int) -> Unit,
    private val onDeleteHadith: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val items = initialItems.toMutableList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_saved_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = view.findViewById<View>(R.id.savedRoot)
        val tvTitle = view.findViewById<TextView>(R.id.tvSavedTitle)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseSaved)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptySaved)
        val rv = view.findViewById<RecyclerView>(R.id.rvSaved)

        root.setBackgroundColor(palette.surface)
        tvTitle.text = title
        tvTitle.setTextColor(palette.textPrimary)
        btnClose.imageTintList = ColorStateList.valueOf(palette.textPrimary)
        tvEmpty.text = emptyMessage
        tvEmpty.setTextColor(palette.textSecondary)

        btnClose.setOnClickListener { dismiss() }

        fun refreshEmptyState() {
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }

        val adapter = SavedAdapter(
            items = items,
            palette = palette,
            onClick = { item ->
                onSelectHadith(item.hadithId)
                dismiss()
            },
            onDelete = { item ->
                onDeleteHadith(item.hadithId)
                val idx = items.indexOfFirst { it.hadithId == item.hadithId }
                if (idx >= 0) {
                    items.removeAt(idx)
                    rv.adapter?.notifyItemRemoved(idx)
                }
                refreshEmptyState()
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        refreshEmptyState()
    }

    private class SavedAdapter(
        private val items: List<SavedDisplayItem>,
        private val palette: ThemePalette,
        private val onClick: (SavedDisplayItem) -> Unit,
        private val onDelete: (SavedDisplayItem) -> Unit
    ) : RecyclerView.Adapter<SavedAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_entry, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvHeading.text = item.heading
            holder.tvHeading.setTextColor(palette.textPrimary)
            holder.tvPreview.text = item.preview
            holder.tvPreview.setTextColor(palette.textSecondary)
            holder.btnDelete.imageTintList = ColorStateList.valueOf(palette.textSecondary)

            holder.itemView.setOnClickListener { onClick(item) }
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvHeading: TextView = v.findViewById(R.id.tvSavedHeading)
            val tvPreview: TextView = v.findViewById(R.id.tvSavedPreview)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteSaved)
        }
    }
}
