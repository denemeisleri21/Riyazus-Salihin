package com.riyazussalihin.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LexiconEntry(
    val root: String,
    val meaning: String,
    val rootInfo: String
)

data class WordLookupResult(
    val originalWord: String,
    val root: String,
    val meaning: String,
    val rootInfo: String,
    val morphologyNote: String
)

class ArabicLexicon(private val context: Context) {

    private var exactMap: Map<String, LexiconEntry> = emptyMap()
    private var stemsMap: Map<String, String> = emptyMap()
    private var rootsMap: Map<String, LexiconEntry> = emptyMap()
    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {
        if (loaded) return@withContext
        val raw = context.assets.open("arabic_lexicon.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)

        val ex = HashMap<String, LexiconEntry>()
        val exObj = root.getJSONObject("exact")
        exObj.keys().forEach { k ->
            val o = exObj.getJSONObject(k)
            ex[k] = LexiconEntry(o.optString("root"), o.optString("meaning"), o.optString("info"))
        }

        val st = HashMap<String, String>()
        val stObj = root.getJSONObject("stems")
        stObj.keys().forEach { k ->
            st[k] = stObj.getString(k)
        }

        val rt = HashMap<String, LexiconEntry>()
        val rtObj = root.getJSONObject("roots")
        rtObj.keys().forEach { k ->
            val o = rtObj.getJSONObject(k)
            rt[k] = LexiconEntry(o.optString("root"), o.optString("meaning"), o.optString("info"))
        }

        exactMap = ex
        stemsMap = st
        rootsMap = rt
        loaded = true
    }

    fun lookup(rawWord: String): WordLookupResult {
        val cleanWord = rawWord.trim { ch ->
            ch !in '؀'..'ۿ' && ch !in 'ݐ'..'ݿ'
        }
        val norm = normalize(cleanWord)
        if (norm.isEmpty()) {
            return WordLookupResult(cleanWord, "—", "Sözlükte karşılığı bulunamadı.", "", "")
        }

        // 1. Direct exact match
        exactMap[norm]?.let {
            return WordLookupResult(cleanWord, it.root, it.meaning, it.rootInfo, "")
        }
        stemsMap[norm]?.let { rk ->
            rootsMap[rk]?.let {
                return WordLookupResult(cleanWord, it.root, it.meaning, it.rootInfo, "")
            }
        }
        rootsMap[norm]?.let {
            return WordLookupResult(cleanWord, it.root, it.meaning, it.rootInfo, "")
        }

        // 2. Layered morphological prefix/suffix stripping
        val notes = mutableListOf<String>()
        val prefixPairs = listOf(
            "وال" to "'و' (ve) + 'ال' (belirlilik takısı)",
            "فال" to "'ف' (bunun üzerine) + 'ال' (belirlilik takısı)",
            "بال" to "'ب' (ile/…de) + 'ال' (belirlilik takısı)",
            "كال" to "'ك' (gibi) + 'ال' (belirlilik takısı)",
            "لل" to "'ل' (için/…e) + 'ال' takısı",
            "ال" to "'ال' (belirlilik takısı)",
            "و" to "'و' (ve) bağlacı",
            "ف" to "'ف' (sonra/bunun üzerine) bağlacı",
            "ب" to "'ب' (ile/sebebiyle) harf-i cerri",
            "ل" to "'ل' (için/…e) harf-i cerri",
            "ك" to "'ك' (gibi) benzetme edatı",
            "س" to "'س' (gelecek zaman eki)"
        )
        val suffixPairs = listOf(
            "هما" to "'ـهما' (ikisinin/ikisine) zamiri",
            "كما" to "'ـكما' (ikinizin/ikinize) zamiri",
            "ون" to "'ـون' (eril çoğul eki)",
            "ين" to "'ـين' (eril çoğul / ikil eki)",
            "ات" to "'ـات' (dişil çoğul eki)",
            "هم" to "'ـهم' (onların/onlara) zamiri",
            "هن" to "'ـهن' (onların/dişil) zamiri",
            "كم" to "'ـكم' (sizin/size) zamiri",
            "ها" to "'ـها' (onun/ona) zamiri",
            "نا" to "'ـنا' (bizim/bize) zamiri",
            "تم" to "'ـتم' (siz yaptınız) fiil eki",
            "وا" to "'ـوا' (onlar yaptılar) çoğul fiil eki",
            "ه" to "'ـه' (onun/ona) zamiri",
            "ك" to "'ـك' (senin/sana) zamiri",
            "ي" to "'ـي' (benim/bana) zamiri"
        )

        var bestStem = norm
        for ((p, pDesc) in prefixPairs) {
            if (norm.startsWith(p) && norm.length - p.length >= 2) {
                val sub = norm.substring(p.length)
                findEntry(sub)?.let { entry ->
                    return WordLookupResult(
                        cleanWord, entry.root, entry.meaning, entry.rootInfo,
                        "Ön ek: $pDesc"
                    )
                }
                // Try prefix + suffix together
                for ((s, sDesc) in suffixPairs) {
                    if (sub.endsWith(s) && sub.length - s.length >= 2) {
                        val core = sub.substring(0, sub.length - s.length)
                        findEntry(core)?.let { entry ->
                            return WordLookupResult(
                                cleanWord, entry.root, entry.meaning, entry.rootInfo,
                                "Ön ek: $pDesc • Son ek: $sDesc"
                            )
                        }
                    }
                }
                if (bestStem == norm) {
                    bestStem = sub
                    notes.add("Ön ek: $pDesc")
                }
            }
        }

        // Try suffix only
        for ((s, sDesc) in suffixPairs) {
            if (norm.endsWith(s) && norm.length - s.length >= 2) {
                val core = norm.substring(0, norm.length - s.length)
                findEntry(core)?.let { entry ->
                    return WordLookupResult(
                        cleanWord, entry.root, entry.meaning, entry.rootInfo,
                        "Son ek: $sDesc"
                    )
                }
                if (bestStem.endsWith(s) && bestStem.length - s.length >= 3) {
                    bestStem = bestStem.substring(0, bestStem.length - s.length)
                    notes.add("Son ek: $sDesc")
                }
            }
        }

        // 3. Algorithmic triliteral root estimation (strip pattern augment letters ا، ت، م، ن، ي، س)
        val estRoot = estimateTriliteralRoot(bestStem)
        rootsMap[estRoot]?.let { entry ->
            val morph = if (notes.isNotEmpty()) notes.joinToString(" • ") else "Türemiş/çekimli kelime (gövde: $bestStem)"
            return WordLookupResult(cleanWord, entry.root, entry.meaning, entry.rootInfo, morph)
        }

        // 4. Graceful fallback for uncommon words / proper nouns (never dead-end!)
        val spacedRoot = if (estRoot.length >= 3) estRoot.toCharArray().joinToString(" - ") else bestStem
        return WordLookupResult(
            originalWord = cleanWord,
            root = "Tahminî kök: $spacedRoot",
            meaning = "Bu kelime yerleşik sözlükte birebir kayıtlı değil; bağlamdaki tam karşılığı için hemen alttaki Türkçe meale/çeviriye bakabilirsiniz.",
            rootInfo = "Arapça kelimelerin çoğu üç harfli (sülâsî) bir kökten türer. Ön ve son ekler ayrıştırıldığında bu kelimenin gövdesi '$bestStem' olarak görünmektedir.",
            morphologyNote = notes.joinToString(" • ")
        )
    }

    private fun findEntry(candidate: String): LexiconEntry? {
        exactMap[candidate]?.let { return it }
        stemsMap[candidate]?.let { rk -> rootsMap[rk]?.let { return it } }
        rootsMap[candidate]?.let { return it }
        return null
    }

    private fun estimateTriliteralRoot(stem: String): String {
        var s = stem
        // Common verb/participle prefixes: مست، است، مت، من، يت، تت، نت، ات، م، ي، ت، ن، ا
        val morphPrefixes = listOf("مست", "است", "مت", "من", "يت", "تت", "نت", "ات", "م", "ي", "ت", "ن", "ا")
        for (mp in morphPrefixes) {
            if (s.startsWith(mp) && s.length - mp.length >= 3) {
                val cand = s.substring(mp.length)
                if (rootsMap.containsKey(cand) || stemsMap.containsKey(cand)) return cand
                s = cand
                break
            }
        }
        // Strip internal long vowels / feminine marker if length > 3
        if (s.length > 3) {
            val consonants = s.filter { it !in charArrayOf('ا', 'و', 'ي', 'ه') }
            if (consonants.length == 3) return consonants
        }
        return if (s.length >= 3) s.substring(0, 3) else s
    }

    companion object {
        private val HARAKA_REGEX = Regex("[\u064B-\u065F\u0670\u0656\u0610-\u061A\u06D6-\u06ED\u0640]")

        fun stripHarakat(text: String): String = HARAKA_REGEX.replace(text, "")

        fun normalize(word: String): String {
            val noH = stripHarakat(word)
            val sb = StringBuilder(noH.length)
            for (ch in noH) {
                val mapped = when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ى' -> 'ي'
                    'ة' -> 'ه'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    else -> ch
                }
                if (mapped in 'ء'..'ي') {
                    sb.append(mapped)
                }
            }
            return sb.toString()
        }
    }
}
