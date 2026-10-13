package app.sakina.quran.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class EdKind { TAFSIR, TRANSLATION, TRANSLIT }

/**
 * A tafsir / translation edition. Bundled ones live in assets/tafsir (verbatim Tanzil exports, gzip);
 * the rest are downloaded on the user's device from their official source (see assets/tafsir/LICENSES.txt).
 */
data class Edition(
    val id: String, val kind: EdKind, val lang: String, val nameAr: String, val nameEn: String, val author: String,
    val bundled: Boolean = false, val approxMb: Int = 1, val source: String = "Tanzil",
) {
    val rtl: Boolean get() = lang in setOf("ar", "ur", "fa")
    /** Where the downloadable copy comes from. */
    val url: String get() = if (source == "AlQuranCloud") "https://api.alquran.cloud/v1/quran/$id" else "https://tanzil.net/trans/?transID=$id&type=txt-2"
}

object Editions {
    val ALL: List<Edition> = listOf(
        Edition("ar.muyassar", EdKind.TAFSIR, "ar", "التفسير الميسر", "Al-Muyassar", "مجمع الملك فهد", bundled = true, approxMb = 3),
        Edition("ar.jalalayn", EdKind.TAFSIR, "ar", "تفسير الجلالين", "Al-Jalalayn", "المحلي والسيوطي", bundled = true, approxMb = 2),
        Edition("ar.qurtubi", EdKind.TAFSIR, "ar", "تفسير القرطبي", "Al-Qurtubi", "أبو عبد الله القرطبي", approxMb = 30, source = "AlQuranCloud"),
        Edition("ar.baghawi", EdKind.TAFSIR, "ar", "تفسير البغوي", "Al-Baghawi", "الحسين بن مسعود البغوي", approxMb = 16, source = "AlQuranCloud"),
        Edition("en.sahih", EdKind.TRANSLATION, "en", "الإنجليزية — صحيح إنترناشونال", "English — Saheeh International", "Saheeh International", bundled = true),
        Edition("en.transliteration", EdKind.TRANSLIT, "en", "النطق بالحروف اللاتينية", "Transliteration", "Tanzil", bundled = true),
        Edition("fr.hamidullah", EdKind.TRANSLATION, "fr", "الفرنسية — حميد الله", "French — Hamidullah", "Muhammad Hamidullah"),
        Edition("ur.jalandhry", EdKind.TRANSLATION, "ur", "الأردية — جالندهري", "Urdu — Jalandhry", "Fateh Muhammad Jalandhry", approxMb = 2),
        Edition("id.indonesian", EdKind.TRANSLATION, "id", "الإندونيسية — وزارة الشؤون الدينية", "Indonesian — Ministry of Religious Affairs", "Kementerian Agama"),
        Edition("tr.diyanet", EdKind.TRANSLATION, "tr", "التركية — الشؤون الدينية", "Turkish — Diyanet", "Diyanet İşleri"),
        Edition("ms.basmeih", EdKind.TRANSLATION, "ms", "الملايوية — بسميه", "Malay — Basmeih", "Abdullah Muhammad Basmeih", approxMb = 2),
        Edition("es.cortes", EdKind.TRANSLATION, "es", "الإسبانية — كورتيس", "Spanish — Cortes", "Julio Cortes"),
        Edition("de.bubenheim", EdKind.TRANSLATION, "de", "الألمانية — بوبنهايم وإلياس", "German — Bubenheim & Elyas", "A. S. F. Bubenheim, N. Elyas"),
        Edition("ru.kuliev", EdKind.TRANSLATION, "ru", "الروسية — كولييف", "Russian — Kuliev", "Elmir Kuliev", approxMb = 2),
        Edition("bn.bengali", EdKind.TRANSLATION, "bn", "البنغالية — محي الدين خان", "Bengali — Muhiuddin Khan", "Muhiuddin Khan", approxMb = 3),
        Edition("fa.makarem", EdKind.TRANSLATION, "fa", "الفارسية — مكارم الشيرازي", "Persian — Makarem Shirazi", "Naser Makarem Shirazi", approxMb = 2),
        Edition("hi.hindi", EdKind.TRANSLATION, "hi", "الهندية — فاروق خان والندوي", "Hindi — Farooq Khan & Nadwi", "Suhel Farooq Khan, Saifur Rahman Nadwi", approxMb = 3),
        Edition("zh.jian", EdKind.TRANSLATION, "zh", "الصينية — ما جيان", "Chinese — Ma Jian", "Ma Jian"),
        Edition("sw.barwani", EdKind.TRANSLATION, "sw", "السواحيلية — البرواني", "Swahili — Al-Barwani", "Ali Muhsin Al-Barwani"),
        Edition("ha.gumi", EdKind.TRANSLATION, "ha", "الهوسا — غومي", "Hausa — Gumi", "Abubakar Mahmoud Gumi"),
        Edition("so.abduh", EdKind.TRANSLATION, "so", "الصومالية — عبده", "Somali — Abduh", "Mahmud Muhammad Abduh"),
    )

    fun get(id: String) = ALL.firstOrNull { it.id == id }

    private val cache = LinkedHashMap<String, Array<String>>()
    private val lock = Mutex()

    private fun file(ctx: Context, id: String) = File(File(ctx.filesDir, "editions").apply { mkdirs() }, "$id.txt")

