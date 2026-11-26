package com.riyazussalihin.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.riyazussalihin.app.R
import com.riyazussalihin.app.data.WordLookupResult
import com.riyazussalihin.app.model.ThemePalette

class WordDetailBottomSheet(
    private val result: WordLookupResult,
    private val palette: ThemePalette
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_word_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = view.findViewById<View>(R.id.wordDetailRoot)
        val tvHeader = view.findViewById<TextView>(R.id.tvWordDetailHeader)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseWordDetail)
        val tvWord = view.findViewById<TextView>(R.id.tvWordArabic)
        val tvRootBadge = view.findViewById<TextView>(R.id.tvWordRootBadge)
        val divider = view.findViewById<View>(R.id.wordDivider)
        val tvLabelMeaning = view.findViewById<TextView>(R.id.tvLabelMeaning)
        val tvMeaning = view.findViewById<TextView>(R.id.tvWordMeaning)
        val tvLabelRootInfo = view.findViewById<TextView>(R.id.tvLabelRootInfo)
        val tvRootInfo = view.findViewById<TextView>(R.id.tvWordRootInfo)
        val tvLabelMorph = view.findViewById<TextView>(R.id.tvLabelMorph)
        val tvMorph = view.findViewById<TextView>(R.id.tvWordMorph)

        root.setBackgroundColor(palette.surface)
        tvHeader.setTextColor(palette.textSecondary)
        btnClose.imageTintList = ColorStateList.valueOf(palette.textPrimary)
        btnClose.setOnClickListener { dismiss() }

        tvWord.text = result.originalWord
        tvWord.setTextColor(palette.textPrimary)

        tvRootBadge.text = "${getString(R.string.word_label_root)}: ${result.root}"
        tvRootBadge.setTextColor(palette.accent)

        divider.setBackgroundColor(palette.divider)

        tvLabelMeaning.setTextColor(palette.accent)
        tvMeaning.text = result.meaning
        tvMeaning.setTextColor(palette.textPrimary)

        if (result.rootInfo.isNotBlank()) {
            tvLabelRootInfo.visibility = View.VISIBLE
            tvRootInfo.visibility = View.VISIBLE
            tvLabelRootInfo.setTextColor(palette.accent)
            tvRootInfo.text = result.rootInfo
            tvRootInfo.setTextColor(palette.textSecondary)
        } else {
            tvLabelRootInfo.visibility = View.GONE
            tvRootInfo.visibility = View.GONE
        }

        if (result.morphologyNote.isNotBlank()) {
            tvLabelMorph.visibility = View.VISIBLE
            tvMorph.visibility = View.VISIBLE
            tvLabelMorph.setTextColor(palette.accent)
            tvMorph.text = result.morphologyNote
            tvMorph.setTextColor(palette.textSecondary)
        } else {
            tvLabelMorph.visibility = View.GONE
            tvMorph.visibility = View.GONE
        }
    }
}
