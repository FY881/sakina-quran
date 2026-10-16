package app.sakina.quran.ui.audio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sakina.quran.audio.AudioFiles
import app.sakina.quran.audio.AudioMode
import app.sakina.quran.audio.AudioPrefs
import app.sakina.quran.audio.AyahVoices
import app.sakina.quran.audio.DlState
import app.sakina.quran.audio.DlUnit
import app.sakina.quran.audio.Downloads
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Reciters
import app.sakina.quran.ui.azkar.switchColors
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.quran.rememberQUser
import app.sakina.quran.ui.quran.rememberQuran
import app.sakina.quran.ui.theme.LocalSakina
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Human-readable size (B / KB / MB / GB), digits localised. */
fun fmtBytes(b: Long, ar: Boolean): String {
    val (v, ua, ue) = when {
        b >= 1L shl 30 -> Triple(b / (1L shl 30).toDouble(), "غ.ب", "GB")
        b >= 1L shl 20 -> Triple(b / (1L shl 20).toDouble(), "م.ب", "MB")
        b >= 1L shl 10 -> Triple(b / (1L shl 10).toDouble(), "ك.ب", "KB")
        else -> Triple(b.toDouble(), "بايت", "B")
    }
    val s = if (v >= 100 || b < 1024) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v)
    return digits(s, ar) + " " + if (ar) ua else ue
}

/** Display name for a usage key "ayah/<edition>" or "surah/<moshaf>". */
private fun usageName(ctx: android.content.Context, key: String, ar: Boolean): String {
    val kind = key.substringBefore('/'); val id = key.substringAfter('/')
    return if (kind == "ayah") {
        val v = AyahVoices.ALL.firstOrNull { it.id == id }
        (if (v == null) id else if (ar) v.ar else v.en) + if (ar) " • آية بآية" else " • ayah by ayah"
    } else {
        val mid = id.toIntOrNull()
        val hit = runCatching { Reciters.load(ctx) }.getOrNull()?.firstNotNullOfOrNull { r -> r.moshafs.firstOrNull { it.id == mid }?.let { r to it } }
        if (hit == null) id else (if (ar) hit.first.ar else hit.first.en.ifBlank { hit.first.ar }) + " • " + (if (ar) hit.second.ar else hit.second.en.ifBlank { hit.second.ar })
    }
}

/** AU10 offline downloads: storage per reciter, Wi-Fi-only / data saver, and per-surah / per-juz download with progress + cancel. */
@Composable
fun DownloadsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val ar = isArabic()
    val look = LocalSakina.current.look
    val q = rememberQuran()
    val qp = rememberQUser().prefs
    LaunchedEffect(Unit) { AudioPrefs.load(ctx) }
    val prefs by AudioPrefs.p.collectAsStateWithLifecycle()
    val states by remember { Downloads.states(ctx) }.collectAsStateWithLifecycle(initialValue = emptyMap())
    var mode by remember(prefs.mode) { mutableStateOf(prefs.mode) }
    var juzTab by remember { mutableStateOf(false) }
    var bump by remember { mutableIntStateOf(0) }
    var confirmDel by remember { mutableStateOf<String?>(null) }

    val who = if (mode == AudioMode.AYAH) qp.ayahVoice else qp.moshaf.toString()
    val whoName = remember(mode, who, ar) { usageName(ctx, (if (mode == AudioMode.AYAH) "ayah/" else "surah/") + who, ar) }
    val unitKind = if (juzTab) "juz" else "sura"
    val count = if (juzTab) 30 else 114

    // Storage per reciter folder (refreshed when a download finishes or a reciter is deleted).
    val doneCount = states.values.count { it is DlState.Done }
    val usage by produceState(emptyMap<String, Long>(), bump, doneCount) { value = withContext(Dispatchers.IO) { AudioFiles.usage(ctx) } }
    // (present, total) files per unit of the visible tab.
    val status by produceState(emptyMap<Int, Pair<Int, Int>>(), q, who, mode, juzTab, bump, doneCount) {
        val qq = q ?: return@produceState
        value = withContext(Dispatchers.IO) { (1..count).associateWith { Downloads.status(ctx, qq, DlUnit(mode, who, unitKind, it)) } }
    }

    confirmDel?.let { key ->
        AlertDialog(
            onDismissRequest = { confirmDel = null },
            title = { Text(t("حذف التلاوات المحمّلة؟", "Delete downloaded recitations?")) },
            text = { Text(usageName(ctx, key, ar)) },
            confirmButton = { TextButton(onClick = { AudioFiles.deleteReciter(ctx, key); confirmDel = null; bump++ }) { Text(t("حذف", "Delete"), color = look.warn) } },
            dismissButton = { TextButton(onClick = { confirmDel = null }) { Text(t("إلغاء", "Cancel")) } },
        )
    }

    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackHeader(t("التحميلات", "Downloads"), onBack, subtitle = t("للاستماع دون إنترنت", "Listen offline")) }
        // Storage
        item {
            SCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(14.dp)) {
                val total = usage.values.sum()
                Text(t("المساحة المستخدمة", "Storage used"), style = MaterialTheme.typography.titleMedium)
                Muted(fmtBytes(total, ar))
                if (usage.isEmpty()) {
                    VSpace(6.dp); Muted(t("لا توجد تلاوات محمّلة بعد.", "No recitations downloaded yet."))
                }
                usage.forEach { (key, bytes) ->
                    VSpace(8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(usageName(ctx, key, ar), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Muted(fmtBytes(bytes, ar))
                        }
                        IconButton(onClick = { confirmDel = key }) { Icon(Icons.Outlined.DeleteOutline, t("حذف", "Delete"), tint = look.onCard) }
                    }
                }
            }
        }
        // Network prefs
        item {
            SCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(14.dp)) {
                PrefSwitch(t("عبر Wi-Fi فقط", "Wi-Fi only"), t("لا تحمّل عبر بيانات الجوال", "Don't download over mobile data"), prefs.wifiOnly) { v -> AudioPrefs.update(ctx) { it.copy(wifiOnly = v) } }
                VSpace(6.dp)
                PrefSwitch(t("توفير البيانات", "Data saver"), t("جودة أقل وحجم أصغر للتلاوة آية بآية", "Lower bitrate, smaller files for ayah-by-ayah"), prefs.dataSaver) { v -> AudioPrefs.update(ctx) { it.copy(dataSaver = v) } }
            }
        }
        // Reciter + mode + tabs
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted(t("القارئ", "Reciter"))
                        Text(whoName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { onOpen("reciters") }) { Text(t("تغيير", "Change"), color = look.cta) }
                }
                VSpace(6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("آية بآية", "Ayah by ayah"), selected = mode == AudioMode.AYAH) { mode = AudioMode.AYAH }
                    Chip(t("السورة كاملة", "Whole surah"), selected = mode == AudioMode.SURAH) { mode = AudioMode.SURAH }
                }
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("السور (١١٤)", "Surahs (114)"), selected = !juzTab) { juzTab = false }
                    Chip(t("الأجزاء (٣٠)", "Juz (30)"), selected = juzTab) { juzTab = true }
                }
            }
        }
        if (q == null) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = look.cta) } }
        } else {
            items((1..count).toList(), key = { "$unitKind:$it" }) { n ->
                val u = DlUnit(mode, who, unitKind, n)
                UnitRow(q, u, states[u.key], status[n], ar,
                    onDownload = { Downloads.enqueue(ctx, u) },
                    onCancel = { Downloads.cancel(ctx, u); bump++ })
            }
        }
    }
}