    fun available(ctx: Context, id: String): Boolean = get(id)?.bundled == true || file(ctx, id).let { it.exists() && it.length() > 1000 }

    fun downloadedBytes(ctx: Context, id: String): Long = file(ctx, id).takeIf { it.exists() }?.length() ?: 0L

    /** Ids whose last load failed (cleared on retry / download). */
    val failed: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())

    fun cached(id: String): Array<String>? = cache[id]

    /** Warms the cache in the background so the first ayah sheet / tafsir page opens instantly. */
    private val warmScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)
    fun prewarm(ctx: Context, ids: List<String>) {
        val app = ctx.applicationContext
        ids.distinct().filter { it !in cache && available(app, it) }.forEach { id -> warmScope.launch { runCatching { load(app, id) } } }
    }

    /** Loads an edition as a 6236-slot array (empty strings where missing). Keeps the 4 most recent in memory. */
    suspend fun load(ctx: Context, id: String): Array<String>? = lock.withLock {
        cache[id]?.let { v -> cache.remove(id); cache[id] = v; return@withLock v }
        val q = Quran.load(ctx)
        val ed = get(id) ?: return@withLock null
        val arr = withContext(Dispatchers.IO) {
            runCatching {
                val content = if (ed.bundled) {
                    String(Quran.assetBytes(ctx, "tafsir/$id.txt.gz"), Charsets.UTF_8)
                } else file(ctx, id).takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: return@runCatching null
                Quran.parseTanzil(content, q.suras)
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.getOrNull()
        } ?: run { failed += id; return@withLock null }
        failed -= id
        cache[id] = arr
        while (cache.size > 4) cache.remove(cache.keys.first())
        arr
    }

    /** Downloads an edition to the device (Tanzil txt-2, or Al Quran Cloud JSON converted to the same format). */
    suspend fun download(ctx: Context, id: String, onProgress: (Float) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ed = get(id) ?: error("unknown edition")
            val c = (URL(ed.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 60_000; setRequestProperty("User-Agent", "Sakina/1.0 (Android)")
            }
            val total = c.contentLengthLong.takeIf { it > 0 } ?: (ed.approxMb * 1_000_000L)
            val bytes = c.inputStream.use { ins ->
                val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(32 * 1024); var n: Int; var got = 0L
                while (ins.read(buf).also { n = it } >= 0) { out.write(buf, 0, n); got += n; onProgress((got.toFloat() / total).coerceIn(0f, .98f)) }
                out.toByteArray()
            }
            c.disconnect()
            val text = String(bytes, Charsets.UTF_8)
            val content = if (ed.source == "AlQuranCloud") fromAlQuranCloud(text, ed) else text
            val lines = content.lineSequence().count { it.isNotEmpty() && it[0] != '#' && it.count { ch -> ch == '|' } >= 2 }
            check(lines >= 6000) { "incomplete download ($lines ayat)" }
            file(ctx, id).writeText(content, Charsets.UTF_8)
            lock.withLock { cache.remove(id); failed -= id }
            onProgress(1f)
        }
    }

    /** Converts an Al Quran Cloud edition JSON into Tanzil txt-2 lines, keeping an attribution header. */
    fun fromAlQuranCloud(json: String, ed: Edition): String {
        val data = JSONObject(json).getJSONObject("data")
        val sb = StringBuilder()
        val suras = data.getJSONArray("surahs")
        for (i in 0 until suras.length()) {
            val s = suras.getJSONObject(i); val n = s.getInt("number"); val ayahs = s.getJSONArray("ayahs")
            for (j in 0 until ayahs.length()) {
                val a = ayahs.getJSONObject(j)
                sb.append(n).append('|').append(a.getInt("numberInSurah")).append('|').append(a.getString("text").replace('\n', ' ').replace('\r', ' ')).append('\n')
            }
        }
        sb.append("\n# ").append(ed.nameAr).append(" — ").append(ed.author).append("\n# Source: Al Quran Cloud (https://alquran.cloud), edition ").append(ed.id)
            .append(". Terms: https://alquran.cloud/terms-and-conditions (free non-commercial reproduction, author attributed).\n")
        return sb.toString()
    }

    fun delete(ctx: Context, id: String) { if (get(id)?.bundled != true) file(ctx, id).delete(); cache.remove(id) }
}

/** Tanzil transliteration uses <u>…</U> (long vowels) and <b>…</B> (silent/merged letters). */
data class TranslitRun(val text: String, val long: Boolean, val bold: Boolean)

fun parseTranslit(s: String): List<TranslitRun> {
    val out = ArrayList<TranslitRun>()
    val re = Regex("""<([ub])>(.*?)</[ubUB]>""", RegexOption.IGNORE_CASE)
    var i = 0
    for (m in re.findAll(s)) {
        if (m.range.first > i) out += TranslitRun(s.substring(i, m.range.first), false, false)
        val tag = m.groupValues[1].lowercase()
        out += TranslitRun(m.groupValues[2], tag == "u", tag == "b")
        i = m.range.last + 1
    }
    if (i < s.length) out += TranslitRun(s.substring(i), false, false)
    return out.map { it.copy(text = it.text.replace(Regex("<[^>]*>"), "")) }
}
