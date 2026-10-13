package app.sakina.quran.quran

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * A khatma (QR28/QR29): read pages [startPage]..[endPage] between [startDate] and [endDate].
 * [progress] = last page completed (startPage-1 = nothing yet). [log] = pages completed per day.
 */
data class Khatma(
    val id: String,
    val name: String,
    val startPage: Int = 1,
    val endPage: Int = 604,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val progress: Int = startPage - 1,
    val log: Map<LocalDate, Int> = emptyMap(),
    /** Split the daily wird over the five prayers. */
    val afterPrayers: Boolean = false,
    /** 0 personal, 1 Ramadan, 2 family/group, 3 hifz revision. */
    val kind: Int = 0,
    val finishedOn: LocalDate? = null,
)

enum class Pace { AHEAD, ON_TRACK, BEHIND, DONE }

object KhatmaPlan {
    const val PAGES = 604
    /** Typical reading speed used for the "minutes per day" plan (QR29). */
    const val MIN_PER_PAGE = 2.0

    fun total(k: Khatma) = (k.endPage - k.startPage + 1).coerceAtLeast(1)
    fun done(k: Khatma) = (k.progress - k.startPage + 1).coerceIn(0, total(k))
    fun remaining(k: Khatma) = total(k) - done(k)
    fun percent(k: Khatma) = done(k) * 100 / total(k)
    fun isDone(k: Khatma) = done(k) >= total(k)

    /** Days left including today (min 1). */
    fun daysLeft(k: Khatma, today: LocalDate): Int = (ChronoUnit.DAYS.between(today, k.endDate) + 1).toInt().coerceAtLeast(1)
    fun totalDays(k: Khatma): Int = (ChronoUnit.DAYS.between(k.startDate, k.endDate) + 1).toInt().coerceAtLeast(1)

    fun doneToday(k: Khatma, today: LocalDate) = k.log[today] ?: 0

    /**
     * Today's wird size with automatic catch-up rebalancing: what is left at the start of today
     * spread evenly over the days left (including today).
     */
    fun todayTarget(k: Khatma, today: LocalDate): Int {
        if (isDone(k)) return 0
        val leftAtStart = remaining(k) + doneToday(k, today)
        return ceil(leftAtStart.toDouble() / daysLeft(k, today)).toInt().coerceIn(1, leftAtStart.coerceAtLeast(1))
    }

    /** Pages of today's wird (first..last), starting where the user was at the start of today. */
    fun todayRange(k: Khatma, today: LocalDate): IntRange {
        val first = (k.progress - doneToday(k, today) + 1).coerceIn(k.startPage, k.endPage)
        val last = (first + todayTarget(k, today) - 1).coerceIn(first, k.endPage)
        return first..last
    }

    fun todayLeft(k: Khatma, today: LocalDate) = (todayTarget(k, today) - doneToday(k, today)).coerceAtLeast(0)

    /** Pages that should be finished by the end of [day] on the original even plan. */
    fun expectedBy(k: Khatma, day: LocalDate): Int {
        val elapsed = (ChronoUnit.DAYS.between(k.startDate, day) + 1).toInt().coerceIn(0, totalDays(k))
        return (total(k).toLong() * elapsed / totalDays(k)).toInt()
    }

    fun pace(k: Khatma, today: LocalDate): Pace {
        if (isDone(k)) return Pace.DONE
        val before = expectedBy(k, today.minusDays(1))
        val d = done(k)
        return when {
            d >= expectedBy(k, today) + 1 -> Pace.AHEAD
            d >= before -> Pace.ON_TRACK
            else -> Pace.BEHIND
        }
    }

    /** Splits [pages] into five portions (Fajr..Isha), larger portions first, summing to [pages]. */
    fun afterPrayerSplit(pages: Int): List<Int> {
        if (pages <= 0) return List(5) { 0 }
        val base = pages / 5; val extra = pages % 5
        return List(5) { i -> base + if (i < extra) 1 else 0 }
    }

