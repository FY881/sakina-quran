package app.sakina.quran.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.Prefs
import app.sakina.quran.prayer.DayTimes
import app.sakina.quran.prayer.Hijri
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.SectionCard
import app.sakina.quran.ui.common.formatTime
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.modeIcon
import app.sakina.quran.ui.common.modeLabel
import app.sakina.quran.ui.common.prayerName
import app.sakina.quran.ui.common.rememberNow
import app.sakina.quran.ui.common.t
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TimesCard(
    day: DayTimes, prefs: Prefs, highlight: Prayer?, arabic: Boolean, title: String,
    onCycleMode: ((Prayer) -> Unit)? = null,
) {
    val friday = day.date.dayOfWeek == DayOfWeek.FRIDAY
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            day.times.forEach { (p, time) ->
                val on = p == highlight
                val mode = prefs.modes[p] ?: AlertMode.OFF
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        prayerName(p, friday), style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (p == Prayer.SUNRISE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(time, arabic), style = MaterialTheme.typography.bodyLarge, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                    if (onCycleMode != null) {
                        val label = modeLabel(mode)
                        IconButton(
                            onClick = { onCycleMode(p) },
                            modifier = Modifier.semantics { stateDescription = label; role = Role.Button },
                        ) {
                            Icon(
                                modeIcon(mode), contentDescription = label,
                                tint = if (mode == AlertMode.OFF) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else Spacer(Modifier.width(14.dp))
                }
            }
        }
    }
}

@Composable
fun PrayerScreen(prefs: Prefs, onCycleMode: (Prayer) -> Unit, onSettings: () -> Unit, onCity: () -> Unit, fixedNow: ZonedDateTime? = null) {
    val now = fixedNow ?: rememberNow(60_000)
    val ar = isArabic()
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val place = prefs.place
    val today = now.withZoneSameInstant(place.zone).toLocalDate()
    val date = today.plusDays(offset.toLong())
    val day = PrayerEngine.day(place, date, prefs.calc)
    val next = PrayerEngine.next(place, now, prefs.calc)
    val dateStr = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale(ar)))
    val hijri = Hijri.format(date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant().toEpochMilli(), ar, prefs.hijriAdj)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(place.label(ar), style = MaterialTheme.typography.titleLarge)
                        Text(if (ar) prefs.calc.method.ar else prefs.calc.method.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (ar) prefs.calc.madhab.ar else prefs.calc.madhab.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = onCity) { Text(t("تغيير", "Change")) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, t("اليوم السابق", "Previous day")) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (offset == 0) t("اليوم", "Today") else dateStr, style = MaterialTheme.typography.titleMedium)
                    Text(hijri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { offset++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, t("اليوم التالي", "Next day")) }
            }
        }
        item {
            TimesCard(day, prefs, if (offset == 0) next.prayer else null, ar, title = t("مواقيت الصلاة", "Prayer times"), onCycleMode = onCycleMode)
        }
        item {
            SectionCard(t("أوقات الليل والكراهة", "Night & disliked times")) {
                InfoRow(t("منتصف الليل الشرعي", "Islamic midnight"), formatTime(day.midnight, ar))
                InfoRow(t("بداية الثلث الأخير", "Last third begins"), formatTime(day.lastThird, ar))
                InfoRow(t("الإمساك (قبل الفجر بـ١٠ د)", "Imsak (10 min before Fajr)"), formatTime(day.times.getValue(Prayer.FAJR).minusMinutes(10), ar))
                InfoRow(t("الكراهة بعد الشروق حتى", "Disliked after sunrise until"), formatTime(day.times.getValue(Prayer.SUNRISE).plusMinutes(15), ar))
                InfoRow(t("الكراهة قبل الظهر من", "Disliked before Dhuhr from"), formatTime(day.times.getValue(Prayer.DHUHR).minusMinutes(5), ar))
                InfoRow(t("الكراهة قبل المغرب من", "Disliked before Maghrib from"), formatTime(day.times.getValue(Prayer.MAGHRIB).minusMinutes(15), ar))
            }
        }
        item {
            FilledTonalButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(t("إعدادات الأذان والحساب", "Adhan & calculation settings"))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
