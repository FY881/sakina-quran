package app.sakina.quran.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.sakina.quran.quran.Khatma
import app.sakina.quran.quran.KhatmaPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.quranStore by preferencesDataStore(name = "quran")

/** A personal note + tags on an ayah (QR10). */
data class AyahNote(val text: String, val tags: List<String>, val at: Long)

/** Reader preferences (Q2). */
data class ReaderPrefs(
    val fontSize: Int = 26,
    /** 0 = Uthmani (Tanzil), 1 = Imlaei / simple (Tanzil). */
    val script: Int = 0,
    /** Selected translation ids (max 3, TF05); null = automatic (English UI → Saheeh, Arabic UI → none). */
    val translations: List<String>? = null,
    val tafsir: String = "ar.muyassar",
    val inlineTafsir: Boolean = false,
    val translit: Boolean = false,
    val tajweed: Boolean = false,
    /** Tajweed palette: 0 classic, 1 high contrast, 2 colour-blind safe (AC03). */
    val palette: Int = 0,
    /** Mushaf paper: 0 follow app theme, 1 sepia, 2 night black. */
    val paper: Int = 0,
    val autoScroll: Int = 3,
    val keepOn: Boolean = true,
    /** mp3quran reciter id + moshaf id for full-surah listening; Islamic Network edition for ayah audio. */
    val reciter: Int = 123,
    val moshaf: Int = 123,
    val ayahVoice: String = "ar.alafasy",
    /** Labels for the five highlight colours (QR09). */
    val legend: List<String> = listOf("", "", "", "", ""),
    val lastPage: Int = 1,
    val dailyGoal: Int = 4,
    val showHasanat: Boolean = true,
    val wirdReminder: Boolean = false,
    val wirdAt: Int = 21 * 60,
)

data class QuranUser(
    val prefs: ReaderPrefs = ReaderPrefs(),
    /** "s:a" → colour index 0..4. */
    val highlights: Map<String, Int> = emptyMap(),
    val notes: Map<String, AyahNote> = emptyMap(),
    /** Collection name → ordered "s:a" refs (QR08). "★" is the default favourites list. */
    val collections: Map<String, List<String>> = emptyMap(),
    val khatmas: List<Khatma> = emptyList(),
    val activeKhatma: String = "",
    /** Mushaf pages read per day (QR30). */
    val pagesRead: Map<LocalDate, Set<Int>> = emptyMap(),
    /** Seconds spent reading per day. */
    val readSeconds: Map<LocalDate, Int> = emptyMap(),
    /** Days excused from the reading streak: travel, illness, period (QR32). */
    val excused: Set<LocalDate> = emptySet(),
) {
    val active: Khatma? get() = khatmas.firstOrNull { it.id == activeKhatma } ?: khatmas.firstOrNull { it.finishedOn == null }
    val favourites: List<String> get() = collections[FAV] ?: emptyList()
    fun isFav(s: Int, a: Int) = "$s:$a" in favourites
    val allTags: List<String> get() = notes.values.flatMap { it.tags }.distinct().sorted()

    companion object { const val FAV = "★" }
}

/** Persists all Quran-reading state as JSON blobs (one DataStore file, separate from UserData). */
class QuranStore(context: Context) {
    private val ds = context.applicationContext.quranStore
    private object K {
        val prefs = stringPreferencesKey("prefs")
        val hl = stringPreferencesKey("highlights")
        val notes = stringPreferencesKey("notes")
        val col = stringPreferencesKey("collections")
        val kh = stringPreferencesKey("khatmas")
        val active = stringPreferencesKey("activeKhatma")
        val pages = stringPreferencesKey("pagesRead")
        val secs = stringPreferencesKey("readSeconds")
        val exc = stringPreferencesKey("excused")
    }

    val data: Flow<QuranUser> = ds.data.map(::decode)
    suspend fun now(): QuranUser = data.first()

