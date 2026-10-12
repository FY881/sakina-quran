package app.sakina.quran.ui.prayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.UserData
import app.sakina.quran.prayer.DayTimes
import app.sakina.quran.prayer.Hijri
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.countdown
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.formatTime
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.modeIcon
import app.sakina.quran.ui.common.modeLabel
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.prayerName
import app.sakina.quran.ui.common.rememberHealth
import app.sakina.quran.ui.common.rememberNow
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.home.FARD
import app.sakina.quran.ui.home.currentPrayer
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.icons.ico
import app.sakina.quran.ui.kit.Badge
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.ListRow
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** Compact list of a day's times (kept for widgets/other screens). */
@Composable
fun TimesCard(
    day: DayTimes, prefs: Prefs, highlight: Prayer?, arabic: Boolean, title: String,
    onCycleMode: ((Prayer) -> Unit)? = null,
) {
    val friday = day.date.dayOfWeek == DayOfWeek.FRIDAY
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            day.times.forEach { (p, time) ->
                val mode = prefs.modes[p] ?: AlertMode.OFF
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(prayerName(p, friday), fontWeight = if (p == highlight) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(formatTime(time, arabic))
                    if (onCycleMode != null) IconButton(onClick = { onCycleMode(p) }) { Icon(modeIcon(mode), modeLabel(mode)) }
                }
            }
        }
    }
}

