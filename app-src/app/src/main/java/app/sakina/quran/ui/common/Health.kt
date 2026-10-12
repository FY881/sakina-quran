package app.sakina.quran.ui.common

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Something that can stop the adhan from ringing on time, with the settings screen that fixes it. */
data class HealthIssue(val key: String, val ar: String, val en: String, val fix: Intent)

fun healthIssues(ctx: Context): List<HealthIssue> {
    val out = mutableListOf<HealthIssue>()
    val pkg = ctx.packageName
    if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
        val i = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        out += HealthIssue("notif", "الإشعارات متوقفة، فلن يظهر تنبيه الأذان", "Notifications are off, so adhan alerts can't show", i)
    }
    if (Build.VERSION.SDK_INT >= 31) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (am != null && !am.canScheduleExactAlarms()) {
            out += HealthIssue(
                "exact", "التنبيهات الدقيقة غير مسموحة، قد يتأخر الأذان", "Exact alarms are not allowed, the adhan may be late",
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg")),
            )
        }
    }
    val pm = ctx.getSystemService(PowerManager::class.java)
    if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
        out += HealthIssue(
            "battery", "توفير البطارية قد يؤخر الأذان، فعّل التنبيهات المضمونة", "Battery saving may delay the adhan, enable reliable alerts",
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
    }
    return out
}

/** Health issues, re-checked every time the screen resumes (e.g. after returning from settings). */
@Composable
fun rememberHealth(): List<HealthIssue> {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    return remember(tick) { runCatching { healthIssues(ctx) }.getOrDefault(emptyList()) }
}

fun Context.openSafely(i: Intent) {
    runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .recoverCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
