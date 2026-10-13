package app.sakina.quran.quran

/**
 * Sakina's own tajweed rule engine (HZ12). It reads the unaltered Tanzil Uthmani text and returns colour spans;
 * the text itself is never changed. Rules follow the Hafs 'an 'Asim conventions encoded in the Uthmani script:
 * a bare nun/mim (no sukun) signals a hidden or merged sound, U+06E2/U+06ED mark iqlab, U+0653 marks a long madd,
 * U+0670/U+06E5/U+06E6 are the small alif/waw/ya of the natural madd.
 */
enum class TajRule(val ar: String, val en: String, val descAr: String, val descEn: String) {
    SILENT("لا يُنطق", "Silent", "همزة الوصل في وسط الكلام، واللام الشمسية: تُكتب ولا تُنطق.", "Hamzat al-wasl inside speech and the sun-letter lam: written but not pronounced."),
    GHUNNAH("غنّة", "Ghunnah", "النون والميم المشددتان: غنّة بمقدار حركتين تخرج من الخيشوم.", "Doubled nun and mim: a nasal sound held for two counts."),
    IKHFA("إخفاء", "Ikhfa", "النون الساكنة أو التنوين قبل أحد حروف الإخفاء الخمسة عشر: تُخفى مع غنّة.", "Nun sakinah or tanween before one of the 15 ikhfa letters: concealed with ghunnah."),
    IKHFA_SHAFAWI("إخفاء شفوي", "Labial ikhfa", "الميم الساكنة قبل الباء: تُخفى مع غنّة.", "Mim sakinah before ba: concealed with ghunnah."),
    IDGHAM_GHUNNAH("إدغام بغنّة", "Idgham with ghunnah", "النون الساكنة أو التنوين قبل (ي ن م و): تُدغم مع غنّة.", "Nun sakinah or tanween before ya, nun, mim, waw: merged with ghunnah."),
    IDGHAM_NO_GHUNNAH("إدغام بلا غنّة", "Idgham without ghunnah", "النون الساكنة أو التنوين قبل اللام أو الراء: تُدغم دون غنّة.", "Nun sakinah or tanween before lam or ra: merged without ghunnah."),
    IDGHAM_SHAFAWI("إدغام شفوي", "Labial idgham", "الميم الساكنة قبل الميم: تُدغم مع غنّة.", "Mim sakinah before mim: merged with ghunnah."),
    IQLAB("إقلاب", "Iqlab", "النون الساكنة أو التنوين قبل الباء: تُقلب ميمًا مخفاة مع غنّة.", "Nun sakinah or tanween before ba: turned into a hidden mim with ghunnah."),
    QALQALAH("قلقلة", "Qalqalah", "حروف (قطب جد) إذا سكنت أو وُقف عليها: تُنطق بنبرة واضحة.", "The letters qaf, ta, ba, jim, dal when silent or at a stop: pronounced with an echo."),
    MADD_NATURAL("مد طبيعي", "Natural madd", "مد بمقدار حركتين (الألف والواو والياء الصغيرة).", "Prolonged two counts (small alif, waw and ya)."),
    MADD_PERMISSIBLE("مد جائز منفصل", "Separated madd", "حرف مد في آخر كلمة وبعده همزة في أول الكلمة التالية: ٢ أو ٤ أو ٥ حركات.", "A madd letter ending a word, hamza starting the next: 2, 4 or 5 counts."),
    MADD_OBLIGATORY("مد واجب متصل", "Connected madd", "حرف مد بعده همزة في الكلمة نفسها: ٤ أو ٥ حركات.", "A madd letter followed by hamza in the same word: 4 or 5 counts."),
    MADD_NECESSARY("مد لازم", "Necessary madd", "حرف مد بعده سكون أصلي أو شدّة، ومنه فواتح السور: ست حركات.", "A madd letter followed by an original sukun or shaddah, incl. the opening letters: 6 counts."),
}

data class TajSpan(val start: Int, val end: Int, val rule: TajRule)

object Tajweed {
    private const val SUKUN = '\u0652'
    private const val SHADDA = '\u0651'
    private const val MADDAH = '\u0653'
    private const val WASL = '\u0671'
    private val TANWEEN = setOf('\u064B', '\u064C', '\u064D')
    private val HARAKAT = setOf('\u064E', '\u064F', '\u0650', '\u064B', '\u064C', '\u064D')
    private val IQLAB_MARKS = setOf('\u06E2', '\u06ED')
    private val SMALL_MADD = setOf('\u0670', '\u06E5', '\u06E6')
    private val THROAT = setOf('ء', 'أ', 'إ', 'ؤ', 'ئ', 'ه', 'ع', 'ح', 'غ', 'خ')
    private val IKHFA_LETTERS = "تثجدذزسشصضطظفقك".toSet()
    private val QALQALAH = "قطبجد".toSet()
    private val HAMZAS = setOf('ء', 'أ', 'إ', 'ؤ', 'ئ', '\u0654')

