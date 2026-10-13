package app.sakina.quran.quran

/** One block of a mushaf page (QR02): surah header, basmala, or an ayah. Page boundaries come from Tanzil's 604-page Madinah metadata. */
sealed class Seg {
    data class Header(val sura: Int) : Seg()
    data class Basmala(val sura: Int) : Seg()
    data class Ayah(val g: Int, val sura: Int, val ayah: Int) : Seg()
}

object MushafLayout {
    /** Blocks on [page]: every surah that starts on the page gets a header (and a basmala, except al-Fatiha and at-Tawbah). */
    fun segments(q: Quran, page: Int): List<Seg> {
        val out = ArrayList<Seg>()
        for (g in q.pageStart(page)..q.pageEnd(page)) {
            val (s, a) = q.fromGlobal(g)
            if (a == 1) {
                out += Seg.Header(s)
                if (s != 1 && s != 9) out += Seg.Basmala(s)
            }
            out += Seg.Ayah(g, s, a)
        }
        return out
    }

    /** Surahs that appear on [page] (for the page header). */
    fun surasOn(q: Quran, page: Int): List<Int> = (q.pageStart(page)..q.pageEnd(page)).map { q.fromGlobal(it).first }.distinct()

    /** Ayah text shown on the page: the Tanzil text, with ayah 1's basmala prefix shown as its own block instead. */
    fun ayahText(q: Quran, s: Int, a: Int, simple: Array<String>?): String {
        if (simple == null) return q.displayAyah(s, a)
        val t = simple[q.global(s, a)]
        if (a != 1 || s == 1 || s == 9) return t
        val w = t.split(' ')
        fun n(x: String) = app.sakina.quran.ui.tabs.normalize(x).replace("\u0651", "")
        return if (w.size > 4 && n(w.take(4).joinToString(" ")) == n(simple[0])) w.drop(4).joinToString(" ") else t
    }
}
