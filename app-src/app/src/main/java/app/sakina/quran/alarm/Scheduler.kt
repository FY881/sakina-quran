package app.sakina.quran.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.Prefs
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.widget.PrayerWidget
import java.time.DayOfWeek
import java.time.ZonedDateTime

/** One upcoming alert. kind: 0 = at prayer time, 1 = reminder before it. */
data class Event(val prayer: Prayer, val kind: Int, val at: ZonedDateTime, val prayerAt: ZonedDateTime)

/** An azkar reminder: kind = "morning" | "evening" | "sleep". */
data class AzkarEvent(val kind: String, val at: ZonedDateTime)

object Scheduler {
    const val ACTION = "app.sakina.quran.PRAYER_ALARM"
    const val ACTION_AZKAR = "app.sakina.quran.AZKAR_ALARM"

    /**
     * Azkar reminders in the next ~2 days, sorted. Morning = Fajr + offset (kept before sunrise),
     * evening = Asr + offset (kept before Maghrib), sleep = fixed local time. Pure function: unit-tested.
     */
    fun azkarUpcoming(p: Prefs, now: ZonedDateTime): List<AzkarEvent> {
        if (!p.azMorning && !p.azEvening && !p.azSleep) return emptyList()
        val local = now.withZoneSameInstant(p.place.zone)
        val out = ArrayList<AzkarEvent>()
        for (d in 0..2) {
            val date = local.toLocalDate().plusDays(d.toLong())
            val day = PrayerEngine.day(p.place, date, p.calc)
            if (p.azMorning) {
                val fajr = day.times.getValue(Prayer.FAJR); val sunrise = day.times.getValue(Prayer.SUNRISE)
                var t = fajr.plusMinutes(p.azMorningMin.toLong())
                if (!t.isBefore(sunrise.minusMinutes(5))) t = sunrise.minusMinutes(15).let { x -> if (x.isAfter(fajr)) x else fajr }
                out += AzkarEvent("morning", t)
            }
            if (p.azEvening) {
                val asr = day.times.getValue(Prayer.ASR); val maghrib = day.times.getValue(Prayer.MAGHRIB)
                var t = asr.plusMinutes(p.azEveningMin.toLong())
                if (!t.isBefore(maghrib.minusMinutes(5))) t = maghrib.minusMinutes(20).let { x -> if (x.isAfter(asr)) x else asr }
                out += AzkarEvent("evening", t)
            }
            if (p.azSleep) out += AzkarEvent("sleep", date.atStartOfDay(p.place.zone).plusMinutes(p.azSleepAt.toLong()))
        }
        return out.filter { it.at.isAfter(local) }.sortedBy { it.at }
    }

    /** Schedules the next azkar reminder (separate PendingIntent from prayer alerts). */
    fun rescheduleAzkar(c: Context, p: Prefs, now: ZonedDateTime = ZonedDateTime.now()) {
        val am = c.getSystemService(AlarmManager::class.java)
        val ev = azkarUpcoming(p, now).firstOrNull()
        val pi = PendingIntent.getBroadcast(
            c, 9, Intent(c, AlarmReceiver::class.java).setAction(ACTION_AZKAR).apply {
                if (ev != null) { putExtra("azkar", ev.kind); putExtra("fire", ev.at.toInstant().toEpochMilli()) }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        if (ev == null) return
        val t = ev.at.toInstant().toEpochMilli()
        if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60_000L, pi)
    }

    /** All alerts in the next ~2 days, sorted. Pure function: unit-tested. */
    fun upcoming(p: Prefs, now: ZonedDateTime): List<Event> {
        val local = now.withZoneSameInstant(p.place.zone)
        val out = ArrayList<Event>()
        for (d in 0..2) {
            val day = PrayerEngine.day(p.place, local.toLocalDate().plusDays(d.toLong()), p.calc)
            for ((prayer, at) in day.times) {
                val mode = p.modes[prayer] ?: AlertMode.OFF
                if (mode == AlertMode.OFF) continue
                if (at.isAfter(local)) out += Event(prayer, 0, at, at)
                val pre = if (prayer == Prayer.DHUHR && at.dayOfWeek == DayOfWeek.FRIDAY && p.jumuahReminder) maxOf(p.preMin, 45) else p.preMin
                if (pre > 0 && prayer != Prayer.SUNRISE) {
                    val r = at.minusMinutes(pre.toLong())
                    if (r.isAfter(local)) out += Event(prayer, 1, r, at)
                }
            }
        }
        return out.sortedBy { it.at }
    }

    fun canExact(c: Context): Boolean {
        val am = c.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /** Schedules only the next alert; each firing schedules the following one. */
    fun reschedule(c: Context, p: Prefs, now: ZonedDateTime = ZonedDateTime.now()) {
        runCatching { rescheduleAzkar(c, p, now) }
        val am = c.getSystemService(AlarmManager::class.java)
        val ev = upcoming(p, now).firstOrNull()
        val pi = PendingIntent.getBroadcast(
            c, 7, Intent(c, AlarmReceiver::class.java).setAction(ACTION).apply {
                if (ev != null) {
                    putExtra("prayer", ev.prayer.name); putExtra("kind", ev.kind)
                    putExtra("at", ev.prayerAt.toInstant().toEpochMilli()); putExtra("fire", ev.at.toInstant().toEpochMilli())
                }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        PrayerWidget.updateAll(c)
        if (ev == null) return
        val t = ev.at.toInstant().toEpochMilli()
        if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, t, 60_000L, pi)
    }
}
