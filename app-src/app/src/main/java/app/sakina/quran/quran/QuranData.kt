package app.sakina.quran.quran

import android.content.Context
import app.sakina.quran.ui.tabs.normalize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Surah metadata from Tanzil's quran-data.xml. [start] = global index of its first ayah (0-based). */
data class Sura(val index: Int, val ayas: Int, val start: Int, val name: String, val tname: String, val ename: String, val meccan: Boolean, val order: Int = 0, val rukus: Int = 0)

data class AyahRef(val sura: Int, val ayah: Int, val text: String)

class Quran(
    val suras: List<Sura>, val text: Array<String>, private val pages: IntArray, private val juzStarts: IntArray,
    /** Global index of the first ayah of each of the 240 hizb quarters (rub'). */
    val quarters: IntArray = IntArray(0),
    /** Global indices of the 15 sajda ayat. */
    val sajdas: Set<Int> = emptySet(),
    /** True when the bundled Uthmani file's MD5 equals the Tanzil original (QR01). */
    val verified: Boolean = false,
) {
    val pageCount: Int get() = pages.size
    /** First global ayah index on mushaf page [p] (1..604). */
    fun pageStart(p: Int): Int = pages[(p - 1).coerceIn(0, pages.size - 1)]
    /** Last global ayah index on mushaf page [p]. */
    fun pageEnd(p: Int): Int = if (p >= pages.size) text.size - 1 else pages[p.coerceAtLeast(1)] - 1
    fun pageOfGlobal(g: Int): Int = (pages.indexOfLast { it <= g }.coerceAtLeast(0)) + 1
    fun juzOfGlobal(g: Int): Int = juzStarts.indexOfLast { it <= g }.coerceAtLeast(0) + 1
    /** Hizb quarter 1..240 containing global ayah [g]. */
    fun quarterOfGlobal(g: Int): Int = quarters.indexOfLast { it <= g }.coerceAtLeast(0) + 1
    /** First mushaf page of juz [j]. */
    fun juzPage(j: Int): Int = pageOfGlobal(juzStarts[(j - 1).coerceIn(0, juzStarts.size - 1)])
    fun quarterPage(q: Int): Int = if (quarters.isEmpty()) 1 else pageOfGlobal(quarters[(q - 1).coerceIn(0, quarters.size - 1)])
    /** Arabic letters (no diacritics, spaces or marks) in ayah [g] — basis of the hasanat estimate (QR31). */
    fun letters(g: Int): Int = text[g].count { it in '\u0621'..'\u064A' || it == '\u0671' }
    fun pageLetters(p: Int): Int { var n = 0; for (g in pageStart(p)..pageEnd(p)) n += letters(g); return n }

    fun ayah(sura: Int, ayah: Int): String = text[suras[sura - 1].start + ayah - 1]
    fun global(sura: Int, ayah: Int) = suras[sura - 1].start + ayah - 1
    /** Mushaf (Madinah) page 1..604 of an ayah. */
    fun page(sura: Int, ayah: Int): Int { val g = global(sura, ayah); var p = pages.indexOfLast { it <= g }; if (p < 0) p = 0; return p + 1 }
    fun juz(sura: Int, ayah: Int): Int { val g = global(sura, ayah); return juzStarts.indexOfLast { it <= g }.coerceAtLeast(0) + 1 }

    /** Basmala exactly as Tanzil writes it (ayah 1:1). */
    val basmala: String get() = text[0]

    /** Ayah text for display: ayah 1 of surahs other than 1 and 9 carries the basmala prefix in Tanzil; it is shown as a header instead. */
    fun displayAyah(sura: Int, ayah: Int): String {
        val t = ayah(sura, ayah)
        if (ayah != 1 || sura == 1 || sura == 9) return t
        val w = t.split(' ')
        return if (w.size > 4 && normalize(w.take(4).joinToString(" ")).replace("\u0651", "") == normalize(basmala).replace("\u0651", "")) w.drop(4).joinToString(" ") else t
    }

    fun fromGlobal(g: Int): Pair<Int, Int> { val s = suras.last { it.start <= g }; return s.index to (g - s.start + 1) }
    fun juzStart(j: Int): Pair<Int, Int> = fromGlobal(juzStarts[(j - 1).coerceIn(0, juzStarts.size - 1)])

    private val norm: Array<String> by lazy { Array(text.size) { normalize(text[it]) } }

    /** Diacritic-insensitive search over the whole text (max [limit] hits). */
    fun search(q: String, limit: Int = 200): Pair<Int, List<AyahRef>> {
        val n = normalize(q.trim())
        if (n.length < 2) return 0 to emptyList()
        val out = ArrayList<AyahRef>(); var total = 0
        for (s in suras) for (a in 1..s.ayas) {
            val g = s.start + a - 1
            if (norm[g].contains(n)) { total++; if (out.size < limit) out += AyahRef(s.index, a, text[g]) }
        }
        return total to out
    }

    companion object {
        /** MD5 of Tanzil's quran-uthmani.txt (Uthmani 1.1) exactly as downloaded, copyright block included. */
        const val UTHMANI_MD5 = "f5af117f2eef3d4258f98b9bea753611"
        /** MD5 of Tanzil's simple text (Simple 1.1, pause marks + sajdah + tatweel) before gzip. */
        const val SIMPLE_MD5 = "721dfdd25357d209012fea09bfc74768"

        /**
         * Reads a bundled asset that is stored gzipped in the source tree. The Android asset merger
         * un-gzips "*.gz" assets and drops the extension, so the APK holds the plain file: try that
         * first, then the gzipped name (unit tests / other packagers). Bytes are identical either way.
         */
        fun assetBytes(ctx: Context, gzPath: String): ByteArray {
            val plain = gzPath.removeSuffix(".gz")
            return runCatching { ctx.assets.open(plain).use { it.readBytes() } }.getOrElse {
                java.util.zip.GZIPInputStream(ctx.assets.open(gzPath)).use { it.readBytes() }
            }
        }

        fun md5(b: ByteArray): String = java.security.MessageDigest.getInstance("MD5").digest(b).joinToString("") { "%02x".format(it) }

        /** Parses Tanzil "sura|aya|text" lines (comments '#' skipped) into a 6236-slot array. */
        fun parseTanzil(content: String, suras: List<Sura>): Array<String> {
            val text = arrayOfNulls<String>(6236)
            content.lineSequence().forEach { l ->
                if (l.isEmpty() || l[0] == '#') return@forEach
                val p1 = l.indexOf('|'); val p2 = l.indexOf('|', p1 + 1)
                if (p1 <= 0 || p2 <= 0) return@forEach
                val s = l.substring(0, p1).toIntOrNull() ?: return@forEach
                val a = l.substring(p1 + 1, p2).toIntOrNull() ?: return@forEach
                if (s !in 1..suras.size || a !in 1..suras[s - 1].ayas) return@forEach
                text[suras[s - 1].start + a - 1] = l.substring(p2 + 1).trimEnd('\r')
            }
            return Array(text.size) { text[it] ?: "" }
        }

        @Volatile private var simpleCache: Array<String>? = null
        @Volatile var simpleVerified: Boolean = false; private set

        /** Tanzil "simple" (Imlaei) script, loaded on demand (QR13). */
        suspend fun simple(ctx: Context): Array<String> = simpleCache ?: withContext(Dispatchers.IO) {
            val q = load(ctx)
            val bytes = assetBytes(ctx, "quran/quran-simple.txt.gz")
            simpleVerified = md5(bytes) == SIMPLE_MD5
            parseTanzil(String(bytes, Charsets.UTF_8), q.suras).also { simpleCache = it }
        }

        @Volatile private var cached: Quran? = null
        private val lock = Mutex()

        suspend fun load(ctx: Context): Quran = cached ?: lock.withLock {
            cached ?: withContext(Dispatchers.IO) { parse(ctx.applicationContext) }.also { cached = it }
        }

        private fun attr(tag: String, name: String): String? = Regex("""\b$name="([^"]*)"""").find(tag)?.groupValues?.get(1)

        private fun parse(ctx: Context): Quran = fromData(
            ctx.assets.open("quran/quran-data.xml").bufferedReader(Charsets.UTF_8).use { it.readText() },
            ctx.assets.open("quran/quran-uthmani.txt").use { it.readBytes() },
        )

        /** Builds the Quran from Tanzil's metadata XML and the raw Uthmani file (pure: unit-tested). */
        fun fromData(xml: String, bytes: ByteArray): Quran {
            val suras = Regex("""<sura [^>]*/>""").findAll(xml).map { m ->
                val t = m.value
                Sura(attr(t, "index")!!.toInt(), attr(t, "ayas")!!.toInt(), attr(t, "start")!!.toInt(), attr(t, "name")!!, attr(t, "tname")!!, attr(t, "ename")!!, attr(t, "type") == "Meccan",
                    attr(t, "order")?.toIntOrNull() ?: 0, attr(t, "rukus")?.toIntOrNull() ?: 0)
            }.toList()
            fun starts(tag: String) = Regex("""<$tag [^>]*/>""").findAll(xml).map { m ->
                val s = attr(m.value, "sura")!!.toInt(); val a = attr(m.value, "aya")!!.toInt(); suras[s - 1].start + a - 1
            }.toList().toIntArray()
            val text = parseTanzil(String(bytes, Charsets.UTF_8), suras)
            return Quran(suras, text, starts("page"), starts("juz"), starts("quarter"), starts("sajda").toSet(), md5(bytes) == UTHMANI_MD5)
        }
    }
}
