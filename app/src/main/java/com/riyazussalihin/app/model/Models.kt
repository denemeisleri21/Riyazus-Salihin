package com.riyazussalihin.app.model

import android.content.Context
import androidx.core.content.ContextCompat
import com.riyazussalihin.app.R

data class IntroBlock(
    val type: String, // "verse" or "note"
    val arabic: String,
    val turkish: String
)

data class Chapter(
    val id: Int,
    val volume: Int,
    val title: String,
    val isKitap: Boolean,
    val printedPage: Int,
    val introBlocks: List<IntroBlock>,
    val hadithCount: Int,
    val startHadithId: Int
)

data class Hadith(
    val id: Int,
    val chapterId: Int,
    val volume: Int,
    val printedPage: Int,
    val arabic: String,
    val turkish: String,
    val source: String
)

sealed class ReaderPage {
    data class ChapterIntro(val chapter: Chapter) : ReaderPage()
    data class HadithItem(val hadith: Hadith, val chapter: Chapter?) : ReaderPage()
}

data class BookDatabase(
    val title: String,
    val subtitle: String,
    val author: String,
    val translators: String,
    val chapters: List<Chapter>,
    val hadiths: List<Hadith>,
    val pages: List<ReaderPage>
)

data class BookmarkEntry(
    val hadithId: Int,
    val chapterTitle: String,
    val timestamp: Long
)

enum class ReadingMode {
    VERTICAL_CONTINUOUS,
    HORIZONTAL_CONTINUOUS,
    HADITH_PAGER
}

enum class AppTheme {
    SHAMUA,
    WHITE,
    NIGHT
}

data class ThemePalette(
    val background: Int,
    val surface: Int,
    val card: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accent: Int,
    val divider: Int
) {
    companion object {
        fun from(context: Context, theme: AppTheme): ThemePalette {
            return when (theme) {
                AppTheme.SHAMUA -> ThemePalette(
                    background = ContextCompat.getColor(context, R.color.shamua_bg),
                    surface = ContextCompat.getColor(context, R.color.shamua_surface),
                    card = ContextCompat.getColor(context, R.color.shamua_card),
                    textPrimary = ContextCompat.getColor(context, R.color.shamua_text),
                    textSecondary = ContextCompat.getColor(context, R.color.shamua_text_secondary),
                    accent = ContextCompat.getColor(context, R.color.shamua_accent),
                    divider = ContextCompat.getColor(context, R.color.shamua_divider)
                )
                AppTheme.WHITE -> ThemePalette(
                    background = ContextCompat.getColor(context, R.color.white_bg),
                    surface = ContextCompat.getColor(context, R.color.white_surface),
                    card = ContextCompat.getColor(context, R.color.white_card),
                    textPrimary = ContextCompat.getColor(context, R.color.white_text),
                    textSecondary = ContextCompat.getColor(context, R.color.white_text_secondary),
                    accent = ContextCompat.getColor(context, R.color.white_accent),
                    divider = ContextCompat.getColor(context, R.color.white_divider)
                )
                AppTheme.NIGHT -> ThemePalette(
                    background = ContextCompat.getColor(context, R.color.night_bg),
                    surface = ContextCompat.getColor(context, R.color.night_surface),
                    card = ContextCompat.getColor(context, R.color.night_card),
                    textPrimary = ContextCompat.getColor(context, R.color.night_text),
                    textSecondary = ContextCompat.getColor(context, R.color.night_text_secondary),
                    accent = ContextCompat.getColor(context, R.color.night_accent),
                    divider = ContextCompat.getColor(context, R.color.night_divider)
                )
            }
        }
    }
}

data class ReaderSettings(
    val readingMode: ReadingMode = ReadingMode.VERTICAL_CONTINUOUS,
    val showArabic: Boolean = true,
    val showHarakat: Boolean = true,
    val showTurkish: Boolean = true,
    val fontSizeSp: Float = 18f,
    val theme: AppTheme = AppTheme.SHAMUA
)
