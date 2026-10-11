package app.sakina.quran

import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PrayerEngineTest {
    private val cairo = PrayerEngine.CAIRO

    @Test fun orderIsMonotonic() {
        val d = PrayerEngine.day(cairo, LocalDate.of(2026, 10, 11))
        val list = d.times.values.toList()
        for (i in 1 until list.size) assertTrue(list[i].isAfter(list[i - 1]))
    }

    /** Reference: Egyptian Survey method, Cairo, 2026-10-11 (EEST): per api.aladhan.com method=5: Fajr 05:28, Dhuhr 12:42, Asr 16:01, Maghrib 18:29, Isha 19:46. */
    @Test fun cairoReasonable() {
        val d = PrayerEngine.day(cairo, LocalDate.of(2026, 10, 11))
        fun mins(p: Prayer) = d.times.getValue(p).let { it.hour * 60 + it.minute }
        assertNear(5 * 60 + 28, mins(Prayer.FAJR), 2)
        assertNear(12 * 60 + 42, mins(Prayer.DHUHR), 2)
        assertNear(16 * 60 + 1, mins(Prayer.ASR), 2)
        assertNear(18 * 60 + 29, mins(Prayer.MAGHRIB), 2)
        assertNear(19 * 60 + 46, mins(Prayer.ISHA), 2)
    }

    @Test fun nextAfterIshaIsTomorrowFajr() {
        val late = ZonedDateTime.of(2026, 10, 11, 23, 30, 0, 0, cairo.zone)
        val n = PrayerEngine.next(cairo, late)
        assertEquals(Prayer.FAJR, n.prayer)
        assertEquals(1, ChronoUnit.DAYS.between(late.toLocalDate(), n.at.toLocalDate()))
    }

    private fun assertNear(expected: Int, actual: Int, tol: Int) =
        assertTrue("expected ~$expected got $actual", kotlin.math.abs(expected - actual) <= tol)
}
