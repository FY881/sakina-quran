package app.sakina.quran.ui.dhikr

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.media.AudioManager
import app.sakina.quran.VolumeKeys
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.data.UserData
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.tabs.normalize
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TasbihPreset(val id: String, val text: String, val en: String, val target: Int, val virtueAr: String, val virtueEn: String)

val TASBIH_PRESETS = listOf(
    TasbihPreset("subhan", "سُبْحَانَ اللَّهِ", "SubhanAllah", 33, "دبر كل صلاة ٣٣ مرة", "33 times after every prayer"),
    TasbihPreset("hamd", "الْحَمْدُ لِلَّهِ", "Alhamdulillah", 33, "تملأ الميزان", "Fills the scale"),
    TasbihPreset("akbar", "اللَّهُ أَكْبَرُ", "Allahu Akbar", 34, "تمام المئة بعد الصلاة", "Completes the hundred after prayer"),
    TasbihPreset("tahlil", "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illa Allah", 100, "أفضل الذكر", "The best remembrance"),
    TasbihPreset("istighfar", "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah", 100, "كان النبي ﷺ يستغفر في اليوم أكثر من سبعين مرة", "The Prophet ﷺ sought forgiveness more than seventy times a day"),
    TasbihPreset("salawat", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "Salawat on the Prophet ﷺ", 100, "من صلّى عليّ واحدة صلّى الله عليه بها عشرًا", "Whoever sends one blessing, Allah sends ten upon him"),
    TasbihPreset("bihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "SubhanAllahi wa bihamdihi", 100, "حُطّت خطاياه وإن كانت مثل زبد البحر", "Sins wiped away even if like the foam of the sea"),
    TasbihPreset("hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "La hawla wa la quwwata illa billah", 100, "كنز من كنوز الجنة", "A treasure of Paradise"),
)

private val TARGETS = listOf(33, 99, 100, 1000, 0)

/** Tasbih Fatimah: 33 × SubhanAllah, 33 × Alhamdulillah, 34 × Allahu Akbar with auto-advance (TS02). */
private val FATIMAH = TasbihPreset("fatimah", "تسبيح فاطمة ٣٣/٣٣/٣٤", "Tasbih Fatimah 33/33/34", 100, "التسبيح والتحميد والتكبير بعد الصلاة وقبل النوم", "SubhanAllah, Alhamdulillah, Allahu Akbar — after prayer and before sleep")

/** Stage of Tasbih Fatimah for a running count: (preset, done in stage, stage size). */
fun fatimahStage(count: Int): Triple<TasbihPreset, Int, Int> {
    val c = count % 100
    return when {
        c < 33 -> Triple(TASBIH_PRESETS[0], c, 33)
        c < 66 -> Triple(TASBIH_PRESETS[1], c - 33, 33)
        else -> Triple(TASBIH_PRESETS[2], c - 66, 34)
    }
}

private val SKINS = listOf("حلقة" to "Ring", "خرز" to "Beads", "بسيط" to "Minimal")

@Composable
fun TasbihScreen(
    user: UserData, onBack: () -> Unit, onTap: () -> Unit, onUndo: () -> Unit, onSave: () -> Unit,
    onPreset: (String, Int) -> Unit, onTarget: (Int) -> Unit, onClearHistory: () -> Unit,
    onSkin: (Int) -> Unit = {}, onSound: (Boolean) -> Unit = {}, onVolumeKeys: (Boolean) -> Unit = {},
    onAddCustom: (String, Int) -> Unit = { _, _ -> }, onDeleteCustom: (String) -> Unit = {},
) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val view = LocalView.current
    val ctx = LocalContext.current
    val custom = user.tasbihCustom.map { TasbihPreset(it.id, it.text, it.text, it.count, "ذكرك الخاص", "Your own dhikr") }
    val all = TASBIH_PRESETS + FATIMAH + custom
    val preset = all.firstOrNull { it.id == user.tasbihPreset } ?: TASBIH_PRESETS[0]
    val fatimah = preset.id == "fatimah"
    // Local count for instant feedback; DataStore is the source of truth.
    var count by remember { mutableIntStateOf(user.tasbihCount) }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(user.tasbihCount) { if (!touched) count = user.tasbihCount }
    val target = if (fatimah) 100 else user.tasbihTarget
    val round = if (target > 0) count % target else count
    val rounds = if (target > 0) count / target else 0
    val stage = if (fatimah) fatimahStage(count) else null
    val shownText = stage?.first?.text ?: preset.text
    val progress by animateFloatAsState(if (target > 0) (if (round == 0 && count > 0) 1f else round / target.toFloat()) else 0f, tween(250), label = "p")
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var addText by rememberSaveable { mutableStateOf("") }
    var addTarget by rememberSaveable { mutableIntStateOf(33) }

    fun tap() {
        touched = true
        count++
        onTap()
        val stageEnd = fatimah && (count % 100 == 33 || count % 100 == 66)
        val hit = (target > 0 && count % target == 0) || stageEnd
        view.performHapticFeedback(if (hit) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
        if (user.tasbihSound) runCatching { ctx.getSystemService(AudioManager::class.java).playSoundEffect(AudioManager.FX_KEY_CLICK, .6f) }
        scope.launch { pulse.snapTo(.96f); pulse.animateTo(1f, tween(140)) }
    }
    val tapNow by rememberUpdatedState(::tap)
    // Volume keys count while this screen is open (TS01); screen stays on.
    DisposableEffect(user.tasbihVolumeKeys) {
        VolumeKeys.handler = if (user.tasbihVolumeKeys) ({ tapNow() }) else null
        view.keepScreenOn = true
        onDispose { VolumeKeys.handler = null; view.keepScreenOn = false }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            BackHeader(t("السبحة", "Tasbih"), onBack, subtitle = t("اليوم: ${num(user.tasbihOn(today) + count, true)} • الإجمالي: ${num(user.tasbihTotal, true)}", "Today: ${user.tasbihOn(today) + count} • Lifetime: ${user.tasbihTotal}")) {
                IconButton(onClick = { onUndo(); count = (count - 1).coerceAtLeast(0) }) { Icon(Icons.AutoMirrored.Outlined.Undo, t("تراجع", "Undo"), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { onSave(); count = 0 }) { Icon(Icons.Outlined.RestartAlt, t("حفظ وتصفير", "Save & reset"), tint = MaterialTheme.colorScheme.onBackground) }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(all) { p -> Chip(if (ar) p.text else p.en, selected = p.id == preset.id, onClick = { if (p.id != preset.id) { onSave(); count = 0; onPreset(p.id, p.target) } }) }
            }
        }
        // The big counter
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(shownText, fontFamily = QuranFont, fontSize = 30.sp, lineHeight = 54.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Muted(if (stage != null) digits(t("المرحلة: ${stage.second}/${stage.third} — ثم ينتقل تلقائيًا", "Stage: ${stage.second}/${stage.third} — advances automatically"), ar) else t(preset.virtueAr, preset.virtueEn))
                VSpace(14.dp)
                Box(
                    Modifier.size(270.dp).scale(pulse.value).clip(CircleShape).surface(CircleShape, strong = true)
                        .clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple()) { tap() },
                    contentAlignment = Alignment.Center,
                ) {
                    val track = look.onCardMuted.copy(alpha = .18f); val arc = look.cta
                    when (user.tasbihSkin) {
                        1 -> Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                            // 33 beads; the filled ones show the position in the current 33.
                            val beads = 33; val r = size.minDimension / 2f - 9.dp.toPx(); val c = Offset(size.width / 2, size.height / 2)
                            val lit = if (count == 0) 0 else ((count - 1) % beads) + 1
                            for (i in 0 until beads) {
                                val a = Math.toRadians(-90.0 + i * 360.0 / beads)
                                val p = Offset(c.x + r * kotlin.math.cos(a).toFloat(), c.y + r * kotlin.math.sin(a).toFloat())
                                drawCircle(if (i < lit) arc else track, radius = if (i == lit - 1) 9.dp.toPx() else 7.dp.toPx(), center = p)
                            }
                        }
                        2 -> {}
                        else -> Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                            val w = 14.dp.toPx()
                            drawArc(track, 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                            if (progress > 0f) drawArc(arc, -90f, 360f * progress, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num(if (target > 0 && round == 0 && count > 0) target else round, ar), fontSize = 72.sp, fontWeight = FontWeight.Bold, color = look.onCard)
                        Muted(if (target > 0) t("من ${num(target, true)}", "of $target") else t("بلا هدف", "No target"))
                        if (rounds > 0) Text(t("الدورة ${num(rounds, true)} ✓", "Round $rounds ✓"), color = look.cta, style = MaterialTheme.typography.labelLarge)
                    }
                }
                VSpace(8.dp)
                Muted(if (user.tasbihVolumeKeys) t("اضغط على الدائرة أو زر الصوت للتسبيح", "Tap the circle or a volume key to count") else t("اضغط على الدائرة للتسبيح", "Tap the circle to count"))
            }
        }
        if (!fatimah) item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(t("الهدف", "Target"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TARGETS.forEach { n -> Chip(if (n == 0) "∞" else num(n, ar), selected = target == n, onClick = { onTarget(n) }) }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(Modifier.weight(1f), Ico.TASBIH, num(user.tasbihOn(today) + count, ar), t("اليوم", "Today"))
                Stat(Modifier.weight(1f), Ico.FIRE, num(tasbihStreak(user, today, count), ar), t("أيام متتالية", "Day streak"))
                Stat(Modifier.weight(1f), Ico.TROPHY, num(user.tasbihTotal, ar), t("الإجمالي", "Lifetime"))
            }
        }
        // Last 7 days chart (TS03)
        item {
            SCard(Modifier.padding(horizontal = 16.dp)) {
                Text(t("آخر ٧ أيام", "Last 7 days"), style = MaterialTheme.typography.titleMedium)
                VSpace(10.dp)
                val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                val vals = days.map { d -> user.tasbihOn(d) + if (d == today) count else 0 }
                val max = (vals.maxOrNull() ?: 0).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    days.forEachIndexed { i, d ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (vals[i] > 0) Text(num(vals[i], ar), style = MaterialTheme.typography.labelSmall, color = look.onCardMuted, maxLines = 1)
                            Box(Modifier.fillMaxWidth().height((80f * vals[i] / max).coerceAtLeast(3f).dp).clip(RoundedCornerShape(6.dp)).background(if (d == today) look.cta else look.cta.copy(alpha = .45f)))
                            VSpace(4.dp)
                            Text(if (ar) "نثرخجسح"[d.dayOfWeek.value - 1].toString() else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale(false)).take(2), style = MaterialTheme.typography.labelSmall, color = look.onCardMuted, maxLines = 1)
                        }
                    }
                }
            }
        }
        // Options: skins, sound, volume keys (TS06/TS01)
        item {
            SCard(Modifier.padding(horizontal = 16.dp)) {
                Text(t("الشكل والصوت", "Look & sound"), style = MaterialTheme.typography.titleMedium)
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SKINS.forEachIndexed { i, s -> Chip(if (ar) s.first else s.second, selected = user.tasbihSkin == i, onClick = { onSkin(i) }) }
                }
                VSpace(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("صوت نقرة خفيف", "Soft click sound"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(user.tasbihSound, onSound, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("العدّ بزر الصوت", "Count with volume keys"), style = MaterialTheme.typography.bodyLarge)
                        Muted(t("تبقى الشاشة مضاءة أثناء التسبيح", "The screen stays on while counting"))
                    }
                    Switch(user.tasbihVolumeKeys, onVolumeKeys, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                VSpace(4.dp)
                Muted(t("العدّ بالصوت (قول «سبحان الله») قيد التطوير ليعمل على جهازك دون إنترنت.", "Voice counting (saying “SubhanAllah”) is coming, fully on-device."))
            }
        }
        // Custom dhikr (TS03)
        item {
            SCard(Modifier.padding(horizontal = 16.dp)) {
                Text(t("ذكر خاص", "Your own dhikr"), style = MaterialTheme.typography.titleMedium)
                VSpace(8.dp)
                Box(Modifier.fillMaxWidth().surface(CircleShape, elevated = false).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (addText.isEmpty()) Muted(t("اكتب الذكر…", "Type the dhikr…"))
                    BasicTextField(addText, { addText = it.take(120) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
                }
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(33, 100, 1000, 0).forEach { n -> Chip(if (n == 0) "∞" else num(n, ar), selected = addTarget == n, onClick = { addTarget = n }) }
                    Spacer(Modifier.weight(1f))
                    Chip(t("إضافة", "Add"), selected = addText.isNotBlank(), onClick = { if (addText.isNotBlank()) { onAddCustom(addText, addTarget); addText = "" } })
                }
                custom.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.text, Modifier.weight(1f), fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 32.sp)
                        Muted(if (c.target == 0) "∞" else num(c.target, ar))
                        IconButton(onClick = { onDeleteCustom(c.id) }) { Icon(Icons.Outlined.Close, t("حذف", "Delete"), tint = look.onCardMuted) }
                    }
                }
            }
        }
        item {
            SectionHeader(t("السجل", "History"), Modifier.padding(horizontal = 12.dp)) {
                if (user.tasbihHistory.isNotEmpty()) Chip(t("مسح", "Clear"), onClick = onClearHistory)
            }
        }
        if (user.tasbihHistory.isEmpty()) item {
            SCard(Modifier.padding(horizontal = 16.dp)) { Muted(t("عند الضغط على «حفظ وتصفير» أو تغيير الذكر تُحفظ الجلسة هنا.", "Sessions are saved here when you tap “save & reset” or switch dhikr.")) }
        } else item {
            SCard(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                val fmt = DateTimeFormatter.ofPattern("d MMM • h:mm a", locale(ar))
                user.tasbihHistory.take(12).forEach { h ->
                    val p = all.firstOrNull { it.id == h.preset }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (p == null) h.preset else if (ar) p.text else p.en, style = MaterialTheme.typography.bodyLarge)
                            Muted(digits(Instant.ofEpochMilli(h.at).atZone(ZoneId.systemDefault()).format(fmt), ar))
                        }
                        Text(num(h.count, ar), style = MaterialTheme.typography.titleMedium, color = look.cta)
                    }
                }
            }
        }
    }
}

