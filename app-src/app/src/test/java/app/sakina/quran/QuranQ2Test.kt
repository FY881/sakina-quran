package app.sakina.quran

import app.sakina.quran.alarm.WirdAlarm
import app.sakina.quran.azkar.arabicIndic
import app.sakina.quran.data.QuranStore
import app.sakina.quran.data.ReaderPrefs
import app.sakina.quran.quran.Editions
import app.sakina.quran.quran.Khatma
import app.sakina.quran.quran.KhatmaPlan
import app.sakina.quran.quran.MushafLayout
import app.sakina.quran.quran.Pace
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.ReadingStats
import app.sakina.quran.quran.Reciters
import app.sakina.quran.quran.Seg
import app.sakina.quran.quran.SuraInfo
import app.sakina.quran.quran.TajRule
import app.sakina.quran.quran.Tajweed
import app.sakina.quran.quran.Topics
import app.sakina.quran.quran.parseTranslit
import app.sakina.quran.ui.quran.searchEdition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.zip.GZIPInputStream

class QuranQ2Test {
    private fun asset(n: String) = File("src/main/assets/$n")
    private fun gz(n: String) = GZIPInputStream(asset(n).inputStream()).use { it.readBytes() }
    private val q: Quran by lazy { Quran.fromData(asset("quran/quran-data.xml").readText(), asset("quran/quran-uthmani.txt").readBytes()) }

    // ── Text integrity (QR01 / QR13 / TF01-06)
    @Test fun uthmaniIsVerbatimTanzil() {
        assertEquals(Quran.UTHMANI_MD5, Quran.md5(asset("quran/quran-uthmani.txt").readBytes()))
        assertTrue(q.verified)
        assertEquals(6236, q.text.size)
        assertTrue(q.text.all { it.isNotBlank() })
    }

    @Test fun simpleScriptIsVerbatimTanzil() {
        val b = gz("quran/quran-simple.txt.gz")
        assertEquals(Quran.SIMPLE_MD5, Quran.md5(b))
        val t = Quran.parseTanzil(String(b, Charsets.UTF_8), q.suras)
        assertTrue(t.all { it.isNotBlank() })
        assertEquals("قُلْ هُوَ اللَّهُ أَحَدٌ", MushafLayout.ayahText(q, 112, 1, t))
        assertTrue(MushafLayout.ayahText(q, 97, 1, t).startsWith("إِنَّا"))
        assertTrue(MushafLayout.ayahText(q, 9, 1, t).startsWith("بَرَاءَةٌ"))
    }

    @Test fun bundledEditionsAreVerbatimAndComplete() {
        val md5 = mapOf(
            "ar.muyassar" to "cd50f90a18aee027083280cd576f83d0", "ar.jalalayn" to "10fa9c0aa2b88ca00df5e5851bec4804",
            "en.sahih" to "f04b8e73a9e32c15803b3721ea47896f", "en.transliteration" to "1af10c137a1a3cfd2b81ef7a3cb2eb0e",
        )
        for ((id, sum) in md5) {
            val b = gz("tafsir/$id.txt.gz")
            assertEquals(id, sum, Quran.md5(b))
            val arr = Quran.parseTanzil(String(b, Charsets.UTF_8), q.suras)
            assertEquals(id, 6236, arr.count { it.isNotBlank() })
            assertTrue(Editions.get(id)!!.bundled)
        }
        assertEquals(Editions.ALL.size, Editions.ALL.map { it.id }.toSet().size)
        assertTrue(Editions.ALL.filter { !it.bundled }.all { it.url.startsWith("https://") })
    }

    @Test fun alQuranCloudConversion() {
        val json = """{"data":{"surahs":[{"number":1,"ayahs":[{"numberInSurah":1,"text":"a\nb"},{"numberInSurah":2,"text":"c"}]}]}}"""
        val out = Editions.fromAlQuranCloud(json, Editions.get("ar.qurtubi")!!)
        val arr = Quran.parseTanzil(out, q.suras)
        assertEquals("a b", arr[0]); assertEquals("c", arr[1])
        assertTrue(out.contains("alquran.cloud"))
    }

    @Test fun translitTags() {
        val r = parseTranslit("Bismi All<u>a</U>hi a<b>l</B>rra")
        assertEquals("Bismi Allahi alrra", r.joinToString("") { it.text })
        assertTrue(r.any { it.long && it.text == "a" }); assertTrue(r.any { it.bold && it.text == "l" })
    }

    // ── Mushaf layout (QR02 / QR04)
    @Test fun mushafPagesCoverEveryAyahOnce() {
        assertEquals(604, q.pageCount)
        var next = 0; var headers = 0
        for (p in 1..604) {
            assertEquals("page $p start", next, q.pageStart(p))
            assertTrue(q.pageEnd(p) >= q.pageStart(p))
            next = q.pageEnd(p) + 1
            val segs = MushafLayout.segments(q, p)
            headers += segs.count { it is Seg.Header }
            assertEquals(q.pageEnd(p) - q.pageStart(p) + 1, segs.count { it is Seg.Ayah })
        }
        assertEquals(6236, next)
        assertEquals(114, headers)
        assertEquals(240, q.quarters.size); assertEquals(15, q.sajdas.size)
        assertEquals(1, q.page(1, 1)); assertEquals(2, q.page(2, 1)); assertEquals(604, q.page(114, 6))
        assertEquals(42, q.page(2, 255)); assertEquals(22, q.juzPage(2)); assertEquals(582, q.juzPage(30))
    }

