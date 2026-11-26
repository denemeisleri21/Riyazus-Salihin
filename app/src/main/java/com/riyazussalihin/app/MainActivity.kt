package com.riyazussalihin.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.riyazussalihin.app.data.ArabicLexicon
import com.riyazussalihin.app.data.BookRepository
import com.riyazussalihin.app.data.PreferencesManager
import com.riyazussalihin.app.databinding.ActivityMainBinding
import com.riyazussalihin.app.model.BookDatabase
import com.riyazussalihin.app.model.Hadith
import com.riyazussalihin.app.model.ReaderPage
import com.riyazussalihin.app.model.ReaderSettings
import com.riyazussalihin.app.model.ReadingMode
import com.riyazussalihin.app.model.ThemePalette
import com.riyazussalihin.app.ui.ReaderAdapter
import com.riyazussalihin.app.ui.SavedDisplayItem
import com.riyazussalihin.app.ui.SavedListBottomSheet
import com.riyazussalihin.app.ui.SettingsBottomSheet
import com.riyazussalihin.app.ui.TocBottomSheet
import com.riyazussalihin.app.ui.WordDetailBottomSheet
import kotlinx.coroutines.launch
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BookRepository
    private lateinit var lexicon: ArabicLexicon
    private var lastWordTapTime: Long = 0L
    private lateinit var prefs: PreferencesManager

    private var bookDb: BookDatabase? = null
    private var readerAdapter: ReaderAdapter? = null
    private var settings: ReaderSettings = ReaderSettings()
    private var currentPageIndex: Int = 0
    private var chromeVisible: Boolean = true
    private var isRestoringPosition: Boolean = false

    // Center-tap tracking in dispatchTouchEvent (100% reliable across ScrollView, RecyclerView, ViewPager2)
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTime: Long = 0L
    private var validCenterTapCandidate: Boolean = false
    private var touchSlop: Int = 24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        repository = BookRepository(this)
        lexicon = ArabicLexicon(this)
        prefs = PreferencesManager(this)
        settings = prefs.loadSettings()

        enableFullScreenImmersive()
        applyThemeToChrome()
        setupMenuActions()
        loadBookData()
    }

    override fun onPause() {
        super.onPause()
        if (bookDb != null && !isRestoringPosition) {
            prefs.saveLastPageIndex(currentPageIndex, sync = true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bookDb != null && !isRestoringPosition) {
            prefs.saveLastPageIndex(currentPageIndex, sync = true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableFullScreenImmersive()
        }
    }

    /**
     * Ekranın ortasına dokunulduğunda alt-üst menüyü açıp kapatır.
     * ScrollView / RecyclerView / ViewPager2 kaydırmalarını veya buton tıklamalarını etkilemez.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (bookDb != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    downTime = System.currentTimeMillis()
                    val screenH = resources.displayMetrics.heightPixels.toFloat()
                    val yRatio = downY / screenH
                    validCenterTapCandidate = (yRatio in 0.16f..0.84f) &&
                        !isPointInsideVisibleView(binding.topBar, downX, downY) &&
                        !isPointInsideVisibleView(binding.bottomBar, downX, downY) &&
                        !isPointOnImageButton(binding.rootContainer, downX, downY)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (validCenterTapCandidate) {
                        val dist = hypot((ev.rawX - downX).toDouble(), (ev.rawY - downY).toDouble())
                        if (dist > touchSlop) {
                            validCenterTapCandidate = false
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val wasCandidate = validCenterTapCandidate
                    val elapsed = System.currentTimeMillis() - downTime
                    val dist = hypot((ev.rawX - downX).toDouble(), (ev.rawY - downY).toDouble())
                    validCenterTapCandidate = false
                    val handled = super.dispatchTouchEvent(ev)
                    val tappedWordJustNow = (System.currentTimeMillis() - lastWordTapTime) < 180L
                    if (wasCandidate && !tappedWordJustNow && elapsed < 300L && dist <= touchSlop) {
                        toggleChromeVisibility()
                    }
                    return handled
                }
                MotionEvent.ACTION_CANCEL -> {
                    validCenterTapCandidate = false
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isPointInsideVisibleView(view: View, rawX: Float, rawY: Float): Boolean {
        if (view.visibility != View.VISIBLE || view.alpha < 0.1f) return false
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(rawX.toInt(), rawY.toInt())
    }

    private fun isPointOnImageButton(root: View, rawX: Float, rawY: Float): Boolean {
        if (root.visibility != View.VISIBLE) return false
        if (root is ImageButton) {
            return isPointInsideVisibleView(root, rawX, rawY)
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                if (isPointOnImageButton(root.getChildAt(i), rawX, rawY)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Saat/şebeke durum çubuğunu ve alttaki telefon gezinti tuşlarını gizler.
     */
    private fun enableFullScreenImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun toggleChromeVisibility() {
        chromeVisible = !chromeVisible
        binding.topBar.animate().cancel()
        binding.bottomBar.animate().cancel()
        if (chromeVisible) {
            binding.topBar.visibility = View.VISIBLE
            binding.bottomBar.visibility = View.VISIBLE
            binding.topBar.animate().translationY(0f).alpha(1f).setDuration(180).start()
            binding.bottomBar.animate().translationY(0f).alpha(1f).setDuration(180).start()
        } else {
            val topH = if (binding.topBar.height > 0) binding.topBar.height.toFloat() else 200f
            val botH = if (binding.bottomBar.height > 0) binding.bottomBar.height.toFloat() else 200f
            binding.topBar.animate()
                .translationY(-topH)
                .alpha(0f)
                .setDuration(180)
                .withEndAction { binding.topBar.visibility = View.GONE }
                .start()
            binding.bottomBar.animate()
                .translationY(botH)
                .alpha(0f)
                .setDuration(180)
                .withEndAction { binding.bottomBar.visibility = View.GONE }
                .start()
        }
        enableFullScreenImmersive()
    }

    private fun loadBookData() {
        binding.loadingContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            lexicon.load()
            val db = repository.loadBook()
            bookDb = db
            binding.loadingContainer.visibility = View.GONE

            val savedIndex = prefs.getLastPageIndex().coerceIn(0, (db.pages.size - 1).coerceAtLeast(0))
            currentPageIndex = savedIndex

            val adapter = ReaderAdapter(
                pages = db.pages,
                isBookmarked = { hid -> prefs.isBookmarked(hid) },
                isFavorite = { hid -> prefs.isFavorite(hid) },
                onToggleBookmark = { h -> handleToggleBookmark(h) },
                onToggleFavorite = { h -> handleToggleFavorite(h) },
                onShareHadith = { h -> shareHadith(h) },
                onArabicWordClick = { word ->
                    lastWordTapTime = System.currentTimeMillis()
                    val result = lexicon.lookup(word)
                    val pal = ThemePalette.from(this@MainActivity, settings.theme)
                    WordDetailBottomSheet(result, pal).show(supportFragmentManager, "word_detail")
                }
            )
            readerAdapter = adapter
            applyReadingModeAndJump(savedIndex)
        }
    }

    private fun applyReadingModeAndJump(targetIndex: Int) {
        val db = bookDb ?: return
        val adapter = readerAdapter ?: return
        val safeIndex = targetIndex.coerceIn(0, (db.pages.size - 1).coerceAtLeast(0))
        val palette = ThemePalette.from(this, settings.theme)

        isRestoringPosition = true
        adapter.updateAppearance(settings, palette)

        when (settings.readingMode) {
            ReadingMode.VERTICAL_CONTINUOUS, ReadingMode.HORIZONTAL_CONTINUOUS -> {
                binding.viewPagerHadith.visibility = View.GONE
                binding.viewPagerHadith.unregisterOnPageChangeCallback(pagerCallback)
                binding.viewPagerHadith.adapter = null

                val orientation = if (settings.readingMode == ReadingMode.HORIZONTAL_CONTINUOUS) {
                    LinearLayoutManager.HORIZONTAL
                } else {
                    LinearLayoutManager.VERTICAL
                }
                val lm = LinearLayoutManager(this, orientation, false)
                binding.recyclerContinuous.layoutManager = lm
                binding.recyclerContinuous.adapter = adapter
                binding.recyclerContinuous.visibility = View.VISIBLE
                binding.recyclerContinuous.clearOnScrollListeners()
                binding.recyclerContinuous.scrollToPosition(safeIndex)
                binding.recyclerContinuous.post {
                    lm.scrollToPositionWithOffset(safeIndex, 0)
                    isRestoringPosition = false
                    onCurrentPageChanged(safeIndex)
                    binding.recyclerContinuous.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            if (isRestoringPosition) return
                            val first = lm.findFirstVisibleItemPosition()
                            if (first != RecyclerView.NO_POSITION && first != currentPageIndex) {
                                onCurrentPageChanged(first)
                            }
                        }
                    })
                }
            }

            ReadingMode.HADITH_PAGER -> {
                binding.recyclerContinuous.visibility = View.GONE
                binding.recyclerContinuous.clearOnScrollListeners()
                binding.recyclerContinuous.adapter = null

                binding.viewPagerHadith.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                binding.viewPagerHadith.adapter = adapter
                binding.viewPagerHadith.visibility = View.VISIBLE
                binding.viewPagerHadith.unregisterOnPageChangeCallback(pagerCallback)
                binding.viewPagerHadith.setCurrentItem(safeIndex, false)
                binding.viewPagerHadith.post {
                    binding.viewPagerHadith.setCurrentItem(safeIndex, false)
                    isRestoringPosition = false
                    onCurrentPageChanged(safeIndex)
                    binding.viewPagerHadith.registerOnPageChangeCallback(pagerCallback)
                }
            }
        }
    }

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (!isRestoringPosition) {
                onCurrentPageChanged(position)
            }
        }
    }

    private fun currentHadithOrNull(): Hadith? {
        val db = bookDb ?: return null
        return when (val page = db.pages.getOrNull(currentPageIndex)) {
            is ReaderPage.HadithItem -> page.hadith
            is ReaderPage.ChapterIntro -> {
                val startId = page.chapter.startHadithId
                if (startId > 0) repository.getHadith(startId) else null
            }
            null -> null
        }
    }

    private fun onCurrentPageChanged(index: Int) {
        val db = bookDb ?: return
        if (index !in db.pages.indices) return
        currentPageIndex = index
        prefs.saveLastPageIndex(index)

        when (val page = db.pages[index]) {
            is ReaderPage.ChapterIntro -> {
                binding.tvTopChapter.text = page.chapter.title
                binding.tvTopSubtitle.text = "${page.chapter.volume}. Cilt • Bölüm Girişi ve Âyetler"
            }
            is ReaderPage.HadithItem -> {
                binding.tvTopChapter.text = page.chapter?.title ?: db.title
                binding.tvTopSubtitle.text =
                    "${page.hadith.volume}. Cilt • Hadis ${page.hadith.id} / ${db.hadiths.size}"
            }
        }

        currentHadithOrNull()?.let { refreshTopActionIcons(it.id) }
    }

    private fun refreshTopActionIcons(hadithId: Int) {
        val pal = ThemePalette.from(this, settings.theme)
        val bookmarked = prefs.isBookmarked(hadithId)
        val favorited = prefs.isFavorite(hadithId)

        binding.btnTopBookmark.setImageResource(
            if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
        )
        binding.btnTopBookmark.imageTintList =
            ColorStateList.valueOf(if (bookmarked) pal.accent else pal.textSecondary)

        binding.btnTopFavorite.setImageResource(
            if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
        binding.btnTopFavorite.imageTintList =
            ColorStateList.valueOf(if (favorited) pal.accent else pal.textSecondary)
    }

    private fun applyThemeToChrome() {
        val pal = ThemePalette.from(this, settings.theme)
        binding.rootContainer.setBackgroundColor(pal.background)
        binding.tvLoading.setTextColor(pal.textPrimary)

        binding.topBar.setBackgroundColor(pal.surface)
        binding.tvTopChapter.setTextColor(pal.textPrimary)
        binding.tvTopSubtitle.setTextColor(pal.textSecondary)

        binding.bottomBar.setBackgroundColor(pal.surface)
        val navTint = ColorStateList.valueOf(pal.textPrimary)
        binding.ivNavToc.imageTintList = navTint
        binding.ivNavBookmarks.imageTintList = navTint
        binding.ivNavFavorites.imageTintList = navTint
        binding.ivNavSettings.imageTintList = navTint
        binding.tvNavToc.setTextColor(pal.textPrimary)
        binding.tvNavBookmarks.setTextColor(pal.textPrimary)
        binding.tvNavFavorites.setTextColor(pal.textPrimary)
        binding.tvNavSettings.setTextColor(pal.textPrimary)

        currentHadithOrNull()?.let { refreshTopActionIcons(it.id) }
    }

    private fun setupMenuActions() {
        binding.btnTopBookmark.setOnClickListener {
            val hadith = currentHadithOrNull() ?: return@setOnClickListener
            handleToggleBookmark(hadith)
            readerAdapter?.notifyItemChanged(currentPageIndex)
        }

        binding.btnTopFavorite.setOnClickListener {
            val hadith = currentHadithOrNull() ?: return@setOnClickListener
            handleToggleFavorite(hadith)
            readerAdapter?.notifyItemChanged(currentPageIndex)
        }

        binding.navToc.setOnClickListener {
            val db = bookDb ?: return@setOnClickListener
            val pal = ThemePalette.from(this, settings.theme)
            TocBottomSheet(
                allChapters = db.chapters,
                maxHadithId = db.hadiths.lastOrNull()?.id ?: 1903,
                palette = pal,
                onSelectChapter = { ch -> jumpToPageIndex(repository.getPageIndexForChapter(ch.id)) },
                onSelectHadithId = { hid -> jumpToPageIndex(repository.getPageIndexForHadith(hid)) }
            ).show(supportFragmentManager, "toc_sheet")
        }

        binding.navBookmarks.setOnClickListener {
            val pal = ThemePalette.from(this, settings.theme)
            val entries = prefs.getBookmarks().map { bm ->
                val h = repository.getHadith(bm.hadithId)
                SavedDisplayItem(
                    hadithId = bm.hadithId,
                    heading = "Hadis ${bm.hadithId} • ${bm.chapterTitle}",
                    preview = h?.turkish?.take(120) ?: ""
                )
            }
            SavedListBottomSheet(
                title = getString(R.string.menu_bookmarks),
                emptyMessage = getString(R.string.empty_bookmarks),
                initialItems = entries,
                palette = pal,
                onSelectHadith = { hid -> jumpToPageIndex(repository.getPageIndexForHadith(hid)) },
                onDeleteHadith = { hid ->
                    prefs.removeBookmark(hid)
                    refreshCurrentItemState()
                }
            ).show(supportFragmentManager, "bookmarks_sheet")
        }

        binding.navFavorites.setOnClickListener {
            val pal = ThemePalette.from(this, settings.theme)
            val favItems = prefs.getFavoriteIds().sorted().mapNotNull { hid ->
                val h = repository.getHadith(hid) ?: return@mapNotNull null
                val ch = repository.getChapter(h.chapterId)
                SavedDisplayItem(
                    hadithId = h.id,
                    heading = "Hadis ${h.id} • ${ch?.title ?: ""}",
                    preview = h.turkish.take(120)
                )
            }
            SavedListBottomSheet(
                title = getString(R.string.menu_favorites),
                emptyMessage = getString(R.string.empty_favorites),
                initialItems = favItems,
                palette = pal,
                onSelectHadith = { hid -> jumpToPageIndex(repository.getPageIndexForHadith(hid)) },
                onDeleteHadith = { hid ->
                    prefs.removeFavorite(hid)
                    refreshCurrentItemState()
                }
            ).show(supportFragmentManager, "favorites_sheet")
        }

        binding.navSettings.setOnClickListener {
            SettingsBottomSheet(
                currentSettings = settings,
                onSettingsChanged = { updated ->
                    val modeChanged = updated.readingMode != settings.readingMode
                    settings = updated
                    prefs.saveSettings(updated)
                    applyThemeToChrome()
                    if (modeChanged) {
                        applyReadingModeAndJump(currentPageIndex)
                    } else {
                        readerAdapter?.updateAppearance(
                            settings,
                            ThemePalette.from(this, settings.theme)
                        )
                    }
                }
            ).show(supportFragmentManager, "settings_sheet")
        }
    }

    private fun jumpToPageIndex(pageIndex: Int) {
        val db = bookDb ?: return
        val safeIdx = pageIndex.coerceIn(0, (db.pages.size - 1).coerceAtLeast(0))
        when (settings.readingMode) {
            ReadingMode.HADITH_PAGER -> binding.viewPagerHadith.setCurrentItem(safeIdx, false)
            else -> {
                val lm = binding.recyclerContinuous.layoutManager as? LinearLayoutManager
                if (lm != null) {
                    lm.scrollToPositionWithOffset(safeIdx, 0)
                } else {
                    binding.recyclerContinuous.scrollToPosition(safeIdx)
                }
            }
        }
        onCurrentPageChanged(safeIdx)
    }

    private fun handleToggleBookmark(hadith: Hadith) {
        val chTitle = repository.getChapter(hadith.chapterId)?.title ?: ""
        val added = prefs.toggleBookmark(hadith.id, chTitle)
        Toast.makeText(
            this,
            if (added) R.string.bookmark_added else R.string.bookmark_removed,
            Toast.LENGTH_SHORT
        ).show()
        refreshTopActionIcons(hadith.id)
    }

    private fun handleToggleFavorite(hadith: Hadith) {
        val added = prefs.toggleFavorite(hadith.id)
        Toast.makeText(
            this,
            if (added) R.string.favorite_added else R.string.favorite_removed,
            Toast.LENGTH_SHORT
        ).show()
        refreshTopActionIcons(hadith.id)
    }

    private fun refreshCurrentItemState() {
        currentHadithOrNull()?.let { refreshTopActionIcons(it.id) }
        readerAdapter?.notifyDataSetChanged()
    }

    private fun shareHadith(hadith: Hadith) {
        val chTitle = repository.getChapter(hadith.chapterId)?.title ?: "Riyâzü’s-Sâlihîn"
        val shareText = buildString {
            appendLine("Riyâzü’s-Sâlihîn — $chTitle (Hadis ${hadith.id})")
            appendLine()
            if (settings.showArabic && hadith.arabic.isNotBlank()) {
                appendLine(hadith.arabic)
                appendLine()
            }
            if (settings.showTurkish && hadith.turkish.isNotBlank()) {
                appendLine(hadith.turkish)
            }
            if (hadith.source.isNotBlank()) {
                appendLine()
                append(hadith.source)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_hadith)))
    }
}
