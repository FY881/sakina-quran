package app.sakina.quran.azkar

import android.content.Context
import app.sakina.quran.quran.Quran
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.tabs.normalize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * One dhikr / dua. Arabic [text] is reproduced exactly as in the source dataset (or Tanzil for Quran ayat).
 * [id] is stable: "h<chapter>.<i>" (Hisn), "m<order>" (morning/evening DB), "q<s>:<a>" (Quran), "c<n>" (user's own).
 */
data class Dhikr(
    val id: String,
    val text: String,
    val count: Int,
    val ref: String = "",
    val virtue: String = "",
    val countLabel: String = "",
    /** Licence-cleared audio (mp3quran.net only for now); null = audio hook, not yet available. */
    val audio: String? = null,
    /** For Quran ayat: sura/ayah of the first ayah, to open the mushaf there. */
    val sura: Int = 0,
    val ayah: Int = 0,
    val chapter: Int = -1,
) {
    /** Source reference for display: Arabic text, so its numbers use Arabic-Indic digits (data stays byte-identical). */
    val refAr: String get() = arabicIndic(ref)
    val virtueAr: String get() = arabicIndic(virtue)
}

/** Western → Arabic-Indic digits for Arabic-script references ("مسلم 4/ 2083" → "مسلم ٤/ ٢٠٨٣"). */
fun arabicIndic(s: String): String = buildString(s.length) { s.forEach { c -> append(if (c in '0'..'9') '\u0660' + (c - '0') else c) } }

data class Chapter(val index: Int, val title: String, val items: List<Dhikr>)

/** Sakina's own grouping of the Hisn chapters (titles written by Sakina). */
enum class AzCat(val ar: String, val en: String, val ico: Ico, val chapters: List<Int>) {
    MORNING_EVENING("الصباح والمساء", "Morning & evening", Ico.SUNRISE, listOf(26, 27)),
    SLEEP("النوم والاستيقاظ", "Sleep & waking", Ico.NIGHT, listOf(0, 28, 29, 30, 31)),
    PRAYER("الصلاة والأذان", "Prayer & adhan", Ico.MOSQUE, (14..25).toList() + listOf(32, 33)),
    DAILY("اليوم والبيت", "Daily life & home", Ico.WUDU, (1..13).toList()),
    DISTRESS("الهم والكرب", "Worry & hardship", Ico.DUA, (34..46).toList() + listOf(106, 122, 123, 124, 125, 126, 128)),
    SICK("المرض والوفاة", "Illness & death", Ico.SEEDLING, (49..60).toList()),
    NATURE("المطر والطبيعة", "Rain & nature", Ico.FULLMOON, (61..67).toList()),
    FOOD("الطعام والصيام", "Food & fasting", Ico.DATES, (68..76).toList()),
    TRAVEL("السفر", "Travel", Ico.LOCATION, (95..105).toList()),
    SOCIAL("المعاملات والآداب", "People & manners", Ico.FAMILY, listOf(47, 48, 127) + (77..94).toList() + (107..114).toList()),
    HAJJ("الحج والعمرة", "Hajj & Umrah", Ico.KAABA, (115..121).toList()),
    DHIKR("الذكر والاستغفار", "Dhikr & forgiveness", Ico.TASBIH, (129..132).toList());

    companion object {
        fun of(chapter: Int): AzCat? = entries.firstOrNull { chapter in it.chapters }
    }
}

/** A sitting of azkar read with the counter (morning, evening, a Hisn chapter, …). */
data class Session(val id: String, val ar: String, val en: String, val ico: Ico, val items: List<Dhikr>)

/** Built-in sessions shown as tiles. id → (ar, en, icon). */
val SESSION_META: List<Triple<String, Pair<String, String>, Ico>> = listOf(
    Triple("morning", "أذكار الصباح" to "Morning azkar", Ico.SUNRISE),
    Triple("evening", "أذكار المساء" to "Evening azkar", Ico.MAGHRIB),
    Triple("sleep", "أذكار النوم" to "Before sleep", Ico.NIGHT),
    Triple("waking", "أذكار الاستيقاظ" to "On waking", Ico.FAJR),
    Triple("after_prayer", "أذكار بعد الصلاة" to "After prayer", Ico.MOSQUE),
    Triple("mosque", "المسجد والأذان" to "Mosque & adhan", Ico.ADHAN),
    Triple("travel", "أذكار السفر" to "Travel", Ico.LOCATION),
    Triple("ruqyah", "الرقية الشرعية" to "Ruqyah", Ico.PRIVACY),
)

/** Quranic duas: (sura, firstAyah, lastAyah, topicAr, topicEn). Text always comes from Tanzil. */
val QURANIC_DUAS: List<QDua> = listOf(
    QDua(1, 6, 7, "الهداية إلى الصراط المستقيم", "Guidance to the straight path"),
    QDua(2, 127, 128, "قبول العمل والتوبة", "Acceptance and repentance"),
    QDua(2, 201, 201, "حسنة الدنيا والآخرة", "Good in this world and the next"),
    QDua(2, 250, 250, "الصبر والثبات", "Patience and steadfastness"),
    QDua(2, 286, 286, "التخفيف والعفو والنصر", "Ease, pardon and victory"),
    QDua(3, 8, 9, "ثبات القلب", "A steadfast heart"),
    QDua(3, 16, 16, "المغفرة والوقاية من النار", "Forgiveness and protection from the Fire"),
    QDua(3, 38, 38, "الذرية الطيبة", "Righteous offspring"),
    QDua(3, 53, 53, "الإيمان والاتباع", "Faith and following the Messenger"),
    QDua(3, 147, 147, "غفران الذنوب والثبات", "Forgiveness and firm footing"),
    QDua(3, 191, 194, "دعاء أولي الألباب", "The prayer of people of understanding"),
    QDua(7, 23, 23, "دعاء آدم بالتوبة", "Adam's prayer of repentance"),
    QDua(7, 47, 47, "النجاة من رفقة الظالمين", "Not to be with the wrongdoers"),
    QDua(7, 126, 126, "الصبر وحسن الخاتمة", "Patience and dying as Muslims"),
    QDua(10, 85, 86, "النجاة من الفتنة", "Safety from trials"),
    QDua(11, 47, 47, "دعاء نوح بالمغفرة", "Nuh's prayer for forgiveness"),
    QDua(12, 101, 101, "حسن الخاتمة واللحاق بالصالحين", "To die a Muslim and join the righteous"),
    QDua(14, 40, 41, "إقامة الصلاة والمغفرة للوالدين", "Establishing prayer; forgiveness for parents"),
    QDua(17, 24, 24, "الرحمة للوالدين", "Mercy for one's parents"),
    QDua(17, 80, 80, "مُدخل صدق ومُخرج صدق", "A truthful entrance and exit"),
    QDua(18, 10, 10, "الرحمة والرشد", "Mercy and right guidance"),
    QDua(20, 25, 28, "انشراح الصدر وتيسير الأمر", "An open heart and an easy task"),
    QDua(20, 114, 114, "الزيادة في العلم", "Increase in knowledge"),
    QDua(21, 83, 83, "دعاء أيوب في الضر", "Ayyub's prayer in affliction"),
    QDua(21, 87, 87, "دعاء ذي النون", "The prayer of Dhun-Nun (Yunus)"),
    QDua(21, 89, 89, "دعاء زكريا بالولد", "Zakariyya's prayer for a child"),
    QDua(23, 29, 29, "المنزل المبارك", "A blessed landing place"),
    QDua(23, 97, 98, "الاستعاذة من الشياطين", "Refuge from the devils"),
    QDua(23, 109, 109, "المغفرة والرحمة", "Forgiveness and mercy"),
    QDua(23, 118, 118, "خير الراحمين", "The best of the merciful"),
    QDua(25, 65, 66, "صرف عذاب جهنم", "Averting the punishment of Hell"),
    QDua(25, 74, 74, "قرة أعين من الأزواج والذرية", "Spouses and children as joy of the eyes"),
    QDua(26, 83, 85, "الحكمة والصلاح وجنة النعيم", "Wisdom, righteousness and Paradise"),
    QDua(27, 19, 19, "شكر النعمة والعمل الصالح", "Gratitude and righteous deeds"),
    QDua(28, 16, 16, "الاعتراف بالظلم والمغفرة", "Admitting wrong, seeking forgiveness"),
    QDua(28, 24, 24, "الفقر إلى الله", "In need of Allah's good"),
    QDua(40, 7, 9, "دعاء حملة العرش للمؤمنين", "The angels' prayer for the believers"),
    QDua(46, 15, 15, "شكر النعمة وصلاح الذرية", "Gratitude and righteous children"),
    QDua(59, 10, 10, "سلامة الصدر للمؤمنين", "A heart free of rancour"),
    QDua(60, 4, 5, "التوكل والإنابة", "Reliance and turning to Allah"),
    QDua(66, 8, 8, "إتمام النور والمغفرة", "Perfect our light"),
    QDua(66, 11, 11, "دعاء امرأة فرعون", "The prayer of Pharaoh's wife"),
    QDua(71, 28, 28, "المغفرة للوالدين والمؤمنين", "Forgiveness for parents and believers"),
)

data class QDua(val sura: Int, val from: Int, val to: Int, val topicAr: String, val topicEn: String)

/** Ruqyah ayat commonly recited (sura, from, to, repeat). Text always comes from Tanzil. */
val RUQYAH_AYAT: List<IntArray> = listOf(
    intArrayOf(1, 1, 7, 1), intArrayOf(2, 1, 5, 1), intArrayOf(2, 102, 102, 1), intArrayOf(2, 163, 164, 1),
    intArrayOf(2, 255, 257, 1), intArrayOf(2, 284, 286, 1), intArrayOf(3, 18, 19, 1), intArrayOf(7, 54, 56, 1),
    intArrayOf(7, 117, 122, 1), intArrayOf(10, 81, 82, 1), intArrayOf(20, 69, 69, 1), intArrayOf(23, 115, 118, 1),
    intArrayOf(37, 1, 10, 1), intArrayOf(46, 29, 32, 1), intArrayOf(55, 33, 36, 1), intArrayOf(59, 21, 24, 1),
    intArrayOf(72, 1, 9, 1), intArrayOf(112, 1, 4, 3), intArrayOf(113, 1, 5, 3), intArrayOf(114, 1, 6, 3),
)

/** Hisn chapters whose duas are used for ruqyah (sick visit, pain, evil eye, children, shaytan). */
val RUQYAH_CHAPTERS = listOf(49, 124, 125, 48, 45, 128)

/** Hisn chapters shown as "Prophetic duas" (all from the Sunnah with references). */
val PROPHETIC_CHAPTERS = listOf(34, 35, 41, 43, 40, 44, 53, 129, 23, 32, 82, 106)

/** Hisn chapter used for the Istikhara guide. */
const val ISTIKHARA_CHAPTER = 25

class Azkar(val chapters: List<Chapter>, val morningEvening: List<Dhikr>, private val meTypes: Map<String, Int>) {
    private val byId: Map<String, Dhikr> by lazy { (chapters.flatMap { it.items } + morningEvening).associateBy { it.id } }
    fun dhikr(id: String): Dhikr? = byId[id]
    fun chapter(i: Int): Chapter? = chapters.getOrNull(i)

    val morning: List<Dhikr> get() = morningEvening.filter { meTypes[it.id] != 2 }
    val evening: List<Dhikr> get() = morningEvening.filter { meTypes[it.id] != 1 }

    private fun join(vararg ch: Int) = ch.flatMap { chapters.getOrNull(it)?.items ?: emptyList() }

    /** Builds a session. [quran] is needed only for "ruqyah". [custom] = the user's own azkar; [favs] = favourite ids. */
    fun session(id: String, quran: Quran? = null, custom: List<Dhikr> = emptyList(), favs: Collection<String> = emptyList()): Session? {
        SESSION_META.firstOrNull { it.first == id }?.let { (sid, names, ico) ->
            val items = when (sid) {
                "morning" -> morning
                "evening" -> evening
                "sleep" -> join(28)
                "waking" -> join(0)
                "after_prayer" -> join(24)
                "mosque" -> join(11, 12, 13, 14)
                "travel" -> join(95, 96, 97, 102, 103, 104, 105, 100, 101)
                "ruqyah" -> ruqyah(quran ?: return null)
                else -> emptyList()
            }
            return Session(sid, names.first, names.second, ico, items)
        }
        if (id == "fav") return Session("fav", "أذكاري المفضلة", "My favourites", Ico.STAR, favs.mapNotNull { dhikr(it) ?: quran?.let { q -> quranDhikr(q, it) } })
        if (id == "custom") return Session("custom", "أذكاري الخاصة", "My own azkar", Ico.JOURNAL, custom)
        if (id.startsWith("h")) {
            val c = chapter(id.drop(1).toIntOrNull() ?: return null) ?: return null
            return Session(id, c.title, c.title, AzCat.of(c.index)?.ico ?: Ico.AZKAR, c.items)
        }
        return null
    }

    fun ruqyah(q: Quran): List<Dhikr> =
        RUQYAH_AYAT.map { r -> ayatDhikr(q, r[0], r[1], r[2], r[3]) } + RUQYAH_CHAPTERS.flatMap { chapters.getOrNull(it)?.items ?: emptyList() }

    /** Full-text search (diacritics-insensitive) over chapter titles and every dhikr. */
    fun search(query: String, limit: Int = 120): List<Pair<Dhikr, String>> {
        val n = normalize(query.trim())
        if (n.length < 2) return emptyList()
        val out = ArrayList<Pair<Dhikr, String>>()
        for (c in chapters) {
            val titleHit = normalize(c.title).contains(n)
            c.items.forEachIndexed { i, d ->
                if (out.size < limit && (titleHit && i == 0 || normalize(d.text).contains(n) || normalize(d.ref).contains(n))) out += d to c.title
            }
        }
        for (d in morningEvening) if (out.size < limit && (normalize(d.text).contains(n) || normalize(d.virtue).contains(n))) out += d to "أذكار الصباح والمساء"
        return out
    }

    companion object {
        @Volatile private var cached: Azkar? = null
        private val lock = Mutex()

        suspend fun load(ctx: Context): Azkar = cached ?: lock.withLock {
            cached ?: withContext(Dispatchers.IO) {
                val a = ctx.applicationContext.assets
                parse(a.open("azkar/hisn.json").bufferedReader().use { it.readText() }, a.open("azkar/morning_evening.json").bufferedReader().use { it.readText() })
            }.also { cached = it }
        }

        /** Pure parser (unit-tested on the JVM with org.json). */
        fun parse(hisnJson: String, meJson: String): Azkar {
            val h = JSONArray(hisnJson)
            val chapters = (0 until h.length()).map { ci ->
                val o = h.getJSONObject(ci)
                val idx = o.getInt("i")
                val arr = o.getJSONArray("a")
                Chapter(idx, o.getString("t"), (0 until arr.length()).map { k ->
                    val d = arr.getJSONObject(k)
                    Dhikr("h$idx.$k", d.getString("t"), d.optInt("c", 1).coerceAtLeast(1), d.optString("r"), chapter = idx)
                })
            }
            val m = JSONArray(meJson)
            val types = HashMap<String, Int>()
            val me = (0 until m.length()).map { k ->
                val d = m.getJSONObject(k)
                val id = "m" + d.getInt("o")
                types[id] = d.optInt("y", 0)
                Dhikr(id, d.getString("t"), d.optInt("c", 1).coerceAtLeast(1), d.optString("s"), d.optString("f"), d.optString("cd"), d.optString("au").takeIf { it.startsWith("https://") && "mp3quran.net" in it })
            }
            return Azkar(chapters, me, types)
        }

        fun ayatDhikr(q: Quran, s: Int, from: Int, to: Int, repeat: Int = 1): Dhikr {
            val text = (from..to).joinToString(" ") { a -> q.displayAyah(s, a) + " ﴿" + arabicDigits(a) + "﴾" }
            val name = q.suras[s - 1].name
            val ref = if (from == to) "سورة $name: ${arabicDigits(from)}" else "سورة $name: ${arabicDigits(from)}–${arabicDigits(to)}"
            return Dhikr(if (from == to) "q$s:$from" else "q$s:$from-$to", text, repeat, ref, sura = s, ayah = from)
        }

        /** Resolves a favourite id "q<s>:<a>" or "q<s>:<a>-<b>" against the Quran. */
        fun quranDhikr(q: Quran, id: String): Dhikr? {
            if (!id.startsWith("q")) return null
            val s = id.drop(1).substringBefore(':').toIntOrNull() ?: return null
            val r = id.substringAfter(':')
            val a = r.substringBefore('-').toIntOrNull() ?: return null
            val b = r.substringAfter('-', r).toIntOrNull() ?: a
            if (s !in 1..114 || a < 1 || b > q.suras[s - 1].ayas) return null
            return ayatDhikr(q, s, a, b)
        }

        fun arabicDigits(n: Int) = n.toString().map { if (it in '0'..'9') '\u0660' + (it - '0') else it }.joinToString("")
    }
}
