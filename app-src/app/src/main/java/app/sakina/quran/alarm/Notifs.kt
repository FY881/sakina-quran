package app.sakina.quran.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sakina.quran.MainActivity
import app.sakina.quran.R

object Notifs {
    const val CH_ADHAN = "adhan_player"      // ongoing notification while adhan plays (silent channel; audio from service)
    const val CH_NOTIFY = "prayer_notify"    // default sound
    const val CH_SILENT = "prayer_silent"
    const val CH_REMIND = "prayer_remind"
    const val CH_FALLBACK = "adhan_fallback" // takbeer sound when the player service may not start
    const val ID_ADHAN = 4100
    const val ID_PRAYER = 4200
    const val ID_REMIND = 4300

    fun ensure(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        val ar = c.resources.configuration.locales[0].language == "ar"
        fun t(a: String, e: String) = if (ar) a else e
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        nm.createNotificationChannels(listOf(
            NotificationChannel(CH_ADHAN, t("الأذان", "Adhan"), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null); description = t("يظهر أثناء تشغيل الأذان", "Shown while the adhan plays")
            },
            NotificationChannel(CH_NOTIFY, t("تنبيه دخول الوقت", "Prayer time alert"), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CH_SILENT, t("تنبيه صامت", "Silent prayer alert"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null); enableVibration(false)
            },
            NotificationChannel(CH_REMIND, t("تذكير قبل الصلاة", "Reminder before prayer"), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_FALLBACK, t("تكبير احتياطي", "Takbeer fallback"), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(Uri.parse("android.resource://${c.packageName}/${R.raw.takbeer}"), attrs)
            },
        ))
    }

    fun openApp(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 1, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun post(c: Context, id: Int, channel: String, title: String, text: String) {
        ensure(c)
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_stat_sakina)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(c).notify(id, n) }
    }
}