    /** End date for a given number of pages per day. */
    fun endForPace(start: LocalDate, startPage: Int, endPage: Int, pagesPerDay: Int): LocalDate {
        val days = ceil((endPage - startPage + 1).toDouble() / pagesPerDay.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        return start.plusDays((days - 1).toLong())
    }

    fun pagesPerDay(k: Khatma) = ceil(total(k).toDouble() / totalDays(k)).toInt()
    fun minutesPerDay(pages: Int) = (pages * MIN_PER_PAGE).roundToInt()

    /** Projected finish date at the user's actual average pace so far (null when nothing read yet). */
    fun projectedFinish(k: Khatma, today: LocalDate): LocalDate? {
        val d = done(k); if (d <= 0) return null
        if (isDone(k)) return k.finishedOn ?: today
        val elapsed = (ChronoUnit.DAYS.between(k.startDate, today) + 1).coerceAtLeast(1)
        val perDay = d.toDouble() / elapsed
        return today.plusDays(ceil(remaining(k) / perDay).toLong())
    }

    /** Records reading up to [page] (only forward moves count). */
    fun advanceTo(k: Khatma, page: Int, today: LocalDate): Khatma {
        val p = page.coerceIn(k.startPage - 1, k.endPage)
        if (p <= k.progress) return k
        val add = p - k.progress
        val nk = k.copy(progress = p, log = k.log + (today to ((k.log[today] ?: 0) + add)))
        return if (isDone(nk) && nk.finishedOn == null) nk.copy(finishedOn = today) else nk
    }

    /** Marks today's wird complete. */
    fun completeToday(k: Khatma, today: LocalDate): Khatma = advanceTo(k, todayRange(k, today).last, today)
}

/** Reading statistics and goals (QR30–QR32). Keys are days; values are mushaf pages read that day. */
object ReadingStats {
    /** Consecutive days (ending today, or yesterday if today is not met yet) that met [goal] pages or were excused. */
    fun streak(pages: Map<LocalDate, Set<Int>>, goal: Int, excused: Set<LocalDate>, today: LocalDate): Int {
        fun ok(d: LocalDate) = (pages[d]?.size ?: 0) >= goal.coerceAtLeast(1) || d in excused
        var d = if (ok(today)) today else today.minusDays(1)
        var n = 0
        while (n < 3650 && ok(d)) { n++; d = d.minusDays(1) }
        return n
    }

    fun pagesIn(pages: Map<LocalDate, Set<Int>>, from: LocalDate, to: LocalDate): Int =
        pages.filterKeys { !it.isBefore(from) && !it.isAfter(to) }.values.sumOf { it.size }

    /** Distinct mushaf pages ever opened. */
    fun distinctPages(pages: Map<LocalDate, Set<Int>>): Int = pages.values.flatten().toSet().size

    /** Hasanat estimate: 10 per letter (Tirmidhi 2910) over all pages read, by letters on each page. */
    fun hasanat(pages: Map<LocalDate, Set<Int>>, lettersOnPage: (Int) -> Int): Long =
        pages.values.sumOf { s -> s.sumOf { lettersOnPage(it).toLong() } } * 10

    /** Heat level 0..4 for a day's page count. */
    fun level(n: Int): Int = when { n <= 0 -> 0; n < 3 -> 1; n < 10 -> 2; n < 20 -> 3; else -> 4 }

    // ── Challenge templates (QR32)
    /** Al-Kahf is pages 293–304 in the Madinah mushaf. */
    val KAHF = 293..304
    /** Al-Mulk is pages 562–564. */
    val MULK = 562..564

    /** The last [weeks] Fridays, each with whether any page of al-Kahf was read that day. */
    fun kahfFridays(pages: Map<LocalDate, Set<Int>>, today: LocalDate, weeks: Int = 8): List<Pair<LocalDate, Boolean>> {
        var f = today; while (f.dayOfWeek != DayOfWeek.FRIDAY) f = f.minusDays(1)
        return (0 until weeks).map { w -> val d = f.minusWeeks(w.toLong()); d to (pages[d]?.any { it in KAHF } == true) }.reversed()
    }

    /** Days in the last [days] on which every page of al-Mulk was read. */
    fun mulkNights(pages: Map<LocalDate, Set<Int>>, today: LocalDate, days: Int = 7): Int =
        (0 until days).count { i -> pages[today.minusDays(i.toLong())]?.containsAll(MULK.toList()) == true }

    /** Days in the last [days] with at least a juz (20 pages). */
    fun juzDays(pages: Map<LocalDate, Set<Int>>, today: LocalDate, days: Int = 30): Int =
        (0 until days).count { i -> (pages[today.minusDays(i.toLong())]?.size ?: 0) >= 20 }
}
