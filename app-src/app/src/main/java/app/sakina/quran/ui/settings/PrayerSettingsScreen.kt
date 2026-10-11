package app.sakina.quran.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.sakina.quran.alarm.AdhanService
import app.sakina.quran.alarm.Scheduler
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.VOICES
import app.sakina.quran.prayer.AsrMadhab
import app.sakina.quran.prayer.HighLat
import app.sakina.quran.prayer.Method
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.ui.common.SectionCard
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.modeLabel
import app.sakina.quran.ui.common.prayerName
import app.sakina.quran.ui.common.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerSettingsScreen(prefs: Prefs, repo: SettingsRepo, onCity: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    var methodDialog by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val exactOk = Scheduler.canExact(ctx)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!exactOk) item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        t("لكي يُرفع الأذان في وقته بالضبط، اسمح لسكينة بضبط المنبهات الدقيقة.", "To play the adhan exactly on time, allow Sakina to set exact alarms."),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
                    )
                }
                FilledTonalButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text(t("السماح الآن", "Allow now")) }
            }
        }
        item {
            SectionCard(t("الموقع", "Location")) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onCity), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(prefs.place.label(ar), style = MaterialTheme.typography.bodyLarge)
                        Text("%.4f, %.4f • %s".format(prefs.place.lat, prefs.place.lng, prefs.place.zone.id), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onCity) { Text(t("تغيير", "Change")) }
                }
            }
        }
        item {
            SectionCard(t("طريقة الحساب", "Calculation method")) {
                Row(Modifier.fillMaxWidth().clickable { methodDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ar) prefs.calc.method.ar else prefs.calc.method.en, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { methodDialog = true }) { Text(t("اختيار", "Choose")) }
                }
                Text(t("مذهب العصر", "Asr juristic method"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsrMadhab.entries.forEach { m ->
                        FilterChip(prefs.calc.madhab == m, { scope.launch { repo.setMadhab(m) } }, { Text(if (ar) m.ar else m.en) })
                    }
                }
                Text(t("خطوط العرض العليا", "High latitude rule"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HighLat.entries.forEach { h ->
                        FilterChip(prefs.calc.highLat == h, { scope.launch { repo.setHighLat(h) } }, { Text(if (ar) h.ar else h.en) })
                    }
                }
            }
        }
        item {
            SectionCard(t("تنبيه كل صلاة", "Alert for each prayer")) {
                Prayer.entries.forEach { p ->
                    Text(prayerName(p), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlertMode.entries.forEach { m ->
                            FilterChip(prefs.modes[p] == m, { scope.launch { repo.setMode(p, m) } }, { Text(modeLabel(m)) })
                        }
                    }
                }
            }
        }
        item {
            SectionCard(t("صوت الأذان", "Adhan voice")) {
                VoicePicker(t("لكل الصلوات", "All prayers"), prefs.voice, playing, { scope.launch { repo.setVoice(it) } }) { id ->
                    if (playing) { AdhanService.stop(ctx); playing = false }
                    else { ContextCompat.startForegroundService(ctx, AdhanService.intent(ctx, Prayer.DHUHR, id, false)); playing = true }
                }
                VoicePicker(t("لصلاة الفجر", "For Fajr"), prefs.fajrVoice, playing, { scope.launch { repo.setFajrVoice(it) } }) { id ->
                    if (playing) { AdhanService.stop(ctx); playing = false }
                    else { ContextCompat.startForegroundService(ctx, AdhanService.intent(ctx, Prayer.FAJR, id, false)); playing = true }
                }
                Text(
                    t("يحترم سكينة الوضع الصامت والاهتزاز: يصلك إشعار صامت بدل الأذان.", "Sakina respects silent/vibrate mode: you get a silent alert instead of the adhan."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SectionCard(t("التذكير قبل الصلاة", "Reminder before prayer")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 5, 10, 15, 20, 30).forEach { m ->
                        FilterChip(prefs.preMin == m, { scope.launch { repo.setPreMin(m) } }, { Text(if (m == 0) t("بدون", "Off") else t("$m دقيقة", "$m min")) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("تذكير الجمعة قبلها بـ٤٥ دقيقة (سورة الكهف والتبكير)", "Jumu'ah reminder 45 min early (Al-Kahf, go early)"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(prefs.jumuahReminder, { scope.launch { repo.setJumuah(it) } })
                }
            }
        }
        item {
            SectionCard(t("ضبط يدوي بالدقائق", "Manual adjustments (minutes)")) {
                Prayer.entries.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prayerName(p), Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { repo.setAdj(p, prefs.calc.adj[p.ordinal] - 1) } }) { Icon(Icons.Outlined.Remove, t("إنقاص", "Decrease")) }
                        Text("%+d".format(prefs.calc.adj[p.ordinal]), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { scope.launch { repo.setAdj(p, prefs.calc.adj[p.ordinal] + 1) } }) { Icon(Icons.Outlined.Add, t("زيادة", "Increase")) }
                    }
                }
            }
        }
        item {
            SectionCard(t("تصحيح التاريخ الهجري", "Hijri date correction")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("بالأيام", "Days"), Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { repo.setHijriAdj(prefs.hijriAdj - 1) } }) { Icon(Icons.Outlined.Remove, t("إنقاص", "Decrease")) }
                    Text("%+d".format(prefs.hijriAdj), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { scope.launch { repo.setHijriAdj(prefs.hijriAdj + 1) } }) { Icon(Icons.Outlined.Add, t("زيادة", "Increase")) }
                }
            }
        }
        item {
            SectionCard(t("لو الأذان بيتأخر أو مش بيشتغل", "If the adhan is late or silent")) {
                Text(
                    t("بعض الهواتف (شاومي، أوبو، سامسونج…) توقف التطبيقات لتوفير البطارية. استثنِ سكينة من تحسين البطارية.",
                        "Some phones (Xiaomi, Oppo, Samsung…) stop apps to save battery. Exclude Sakina from battery optimization."),
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalButton(onClick = {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text(t("فتح إعدادات البطارية", "Open battery settings")) }
            }
        }
    }

    if (methodDialog) AlertDialog(
        onDismissRequest = { methodDialog = false },
        confirmButton = { TextButton(onClick = { methodDialog = false }) { Text(t("تم", "Done")) } },
        title = { Text(t("طريقة الحساب", "Calculation method")) },
        text = {
            Column {
                Method.entries.forEach { m ->
                    Row(Modifier.fillMaxWidth().clickable { scope.launch { repo.setMethod(m) }; methodDialog = false }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(prefs.calc.method == m, { scope.launch { repo.setMethod(m) }; methodDialog = false })
                        Text(if (ar) m.ar else m.en)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoicePicker(title: String, selected: Int, playing: Boolean, onSelect: (Int) -> Unit, onPreview: (Int) -> Unit) {
    val ar = isArabic()
    Text(title, style = MaterialTheme.typography.labelLarge)
    Column {
        VOICES.forEach { v ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(v.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == v.id, { onSelect(v.id) })
                Text(if (ar) v.ar else v.en, Modifier.weight(1f))
                IconButton(onClick = { onPreview(v.id) }) {
                    Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, t("تجربة الصوت", "Preview"), Modifier.size(22.dp))
                }
            }
        }
    }
}
