package com.riyazussalihin.app.data

import android.content.Context
import com.riyazussalihin.app.model.AppTheme
import com.riyazussalihin.app.model.BookmarkEntry
import com.riyazussalihin.app.model.ReaderSettings
import com.riyazussalihin.app.model.ReadingMode
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("riyazus_salihin_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): ReaderSettings {
        val modeName = prefs.getString(KEY_READING_MODE, ReadingMode.VERTICAL_CONTINUOUS.name)
        val themeName = prefs.getString(KEY_THEME, AppTheme.SHAMUA.name)
        return ReaderSettings(
            readingMode = runCatching { ReadingMode.valueOf(modeName!!) }
                .getOrDefault(ReadingMode.VERTICAL_CONTINUOUS),
            showArabic = prefs.getBoolean(KEY_SHOW_ARABIC, true),
            showTurkish = prefs.getBoolean(KEY_SHOW_TURKISH, true),
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, 18f),
            theme = runCatching { AppTheme.valueOf(themeName!!) }
                .getOrDefault(AppTheme.SHAMUA)
        )
    }

    fun saveSettings(settings: ReaderSettings) {
        prefs.edit()
            .putString(KEY_READING_MODE, settings.readingMode.name)
            .putBoolean(KEY_SHOW_ARABIC, settings.showArabic)
            .putBoolean(KEY_SHOW_TURKISH, settings.showTurkish)
            .putFloat(KEY_FONT_SIZE, settings.fontSizeSp)
            .putString(KEY_THEME, settings.theme.name)
            .apply()
    }

    fun getLastPageIndex(): Int = prefs.getInt(KEY_LAST_PAGE_INDEX, 0)

    fun saveLastPageIndex(pageIndex: Int, sync: Boolean = false) {
        val editor = prefs.edit().putInt(KEY_LAST_PAGE_INDEX, pageIndex)
        if (sync) editor.commit() else editor.apply()
    }

    // ---- Bookmarks ----
    fun getBookmarks(): MutableList<BookmarkEntry> {
        val raw = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val list = mutableListOf<BookmarkEntry>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookmarkEntry(
                        hadithId = obj.getInt("hadithId"),
                        chapterTitle = obj.optString("chapterTitle", ""),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        }
        return list
    }

    fun isBookmarked(hadithId: Int): Boolean = getBookmarks().any { it.hadithId == hadithId }

    fun toggleBookmark(hadithId: Int, chapterTitle: String): Boolean {
        val list = getBookmarks()
        val existing = list.indexOfFirst { it.hadithId == hadithId }
        val added = if (existing >= 0) {
            list.removeAt(existing)
            false
        } else {
            list.add(0, BookmarkEntry(hadithId, chapterTitle, System.currentTimeMillis()))
            true
        }
        saveBookmarks(list)
        return added
    }

    fun removeBookmark(hadithId: Int) {
        val list = getBookmarks()
        list.removeAll { it.hadithId == hadithId }
        saveBookmarks(list)
    }

    private fun saveBookmarks(list: List<BookmarkEntry>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("hadithId", b.hadithId)
                    .put("chapterTitle", b.chapterTitle)
                    .put("timestamp", b.timestamp)
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    // ---- Favorites ----
    fun getFavoriteIds(): MutableSet<Int> {
        val raw = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    fun isFavorite(hadithId: Int): Boolean = getFavoriteIds().contains(hadithId)

    fun toggleFavorite(hadithId: Int): Boolean {
        val set = getFavoriteIds()
        val added = if (set.contains(hadithId)) {
            set.remove(hadithId)
            false
        } else {
            set.add(hadithId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, set.map { it.toString() }.toSet()).apply()
        return added
    }

    fun removeFavorite(hadithId: Int) {
        val set = getFavoriteIds()
        set.remove(hadithId)
        prefs.edit().putStringSet(KEY_FAVORITES, set.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_READING_MODE = "reading_mode"
        private const val KEY_SHOW_ARABIC = "show_arabic"
        private const val KEY_SHOW_TURKISH = "show_turkish"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_THEME = "app_theme"
        private const val KEY_LAST_PAGE_INDEX = "last_page_index"
        private const val KEY_BOOKMARKS = "bookmarks_json"
        private const val KEY_FAVORITES = "favorite_ids"
    }
}