    /** Display-only: no-break space before each waqf mark (U+06D6–U+06DC) so it never wraps away from its word. Same length. */
    fun glueWaqf(text: String): String {
        if (text.indexOf(' ') < 0) return text
        val c = text.toCharArray()
        for (i in 0 until c.size - 1) if (c[i] == ' ' && c[i + 1] in '\u06D6'..'\u06DC') c[i] = '\u00A0'
        return String(c)
    }

    fun isLetter(c: Char) = c in '\u0621'..'\u064A' || c == WASL
    /** Marks that belong to the preceding letter (harakat, sukun, shaddah, small letters, Quranic annotation signs). */
    fun isMark(c: Char) = c in '\u064B'..'\u065F' || c == '\u0670' || c in '\u06DF'..'\u06E8' || c in '\u06EA'..'\u06ED' || c == '\u0640'
    /** Pause signs and separators between words. */
    private fun isGap(c: Char) = c == ' ' || c in '\u06D6'..'\u06DE' || c == '\u06E9' || c == '\u00A0' || c == '\u200C'

    private class Cl(val letter: Char, val start: Int, val end: Int, val marks: String, val wordStart: Boolean)

    private fun clusters(t: String): List<Cl> {
        val out = ArrayList<Cl>()
        var i = 0; var newWord = true
        while (i < t.length) {
            val c = t[i]
            if (isLetter(c)) {
                var j = i + 1
                while (j < t.length && isMark(t[j])) j++
                out += Cl(c, i, j, t.substring(i + 1, j), newWord)
                newWord = false; i = j
            } else { if (isGap(c)) newWord = true; i++ }
        }
        return out
    }

    /** Colour spans for one ayah, sorted and non-overlapping. */
    fun spans(t: String): List<TajSpan> {
        val cl = clusters(t)
        if (cl.isEmpty()) return emptyList()
        val rule = arrayOfNulls<TajRule>(t.length)
        fun mark(c: Cl, r: TajRule, override: Boolean = true) { for (k in c.start until c.end) if (override || rule[k] == null) rule[k] = r }
        fun bare(c: Cl) = c.marks.none { it in HARAKAT || it == SUKUN || it == SHADDA || it == MADDAH } && c.marks.none { it == '\u06E1' }
        /** The next pronounced cluster after index [k], skipping the silent alif/alif maqsura that carry fathatan. */
        fun nextOf(k: Int): Cl? {
            var j = k + 1
            while (j < cl.size && (cl[j].letter == 'ا' || cl[j].letter == 'ى') && cl[j].marks.isEmpty() && !cl[j].wordStart) j++
            return cl.getOrNull(j)
        }
        for ((k, c) in cl.withIndex()) {
            val last = k == cl.size - 1
            // Silent hamzat al-wasl (not at the start of the ayah) and sun-letter lam.
            if (c.letter == WASL && k > 0) mark(c, TajRule.SILENT)
            if (c.letter == 'ل' && k > 0 && cl[k - 1].letter == WASL && c.marks.isEmpty()) {
                val n = cl.getOrNull(k + 1)
                if (n != null && SHADDA in n.marks) mark(c, TajRule.SILENT)
            }
            // Ghunnah on doubled nun/mim.
            if ((c.letter == 'ن' || c.letter == 'م') && SHADDA in c.marks) mark(c, TajRule.GHUNNAH, override = false)
            // Qalqalah: silent qalqalah letter, or the last letter of the ayah (stop).
            if (c.letter in QALQALAH && (SUKUN in c.marks || last)) mark(c, TajRule.QALQALAH)
            // Nun sakinah (bare nun) and tanween.
            val tanween = c.marks.any { it in TANWEEN }
            val nunSakin = c.letter == 'ن' && bare(c)
            if ((tanween || nunSakin) && !last) {
                val n = nextOf(k)
                val r = when {
                    c.marks.any { it in IQLAB_MARKS } -> TajRule.IQLAB
                    n == null -> null
                    n.letter in THROAT -> null
                    n.letter == 'ب' -> TajRule.IQLAB
                    n.letter in "ينمو" -> if (nunSakin && !n.wordStart) null else TajRule.IDGHAM_GHUNNAH
                    n.letter in "لر" -> if (n.wordStart) TajRule.IDGHAM_NO_GHUNNAH else null
                    n.letter in IKHFA_LETTERS -> TajRule.IKHFA
                    else -> null
                }
                if (r != null) { mark(c, r); if (r != TajRule.IQLAB && n != null) mark(n, r, override = false) }
            }
            // Mim sakinah (bare mim).
            if (c.letter == 'م' && bare(c) && !last) {
                val n = cl.getOrNull(k + 1)
                if (n?.letter == 'ب') mark(c, TajRule.IKHFA_SHAFAWI)
                else if (n?.letter == 'م') { mark(c, TajRule.IDGHAM_SHAFAWI); mark(n, TajRule.IDGHAM_SHAFAWI, override = false) }
            }
            // Madd.
            if (MADDAH in c.marks) {
                val n = cl.getOrNull(k + 1)
                val r = when {
                    // يا / ها of address and attention are separate words written joined: separated madd.
                    c.wordStart && (c.letter == 'ي' || c.letter == 'ه') && '\u0670' in c.marks && n != null && n.letter in HAMZAS -> TajRule.MADD_PERMISSIBLE
                    n == null -> TajRule.MADD_NECESSARY
                    !n.wordStart && (n.letter in HAMZAS || n.marks.contains('\u0654')) -> TajRule.MADD_OBLIGATORY
                    n.wordStart && n.letter in HAMZAS -> TajRule.MADD_PERMISSIBLE
                    SHADDA in n.marks || SUKUN in n.marks -> TajRule.MADD_NECESSARY
                    n.wordStart -> TajRule.MADD_PERMISSIBLE
                    else -> TajRule.MADD_NECESSARY
                }
                mark(c, r)
            } else if (c.marks.any { it in SMALL_MADD }) mark(c, TajRule.MADD_NATURAL, override = false)
        }
        val out = ArrayList<TajSpan>()
        var i = 0
        while (i < t.length) {
            val r = rule[i]
            if (r == null) { i++; continue }
            var j = i + 1
            while (j < t.length && rule[j] == r) j++
            out += TajSpan(i, j, r); i = j
        }
        return out
    }