    @Test fun challengePagesMatchMushaf() {
        assertEquals(ReadingStats.KAHF.first, q.page(18, 1)); assertEquals(ReadingStats.KAHF.last, q.page(18, 110))
        assertEquals(ReadingStats.MULK.first, q.page(67, 1)); assertEquals(ReadingStats.MULK.last, q.page(67, 30))
    }

    @Test fun basmalaShownAsBlockNotDuplicated() {
        val segs = MushafLayout.segments(q, q.page(2, 1))
        assertTrue(segs.any { it is Seg.Basmala })
        assertFalse(q.displayAyah(2, 1).contains("بِسْمِ"))
        assertTrue(MushafLayout.segments(q, q.page(9, 1)).none { it is Seg.Basmala && it.sura == 9 })
    }

    // ── Tajweed (HZ12)
    @Test fun tajweedKnownRules() {
        fun rules(s: Int, a: Int) = q.displayAyah(s, a).let { t -> Tajweed.spans(t).map { it.rule to t.substring(it.start, it.end) } }
        val k = rules(2, 255)
        assertTrue(k.any { it.first == TajRule.IKHFA && it.second.contains("ن") })          // مَن ذَا
        assertTrue(k.any { it.first == TajRule.IDGHAM_GHUNNAH })                             // سِنَةٌ وَلَا
        assertTrue(k.any { it.first == TajRule.IDGHAM_NO_GHUNNAH })                          // نَوْمٌ ۚ لَّهُۥ
        assertTrue(k.any { it.first == TajRule.MADD_OBLIGATORY && it.second.contains("آ") }) // شَآءَ
        assertTrue(k.any { it.first == TajRule.MADD_PERMISSIBLE })                           // لَآ إِلَٰهَ
        assertTrue(k.any { it.first == TajRule.SILENT })
        assertTrue(rules(112, 1).any { it.first == TajRule.QALQALAH && it.second.startsWith("د") })
        assertTrue(rules(1, 7).any { it.first == TajRule.MADD_NECESSARY })                   // ٱلضَّآلِّينَ
        assertTrue(rules(4, 1).any { it.first == TajRule.GHUNNAH })                          // ٱلنَّاسُ
        assertTrue(rules(4, 1).any { it.first == TajRule.IDGHAM_SHAFAWI })                   // خَلَقَكُم مِّن
        assertTrue(rules(2, 27).any { it.first == TajRule.IQLAB })
    }

    @Test fun tajweedSpansAreSaneForWholeQuran() {
        var total = 0
        for (g in 0 until 6236) {
            val (s, a) = q.fromGlobal(g)
            val t = q.displayAyah(s, a)
            var last = 0
            for (sp in Tajweed.spans(t)) {
                assertTrue(sp.start >= last && sp.end > sp.start && sp.end <= t.length)
                last = sp.end; total++
            }
        }
        assertTrue(total > 50_000)
        for (r in TajRule.entries) for (p in 0..2) for (d in listOf(true, false)) assertTrue((Tajweed.color(r, p, d) ushr 24) == 0xFFL)
    }

    // ── Khatma (QR28/QR29) & stats (QR30-32)
    private val d0 = LocalDate.of(2026, 10, 12)
    private fun k30() = Khatma("k", "test", 1, 604, d0, d0.plusDays(29))

    @Test fun khatmaPlanAndRebalance() {
        val k = k30()
        assertEquals(30, KhatmaPlan.totalDays(k))
        assertEquals(21, KhatmaPlan.todayTarget(k, d0))
        assertEquals(1..21, KhatmaPlan.todayRange(k, d0))
        // Nothing read for two days → the rest is spread over 28 days.
        assertEquals(22, KhatmaPlan.todayTarget(k, d0.plusDays(2)))
        assertEquals(Pace.BEHIND, KhatmaPlan.pace(k, d0.plusDays(2)))
        val k2 = KhatmaPlan.completeToday(k, d0)
        assertEquals(21, k2.progress); assertEquals(0, KhatmaPlan.todayLeft(k2, d0)); assertEquals(21, k2.log[d0])
        // Today's range is stable after finishing it.
        assertEquals(1..21, KhatmaPlan.todayRange(k2, d0))
        val k3 = KhatmaPlan.advanceTo(k2, 10, d0)
        assertEquals(k2, k3) // backwards never counts
        var kk = k
        for (i in 0 until 30) kk = KhatmaPlan.completeToday(kk, d0.plusDays(i.toLong()))
        assertTrue(KhatmaPlan.isDone(kk)); assertEquals(Pace.DONE, KhatmaPlan.pace(kk, d0.plusDays(29))); assertNotNull(kk.finishedOn)
    }

