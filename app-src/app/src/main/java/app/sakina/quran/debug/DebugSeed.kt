package app.sakina.quran.debug

import app.sakina.quran.data.QuranStore
import app.sakina.quran.data.QuranUser
import app.sakina.quran.quran.Khatma
import java.time.LocalDate

/**
 * Debug/CI only (called from MainActivity when BuildConfig.DEBUG): realistic sample Quran data so screenshots
 * show filled screens. Never reachable in release builds.
 */
object DebugSeed {
    /** "tajweed=1,paper=1,translit=1,tafsir=1,script=0,tr=en.sahih|fr.hamidullah" */
    suspend fun applyPrefs(store: QuranStore, spec: String) {
        val kv = spec.split(',').mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { p -> p[0].trim() to p[1].trim() } }.toMap()
        store.updatePrefs { p ->
            p.copy(
                tajweed = kv["tajweed"]?.let { it == "1" } ?: p.tajweed,
                paper = kv["paper"]?.toIntOrNull() ?: p.paper,
                translit = kv["translit"]?.let { it == "1" } ?: p.translit,
                inlineTafsir = kv["tafsir"]?.let { it == "1" } ?: p.inlineTafsir,
                script = kv["script"]?.toIntOrNull() ?: p.script,
                palette = kv["palette"]?.toIntOrNull() ?: p.palette,
                translations = when (val v = kv["tr"]) { null -> p.translations; "auto" -> null; else -> v.split('|').filter { it.isNotBlank() } },
            )
        }
    }

    /** Idempotent: re-running (e.g. in the other language) rewrites the same sample data with localised names. */
    suspend fun seed(store: QuranStore, ar: Boolean, today: LocalDate = LocalDate.now()) {
        val cur = store.now()
        val first = cur.khatmas.none { it.id == "kseed" }
        val start = today.minusDays(5)
        val log = (1..5).associate { today.minusDays(it.toLong()) to 19 }
        store.saveKhatma(
            Khatma("kseed", if (ar) "ختمة هذا الشهر" else "This month's khatma", 1, 604, start, start.plusDays(29), progress = 95, log = log, afterPrayers = true),
            makeActive = true,
        )
        store.saveKhatma(Khatma("kseed2", if (ar) "ختمة رمضان الماضي" else "Last Ramadan", 1, 604, today.minusDays(200), today.minusDays(171), progress = 604, finishedOn = today.minusDays(172), kind = 1))
        if (first) {
            // Reading history: 3 weeks, with al-Kahf on Fridays and al-Mulk on some nights.
            val pages = HashMap<LocalDate, MutableSet<Int>>()
            for (i in 0..20) {
                val d = today.minusDays(i.toLong())
                val n = listOf(4, 6, 2, 0, 10, 5, 3, 20, 1, 4, 0, 7, 8, 2, 4, 5, 0, 12, 3, 4, 6)[i]
                val set = pages.getOrPut(d) { mutableSetOf() }
                for (p in 0 until n) set += 1 + (i * 17 + p) % 604
                if (d.dayOfWeek == java.time.DayOfWeek.FRIDAY) set += 293..296
                if (i % 2 == 0) set += 562..564
            }
            store.addPages(pages)
            store.addSeconds(17 * 60, today)
            store.setHighlight(2, 255, 0); store.setHighlight(2, 256, 2); store.setHighlight(2, 286, 1); store.setHighlight(94, 5, 1); store.setHighlight(2, 186, 3)
            for (r in listOf("2:255", "94:5", "65:3")) store.toggleInCollection(QuranUser.FAV, r)
        }
        store.setNote(2, 255, if (ar) "آية الكرسي: أعظم آية في كتاب الله، أقرؤها بعد كل صلاة وقبل النوم." else "Ayat al-Kursi: the greatest verse — after every prayer and before sleep.", listOf(if (ar) "حفظ" else "memorise", if (ar) "أذكار" else "azkar"))
        store.setNote(94, 5, if (ar) "مع العسر يسر — تذكير عند الضيق." else "With hardship comes ease.", listOf(if (ar) "أمل" else "hope"))
        val col = if (ar) "آيات الصبر" else "Verses of patience"
        store.deleteCollection(if (ar) "Verses of patience" else "آيات الصبر")
        if (col !in store.now().collections) {
            store.createCollection(col)
            for (r in listOf("2:153", "2:155", "39:10", "103:3")) store.toggleInCollection(col, r)
        }
    }
}
