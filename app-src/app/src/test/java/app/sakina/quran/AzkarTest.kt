package app.sakina.quran

import app.sakina.quran.alarm.Scheduler
import app.sakina.quran.azkar.AzCat
import app.sakina.quran.azkar.Azkar
import app.sakina.quran.azkar.PROPHETIC_CHAPTERS
import app.sakina.quran.azkar.RUQYAH_CHAPTERS
import app.sakina.quran.data.Prefs
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.dhikr.NAMES_99
import app.sakina.quran.ui.dhikr.makeNamesQuiz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime

class AzkarTest {
    private fun asset(n: String) = File("src/main/assets/azkar/$n").readText()
    private val az by lazy { Azkar.parse(asset("hisn.json"), asset("morning_evening.json")) }

    @Test fun hisnComplete() {
        assertEquals(133, az.chapters.size)
        assertEquals(302, az.chapters.sumOf { it.items.size })
        assertEquals(34, az.morningEvening.size)
        assertTrue(az.chapters.all { c -> c.items.isNotEmpty() && c.items.all { it.text.isNotBlank() && it.count >= 1 } })
    }

    @Test fun everyChapterHasExactlyOneCategory() {
        for (c in az.chapters) assertEquals("chapter ${c.index} ${c.title}", 1, AzCat.entries.count { c.index in it.chapters })
    }

    @Test fun sessionsResolve() {
        // Morning = type 0/1, evening = type 0/2 in the Seen-Arabic set.
        assertTrue(az.morning.size in 20..30)
        assertTrue(az.evening.size in 20..30)
        for (sid in listOf("morning", "evening", "sleep", "waking", "after_prayer", "mosque", "travel")) {
            val s = az.session(sid); assertNotNull(sid, s); assertTrue(sid, s!!.items.isNotEmpty())
        }
        assertTrue(az.session("h25")!!.items.first().text.contains("أَسْتَخِيرُكَ"))
        (PROPHETIC_CHAPTERS + RUQYAH_CHAPTERS).forEach { assertNotNull(az.chapter(it)) }
        // Sleep azkar include the 33/33/34 tasbih (count 33 somewhere).
        assertTrue(az.session("sleep")!!.items.any { it.count == 33 })
    }

    @Test fun searchIsDiacriticInsensitive() {
        val r = az.search("السفر")
        assertTrue(r.isNotEmpty())
        assertTrue(az.search("ا").isEmpty())
    }

    @Test fun audioOnlyFromCleared() {
        az.morningEvening.mapNotNull { it.audio }.forEach { assertTrue(it, it.startsWith("https://") && "mp3quran.net" in it) }
    }

    @Test fun quizIsWellFormed() {
        repeat(20) { s ->
            val q = makeNamesQuiz(s.toLong(), 10, 2)
            assertEquals(10, q.size)
            assertEquals(10, q.map { it.answer }.toSet().size)
            q.forEach { assertEquals(4, it.options.toSet().size); assertTrue(it.answer in it.options); assertTrue(it.options.all { o -> o in NAMES_99.indices }) }
        }
    }

    @Test fun azkarRemindersFollowPrayerTimes() {
        val p = Prefs(azMorning = true, azEvening = true, azSleep = true)
        val now = ZonedDateTime.of(2026, 10, 12, 0, 30, 0, 0, PrayerEngine.CAIRO.zone)
        val ev = Scheduler.azkarUpcoming(p, now)
        val day = PrayerEngine.day(PrayerEngine.CAIRO, LocalDate.of(2026, 10, 12))
        val m = ev.first { it.kind == "morning" }.at
        assertEquals(day.times.getValue(Prayer.FAJR).plusMinutes(20), m)
        assertTrue(m.isBefore(day.times.getValue(Prayer.SUNRISE)))
        val e = ev.first { it.kind == "evening" }.at
        assertEquals(day.times.getValue(Prayer.ASR).plusMinutes(30), e)
        assertTrue(e.isBefore(day.times.getValue(Prayer.MAGHRIB)))
        assertEquals(22, ev.first { it.kind == "sleep" }.at.hour)
        assertTrue(ev.zipWithNext().all { (a, b) -> !b.at.isBefore(a.at) })
        assertTrue(Scheduler.azkarUpcoming(Prefs(), now).isEmpty())
    }
}
