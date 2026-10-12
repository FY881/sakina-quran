package app.sakina.quran.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.ContextCompat
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.prayer.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch { try { block() } finally { pending.finish() } }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = async {
        val repo = SettingsRepo(c)
        val prefs = repo.now()
        if (intent.action == Scheduler.ACTION_AZKAR) {
            val kind = intent.getStringExtra("azkar")
            val fire = intent.getLongExtra("fire", 0L)
            if (kind != null && kotlin.math.abs(System.currentTimeMillis() - fire) < 20 * 60_000L) {
                Notifs.post(c, Notifs.ID_AZKAR, Notifs.CH_AZKAR, Texts.azkarTitle(c, kind), Texts.azkarText(c, kind), open = "azkar/$kind")
            }
            Scheduler.rescheduleAzkar(c, prefs)
            return@async
        }
        val prayer = runCatching { Prayer.valueOf(intent.getStringExtra("prayer")!!) }.getOrNull()
        val at = intent.getLongExtra("at", 0L)
        val kind = intent.getIntExtra("kind", 0)
        // Ignore stale alarms (e.g. delivered >10 min late after the device was off).
        val fire = intent.getLongExtra("fire", at)
        val fresh = prayer != null && kotlin.math.abs(System.currentTimeMillis() - fire) < 10 * 60_000L
        if (prayer != null && fresh) {
            val friday = Instant.ofEpochMilli(at).atZone(prefs.place.zone).dayOfWeek == DayOfWeek.FRIDAY && prayer == Prayer.DHUHR
            if (kind == 1) {
                val min = ((at - System.currentTimeMillis()) / 60_000L + 1).toInt().coerceAtLeast(1)
                Notifs.post(c, Notifs.ID_REMIND, Notifs.CH_REMIND, Texts.before(c, prayer, min, friday), prefs.place.label(Texts.ar(c)))
            } else {
                when (prefs.modes[prayer] ?: AlertMode.OFF) {
                    AlertMode.ADHAN -> {
                        val am = c.getSystemService(AudioManager::class.java)
                        val ringerSilent = am.ringerMode != AudioManager.RINGER_MODE_NORMAL
                        if (ringerSilent) {
                            Notifs.post(c, Notifs.ID_PRAYER, Notifs.CH_SILENT, Texts.timeFor(c, prayer, friday), Texts.hint(c, prayer))
                        } else {
                            val voice = if (prayer == Prayer.FAJR) prefs.fajrVoice else prefs.voice
                            val ok = runCatching {
                                ContextCompat.startForegroundService(c, AdhanService.intent(c, prayer, voice, friday))
                            }.isSuccess
                            if (!ok) Notifs.post(c, Notifs.ID_PRAYER, Notifs.CH_FALLBACK, Texts.timeFor(c, prayer, friday), Texts.hint(c, prayer))
                        }
                    }
                    AlertMode.NOTIFY -> Notifs.post(c, Notifs.ID_PRAYER, Notifs.CH_NOTIFY, Texts.timeFor(c, prayer, friday), Texts.hint(c, prayer))
                    AlertMode.SILENT -> Notifs.post(c, Notifs.ID_PRAYER, Notifs.CH_SILENT, Texts.timeFor(c, prayer, friday), Texts.hint(c, prayer))
                    AlertMode.OFF -> {}
                }
            }
        }
        Scheduler.reschedule(c, prefs)
    }
}

/** Boot, time/timezone change, app update, exact-alarm permission granted. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = async {
        Notifs.ensure(c)
        Scheduler.reschedule(c, SettingsRepo(c).now())
    }
}
