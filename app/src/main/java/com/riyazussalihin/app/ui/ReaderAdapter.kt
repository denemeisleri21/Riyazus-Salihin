package com.riyazussalihin.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.riyazussalihin.app.R
import com.riyazussalihin.app.data.ArabicLexicon
import com.riyazussalihin.app.model.Hadith
import com.riyazussalihin.app.model.ReaderPage
import com.riyazussalihin.app.model.ReaderSettings
import com.riyazussalihin.app.model.ReadingMode
import com.riyazussalihin.app.model.ThemePalette

class ReaderAdapter(
    private val pages: List<ReaderPage>,
    private val isBookmarked: (Int) -> Boolean,
    private val isFavorite: (Int) -> Boolean,
    private val onToggleBookmark: (Hadith) -> Unit,
    private val onToggleFavorite: (Hadith) -> Unit,
    private val onShareHadith: (Hadith) -> Unit,
    private val onArabicWordClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var settings: ReaderSettings = ReaderSettings()
    private var palette: ThemePalette? = null

    fun updateAppearance(newSettings: ReaderSettings, newPalette: ThemePalette) {
        settings = newSettings
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (pages[position]) {
            is ReaderPage.ChapterIntro -> when (settings.readingMode) {
                ReadingMode.VERTICAL_CONTINUOUS -> R.layout.item_intro_vertical
                ReadingMode.HORIZONTAL_CONTINUOUS -> R.layout.item_intro_horizontal_scroll
                ReadingMode.HADITH_PAGER -> R.layout.item_intro_pager
            }
            is ReaderPage.HadithItem -> when (settings.readingMode) {
                ReadingMode.VERTICAL_CONTINUOUS -> R.layout.item_hadith_vertical
                ReadingMode.HORIZONTAL_CONTINUOUS -> R.layout.item_hadith_horizontal_scroll
                ReadingMode.HADITH_PAGER -> R.layout.item_hadith_pager
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_intro_vertical,
            R.layout.item_intro_horizontal_scroll,
            R.layout.item_intro_pager -> ChapterIntroViewHolder(view)
            else -> HadithViewHolder(view)
        }
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pal = palette ?: ThemePalette.from(holder.itemView.context, settings.theme)
        when (val page = pages[position]) {
            is ReaderPage.ChapterIntro -> bindChapterIntro(holder as ChapterIntroViewHolder, page, pal)
            is ReaderPage.HadithItem -> bindHadith(holder as HadithViewHolder, page, position, pal)
        }
    }

    private fun buildClickableArabic(rawArabic: String, textColor: Int): SpannableString {
        val displayed = if (settings.showHarakat) rawArabic else ArabicLexicon.stripHarakat(rawArabic)
        val spannable = SpannableString(displayed)
        val rawTokens = rawArabic.split(Regex("\\s+"))
        val dispTokens = displayed.split(Regex("\\s+"))

        var cursor = 0
        for (i in dispTokens.indices) {
            val dTok = dispTokens[i]
            val rTok = rawTokens.getOrElse(i) { dTok }
            val start = displayed.indexOf(dTok, cursor)
            if (start < 0) continue
            val end = start + dTok.length
            cursor = end

            if (ArabicLexicon.normalize(rTok).length >= 2) {
                spannable.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onArabicWordClick(rTok)
                        }
                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = textColor
                            ds.isUnderlineText = false
                        }
                    },
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannable
    }

    private fun bindChapterIntro(
        holder: ChapterIntroViewHolder,
        page: ReaderPage.ChapterIntro,
        pal: ThemePalette
    ) {
        val ctx = holder.itemView.context
        val ch = page.chapter

        holder.scrollInner?.let { scroll ->
            scroll.background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(pal.card)
                setStroke(2, pal.divider)
            }
        } ?: run {
            holder.itemView.setBackgroundColor(pal.background)
        }

        holder.tvVolumeBadge.text = "${ch.volume}. CİLT • BÖLÜM GİRİŞİ"
        holder.tvVolumeBadge.setTextColor(pal.accent)
        holder.tvTitle.text = ch.title
        holder.tvTitle.setTextColor(pal.textPrimary)
        holder.titleDivider.setBackgroundColor(pal.divider)
        holder.bottomDivider?.setBackgroundColor(pal.divider)

        holder.blocksContainer.removeAllViews()
        val arabicTypeface = ResourcesCompat.getFont(ctx, R.font.noto_naskh_arabic_regular)
        val density = ctx.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }

        ch.introBlocks.forEachIndexed { idx, block ->
            if (block.arabic.isNotBlank() && settings.showArabic) {
                val tvAr = TextView(ctx).apply {
                    text = buildClickableArabic(block.arabic, pal.textPrimary)
                    movementMethod = LinkMovementMethod.getInstance()
                    highlightColor = Color.TRANSPARENT
                    typeface = arabicTypeface
                    textDirection = View.TEXT_DIRECTION_RTL
                    gravity = Gravity.END
                    setTextColor(pal.textPrimary)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp + 5f)
                    setLineSpacing(0f, 1.52f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = if (idx == 0) 0 else dp(12)
                        bottomMargin = dp(8)
                    }
                }
                holder.blocksContainer.addView(tvAr)
            }

            if (block.turkish.isNotBlank() && settings.showTurkish) {
                val isVerse = block.type == "verse"
                val tvTr = TextView(ctx).apply {
                    text = block.turkish
                    setTypeface(Typeface.SERIF, if (isVerse) Typeface.ITALIC else Typeface.NORMAL)
                    setTextColor(if (isVerse) pal.textSecondary else pal.textPrimary)
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        if (isVerse) settings.fontSizeSp - 0.5f else settings.fontSizeSp
                    )
                    setLineSpacing(0f, 1.4f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(12)
                    }
                }
                holder.blocksContainer.addView(tvTr)
            }

            if (idx < ch.introBlocks.lastIndex) {
                val sep = View(ctx).apply {
                    setBackgroundColor(pal.divider)
                    alpha = 0.6f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        topMargin = dp(4)
                        bottomMargin = dp(6)
                    }
                }
                holder.blocksContainer.addView(sep)
            }
        }
    }

    private fun bindHadith(
        holder: HadithViewHolder,
        page: ReaderPage.HadithItem,
        position: Int,
        pal: ThemePalette
    ) {
        val hadith = page.hadith
        val chapter = page.chapter

        holder.scrollInner?.let { scroll ->
            scroll.background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(pal.card)
                setStroke(2, pal.divider)
            }
        } ?: run {
            holder.itemView.setBackgroundColor(pal.background)
        }

        holder.tvChapterLabel.text = chapter?.title ?: "Riyâzü’s-Sâlihîn"
        holder.tvChapterLabel.setTextColor(pal.accent)

        holder.tvHadithBadge.text = "Hadis ${hadith.id}"
        holder.tvHadithBadge.setTextColor(pal.accent)

        if (settings.showArabic && hadith.arabic.isNotBlank()) {
            holder.tvArabic.visibility = View.VISIBLE
            holder.tvArabic.text = buildClickableArabic(hadith.arabic, pal.textPrimary)
            holder.tvArabic.movementMethod = LinkMovementMethod.getInstance()
            holder.tvArabic.highlightColor = Color.TRANSPARENT
            holder.tvArabic.setTextColor(pal.textPrimary)
            holder.tvArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp + 6f)
        } else {
            holder.tvArabic.visibility = View.GONE
        }

        if (settings.showTurkish && hadith.turkish.isNotBlank()) {
            holder.tvTurkish.visibility = View.VISIBLE
            holder.tvTurkish.text = hadith.turkish
            holder.tvTurkish.setTextColor(pal.textPrimary)
            holder.tvTurkish.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
        } else {
            holder.tvTurkish.visibility = View.GONE
        }

        if (hadith.source.isNotBlank() && settings.showTurkish) {
            holder.tvSource.visibility = View.VISIBLE
            holder.tvSource.text = hadith.source
            holder.tvSource.setTextColor(pal.textSecondary)
        } else {
            holder.tvSource.visibility = View.GONE
        }

        holder.itemDivider?.setBackgroundColor(pal.divider)

        val bookmarked = isBookmarked(hadith.id)
        val favorited = isFavorite(hadith.id)

        holder.btnItemBookmark.setImageResource(
            if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
        )
        holder.btnItemBookmark.imageTintList = ColorStateList.valueOf(
            if (bookmarked) pal.accent else pal.textSecondary
        )
        holder.btnItemBookmark.setOnClickListener {
            onToggleBookmark(hadith)
            notifyItemChanged(position)
        }

        holder.btnItemFavorite.setImageResource(
            if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
        holder.btnItemFavorite.imageTintList = ColorStateList.valueOf(
            if (favorited) pal.accent else pal.textSecondary
        )
        holder.btnItemFavorite.setOnClickListener {
            onToggleFavorite(hadith)
            notifyItemChanged(position)
        }

        holder.btnItemShare.imageTintList = ColorStateList.valueOf(pal.textSecondary)
        holder.btnItemShare.setOnClickListener { onShareHadith(hadith) }
    }

    class ChapterIntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVolumeBadge: TextView = view.findViewById(R.id.tvIntroVolumeBadge)
        val tvTitle: TextView = view.findViewById(R.id.tvIntroChapterTitle)
        val titleDivider: View = view.findViewById(R.id.introTitleDivider)
        val blocksContainer: LinearLayout = view.findViewById(R.id.introBlocksContainer)
        val bottomDivider: View? = view.findViewById(R.id.introBottomDivider)
        val scrollInner: View? = view.findViewById(R.id.introScrollInner)
    }

    class HadithViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChapterLabel: TextView = view.findViewById(R.id.tvHadithChapterLabel)
        val tvHadithBadge: TextView = view.findViewById(R.id.tvHadithBadge)
        val btnItemBookmark: ImageButton = view.findViewById(R.id.btnItemBookmark)
        val btnItemFavorite: ImageButton = view.findViewById(R.id.btnItemFavorite)
        val btnItemShare: ImageButton = view.findViewById(R.id.btnItemShare)
        val tvArabic: TextView = view.findViewById(R.id.tvArabic)
        val tvTurkish: TextView = view.findViewById(R.id.tvTurkish)
        val tvSource: TextView = view.findViewById(R.id.tvSource)
        val itemDivider: View? = view.findViewById(R.id.itemDivider)
        val scrollInner: View? = view.findViewById(R.id.scrollInner)
    }
}
