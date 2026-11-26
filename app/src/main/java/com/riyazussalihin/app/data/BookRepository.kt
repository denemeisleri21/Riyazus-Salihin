package com.riyazussalihin.app.data

import android.content.Context
import com.riyazussalihin.app.model.BookDatabase
import com.riyazussalihin.app.model.Chapter
import com.riyazussalihin.app.model.Hadith
import com.riyazussalihin.app.model.IntroBlock
import com.riyazussalihin.app.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BookRepository(private val context: Context) {

    private var cachedDb: BookDatabase? = null
    private var chapterById: Map<Int, Chapter> = emptyMap()
    private var hadithById: Map<Int, Hadith> = emptyMap()
    private var pageIndexByHadithId: Map<Int, Int> = emptyMap()
    private var pageIndexByChapterId: Map<Int, Int> = emptyMap()

    suspend fun loadBook(): BookDatabase = withContext(Dispatchers.IO) {
        cachedDb?.let { return@withContext it }

        val rawJson = context.assets.open("riyazus_salihin.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val root = JSONObject(rawJson)
        val meta = root.getJSONObject("metadata")

        val chaptersJson = root.getJSONArray("chapters")
        val chapters = ArrayList<Chapter>(chaptersJson.length())
        for (i in 0 until chaptersJson.length()) {
            val c = chaptersJson.getJSONObject(i)
            val blocksJson = c.optJSONArray("introBlocks")
            val blocks = ArrayList<IntroBlock>()
            if (blocksJson != null) {
                for (j in 0 until blocksJson.length()) {
                    val b = blocksJson.getJSONObject(j)
                    blocks.add(
                        IntroBlock(
                            type = b.optString("type", "verse"),
                            arabic = b.optString("arabic", ""),
                            turkish = b.optString("turkish", "")
                        )
                    )
                }
            }
            chapters.add(
                Chapter(
                    id = c.getInt("id"),
                    volume = c.getInt("volume"),
                    title = c.getString("title"),
                    isKitap = c.optBoolean("isKitap", false),
                    printedPage = c.optInt("printedPage", 0),
                    introBlocks = blocks,
                    hadithCount = c.optInt("hadithCount", 0),
                    startHadithId = c.optInt("startHadithId", 0)
                )
            )
        }

        val hadithsJson = root.getJSONArray("hadiths")
        val hadiths = ArrayList<Hadith>(hadithsJson.length())
        for (i in 0 until hadithsJson.length()) {
            val h = hadithsJson.getJSONObject(i)
            hadiths.add(
                Hadith(
                    id = h.getInt("id"),
                    chapterId = h.getInt("chapterId"),
                    volume = h.getInt("volume"),
                    printedPage = h.optInt("printedPage", 0),
                    arabic = h.optString("arabic", ""),
                    turkish = h.optString("turkish", ""),
                    source = h.optString("source", "")
                )
            )
        }

        val chMap = chapters.associateBy { it.id }
        val hMap = hadiths.associateBy { it.id }

        // Build unified ReaderPage list:
        // Whenever a chapter starts AND has introBlocks (verses or explanation),
        // insert a dedicated ReaderPage.ChapterIntro BEFORE its first hadith!
        val pages = ArrayList<ReaderPage>(hadiths.size + chapters.size)
        val hPageIdx = HashMap<Int, Int>(hadiths.size)
        val cPageIdx = HashMap<Int, Int>(chapters.size)

        var lastChapterId = -1
        for (h in hadiths) {
            if (h.chapterId != lastChapterId) {
                val ch = chMap[h.chapterId]
                if (ch != null && ch.introBlocks.isNotEmpty()) {
                    if (!cPageIdx.containsKey(ch.id)) {
                        cPageIdx[ch.id] = pages.size
                    }
                    pages.add(ReaderPage.ChapterIntro(ch))
                }
                lastChapterId = h.chapterId
            }
            if (!cPageIdx.containsKey(h.chapterId)) {
                cPageIdx[h.chapterId] = pages.size
            }
            hPageIdx[h.id] = pages.size
            pages.add(ReaderPage.HadithItem(h, chMap[h.chapterId]))
        }

        val db = BookDatabase(
            title = meta.optString("title", "Riyâzü’s-Sâlihîn"),
            subtitle = meta.optString("subtitle", ""),
            author = meta.optString("author", ""),
            translators = meta.optString("translators", ""),
            chapters = chapters,
            hadiths = hadiths,
            pages = pages
        )

        chapterById = chMap
        hadithById = hMap
        pageIndexByHadithId = hPageIdx
        pageIndexByChapterId = cPageIdx
        cachedDb = db
        db
    }

    fun getChapter(chapterId: Int): Chapter? = chapterById[chapterId]

    fun getHadith(hadithId: Int): Hadith? = hadithById[hadithId]

    fun getPageIndexForHadith(hadithId: Int): Int = pageIndexByHadithId[hadithId] ?: 0

    fun getPageIndexForChapter(chapterId: Int): Int {
        pageIndexByChapterId[chapterId]?.let { return it }
        val ch = chapterById[chapterId]
        if (ch != null && ch.startHadithId > 0) {
            return getPageIndexForHadith(ch.startHadithId)
        }
        return 0
    }
}
