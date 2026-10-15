package app.sakina.quran.audio

/*
 * AU1 — pure recitation logic (no Android types, unit-tested in AudioTest):
 * voices, media ids, queue/repeat plans, timing maps, sleep timer and download plans.
 * Sources: Islamic Network CDN (per-ayah, S-IN) and mp3quran.net (full surah + ayat_timing, S-MP3Q).
 * See SOURCES.md §B for the verified terms; audio is streamed or cached on the user's device only.
 */

/** Two ways to listen: ayah-by-ayah files (gapless, exact highlight, hifz loops) or whole-surah files (243 reciters). */
enum class AudioMode { AYAH, SURAH }

/** A per-ayah voice on the Islamic Network CDN. [bitrate] is the best published bitrate, [low] a data-saver one. */
data class AyahVoice(val id: String, val ar: String, val en: String, val bitrate: Int, val low: Int, val style: Style) {
    enum class Style { MURATTAL, MUJAWWAD, MUALLIM }
    fun url(global0: Int, dataSaver: Boolean = false): String =
        "https://cdn.islamic.network/quran/audio/${if (dataSaver) low else bitrate}/$id/${global0 + 1}.mp3"
}

object AyahVoices {
    /** Bitrates probed on cdn.islamic.network on 2026-10-14 (HEAD 200 for ayah 262). */
    val ALL = listOf(
        AyahVoice("ar.alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.husarymujawwad", "محمود خليل الحصري (مجوّد)", "Al-Husary (Mujawwad)", 128, 64, AyahVoice.Style.MUJAWWAD),
        AyahVoice("ar.minshawi", "محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi", 128, 128, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.minshawimujawwad", "المنشاوي (مجوّد)", "Al-Minshawi (Mujawwad)", 64, 64, AyahVoice.Style.MUJAWWAD),
        AyahVoice("ar.abdulbasitmurattal", "عبد الباسط عبد الصمد (مرتّل)", "Abdul Basit (Murattal)", 192, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.abdulsamad", "عبد الباسط عبد الصمد (مجوّد)", "Abdul Basit (Mujawwad)", 64, 64, AyahVoice.Style.MUJAWWAD),
        AyahVoice("ar.mahermuaiqly", "ماهر المعيقلي", "Maher Al-Muaiqly", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.abdurrahmaansudais", "عبد الرحمن السديس", "Abdurrahmaan As-Sudais", 192, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.saoodshuraym", "سعود الشريم", "Saood Ash-Shuraym", 64, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.shaatree", "أبو بكر الشاطري", "Abu Bakr Ash-Shaatree", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.hudhaify", "علي الحذيفي", "Ali Al-Hudhaify", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.ahmedajamy", "أحمد بن علي العجمي", "Ahmed Al-Ajamy", 128, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.muhammadayyoub", "محمد أيوب", "Muhammad Ayyoub", 128, 128, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.muhammadjibreel", "محمد جبريل", "Muhammad Jibreel", 128, 128, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.abdullahbasfar", "عبد الله بصفر", "Abdullah Basfar", 192, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.hanirifai", "هاني الرفاعي", "Hani Ar-Rifai", 192, 64, AyahVoice.Style.MURATTAL),
        AyahVoice("ar.aymanswoaid", "أيمن سويد (تعليمي)", "Ayman Swoaid (teaching)", 64, 64, AyahVoice.Style.MUALLIM),
    )
    fun byId(id: String): AyahVoice = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

/** Style of a whole-surah moshaf, read from its mp3quran name ("… - مرتل", "المصحف المجود", "المعلم"). */
fun moshafStyle(nameAr: String, nameEn: String): AyahVoice.Style = when {
    nameAr.contains("معلم") || nameEn.contains("Teacher", true) || nameEn.contains("Mualim", true) || nameEn.contains("Mo'lim", true) -> AyahVoice.Style.MUALLIM
    nameAr.contains("مجود") || nameAr.contains("مجوّد") || nameEn.contains("Mojawwad", true) || nameEn.contains("Mujawwad", true) -> AyahVoice.Style.MUJAWWAD
    else -> AyahVoice.Style.MURATTAL
}

/** Riwaya part of an mp3quran moshaf name ("حفص عن عاصم - مرتل" → "حفص عن عاصم"). */
fun moshafRiwaya(nameAr: String): String = nameAr.substringBefore(" - ").trim()

/**
 * Media ids carried through the Media3 session.
 * Ayah item: "a|<edition>|<global0>"; surah item: "s|<moshafId>|<sura>|<serverUrl>"; test clip: "t|<global0>".
 */
sealed class MediaRef {
    data class Ayah(val edition: String, val g: Int) : MediaRef()
    data class Surah(val moshaf: Int, val sura: Int, val server: String) : MediaRef()
    data class Test(val g: Int) : MediaRef()

    fun encode(): String = when (this) {
        is Ayah -> "a|$edition|$g"
        is Surah -> "s|$moshaf|$sura|$server"
        is Test -> "t|$g"
    }

    companion object {
        fun decode(id: String?): MediaRef? {
            if (id.isNullOrEmpty()) return null
            val p = id.split('|', limit = 4)
            return runCatching {
                when (p[0]) {
                    "a" -> Ayah(p[1], p[2].toInt())
                    "s" -> Surah(p[1].toInt(), p[2].toInt(), p[3])
                    "t" -> Test(p[1].toInt())
                    else -> null
                }
            }.getOrNull()
        }
        fun surahUrl(server: String, sura: Int) = server.trimEnd('/') + "/" + sura.toString().padStart(3, '0') + ".mp3"
    }
}

/**
 * Repeat settings (AU06 + HZ hifz loops).
 * @param ayahTimes how many times each ayah plays (≥1).
 * @param rangeTimes how many times the whole range plays; 0 = forever.
 * @param gapMs silence after each ayah (pause between repeats, talqin style).
 * @param ladder memorisation loop: ayah k ×ayahTimes, then the range start..k once, for each k.
 */
data class RepeatSpec(val ayahTimes: Int = 1, val rangeTimes: Int = 1, val gapMs: Long = 0, val ladder: Boolean = false) {
    val isPlain get() = ayahTimes <= 1 && rangeTimes == 1 && gapMs == 0L && !ladder
    val infinite get() = rangeTimes == 0
}

object QueuePlan {
    /** Upper bound on queue length so a huge range × repeat never exhausts memory. */
    const val MAX_ITEMS = 6000

    /** One pass (for an infinite range repeat the player loops this pass with REPEAT_MODE_ALL). */
    fun build(from: Int, to: Int, spec: RepeatSpec): List<Int> {
        require(to >= from) { "empty range" }
        val times = spec.ayahTimes.coerceIn(1, 100)
        val pass = ArrayList<Int>()
        if (spec.ladder) {
            for (k in from..to) {
                repeat(times) { pass += k }
                if (k > from) for (j in from..k) pass += j
                if (pass.size > MAX_ITEMS) break
            }
        } else {
            for (k in from..to) { repeat(times) { pass += k }; if (pass.size > MAX_ITEMS) break }
        }
        val passes = if (spec.infinite) 1 else spec.rangeTimes.coerceIn(1, 100)
        val out = ArrayList<Int>(minOf(MAX_ITEMS, pass.size * passes))
        for (i in 0 until passes) { if (out.size + pass.size > MAX_ITEMS) break; out += pass }
        if (out.isEmpty()) out += pass.take(MAX_ITEMS)
        return out
    }

    /** Range of globals for a surah (start .. start+ayas-1). */
    fun surahRange(suraStart: Int, ayas: Int, fromAyah: Int = 1, toAyah: Int = ayas): IntRange =
        (suraStart + fromAyah.coerceIn(1, ayas) - 1)..(suraStart + toAyah.coerceIn(fromAyah.coerceIn(1, ayas), ayas) - 1)
}

/** Ayah → time map of one whole-surah recording (mp3quran ayat_timing). Ayah 0 = intro (isti'adha/basmala). */
class TimingMap(val ayahs: IntArray, val starts: LongArray, val ends: LongArray) {
    init { require(ayahs.size == starts.size && starts.size == ends.size) }
    val size get() = ayahs.size

    /** Ayah being recited at [posMs]; 0 before the first ayah (intro); last ayah after the end. */
    fun ayahAt(posMs: Long): Int {
        if (ayahs.isEmpty()) return 0
        var lo = 0; var hi = starts.size - 1; var best = -1
        while (lo <= hi) { val mid = (lo + hi) ushr 1; if (starts[mid] <= posMs) { best = mid; lo = mid + 1 } else hi = mid - 1 }
        return if (best < 0) 0 else ayahs[best]
    }

    fun startOf(ayah: Int): Long? { val i = ayahs.indexOf(ayah); return if (i < 0) null else starts[i] }
    fun endOf(ayah: Int): Long? { val i = ayahs.indexOf(ayah); return if (i < 0) null else ends[i] }

    /** Compact on-disk form: one "ayah,start,end" line each. */
    fun serialize(): String = buildString { for (i in ayahs.indices) append(ayahs[i]).append(',').append(starts[i]).append(',').append(ends[i]).append('\n') }

    companion object {
        fun parseCompact(s: String): TimingMap? {
            val rows = s.lineSequence().mapNotNull { l -> l.split(',').takeIf { it.size == 3 }?.let { Triple(it[0].toIntOrNull(), it[1].toLongOrNull(), it[2].toLongOrNull()) } }
                .filter { it.first != null && it.second != null && it.third != null }.toList()
            if (rows.isEmpty()) return null
            return TimingMap(rows.map { it.first!! }.toIntArray(), rows.map { it.second!! }.toLongArray(), rows.map { it.third!! }.toLongArray())
        }

        /** Parse the mp3quran JSON array [{"ayah":1,"start_time":ms,"end_time":ms,…}] (minimal parser, no org.json needed). */
        fun parseMp3Quran(json: String): TimingMap? {
            val obj = Regex("""\{[^{}]*\}""").findAll(json)
            val a = ArrayList<Int>(); val st = ArrayList<Long>(); val en = ArrayList<Long>()
            for (m in obj) {
                val o = m.value
                val ay = Regex(""""ayah"\s*:\s*(\d+)""").find(o)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val s = Regex(""""start_time"\s*:\s*(\d+)""").find(o)?.groupValues?.get(1)?.toLongOrNull() ?: continue
                val e = Regex(""""end_time"\s*:\s*(\d+)""").find(o)?.groupValues?.get(1)?.toLongOrNull() ?: s
                a += ay; st += s; en += e
            }
            if (a.isEmpty()) return null
            // Keep ascending start order (the API already returns it; be defensive).
            val idx = st.indices.sortedBy { st[it] }
            return TimingMap(idx.map { a[it] }.toIntArray(), idx.map { st[it] }.toLongArray(), idx.map { en[it] }.toLongArray())
        }
    }
}

/** Sleep timer (AU08). */
sealed class SleepSpec {
    data class Minutes(val minutes: Int) : SleepSpec()
    object EndOfAyah : SleepSpec()
    object EndOfSurah : SleepSpec()

    fun encode(): String = when (this) { is Minutes -> "m$minutes"; EndOfAyah -> "ayah"; EndOfSurah -> "surah" }
    companion object {
        fun decode(s: String?): SleepSpec? = when {
            s == null -> null
            s == "ayah" -> EndOfAyah
            s == "surah" -> EndOfSurah
            s.startsWith("m") -> s.drop(1).toIntOrNull()?.takeIf { it > 0 }?.let { Minutes(it) }
            else -> null
        }
        const val FADE_MS = 8_000L
        /** Volume during the last [FADE_MS] of a timed sleep: 1 → 0 linearly; 1 before the fade starts. */
        fun volumeAt(remainingMs: Long): Float = if (remainingMs >= FADE_MS) 1f else (remainingMs.coerceAtLeast(0L).toFloat() / FADE_MS)
    }
}

/** What one download unit contains (AU10). Files are (url, path relative to the recitations root). */
data class DlFile(val url: String, val rel: String)

object DownloadPlan {
    fun ayahRel(edition: String, g: Int) = "ayah/$edition/${g + 1}.mp3"
    fun surahRel(moshaf: Int, sura: Int) = "surah/$moshaf/${sura.toString().padStart(3, '0')}.mp3"
    fun timingRel(moshaf: Int, sura: Int) = "timing/$moshaf/${sura.toString().padStart(3, '0')}.txt"

    /** Ayah files of one global range. */
    fun ayahFiles(voice: AyahVoice, range: IntRange, dataSaver: Boolean = false) = range.map { g -> DlFile(voice.url(g, dataSaver), ayahRel(voice.id, g)) }

    /** Surahs whose ayat overlap a juz (for whole-surah download of a juz). [suraStarts] 0-based global starts of 114 surahs. */
    fun surahsInRange(suraStarts: IntArray, total: Int, range: IntRange): List<Int> = (1..suraStarts.size).filter { s ->
        val st = suraStarts[s - 1]; val en = if (s < suraStarts.size) suraStarts[s] - 1 else total - 1
        st <= range.last && en >= range.first
    }

    /** Key that identifies a download unit: "<mode>:<voice|moshaf>:<surah|juz>:<n>". */
    fun key(mode: AudioMode, who: String, unit: String, n: Int) = "${mode.name.lowercase()}:$who:$unit:$n"

    fun formatBytes(b: Long, arabic: Boolean): String {
        val kb = 1024.0; val mb = kb * 1024; val gb = mb * 1024
        val (v, u) = when {
            b >= gb -> b / gb to (if (arabic) "ج.ب" else "GB")
            b >= mb -> b / mb to (if (arabic) "م.ب" else "MB")
            b >= kb -> b / kb to (if (arabic) "ك.ب" else "KB")
            else -> b.toDouble() to (if (arabic) "بايت" else "B")
        }
        val s = if (v >= 100 || u == "B" || u == "بايت") "%.0f".format(java.util.Locale.ENGLISH, v) else "%.1f".format(java.util.Locale.ENGLISH, v)
        return "$s $u"
    }
}

/** Download progress of one unit. */
sealed class DlState {
    object Queued : DlState()
    data class Running(val done: Int, val total: Int, val bytes: Long) : DlState() { val fraction get() = if (total == 0) 0f else done.toFloat() / total }
    data class Done(val files: Int, val bytes: Long) : DlState()
    data class Failed(val reason: String, val done: Int, val total: Int) : DlState()
}

/** Speeds offered in the player (AU07, pitch-preserving in ExoPlayer). */
val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
