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

@Composable
fun TasbihScreen(
    user: UserData, onBack: () -> Unit, onTap: () -> Unit, onUndo: () -> Unit, onSave: () -> Unit,
    onPreset: (String, Int) -> Unit, onTarget: (Int) -> Unit, onClearHistory: () -> Unit,
) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val view = LocalView.current
    val preset = TASBIH_PRESETS.firstOrNull { it.id == user.tasbihPreset } ?: TASBIH_PRESETS[0]
    // Local count for instant feedback; DataStore is the source of truth.
    var count by remember { mutableIntStateOf(user.tasbihCount) }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(user.tasbihCount) { if (!touched) count = user.tasbihCount }
    val target = user.tasbihTarget
    val round = if (target > 0) count % target else count
    val rounds = if (target > 0) count / target else 0
    val progress by animateFloatAsState(if (target > 0) (if (round == 0 && count > 0) 1f else round / target.toFloat()) else 0f, tween(250), label = "p")
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            BackHeader(t("السبحة", "Tasbih"), onBack, subtitle = t("اليوم: ${num(user.tasbihOn(today) + count, true)} • الإجمالي: ${num(user.tasbihTotal, true)}", "Today: ${user.tasbihOn(today) + count} • Lifetime: ${user.tasbihTotal}")) {
                IconButton(onClick = { onUndo(); count = (count - 1).coerceAtLeast(0) }) { Icon(Icons.AutoMirrored.Outlined.Undo, t("تراجع", "Undo"), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { onSave(); count = 0 }) { Icon(Icons.Outlined.RestartAlt, t("حفظ وتصفير", "Save & reset"), tint = MaterialTheme.colorScheme.onBackground) }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TASBIH_PRESETS) { p -> Chip(if (ar) p.text else p.en, selected = p.id == preset.id, onClick = { if (p.id != preset.id) { onSave(); count = 0; onPreset(p.id, p.target) } }) }
            }
        }
        // The big counter
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(preset.text, fontFamily = QuranFont, fontSize = 30.sp, lineHeight = 54.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Muted(t(preset.virtueAr, preset.virtueEn))
                VSpace(14.dp)
                Box(
                    Modifier.size(270.dp).scale(pulse.value).clip(CircleShape).surface(CircleShape, strong = true)
                        .clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple()) {
                            touched = true
                            count++
                            onTap()
                            val hit = target > 0 && count % target == 0
                            view.performHapticFeedback(if (hit) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
                            scope.launch { pulse.snapTo(.96f); pulse.animateTo(1f, tween(140)) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val track = look.onCardMuted.copy(alpha = .18f); val arc = look.cta
                    Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                        val w = 14.dp.toPx()
                        drawArc(track, 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                        if (progress > 0f) drawArc(arc, -90f, 360f * progress, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num(if (target > 0 && round == 0 && count > 0) target else round, ar), fontSize = 72.sp, fontWeight = FontWeight.Bold, color = look.onCard)
                        Muted(if (target > 0) t("من ${num(target, true)}", "of $target") else t("بلا هدف", "No target"))
                        if (rounds > 0) Text(t("الدورة ${num(rounds, true)} ✓", "Round $rounds ✓"), color = look.cta, style = MaterialTheme.typography.labelLarge)
                    }
                }
                VSpace(8.dp)
                Muted(t("اضغط على الدائرة للتسبيح", "Tap the circle to count"))
            }
        }
        item {
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
                    val p = TASBIH_PRESETS.firstOrNull { it.id == h.preset }
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
fun NamesScreen(user: UserData, onBack: () -> Unit, onFav: (Int) -> Unit, onTasbih: () -> Unit) {
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
                    val soon = app.sakina.quran.ui.tabs.LocalSoon.current
                    Chip(t("اختبر نفسك", "Take the quiz"), leading = { PackIcon(Ico.TROPHY, 18.dp) }, onClick = { soon.value = app.sakina.quran.ui.tabs.feature("names_quiz") })
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