    @Test fun khatmaHelpers() {
        assertEquals(listOf(5, 4, 4, 4, 4), KhatmaPlan.afterPrayerSplit(21))
        assertEquals(21, KhatmaPlan.afterPrayerSplit(21).sum())
        assertEquals(listOf(1, 1, 0, 0, 0), KhatmaPlan.afterPrayerSplit(2))
        assertEquals(d0.plusDays(29), KhatmaPlan.endForPace(d0, 1, 604, 21))
        assertEquals(d0.plusDays(150), KhatmaPlan.endForPace(d0, 1, 604, 4))
        val k = Khatma("k", "x", 300, 604, d0, d0.plusDays(9))
        assertEquals(305, KhatmaPlan.total(k)); assertEquals(31, KhatmaPlan.todayTarget(k, d0)); assertEquals(300..330, KhatmaPlan.todayRange(k, d0))
    }

    @Test fun khatmaJsonRoundTrip() {
        val k = KhatmaPlan.completeToday(k30().copy(afterPrayers = true, kind = 1), d0)
        assertEquals(k, QuranStore.decodeKhatma(QuranStore.encodeKhatma(k)))
        val p = ReaderPrefs(fontSize = 30, translations = listOf("en.sahih", "fr.hamidullah"), tajweed = true, palette = 2, legend = listOf("a", "", "c", "", ""))
        assertEquals(p, QuranStore.decodePrefs(QuranStore.encodePrefs(p)))
        assertEquals(ReaderPrefs(), QuranStore.decodePrefs(QuranStore.encodePrefs(ReaderPrefs())))
    }

    @Test fun readingStats() {
        val pages = mapOf(d0 to setOf(1, 2, 3, 4), d0.minusDays(1) to setOf(5, 6, 7, 8), d0.minusDays(3) to setOf(9))
        assertEquals(2, ReadingStats.streak(pages, 4, emptySet(), d0))
        assertEquals(4, ReadingStats.streak(pages, 1, setOf(d0.minusDays(2)), d0))
        assertEquals(9, ReadingStats.distinctPages(pages))
        assertEquals(90L, ReadingStats.hasanat(pages) { 1 })
        assertTrue(q.pageLetters(1) > 100)
        val fri = LocalDate.of(2026, 10, 9)
        assertTrue(ReadingStats.kahfFridays(mapOf(fri to setOf(293)), d0).last().second)
    }

    @Test fun wirdAlarmNextTime() {
        val z = ZoneId.of("Africa/Cairo")
        val now = ZonedDateTime.of(2026, 10, 12, 20, 0, 0, 0, z)
        assertEquals(ZonedDateTime.of(2026, 10, 12, 21, 0, 0, 0, z), WirdAlarm.next(now, 21 * 60))
        assertEquals(ZonedDateTime.of(2026, 10, 13, 5, 30, 0, 0, z), WirdAlarm.next(now, 5 * 60 + 30))
    }

    // ── Content (QR20 / QR21 / QR22 / reciters)
    @Test fun translationSearch() {
        val sahih = Quran.parseTanzil(String(gz("tafsir/en.sahih.txt.gz"), Charsets.UTF_8), q.suras)
        val (n, hits) = searchEdition(q, sahih, "merciful")
        assertTrue(n > 100); assertEquals(1, hits.first().sura)
        val muy = Quran.parseTanzil(String(gz("tafsir/ar.muyassar.txt.gz"), Charsets.UTF_8), q.suras)
        assertTrue(searchEdition(q, muy, "الصبر").first > 10)
    }

    @Test fun topicsAreValid() {
        val t = Topics.parse(asset("quran/topics.json").readText())
        assertEquals(24, t.size)
        for (tp in t) for ((s, a) in tp.refs) assertTrue("${tp.id} $s:$a", s in 1..114 && a in 1..q.suras[s - 1].ayas)
    }

    @Test fun surahInfoComplete() {
        assertEquals(114, SuraInfo.count)
        assertTrue((1..114).all { SuraInfo.themeAr(it).isNotBlank() && SuraInfo.themeEn(it).isNotBlank() })
        assertTrue(SuraInfo.VIRTUES.values.flatten().all { it.refAr.isNotBlank() && it.refEn.isNotBlank() })
        assertTrue(q.suras.all { it.order in 1..114 })
    }

    @Test fun recitersCatalogue() {
        val r = Reciters.parse(asset("quran/reciters.json").readText())
        assertEquals(243, r.size)
        val m = r.first { it.id == ReaderPrefs().reciter }.moshafs.first { it.id == ReaderPrefs().moshaf }
        assertTrue(m.has(18)); assertEquals("https://cdn.mp3quran.net/audio/mishary-alafasy/r1/018.mp3", m.url(18))
        assertTrue(r.flatMap { it.moshafs }.all { it.server.startsWith("https://") })
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3", Reciters.ayahUrl("ar.alafasy", q.global(2, 255)))
    }

    @Test fun arabicDigitsInReferences() {
        assertEquals("البخاري مع الفتح ١١/ ١١٣", arabicIndic("البخاري مع الفتح 11/ 113"))
    }
}