    private fun decode(p: Preferences): QuranUser = QuranUser(
        prefs = runCatching { decodePrefs(JSONObject(p[K.prefs] ?: "{}")) }.getOrDefault(ReaderPrefs()),
        highlights = runCatching { JSONObject(p[K.hl] ?: "{}").let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } }.getOrDefault(emptyMap()),
        notes = runCatching {
            JSONObject(p[K.notes] ?: "{}").let { o -> o.keys().asSequence().associateWith { k -> val n = o.getJSONObject(k); AyahNote(n.optString("t"), n.optJSONArray("g").strings(), n.optLong("at")) } }
        }.getOrDefault(emptyMap()),
        collections = runCatching { JSONObject(p[K.col] ?: "{}").let { o -> o.keys().asSequence().associateWith { o.getJSONArray(it).strings() } } }.getOrDefault(emptyMap()),
        khatmas = runCatching { JSONArray(p[K.kh] ?: "[]").let { a -> (0 until a.length()).map { decodeKhatma(a.getJSONObject(it)) } } }.getOrDefault(emptyList()),
        activeKhatma = p[K.active] ?: "",
        pagesRead = runCatching {
            JSONObject(p[K.pages] ?: "{}").let { o -> o.keys().asSequence().associate { LocalDate.parse(it) to o.getJSONArray(it).ints().toSet() } }
        }.getOrDefault(emptyMap()),
        readSeconds = runCatching { JSONObject(p[K.secs] ?: "{}").let { o -> o.keys().asSequence().associate { LocalDate.parse(it) to o.getInt(it) } } }.getOrDefault(emptyMap()),
        excused = runCatching { JSONArray(p[K.exc] ?: "[]").strings().map { LocalDate.parse(it) }.toSet() }.getOrDefault(emptySet()),
    )

    // ── Prefs
    suspend fun updatePrefs(f: (ReaderPrefs) -> ReaderPrefs) = ds.edit { p ->
        val cur = runCatching { decodePrefs(JSONObject(p[K.prefs] ?: "{}")) }.getOrDefault(ReaderPrefs())
        p[K.prefs] = encodePrefs(f(cur)).toString()
    }

    // ── Highlights / notes / collections
    suspend fun setHighlight(s: Int, a: Int, color: Int?) = editObj(K.hl) { o -> if (color == null) o.remove("$s:$a") else o.put("$s:$a", color) }
    suspend fun setNote(s: Int, a: Int, text: String, tags: List<String>) = editObj(K.notes) { o ->
        if (text.isBlank() && tags.isEmpty()) o.remove("$s:$a")
        else o.put("$s:$a", JSONObject().put("t", text.trim().take(4000)).put("g", JSONArray(tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12))).put("at", System.currentTimeMillis()))
    }
    suspend fun toggleInCollection(name: String, ref: String) = editObj(K.col) { o ->
        val cur = o.optJSONArray(name)?.strings() ?: emptyList()
        val next = if (ref in cur) cur - ref else cur + ref
        if (next.isEmpty() && name != QuranUser.FAV) o.remove(name) else o.put(name, JSONArray(next))
    }
    suspend fun createCollection(name: String) = editObj(K.col) { o -> val n = name.trim().take(40); if (n.isNotEmpty() && !o.has(n)) o.put(n, JSONArray()) }
    suspend fun deleteCollection(name: String) = editObj(K.col) { o -> o.remove(name) }

    // ── Khatma
    suspend fun saveKhatma(k: Khatma, makeActive: Boolean = false) = ds.edit { p ->
        val list = runCatching { JSONArray(p[K.kh] ?: "[]").let { a -> (0 until a.length()).map { decodeKhatma(a.getJSONObject(it)) } } }.getOrDefault(emptyList())
        val next = if (list.any { it.id == k.id }) list.map { if (it.id == k.id) k else it } else list + k
        p[K.kh] = JSONArray(next.map(::encodeKhatma)).toString()
        if (makeActive || (p[K.active] ?: "").isEmpty()) p[K.active] = k.id
    }
    suspend fun deleteKhatma(id: String) = ds.edit { p ->
        val list = runCatching { JSONArray(p[K.kh] ?: "[]").let { a -> (0 until a.length()).map { decodeKhatma(a.getJSONObject(it)) } } }.getOrDefault(emptyList())
        val next = list.filterNot { it.id == id }
        p[K.kh] = JSONArray(next.map(::encodeKhatma)).toString()
        if (p[K.active] == id) p[K.active] = next.firstOrNull { it.finishedOn == null }?.id ?: ""
    }
    suspend fun setActive(id: String) = ds.edit { it[K.active] = id }

    /** Records that mushaf page [page] was read today; advances the active khatma if it is the next page(s) of its wird. */
    suspend fun logPage(page: Int, khatmaId: String? = null, today: LocalDate = LocalDate.now()) = ds.edit { p ->
        val o = JSONObject(p[K.pages] ?: "{}")
        val key = today.toString()
        val arr = o.optJSONArray(key)?.ints()?.toMutableSet() ?: mutableSetOf()
        if (arr.add(page)) {
            o.put(key, JSONArray(arr.sorted()))
            // keep ~400 days
            val keys = o.keys().asSequence().toList().sorted()
            if (keys.size > 400) keys.take(keys.size - 400).forEach { o.remove(it) }
            p[K.pages] = o.toString()
        }
        if (khatmaId != null) {
            val list = runCatching { JSONArray(p[K.kh] ?: "[]").let { a -> (0 until a.length()).map { decodeKhatma(a.getJSONObject(it)) } } }.getOrDefault(emptyList())
            val k = list.firstOrNull { it.id == khatmaId }
            // Reading forward inside the khatma counts the page before the one now on screen as finished.
            if (k != null && page - 1 > k.progress && page - 1 <= k.progress + 30) {
                val nk = KhatmaPlan.advanceTo(k, page - 1, today)
                p[K.kh] = JSONArray(list.map { if (it.id == k.id) nk else it }.map(::encodeKhatma)).toString()
            }
        }
    }

    /** Adds many (day → pages) at once (backup restore / debug seed). */
    suspend fun addPages(pages: Map<LocalDate, Set<Int>>) = editObj(K.pages) { o ->
        for ((d, ps) in pages) {
            val cur = o.optJSONArray(d.toString())?.ints()?.toMutableSet() ?: mutableSetOf()
            cur += ps.filter { it in 1..604 }
            o.put(d.toString(), JSONArray(cur.sorted()))
        }
    }

    suspend fun addSeconds(sec: Int, today: LocalDate = LocalDate.now()) = editObj(K.secs) { o -> o.put(today.toString(), o.optInt(today.toString()) + sec) }
    suspend fun toggleExcused(d: LocalDate = LocalDate.now()) = ds.edit { p ->
        val cur = runCatching { JSONArray(p[K.exc] ?: "[]").strings() }.getOrDefault(emptyList())
        val k = d.toString()
        p[K.exc] = JSONArray(if (k in cur) cur - k else cur + k).toString()
    }

    private suspend fun editObj(key: Preferences.Key<String>, f: (JSONObject) -> Unit) = ds.edit { p ->
        val o = runCatching { JSONObject(p[key] ?: "{}") }.getOrDefault(JSONObject()); f(o); p[key] = o.toString()
    }

    companion object {
        fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
        fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else (0 until length()).map { getInt(it) }

        fun encodeKhatma(k: Khatma): JSONObject = JSONObject().put("id", k.id).put("n", k.name).put("sp", k.startPage).put("ep", k.endPage)
            .put("sd", k.startDate.toString()).put("ed", k.endDate.toString()).put("p", k.progress)
            .put("log", JSONObject().apply { k.log.forEach { (d, n) -> put(d.toString(), n) } })
            .put("ap", k.afterPrayers).put("k", k.kind).put("f", k.finishedOn?.toString() ?: "")

        fun decodeKhatma(o: JSONObject): Khatma {
            val sp = o.optInt("sp", 1)
            return Khatma(
                id = o.getString("id"), name = o.optString("n"), startPage = sp, endPage = o.optInt("ep", 604),
                startDate = LocalDate.parse(o.getString("sd")), endDate = LocalDate.parse(o.getString("ed")),
                progress = o.optInt("p", sp - 1),
                log = o.optJSONObject("log")?.let { l -> l.keys().asSequence().associate { LocalDate.parse(it) to l.getInt(it) } } ?: emptyMap(),
                afterPrayers = o.optBoolean("ap"), kind = o.optInt("k"),
                finishedOn = o.optString("f").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
            )
        }

        fun encodePrefs(r: ReaderPrefs): JSONObject = JSONObject().put("fs", r.fontSize).put("sc", r.script)
            .put("tr", r.translations?.let { JSONArray(it) } ?: JSONObject.NULL).put("tf", r.tafsir).put("it", r.inlineTafsir)
            .put("tl", r.translit).put("tj", r.tajweed).put("pl", r.palette).put("pp", r.paper).put("as", r.autoScroll).put("ko", r.keepOn)
            .put("rc", r.reciter).put("ms", r.moshaf).put("av", r.ayahVoice).put("lg", JSONArray(r.legend)).put("lp", r.lastPage)
            .put("dg", r.dailyGoal).put("hs", r.showHasanat).put("wr", r.wirdReminder).put("wa", r.wirdAt)

        fun decodePrefs(o: JSONObject): ReaderPrefs {
            val d = ReaderPrefs()
            return ReaderPrefs(
                fontSize = o.optInt("fs", d.fontSize).coerceIn(16, 44), script = o.optInt("sc", d.script),
                translations = if (!o.has("tr") || o.isNull("tr")) null else o.getJSONArray("tr").strings().take(3),
                tafsir = o.optString("tf", d.tafsir), inlineTafsir = o.optBoolean("it", d.inlineTafsir), translit = o.optBoolean("tl", d.translit),
                tajweed = o.optBoolean("tj", d.tajweed), palette = o.optInt("pl", d.palette), paper = o.optInt("pp", d.paper),
                autoScroll = o.optInt("as", d.autoScroll).coerceIn(1, 10), keepOn = o.optBoolean("ko", d.keepOn),
                reciter = o.optInt("rc", d.reciter), moshaf = o.optInt("ms", d.moshaf), ayahVoice = o.optString("av", d.ayahVoice),
                legend = o.optJSONArray("lg")?.strings()?.let { l -> List(5) { l.getOrElse(it) { "" } } } ?: d.legend,
                lastPage = o.optInt("lp", d.lastPage).coerceIn(1, 604), dailyGoal = o.optInt("dg", d.dailyGoal).coerceIn(1, 60),
                showHasanat = o.optBoolean("hs", d.showHasanat), wirdReminder = o.optBoolean("wr", d.wirdReminder), wirdAt = o.optInt("wa", d.wirdAt),
            )
        }
    }
}
