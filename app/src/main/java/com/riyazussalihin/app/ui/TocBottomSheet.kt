package com.riyazussalihin.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.riyazussalihin.app.R
import com.riyazussalihin.app.model.Chapter
import com.riyazussalihin.app.model.ThemePalette

class TocBottomSheet(
    private val allChapters: List<Chapter>,
    private val maxHadithId: Int,
    private val palette: ThemePalette,
    private val onSelectChapter: (Chapter) -> Unit,
    private val onSelectHadithId: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedVolume: Int = 0
    private var query: String = ""
    private lateinit var adapter: ChapterListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_toc, container, false)

    override fun onStart() {
        super.onStart()
        val sheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.let {
            it.layoutParams.height = (resources.displayMetrics.heightPixels * 0.85).toInt()
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = view.findViewById<View>(R.id.tocRoot)
        val tvTitle = view.findViewById<TextView>(R.id.tvTocTitle)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseToc)
        val etSearch = view.findViewById<EditText>(R.id.etSearchToc)
        val btnJump = view.findViewById<Button>(R.id.btnJumpHadith)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupVolumes)
        val rv = view.findViewById<RecyclerView>(R.id.rvToc)

        root.setBackgroundColor(palette.surface)
        tvTitle.setTextColor(palette.textPrimary)
        btnClose.imageTintList = ColorStateList.valueOf(palette.textPrimary)
        etSearch.setTextColor(palette.textPrimary)
        etSearch.setHintTextColor(palette.textSecondary)

        btnClose.setOnClickListener { dismiss() }

        adapter = ChapterListAdapter(palette) { chapter ->
            onSelectChapter(chapter)
            dismiss()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        fun applyFilter() {
            val q = query.trim().lowercase()
            val filtered = allChapters.filter { ch ->
                val volMatch = selectedVolume == 0 || ch.volume == selectedVolume
                val textMatch = q.isEmpty() ||
                    ch.title.lowercase().contains(q) ||
                    ch.startHadithId.toString() == q
                volMatch && textMatch
            }
            adapter.submitList(filtered)
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedVolume = when (checkedIds.firstOrNull()) {
                R.id.chipVol1 -> 1
                R.id.chipVol2 -> 2
                R.id.chipVol3 -> 3
                else -> 0
            }
            applyFilter()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString() ?: ""
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnJump.setOnClickListener {
            val num = etSearch.text.toString().trim().toIntOrNull()
            if (num != null && num in 1..maxHadithId) {
                onSelectHadithId(num)
                dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Lütfen 1 ile $maxHadithId arasında bir hadis numarası yazın.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        applyFilter()
    }

    private class ChapterListAdapter(
        private val palette: ThemePalette,
        private val onClick: (Chapter) -> Unit
    ) : RecyclerView.Adapter<ChapterListAdapter.VH>() {

        private var items: List<Chapter> = emptyList()

        fun submitList(newItems: List<Chapter>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_toc_chapter, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ch = items[position]
            holder.tvTitle.text = ch.title
            holder.tvTitle.setTextColor(if (ch.isKitap) palette.accent else palette.textPrimary)
            holder.tvTitle.paint.isFakeBoldText = ch.isKitap

            val countText = if (ch.hadithCount > 0) "${ch.hadithCount} hadis" else "Bölüm başlığı"
            holder.tvMeta.text = "${ch.volume}. Cilt • $countText"
            holder.tvMeta.setTextColor(palette.textSecondary)

            holder.tvBadge.text = if (ch.startHadithId > 0) "#${ch.startHadithId}" else "C.${ch.volume}"
            holder.tvBadge.setTextColor(palette.accent)

            holder.itemView.setOnClickListener { onClick(ch) }
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTocChapterTitle)
            val tvMeta: TextView = v.findViewById(R.id.tvTocChapterMeta)
            val tvBadge: TextView = v.findViewById(R.id.tvTocStartHadith)
        }
    }
}
