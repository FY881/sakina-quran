package app.sakina.quran.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import app.sakina.quran.R
import app.sakina.quran.alarm.Notifs
import app.sakina.quran.alarm.Texts
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = updateAll(c)

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun updateAll(c: Context) {
            val m = AppWidgetManager.getInstance(c) ?: return
            val ids = m.getAppWidgetIds(ComponentName(c, PrayerWidget::class.java))
            if (ids.isEmpty()) return
            scope.launch {
                val p = SettingsRepo(c).now()
                val now = ZonedDateTime.now()
                val ar = Texts.ar(c)
                val loc = if (ar) Locale.forLanguageTag("ar-EG") else Locale.ENGLISH
                val fmt = DateTimeFormatter.ofPattern("h:mm", loc)
                val next = PrayerEngine.next(p.place, now, p.calc)
                val friday = next.at.dayOfWeek == DayOfWeek.FRIDAY && next.prayer == Prayer.DHUHR
                val day = PrayerEngine.day(p.place, next.at.toLocalDate(), p.calc)
                val times = day.times.filterKeys { it != Prayer.SUNRISE }.entries.joinToString("  •  ") { (k, v) -> "${Texts.name(c, k)} ${v.format(fmt)}" }
                val rv = RemoteViews(c.packageName, R.layout.widget_prayer).apply {
                    setTextViewText(R.id.w_title, if (ar) "الصلاة القادمة" else "Next prayer")
                    setTextViewText(R.id.w_place, p.place.label(ar))
                    setTextViewText(R.id.w_next, "${Texts.name(c, next.prayer, friday)}  ${next.at.format(DateTimeFormatter.ofPattern("h:mm a", loc))}")
                    setTextViewText(R.id.w_times, times)
                    val left = next.at.toInstant().toEpochMilli() - System.currentTimeMillis()
                    setChronometer(R.id.w_count, SystemClock.elapsedRealtime() + left, null, true)
                    setChronometerCountDown(R.id.w_count, true)
                    setOnClickPendingIntent(R.id.w_root, Notifs.openApp(c))
                }
                m.updateAppWidget(ids, rv)
            }
        }
    }
}