    /** Colour per rule. palette 0 = classic (tuned for light/dark), 1 = high contrast, 2 = colour-blind safe (Okabe–Ito). */
    fun color(r: TajRule, palette: Int, dark: Boolean): Long = when (palette) {
        1 -> when (r) {
            TajRule.SILENT -> if (dark) 0xFF9E9E9E else 0xFF6E6E6E
            TajRule.GHUNNAH -> if (dark) 0xFFFFB000 else 0xFFB35C00
            TajRule.IKHFA, TajRule.IKHFA_SHAFAWI -> if (dark) 0xFFFF6BFF else 0xFF8A008A
            TajRule.IDGHAM_GHUNNAH, TajRule.IDGHAM_NO_GHUNNAH, TajRule.IDGHAM_SHAFAWI -> if (dark) 0xFF4DFF88 else 0xFF006B2E
            TajRule.IQLAB -> if (dark) 0xFF40E0FF else 0xFF005F87
            TajRule.QALQALAH -> if (dark) 0xFFFF5252 else 0xFFB00000
            else -> if (dark) 0xFF8FB0FF else 0xFF0030B0
        }
        2 -> when (r) {
            TajRule.SILENT -> if (dark) 0xFF9E9E9E else 0xFF7A7A7A
            TajRule.GHUNNAH -> 0xFFE69F00
            TajRule.IKHFA, TajRule.IKHFA_SHAFAWI -> 0xFFCC79A7
            TajRule.IDGHAM_GHUNNAH, TajRule.IDGHAM_NO_GHUNNAH, TajRule.IDGHAM_SHAFAWI -> 0xFF009E73
            TajRule.IQLAB -> 0xFF56B4E9
            TajRule.QALQALAH -> 0xFFD55E00
            else -> if (dark) 0xFF7FB8E6 else 0xFF0072B2
        }
        else -> when (r) {
            TajRule.SILENT -> if (dark) 0xFF8C8C8C else 0xFFAAAAAA
            TajRule.GHUNNAH -> 0xFFFF7E1E
            TajRule.IKHFA -> if (dark) 0xFFC77DFF else 0xFF9400A8
            TajRule.IKHFA_SHAFAWI -> if (dark) 0xFFFF6FE3 else 0xFFD500B7
            TajRule.IDGHAM_GHUNNAH -> if (dark) 0xFF2FD1A3 else 0xFF169777
            TajRule.IDGHAM_NO_GHUNNAH -> if (dark) 0xFF5FD35F else 0xFF169200
            TajRule.IDGHAM_SHAFAWI -> if (dark) 0xFF9BE34A else 0xFF58B800
            TajRule.IQLAB -> 0xFF26BFFD
            TajRule.QALQALAH -> if (dark) 0xFFFF4B55 else 0xFFDD0008
            TajRule.MADD_NATURAL -> if (dark) 0xFF8FA8FF else 0xFF537FFF
            TajRule.MADD_PERMISSIBLE -> if (dark) 0xFF7C88FF else 0xFF4050FF
            TajRule.MADD_OBLIGATORY -> if (dark) 0xFF6F8CFF else 0xFF2144C1
            TajRule.MADD_NECESSARY -> if (dark) 0xFF5F7BFF else 0xFF000EBC
        }
    }
}
