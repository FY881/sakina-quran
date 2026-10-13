package app.sakina.quran.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.sakina.quran.data.QuranStore
import app.sakina.quran.quran.KhatmaPlan
import java.time.LocalDate
import java.time.ZonedDateTime

/** Daily khatma wird reminder (QR29): one local alarm at the user's chosen time, skipped when today's wird is done. */
object WirdAlarm {
    const val ACTION = "app.sakina.quran.WIRD_ALARM"

    /** Next fire time strictly after [now] for a reminder at [minutes] after midnight. Pure: unit-tested. */
    fun next(now: ZonedDateTime, minutes: Int): ZonedDateTime {
        val today = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minutes.toLong())
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun pi(c: Context, fire: Long = 0L) = PendingIntent.getBroadcast(
        c, 11, Intent(c, AlarmReceiver::class.java).setAction(ACTION).putExtra("fire", fire),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    suspend fun reschedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val q = QuranStore(c).now()
        am.cancel(pi(c))
        if (!q.prefs.wirdReminder || q.active == null) return
        val t = next(ZonedDateTime.now(), q.prefs.wirdAt).toInstant().toEpochMilli()
        val p = pi(c, t)
        if (Scheduler.canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
        else am.setWindow(AlarmManager.RTC_WAKEUP, t, 10 * 60_000L, p)
    }

    suspend fun fire(c: Context, intent: Intent) {
        val fire = intent.getLongExtra("fire", 0L)
        val q = QuranStore(c).now()
        val k = q.active
        if (k != null && kotlin.math.abs(System.currentTimeMillis() - fire) < 30 * 60_000L && !KhatmaPlan.isDone(k)) {
            val today = LocalDate.now()
            val left = KhatmaPlan.todayLeft(k, today)
            if (left > 0) {
                val r = KhatmaPlan.todayRange(k, today)
                val ar = Texts.ar(c)
                fun d(n: Int) = if (ar) n.toString().map { ch -> if (ch in '0'..'9') '\u0660' + (ch - '0') else ch }.joinToString("") else n.toString()
                Notifs.post(
                    c, Notifs.ID_WIRD, Notifs.CH_QURAN,
                    if (ar) "وردك اليوم: ${k.name}" else "Today's wird: ${k.name}",
                    if (ar) "بقي ${d(left)} صفحة — من الصفحة ${d(r.first)} إلى ${d(r.last)}" else "$left pages left — page ${r.first} to ${r.last}",
                    open = "khatma",
                )
            }
        }
        reschedule(c)
    }
}
