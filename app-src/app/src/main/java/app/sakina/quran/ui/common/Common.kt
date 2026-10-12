package app.sakina.quran.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sakina.quran.R
import app.sakina.quran.data.AlertMode
import app.sakina.quran.prayer.Prayer
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun isArabic(): Boolean = LocalConfiguration.current.locales[0].language == "ar"

/** Inline bilingual text for labels that live only in code. */
@Composable
fun t(ar: String, en: String): String = if (isArabic()) ar else en

fun locale(arabic: Boolean): Locale = if (arabic) Locale.forLanguageTag("ar-EG") else Locale.ENGLISH

fun formatTime(t: ZonedDateTime, arabic: Boolean): String =
    digits(t.format(DateTimeFormatter.ofPattern("h:mm a", locale(arabic))), arabic)

/** Western → Arabic-Indic digits (٠-٩) when [arabic]. */
fun digits(s: String, arabic: Boolean): String =
    if (!arabic) s else buildString(s.length) { s.forEach { c -> append(if (c in '0'..'9') '\u0660' + (c - '0') else c) } }

fun num(n: Int, arabic: Boolean): String = digits(n.toString(), arabic)

/** Short countdown: "1h 23m" / "43m 18s" (Arabic: "١س ٢٣د"). */
fun countdown(d: Duration, arabic: Boolean): String {
    val s = d.seconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    val txt = if (arabic) { if (h > 0) "${h}س ${m}د" else "${m}د ${sec}ث" } else { if (h > 0) "${h}h ${m}m" else "${m}m ${sec}s" }
    return digits(txt, arabic)
}

@Composable
fun ComingSoon(@StringRes title: Int) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.coming_soon_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun prayerName(p: Prayer, friday: Boolean = false): String = stringResource(
    when (p) {
        Prayer.FAJR -> R.string.fajr
        Prayer.SUNRISE -> R.string.sunrise
        Prayer.DHUHR -> if (friday) R.string.jumuah else R.string.dhuhr
        Prayer.ASR -> R.string.asr
        Prayer.MAGHRIB -> R.string.maghrib
        Prayer.ISHA -> R.string.isha
    }
)

fun modeIcon(m: AlertMode): ImageVector = when (m) {
    AlertMode.OFF -> Icons.Outlined.NotificationsOff
    AlertMode.SILENT -> Icons.Outlined.Notifications
    AlertMode.NOTIFY -> Icons.Outlined.NotificationsActive
    AlertMode.ADHAN -> Icons.AutoMirrored.Outlined.VolumeUp
}

@Composable
fun modeLabel(m: AlertMode): String = when (m) {
    AlertMode.OFF -> t("بدون تنبيه", "Off")
    AlertMode.SILENT -> t("إشعار صامت", "Silent")
    AlertMode.NOTIFY -> t("إشعار بصوت", "Sound")
    AlertMode.ADHAN -> t("أذان كامل", "Full adhan")
}

fun AlertMode.nextMode(): AlertMode = AlertMode.entries[(ordinal + 1) % AlertMode.entries.size]

/** Recomposes every [periodMs] with the current time. */
@Composable
fun rememberNow(periodMs: Long = 1_000): ZonedDateTime {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(periodMs) { while (true) { delay(periodMs - System.currentTimeMillis() % periodMs); now = ZonedDateTime.now() } }
    return now
}

fun hms(d: Duration, arabic: Boolean): String {
    val s = d.seconds.coerceAtLeast(0)
    return digits("%d:%02d:%02d".format(Locale.US, s / 3600, (s % 3600) / 60, s % 60), arabic)
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