@Composable
fun PrayerScreen(
    prefs: Prefs, user: UserData, onCycleMode: (Prayer) -> Unit,
    onToggle: (LocalDate, Int) -> Unit, onMarkAll: (LocalDate) -> Unit,
    onSettings: () -> Unit, onCity: () -> Unit, fixedNow: ZonedDateTime? = null,
) {
    val now = fixedNow ?: rememberNow()
    val ar = isArabic()
    val look = LocalSakina.current.look
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var month by rememberSaveable { mutableStateOf(false) }
    val place = prefs.place
    val today = now.withZoneSameInstant(place.zone).toLocalDate()
    val date = today.plusDays(offset.toLong())
    val day = PrayerEngine.day(place, date, prefs.calc)
    val next = PrayerEngine.next(place, now, prefs.calc)
    val cur = next.currentPrayer()
    val friday = date.dayOfWeek == DayOfWeek.FRIDAY
    val dm = digits(date.format(DateTimeFormatter.ofPattern("d MMMM", locale(ar))), ar)
    val title = when (offset) {
        0 -> t("اليوم، ", "Today, ") + dm
        1 -> t("غدًا، ", "Tomorrow, ") + dm
        -1 -> t("أمس، ", "Yesterday, ") + dm
        else -> digits(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale(ar))), ar)
    }
    val hijri = digits(Hijri.format(date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant().toEpochMilli(), ar, prefs.hijriAdj), ar)
    val canMark = !date.isAfter(today)
    val count = user.prayedOn(date)
    val issues = rememberHealth()

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Date navigation
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, t("اليوم السابق", "Previous day"), tint = MaterialTheme.colorScheme.onBackground) }
                Column(Modifier.weight(1f).press(haptic = false) { offset = 0 }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    Muted(hijri)
                }
                IconButton(onClick = { offset++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, t("اليوم التالي", "Next day"), tint = MaterialTheme.colorScheme.onBackground) }
            }
        }
        // ── City + method chips
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(place.label(ar), leading = { PackIcon(Ico.LOCATION, 18.dp) }, onClick = onCity)
                Chip(prefs.calc.method.short(ar), leading = { PackIcon(Ico.SETTINGS, 18.dp) }, onClick = onSettings)
            }
        }
        // ── Progress ring + current / next
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(12.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Ring(count / 5f, 92.dp, 9.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (count >= 5) PackIcon(Ico.DONE, 26.dp)
                            Text(digits("$count/5", ar), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    VSpace(6.dp)
                    Muted(t("صلوات أُدّيت", "prayed"), Modifier.fillMaxWidth().padding(horizontal = 4.dp))
                }
                SCard(Modifier.weight(1.3f).fillMaxHeight(), padding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(look.cta)); Spacer(Modifier.width(6.dp))
                        Muted(t("الآن", "Now") + " • " + prayerName(cur))
                    }
                    VSpace(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(prayerName(next.prayer, next.at.dayOfWeek == DayOfWeek.FRIDAY && next.prayer == Prayer.DHUHR), style = MaterialTheme.typography.titleMedium)
                            Text(formatTime(next.at, ar), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        PackIcon(next.prayer.ico(), 40.dp)
                    }
                    VSpace(6.dp)
                    Text(
                        prayerName(next.prayer) + t(" بعد ", " in ") + countdown(Duration.between(now, next.at), ar),
                        style = MaterialTheme.typography.labelLarge, color = look.cta, maxLines = 1,
                    )
                }
            }
        }
        // ── Imsak | Sunrise
        item {
            SCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniTime(Ico.NIGHT, t("الإمساك", "Imsak"), formatTime(day.times.getValue(Prayer.FAJR).minusMinutes(10), ar), Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(34.dp).background(look.cardBorder))
                    MiniTime(Ico.SUNRISE, t("الشروق", "Sunrise"), formatTime(day.times.getValue(Prayer.SUNRISE), ar), Modifier.weight(1f).padding(start = 16.dp))
                }
            }
        }
        // ── Five prayers
        item {
            SCard(padding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                FARD.forEachIndexed { i, p ->
                    val isCur = offset == 0 && p == cur
                    val isNext = offset == 0 && p == next.prayer && next.at.toLocalDate() == date
                    val prayed = user.isPrayed(date, i)
                    val mode = prefs.modes[p] ?: AlertMode.OFF
                    val name = prayerName(p, friday)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (isCur) look.cta.copy(alpha = .14f) else Color.Transparent)
                            .padding(start = 8.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckCircle(prayed, enabled = canMark, label = name) { onToggle(date, i) }
                        Spacer(Modifier.width(12.dp))
                        PackIcon(p.ico(), 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                            when {
                                isCur -> Badge(t("الآن", "now"), look.cta.copy(alpha = .22f), look.onCard, dot = look.cta)
                                isNext -> Badge(countdown(Duration.between(now, next.at), ar), look.onCardMuted.copy(alpha = .18f), look.onCard)
                            }
                        }
                        Text(formatTime(day.times.getValue(p), ar), style = MaterialTheme.typography.titleMedium, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium)
                        val ml = modeLabel(mode)
                        IconButton(onClick = { onCycleMode(p) }, modifier = Modifier.semantics { stateDescription = ml; role = Role.Button }) {
                            Icon(modeIcon(mode), contentDescription = ml, tint = if (mode == AlertMode.OFF) look.onCardMuted else look.onCard)
                        }
                    }
                }
            }
        }
        // ── Mark all
        item {
            if (canMark && count < 5) CtaPill(t("تحديد الكل كمؤدّاة", "Mark all as prayed"), Modifier.fillMaxWidth(), icon = { Icon(Icons.Outlined.Check, null) }) { onMarkAll(date) }
            else if (count >= 5) Muted(t("ما شاء الله! أتممت صلوات هذا اليوم", "MashaAllah! All prayers done for this day"), Modifier.fillMaxWidth().padding(4.dp))
        }
        // ── Your progress
        item {
            SectionHeader(t("تقدّمك", "Your progress")) {
                Chip(t("أسبوع", "Week"), selected = !month, onClick = { month = false })
                Spacer(Modifier.width(6.dp))
                Chip(t("شهر", "Month"), selected = month, onClick = { month = true })
            }
        }
        item {
            SCard {
                if (!month) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (6 downTo 0).forEach { back ->
                            val d = today.minusDays(back.toLong())
                            val c = user.prayedOn(d)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Muted(d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale(ar)).take(if (ar) 3 else 2))
                                VSpace(4.dp)
                                Box(contentAlignment = Alignment.Center) {
                                    Ring(c / 5f, 34.dp, 4.dp)
                                    Text(num(d.dayOfMonth, ar), style = MaterialTheme.typography.labelMedium, fontWeight = if (d == date) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                } else {
                    val ym = today.withDayOfMonth(1)
                    val days = (0 until today.dayOfMonth).map { ym.plusDays(it.toLong()) }
                    val total = days.sumOf { user.prayedOn(it) }
                    val full = days.count { user.prayedOn(it) >= 5 }
                    Text(digits(t("هذا الشهر: $total صلاة • $full يوم كامل", "This month: $total prayers • $full full days"), ar), style = MaterialTheme.typography.titleSmall)
                }
                VSpace(10.dp)
                Muted(digits(t("سلسلة الأيام الكاملة: ${user.streak(today)} 🔥", "Full-day streak: ${user.streak(today)} 🔥"), ar))
            }
        }
        // ── Night & disliked times
        item {
            SCard {
                Text(t("أوقات الليل والكراهة", "Night & disliked times"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                VSpace(8.dp)
                InfoRow(t("منتصف الليل الشرعي", "Islamic midnight"), formatTime(day.midnight, ar))
                InfoRow(t("بداية الثلث الأخير", "Last third begins"), formatTime(day.lastThird, ar))
                InfoRow(t("الكراهة بعد الشروق حتى", "Disliked after sunrise until"), formatTime(day.times.getValue(Prayer.SUNRISE).plusMinutes(15), ar))
                InfoRow(t("الكراهة قبل الظهر من", "Disliked before Dhuhr from"), formatTime(day.times.getValue(Prayer.DHUHR).minusMinutes(5), ar))
                InfoRow(t("الكراهة قبل المغرب من", "Disliked before Maghrib from"), formatTime(day.times.getValue(Prayer.MAGHRIB).minusMinutes(15), ar))
            }
        }
        // ── Settings & more
        item { SectionHeader(t("الإعدادات والمزيد", "Settings & more")) }
        item {
            SCard(padding = PaddingValues(6.dp)) {
                ListRow(
                    Ico.TOOLS, t("الإعداد واستكشاف الأخطاء", "Setup & troubleshooting"),
                    if (issues.isEmpty()) t("كل شيء سليم ✓", "All good ✓") else digits(t("${issues.size} يحتاج إصلاحًا", "${issues.size} need fixing"), ar),
                    onClick = onSettings,
                )
                ListRow(Ico.ADHAN, t("إعدادات الصلاة والأذان", "Prayer & adhan settings"), if (ar) prefs.calc.madhab.ar else prefs.calc.madhab.en, onClick = onSettings)
                ListRow(Ico.LOCATION, t("الموقع", "Location"), place.label(ar), onClick = onCity)
            }
        }
    }
}

@Composable
private fun MiniTime(ico: Ico, label: String, time: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PackIcon(ico, 26.dp); Spacer(Modifier.width(10.dp))
        Column { Muted(label); Text(time, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun CheckCircle(checked: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    val look = LocalSakina.current.look
    val desc = label + if (checked) " ✓" else ""
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .then(if (checked) Modifier.background(look.cta) else Modifier.border(2.dp, look.onCardMuted.copy(alpha = if (enabled) .8f else .3f), CircleShape))
            .press(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = desc; role = Role.Checkbox },
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = look.onCta) }
}

@Composable
private fun Ring(fraction: Float, size: Dp, stroke: Dp) {
    val look = LocalSakina.current.look
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val s = Size(this.size.width - w, this.size.height - w)
        val tl = Offset(w / 2, w / 2)
        drawArc(look.onCardMuted.copy(alpha = .25f), 0f, 360f, false, tl, s, style = Stroke(w))
        if (fraction > 0f) drawArc(look.cta, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, s, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalSakina.current.look.onCardMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