private fun tasbihStreak(u: UserData, today: LocalDate, live: Int): Int {
    var d = if (u.tasbihOn(today) + live > 0) today else today.minusDays(1)
    var n = 0
    while (n < 3650 && (u.tasbihOn(d) > 0 || (d == today && live > 0))) { n++; d = d.minusDays(1) }
    return n
}

@Composable
fun Stat(modifier: Modifier, ico: Ico, value: String, label: String) {
    SCard(modifier, padding = PaddingValues(12.dp)) {
        PackIcon(ico, 30.dp); VSpace(6.dp)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Muted(label)
    }
}

@Composable
fun NamesScreen(user: UserData, onBack: () -> Unit, onFav: (Int) -> Unit, onTasbih: () -> Unit, onQuiz: () -> Unit = {}) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    var q by rememberSaveable { mutableStateOf("") }
    var favOnly by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableIntStateOf(0) }
    val list = remember(q, favOnly, user.namesFav) {
        val n = normalize(q.trim())
        NAMES_99.filter { (!favOnly || it.n in user.namesFav) && (n.isEmpty() || normalize("${it.ar} ${it.tr} ${it.en} ${it.meaningAr}").contains(n)) }
    }
    val today = NAMES_99[(LocalDate.now().toEpochDay() % 99).toInt()]
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("أسماء الله الحسنى", "The 99 Names of Allah"), onBack, subtitle = t("«إن لله تسعة وتسعين اسمًا، من أحصاها دخل الجنة»", "“Allah has ninety-nine names; whoever preserves them enters Paradise”")) }
        item {
            SCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted(t("اسم اليوم", "Name of the day"))
                        Text(today.ar, fontFamily = QuranFont, fontSize = 38.sp, lineHeight = 66.sp, color = look.onCard)
                        Text(if (ar) today.meaningAr else "${today.tr} — ${today.en}", style = MaterialTheme.typography.bodyMedium)
                    }
                    PackIcon(Ico.NAMES, 70.dp)
                }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("سبّح بها", "Dhikr with it"), leading = { PackIcon(Ico.TASBIH, 18.dp) }, onClick = onTasbih)
                    Chip(t("اختبر نفسك", "Take the quiz"), leading = { PackIcon(Ico.TROPHY, 18.dp) }, onClick = onQuiz)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).surface(CircleShape, elevated = false).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = look.onCardMuted); Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Muted(t("ابحث باسم أو معنى", "Search name or meaning"))
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
                    }
                    if (q.isNotEmpty()) Icon(Icons.Outlined.Close, null, tint = look.onCardMuted, modifier = Modifier.size(20.dp).press { q = "" })
                }
                Chip(t("المفضلة", "Favourites") + " " + num(user.namesFav.size, ar), selected = favOnly, onClick = { favOnly = !favOnly })
            }
        }
        if (list.isEmpty()) item { Muted(t("لا نتائج", "No results"), Modifier.fillMaxWidth().padding(20.dp)) }
        items(list, key = { it.n }) { nm ->
            val fav = nm.n in user.namesFav
            val expanded = open == nm.n
            SCard(onClick = { open = if (expanded) 0 else nm.n }, padding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, look.cta.copy(alpha = .6f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(num(nm.n, ar), style = MaterialTheme.typography.labelLarge, color = look.cta, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nm.ar, fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 46.sp, color = look.onCard)
                        Text(if (ar) nm.meaningAr else "${nm.tr} · ${nm.en}", style = MaterialTheme.typography.bodySmall, color = look.onCardMuted, maxLines = if (expanded) 4 else 1)
                    }
                    IconButton(onClick = { onFav(nm.n) }) {
                        Icon(if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, t("مفضلة", "Favourite"), tint = if (fav) androidx.compose.ui.graphics.Color(0xFFEF4444) else look.onCardMuted)
                    }
                }
                if (expanded) {
                    VSpace(6.dp)
                    Box(Modifier.fillMaxWidth().clip(CircleShape).background(look.onCardMuted.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(if (ar) "${nm.tr} — ${nm.en}" else nm.meaningAr, style = MaterialTheme.typography.bodyMedium, color = look.onCard)
                    }
                    VSpace(6.dp)
                }
            }
        }
        item { Muted(t("القائمة المشهورة المستندة إلى رواية الترمذي. المعاني تقريبية للتدبر.", "The widely used list based on at-Tirmidhi's narration. Meanings are brief aids to reflection."), Modifier.fillMaxWidth().padding(8.dp)) }
    }
}
