package app.sakina.quran

import app.sakina.quran.audio.AudioMode
import app.sakina.quran.audio.AyahVoice
import app.sakina.quran.audio.AyahVoices
import app.sakina.quran.audio.DlState
import app.sakina.quran.audio.DownloadPlan
import app.sakina.quran.audio.MediaRef
import app.sakina.quran.audio.QueuePlan
import app.sakina.quran.audio.RepeatSpec
import app.sakina.quran.audio.SleepSpec
import app.sakina.quran.audio.TimingMap
import app.sakina.quran.audio.moshafRiwaya
import app.sakina.quran.audio.moshafStyle
import app.sakina.quran.quran.Quran
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioTest {
    private fun asset(n: String) = File("src/main/assets/$n")
    private val q: Quran by lazy { Quran.fromData(asset("quran/quran-data.xml").readText(), asset("quran/quran-uthmani.txt").readBytes()) }

    // ── QueuePlan (AU06 / HZ loops)
    @Test fun plainRangeIsSequential() {
        assertEquals(listOf(10, 11, 12), QueuePlan.build(10, 12, RepeatSpec()))
        assertTrue(RepeatSpec().isPlain)
        assertFalse(RepeatSpec(ayahTimes = 2).isPlain)
    }

    @Test fun repeatEachAyah() {
        assertEquals(listOf(5, 5, 5, 6, 6, 6), QueuePlan.build(5, 6, RepeatSpec(ayahTimes = 3)))
    }

    @Test fun repeatWholeRange() {
        assertEquals(listOf(1, 2, 1, 2, 1, 2), QueuePlan.build(1, 2, RepeatSpec(rangeTimes = 3)))
        assertEquals(listOf(0, 0, 1, 1, 0, 0, 1, 1), QueuePlan.build(0, 1, RepeatSpec(ayahTimes = 2, rangeTimes = 2)))
    }

    @Test fun infiniteRangeIsOnePass() {
        val spec = RepeatSpec(rangeTimes = 0)
        assertTrue(spec.infinite)
        assertEquals(listOf(7, 8, 9), QueuePlan.build(7, 9, spec))
    }

    @Test fun ladderLoop() {
        // k=0: 0 ; k=1: 1, then 0..1 ; k=2: 2, then 0..2
        assertEquals(listOf(0, 1, 0, 1, 2, 0, 1, 2), QueuePlan.build(0, 2, RepeatSpec(ladder = true)))
        assertEquals(listOf(3, 3, 4, 4, 3, 4), QueuePlan.build(3, 4, RepeatSpec(ayahTimes = 2, ladder = true)))
    }

    @Test fun singleAyahAndClamping() {
        assertEquals(listOf(42), QueuePlan.build(42, 42, RepeatSpec(ayahTimes = 0)))
        assertEquals(100, QueuePlan.build(0, 0, RepeatSpec(ayahTimes = 500)).size)
    }

    @Test fun queueIsCapped() {
        val whole = QueuePlan.build(0, 6235, RepeatSpec(ayahTimes = 10, rangeTimes = 10))
        assertTrue(whole.size <= QueuePlan.MAX_ITEMS + 10)
        assertTrue(whole.isNotEmpty())
        assertEquals(0, whole.first())
        val passes = QueuePlan.build(0, 999, RepeatSpec(rangeTimes = 100))
        assertTrue(passes.size <= QueuePlan.MAX_ITEMS)
        assertEquals(6000, passes.size) // 6 full passes of 1000 fit, the 7th would overflow
    }

    @Test(expected = IllegalArgumentException::class) fun emptyRangeRejected() {
        QueuePlan.build(5, 4, RepeatSpec())
    }

    @Test fun surahRangeCoerces() {
        assertEquals(100..106, QueuePlan.surahRange(100, 7))
        assertEquals(102..104, QueuePlan.surahRange(100, 7, 3, 5))
        assertEquals(100..106, QueuePlan.surahRange(100, 7, 0, 99))
        assertEquals(105..105, QueuePlan.surahRange(100, 7, 6, 2)) // to < from → single ayah
    }

    // ── TimingMap (whole-surah highlight)
    private val tm = TimingMap(intArrayOf(0, 1, 2, 3), longArrayOf(0, 4000, 9000, 15000), longArrayOf(4000, 9000, 15000, 20000))

    @Test fun ayahAtBoundaries() {
        assertEquals(0, tm.ayahAt(0)); assertEquals(0, tm.ayahAt(3999))
        assertEquals(1, tm.ayahAt(4000)); assertEquals(1, tm.ayahAt(8999))
        assertEquals(2, tm.ayahAt(9000)); assertEquals(3, tm.ayahAt(15000))
        assertEquals(3, tm.ayahAt(99_999)) // after the end → last ayah
        assertEquals(0, tm.ayahAt(-5))
        assertEquals(0, TimingMap(IntArray(0), LongArray(0), LongArray(0)).ayahAt(100))
        val noIntro = TimingMap(intArrayOf(1, 2), longArrayOf(2000, 5000), longArrayOf(5000, 8000))
        assertEquals(0, noIntro.ayahAt(1000)) // before first ayah → intro
    }

    @Test fun startEndLookup() {
        assertEquals(9000L, tm.startOf(2)); assertEquals(15000L, tm.endOf(2))
        assertNull(tm.startOf(9)); assertNull(tm.endOf(9))
    }

    @Test fun compactRoundTrip() {
        val back = TimingMap.parseCompact(tm.serialize())
        assertNotNull(back)
        assertEquals(tm.ayahs.toList(), back!!.ayahs.toList())
        assertEquals(tm.starts.toList(), back.starts.toList())
        assertEquals(tm.ends.toList(), back.ends.toList())
        assertNull(TimingMap.parseCompact(""))
        assertEquals(1, TimingMap.parseCompact("junk\n1,2,3\nx,y,z\n4,5")!!.size)
    }

    @Test fun parseMp3QuranJson() {
        val json = """[{"ayah":0,"polygon":null,"start_time":0,"end_time":5120,"x":"","y":"","page":"x"},
            {"ayah":2,"start_time":12000,"end_time":18000},
            {"ayah":1, "start_time" : 5120, "end_time":12000},
            {"ayah":3,"start_time":18000}]"""
        val m = TimingMap.parseMp3Quran(json)!!
        assertEquals(listOf(0, 1, 2, 3), m.ayahs.toList()) // re-sorted by start
        assertEquals(5120L, m.startOf(1)); assertEquals(18000L, m.endOf(3)) // missing end → start
        assertEquals(2, m.ayahAt(13000))
        assertNull(TimingMap.parseMp3Quran("[]"))
        assertNull(TimingMap.parseMp3Quran("""[{"foo":1}]"""))
    }

    // ── Media ids
    @Test fun mediaRefRoundTrip() {
        val refs = listOf(
            MediaRef.Ayah("ar.alafasy", 261),
            MediaRef.Surah(52, 18, "https://server8.mp3quran.net/afs/"),
            MediaRef.Test(0),
        )
        for (r in refs) assertEquals(r, MediaRef.decode(r.encode()))
        // server url containing '|' survives because of limit = 4
        val odd = MediaRef.Surah(1, 2, "https://x/a|b")
        assertEquals(odd, MediaRef.decode(odd.encode()))
        assertNull(MediaRef.decode(null)); assertNull(MediaRef.decode(""))
        assertNull(MediaRef.decode("z|1")); assertNull(MediaRef.decode("a|ed|nope")); assertNull(MediaRef.decode("s|1"))
    }

    @Test fun surahUrlPadsAndTrims() {
        assertEquals("https://server8.mp3quran.net/afs/001.mp3", MediaRef.surahUrl("https://server8.mp3quran.net/afs/", 1))
        assertEquals("https://h/x/114.mp3", MediaRef.surahUrl("https://h/x", 114))
    }

    // ── Voices
    @Test fun voicesAndUrls() {
        val ids = AyahVoices.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val a = AyahVoices.byId("ar.alafasy")
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3", a.url(261))
        assertEquals("https://cdn.islamic.network/quran/audio/64/ar.alafasy/1.mp3", a.url(0, dataSaver = true))
        assertEquals(AyahVoices.ALL.first(), AyahVoices.byId("unknown"))
        assertTrue(AyahVoices.ALL.all { it.low <= it.bitrate })
    }

    @Test fun moshafNameParsing() {
        assertEquals(AyahVoice.Style.MURATTAL, moshafStyle("حفص عن عاصم - مرتل", "Rewayat Hafs A'n Assem - Murattal"))
        assertEquals(AyahVoice.Style.MUJAWWAD, moshafStyle("المصحف المجود", "Almusshaf Al Mojawwad"))
        assertEquals(AyahVoice.Style.MUALLIM, moshafStyle("المصحف المعلم", "Almusshaf Al Mo'lim"))
        assertEquals("حفص عن عاصم", moshafRiwaya("حفص عن عاصم - مرتل"))
        assertEquals("ورش عن نافع", moshafRiwaya(" ورش عن نافع "))
    }

    // ── Sleep timer
    @Test fun sleepSpecRoundTripAndFade() {
        for (s in listOf(SleepSpec.Minutes(15), SleepSpec.EndOfAyah, SleepSpec.EndOfSurah)) assertEquals(s, SleepSpec.decode(s.encode()))
        assertNull(SleepSpec.decode(null)); assertNull(SleepSpec.decode("m0")); assertNull(SleepSpec.decode("mx")); assertNull(SleepSpec.decode("x"))
        assertEquals(1f, SleepSpec.volumeAt(60_000), 0f)
        assertEquals(1f, SleepSpec.volumeAt(SleepSpec.FADE_MS), 0f)
        assertEquals(0.5f, SleepSpec.volumeAt(SleepSpec.FADE_MS / 2), 1e-6f)
        assertEquals(0f, SleepSpec.volumeAt(-100), 0f)
    }

    // ── Download plans (AU10)
    @Test fun relPathsAndKeys() {
        assertEquals("ayah/ar.husary/1.mp3", DownloadPlan.ayahRel("ar.husary", 0))
        assertEquals("surah/52/009.mp3", DownloadPlan.surahRel(52, 9))
        assertEquals("timing/52/114.txt", DownloadPlan.timingRel(52, 114))
        assertEquals("ayah:ar.alafasy:juz:30", DownloadPlan.key(AudioMode.AYAH, "ar.alafasy", "juz", 30))
        assertEquals("surah:52:surah:2", DownloadPlan.key(AudioMode.SURAH, "52", "surah", 2))
    }

    @Test fun surahAyahFileList() {
        val v = AyahVoices.byId("ar.alafasy")
        val fatiha = q.suras[0]
        val files = DownloadPlan.ayahFiles(v, QueuePlan.surahRange(fatiha.start, fatiha.ayas))
        assertEquals(7, files.size)
        assertEquals("ayah/ar.alafasy/1.mp3", files.first().rel)
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/7.mp3", files.last().url)
        val baqara = q.suras[1]
        assertEquals(286, DownloadPlan.ayahFiles(v, QueuePlan.surahRange(baqara.start, baqara.ayas), dataSaver = true).size)
    }

    @Test fun juzBoundariesAndSurahs() {
        val total = q.suras.sumOf { it.ayas }
        assertEquals(6236, total)
        assertEquals(78 to 1, q.juzStart(30))
        assertEquals(1 to 1, q.juzStart(1))
        assertEquals(2 to 142, q.juzStart(2))
        val starts = q.suras.map { it.start }.toIntArray()
        val juz30 = q.global(78, 1)..(total - 1)
        assertEquals((78..114).toList(), DownloadPlan.surahsInRange(starts, total, juz30))
        val juz1 = 0 until q.global(2, 142)
        assertEquals(listOf(1, 2), DownloadPlan.surahsInRange(starts, total, juz1))
        assertEquals(564, DownloadPlan.ayahFiles(AyahVoices.ALL.first(), juz30).size)
        assertEquals(listOf(114), DownloadPlan.surahsInRange(starts, total, (total - 1)..(total - 1)))
    }

    @Test fun formatBytes() {
        assertEquals("512 B", DownloadPlan.formatBytes(512, false))
        assertEquals("1.5 KB", DownloadPlan.formatBytes(1536, false))
        assertEquals("150 MB", DownloadPlan.formatBytes(150L * 1024 * 1024, false))
        assertEquals("2.0 ج.ب", DownloadPlan.formatBytes(2L * 1024 * 1024 * 1024, true))
        assertEquals("0 بايت", DownloadPlan.formatBytes(0, true))
    }

    @Test fun dlStateFraction() {
        assertEquals(0.25f, DlState.Running(1, 4, 100).fraction, 1e-6f)
        assertEquals(0f, DlState.Running(0, 0, 0).fraction, 0f)
    }
}
