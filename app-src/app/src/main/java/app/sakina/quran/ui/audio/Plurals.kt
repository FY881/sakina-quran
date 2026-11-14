package app.sakina.quran.ui.audio

import app.sakina.quran.ui.common.num

/**
 * Arabic counted noun (العدد والمعدود), pure so it is unit-tested:
 *  1 → [one] («ملف واحد»), 2 → [two] («ملفان»), 3–10 → n + [few] («٧ ملفات»), 11–99 → n + [many] («٢٨٦ ملفًا»),
 *  0 / hundreds (n % 100 in 0..2 for n > 2) → n + [single] («١٠٠ ملف»), n % 100 in 3..10 → [few] («١٠٣ ملفات»).
 */
fun pluralAr(n: Int, one: String, two: String, few: String, many: String, single: String = many): String {
    val r = n % 100
    val word = when {
        n == 1 -> return one
        n == 2 -> return two
        r in 3..10 -> few
        r in 11..99 -> many
        else -> single
    }
    return "${num(n, true)} $word"
}

/** «ملف واحد / ملفان / ٧ ملفات / ٢٨٦ ملفًا»; [afterMin] gives the genitive dual after «من» («من ملفين»). */
fun filesAr(n: Int, afterMin: Boolean = false): String =
    pluralAr(n, "ملف واحد", if (afterMin) "ملفين" else "ملفان", "ملفات", "ملفًا", "ملف")

fun filesEn(n: Int): String = if (n == 1) "1 file" else "$n files"