@Composable
private fun PrefSwitch(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Muted(sub)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = value, onCheckedChange = onChange, colors = switchColors(), modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun UnitRow(q: Quran, u: DlUnit, st: DlState?, have: Pair<Int, Int>?, ar: Boolean, onDownload: () -> Unit, onCancel: () -> Unit) {
    val look = LocalSakina.current.look
    val title = if (u.unit == "juz") t("الجزء ${num(u.n, true)}", "Juz ${u.n}") else {
        val su = q.suras[u.n - 1]
        if (ar) "${num(u.n, true)}. سورة ${su.name}" else "${u.n}. ${su.tname}"
    }
    val (present, total) = have ?: (0 to 0)
    val complete = total > 0 && present >= total
    SCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    st is DlState.Running -> t("جارٍ التحميل ${num(st.done, true)} / ${num(st.total, true)}", "Downloading ${st.done} / ${st.total}")
                    st is DlState.Queued -> t("في الانتظار…", "Waiting…")
                    st is DlState.Failed -> t("تعذّر التحميل، ستُعاد المحاولة", "Download failed; will retry")
                    complete -> t("محمّل على الجهاز", "On device")
                    total == 0 -> t("غير متاح لهذا القارئ", "Not available for this reciter")
                    present > 0 -> t("${num(present, true)} من ${num(total, true)} ملف", "$present of $total files")
                    else -> t("${num(total, true)} ملف", "$total files")
                }
                Muted(sub)
            }
            Spacer(Modifier.width(8.dp))
            when {
                st is DlState.Running || st is DlState.Queued -> {
                    val frac = (st as? DlState.Running)?.fraction
                    Box(contentAlignment = Alignment.Center) {
                        if (frac == null || frac <= 0f) CircularProgressIndicator(Modifier.size(36.dp), color = look.cta, strokeWidth = 3.dp)
                        else CircularProgressIndicator(progress = { frac }, modifier = Modifier.size(36.dp), color = look.cta, strokeWidth = 3.dp)
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, t("إلغاء التحميل", "Cancel download"), tint = look.onCard, modifier = Modifier.size(18.dp)) }
                    }
                }
                complete -> Icon(Icons.Outlined.CheckCircle, t("محمّل", "Downloaded"), tint = look.cta, modifier = Modifier.padding(6.dp))
                total == 0 -> Spacer(Modifier.size(36.dp))
                else -> IconButton(onClick = onDownload) {
                    Icon(if (st is DlState.Failed) Icons.Outlined.ErrorOutline else Icons.Outlined.Download, t("تحميل", "Download"), tint = look.onCard)
                }
            }
        }
        if (st is DlState.Running && st.total > 0) {
            VSpace(6.dp)
            LinearProgressIndicator(progress = { st.fraction }, modifier = Modifier.fillMaxWidth(), color = look.cta, trackColor = look.onCardMuted.copy(alpha = .18f))
        }
    }
}
