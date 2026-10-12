package app.sakina.quran.quran

import android.content.Context
import app.sakina.quran.ui.tabs.normalize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Surah metadata from Tanzil's quran-data.xml. [start] = global index of its first ayah (0-based). */
data class Sura(val index: Int, val ayas: Int, val start: Int, val name: String, val tname: String, val ename: String, val meccan: Boolean)

data class AyahRef(val sura: Int, val ayah: Int, val text: String)

class Quran(val suras: List<Sura>, val text: Array<String>, private val pages: IntArray, private val juzStarts: IntArray) {
    fun ayah(sura: Int, ayah: Int): String = text[suras[sura - 1].start + ayah - 1]
    fun global(sura: Int, ayah: Int) = suras[sura - 1].start + ayah - 1
    /** Mushaf (Madinah) page 1..604 of an ayah. */
    fun page(sura: Int, ayah: Int): Int { val g = global(sura, ayah); var p = pages.indexOfLast { it <= g }; if (p < 0) p = 0; return p + 1 }
    fun juz(sura: Int, ayah: Int): Int { val g = global(sura, ayah); return juzStarts.indexOfLast { it <= g }.coerceAtLeast(0) + 1 }

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
        /** Basmala exactly as it prefixes ayah 1 in the Tanzil text (display only; the asset is never changed). */
        const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

        @Volatile private var cached: Quran? = null
        private val lock = Mutex()

        suspend fun load(ctx: Context): Quran = cached ?: lock.withLock {
            cached ?: withContext(Dispatchers.IO) { parse(ctx.applicationContext) }.also { cached = it }
        }

        private fun attr(tag: String, name: String): String? = Regex("""\b$name="([^"]*)"""").find(tag)?.groupValues?.get(1)

        private fun parse(ctx: Context): Quran {
            val xml = ctx.assets.open("quran/quran-data.xml").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val suras = Regex("""<sura [^>]*/>""").findAll(xml).map { m ->
                val t = m.value
                Sura(attr(t, "index")!!.toInt(), attr(t, "ayas")!!.toInt(), attr(t, "start")!!.toInt(), attr(t, "name")!!, attr(t, "tname")!!, attr(t, "ename")!!, attr(t, "type") == "Meccan")
            }.toList()
            fun starts(tag: String) = Regex("""<$tag [^>]*/>""").findAll(xml).map { m ->
                val s = attr(m.value, "sura")!!.toInt(); val a = attr(m.value, "aya")!!.toInt(); suras[s - 1].start + a - 1
            }.toList().toIntArray()
            val text = arrayOfNulls<String>(6236)
            ctx.assets.open("quran/quran-uthmani.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { l ->
                    if (l.isEmpty() || l[0] == '#') return@forEach
                    val p1 = l.indexOf('|'); val p2 = l.indexOf('|', p1 + 1)
                    if (p1 <= 0 || p2 <= 0) return@forEach
                    val s = l.substring(0, p1).toIntOrNull() ?: return@forEach
                    val a = l.substring(p1 + 1, p2).toIntOrNull() ?: return@forEach
                    text[suras[s - 1].start + a - 1] = l.substring(p2 + 1)
                }
            }
            return Quran(suras, Array(text.size) { text[it] ?: "" }, starts("page"), starts("juz"))
        }
    }
}
