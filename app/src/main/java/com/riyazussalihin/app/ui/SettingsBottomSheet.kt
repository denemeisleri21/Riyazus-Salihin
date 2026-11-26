package com.riyazussalihin.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.riyazussalihin.app.R
import com.riyazussalihin.app.model.AppTheme
import com.riyazussalihin.app.model.ReaderSettings
import com.riyazussalihin.app.model.ReadingMode
import com.riyazussalihin.app.model.ThemePalette

class SettingsBottomSheet(
    private var currentSettings: ReaderSettings,
    private val onSettingsChanged: (ReaderSettings) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = view.findViewById<View>(R.id.settingsRoot)
        val tvTitle = view.findViewById<TextView>(R.id.tvSettingsTitle)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseSettings)
        val chipGroupMode = view.findViewById<ChipGroup>(R.id.chipGroupReadingMode)
        val switchArabic = view.findViewById<MaterialSwitch>(R.id.switchArabic)
        val switchTurkish = view.findViewById<MaterialSwitch>(R.id.switchTurkish)
        val sliderFont = view.findViewById<Slider>(R.id.sliderFontSize)
        val tvFontVal = view.findViewById<TextView>(R.id.tvFontSizeValue)
        val btnShamua = view.findViewById<MaterialButton>(R.id.btnThemeShamua)
        val btnWhite = view.findViewById<MaterialButton>(R.id.btnThemeWhite)
        val btnNight = view.findViewById<MaterialButton>(R.id.btnThemeNight)

        val labels = listOf<TextView>(
            view.findViewById(R.id.tvLabelMode),
            view.findViewById(R.id.tvLabelLang),
            view.findViewById(R.id.tvLabelFontSize),
            view.findViewById(R.id.tvLabelTheme)
        )

        fun applySheetTheme() {
            val pal = ThemePalette.from(requireContext(), currentSettings.theme)
            root.setBackgroundColor(pal.surface)
            tvTitle.setTextColor(pal.textPrimary)
            btnClose.imageTintList = ColorStateList.valueOf(pal.textPrimary)
            labels.forEach { it.setTextColor(pal.accent) }
            switchArabic.setTextColor(pal.textPrimary)
            switchTurkish.setTextColor(pal.textPrimary)
            tvFontVal.setTextColor(pal.textPrimary)

            btnShamua.strokeWidth = if (currentSettings.theme == AppTheme.SHAMUA) 6 else 2
            btnWhite.strokeWidth = if (currentSettings.theme == AppTheme.WHITE) 6 else 2
            btnNight.strokeWidth = if (currentSettings.theme == AppTheme.NIGHT) 6 else 2
        }

        // Initial values
        when (currentSettings.readingMode) {
            ReadingMode.VERTICAL_CONTINUOUS -> chipGroupMode.check(R.id.chipModeVertical)
            ReadingMode.HORIZONTAL_CONTINUOUS -> chipGroupMode.check(R.id.chipModeHorizontal)
            ReadingMode.HADITH_PAGER -> chipGroupMode.check(R.id.chipModePager)
        }
        switchArabic.isChecked = currentSettings.showArabic
        switchTurkish.isChecked = currentSettings.showTurkish
        sliderFont.value = currentSettings.fontSizeSp.coerceIn(14f, 30f)
        tvFontVal.text = "${sliderFont.value.toInt()} sp"
        applySheetTheme()

        btnClose.setOnClickListener { dismiss() }

        chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chipModeHorizontal -> ReadingMode.HORIZONTAL_CONTINUOUS
                R.id.chipModePager -> ReadingMode.HADITH_PAGER
                else -> ReadingMode.VERTICAL_CONTINUOUS
            }
            if (mode != currentSettings.readingMode) {
                currentSettings = currentSettings.copy(readingMode = mode)
                onSettingsChanged(currentSettings)
            }
        }

        switchArabic.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !switchTurkish.isChecked) {
                switchArabic.isChecked = true
                Toast.makeText(requireContext(), R.string.at_least_one_language, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            currentSettings = currentSettings.copy(showArabic = isChecked)
            onSettingsChanged(currentSettings)
        }

        switchTurkish.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !switchArabic.isChecked) {
                switchTurkish.isChecked = true
                Toast.makeText(requireContext(), R.string.at_least_one_language, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            currentSettings = currentSettings.copy(showTurkish = isChecked)
            onSettingsChanged(currentSettings)
        }

        sliderFont.addOnChangeListener { _, value, fromUser ->
            tvFontVal.text = "${value.toInt()} sp"
            if (fromUser) {
                currentSettings = currentSettings.copy(fontSizeSp = value)
                onSettingsChanged(currentSettings)
            }
        }

        fun selectTheme(theme: AppTheme) {
            currentSettings = currentSettings.copy(theme = theme)
            applySheetTheme()
            onSettingsChanged(currentSettings)
        }

        btnShamua.setOnClickListener { selectTheme(AppTheme.SHAMUA) }
        btnWhite.setOnClickListener { selectTheme(AppTheme.WHITE) }
        btnNight.setOnClickListener { selectTheme(AppTheme.NIGHT) }
    }
}
