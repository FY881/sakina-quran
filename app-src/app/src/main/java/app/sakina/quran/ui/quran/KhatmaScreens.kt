package app.sakina.quran.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sakina.quran.alarm.WirdAlarm
import app.sakina.quran.quran.Khatma
import app.sakina.quran.quran.KhatmaPlan
import app.sakina.quran.quran.Pace
import app.sakina.quran.quran.ReadingStats
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun fmtDate(d: LocalDate, ar: Boolean): String = digits(d.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(ar))), ar)

private val PRAYERS_AR = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
private val PRAYERS_EN = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

/** Khatma home (QR28/QR29): active khatma with today's wird, other khatmas, reading stats link. */
@Composable
fun KhatmaScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val today = LocalDate.now()
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("الختمة والورد اليومي", "Khatma & daily wird"), onBack, subtitle = t("خطط، تابع، وأكمل ختمتك بإذن الله", "Plan, track and complete your khatma")) }
        val active = qu.active
        if (active == null) item {
            SCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PackIcon(Ico.KHATMA, 72.dp); VSpace(8.dp)
                    Text(t("ابدأ ختمتك الأولى", "Start your first khatma"), style = MaterialTheme.typography.titleLarge)
                    Muted(t("اختر مدة الختمة، وسكينة تحسب وردك اليومي وتعيد التوزيع تلقائيًا إن تأخرت أو تقدّمت.", "Pick a duration; Sakina computes your daily wird and rebalances automatically if you fall behind or get ahead."), Modifier.fillMaxWidth())
                    VSpace(12.dp)
                    CtaPill(t("خطة ختمة جديدة", "New khatma plan")) { onOpen("khatma_new") }
                }
            }
        } else item { KhatmaCard(active, today, primary = true, onOpen = onOpen) }
        if (active != null) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CtaPill(t("ختمة جديدة", "New khatma"), Modifier.weight(1f)) { onOpen("khatma_new") }
                GhostPill(t("إحصاءات القراءة", "Reading stats"), Modifier.weight(1f)) { onOpen("quran_stats") }
            }
        }
        // Daily reminder
        item {
            SCard(padding = PaddingValues(14.dp)) {
                val ctx = LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("تذكير الورد اليومي", "Daily wird reminder"), style = MaterialTheme.typography.titleSmall)
                        val h = qu.prefs.wirdAt / 60; val m = qu.prefs.wirdAt % 60
                        Muted(t("كل يوم الساعة ${digits("%d:%02d".format(h, m), true)} إن لم تُتمّ وردك", "Every day at ${"%d:%02d".format(h, m)} if your wird isn't done"))
                    }
                    Switch(qu.prefs.wirdReminder, { v -> scope.launch { store.updatePrefs { it.copy(wirdReminder = v) }; runCatching { WirdAlarm.reschedule(ctx) } } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                if (qu.prefs.wirdReminder) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(5 * 60 + 30, 13 * 60, 17 * 60, 21 * 60).forEach { mins ->
                        Chip(digits("%d:%02d".format(mins / 60, mins % 60), ar), selected = qu.prefs.wirdAt == mins, onClick = {
                            scope.launch { store.updatePrefs { it.copy(wirdAt = mins) }; runCatching { WirdAlarm.reschedule(ctx) } }
                        })
                    }
                }
            }
        }
        val others = qu.khatmas.filter { it.id != active?.id }
        if (others.isNotEmpty()) {
            item { SectionHeader(t("ختماتي الأخرى", "My other khatmas")) }
            items(others, key = { it.id }) { k ->
                KhatmaCard(k, today, primary = false, onOpen = onOpen, onActivate = { scope.launch { store.setActive(k.id) } })
            }
        }
        item {
            Muted(t("«من قرأ حرفًا من كتاب الله فله به حسنة، والحسنة بعشر أمثالها» — سنن الترمذي ٢٩١٠", "\"Whoever reads a letter of the Book of Allah has a good deed, and a good deed is multiplied by ten.\" — Jami' at-Tirmidhi 2910"), Modifier.fillMaxWidth().padding(8.dp))
        }
    }
}

@Composable
private fun KhatmaCard(k: Khatma, today: LocalDate, primary: Boolean, onOpen: (String) -> Unit, onActivate: (() -> Unit)? = null) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val pace = KhatmaPlan.pace(k, today)
    SCard(strong = primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(k.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Muted(t("من ${fmtDate(k.startDate, true)} إلى ${fmtDate(k.endDate, true)}", "${fmtDate(k.startDate, false)} → ${fmtDate(k.endDate, false)}"))
            }
            val (lbl, col) = when (pace) {
                Pace.DONE -> t("تمّت", "Completed") to look.cta
                Pace.AHEAD -> t("متقدّم", "Ahead") to look.cta
                Pace.ON_TRACK -> t("في الموعد", "On track") to look.cta
                Pace.BEHIND -> t("متأخر قليلًا", "Behind") to look.warn
            }
            // Badge colours are computed so text stays ≥ 4.5:1 (WCAG AA) in every theme and mode.
            val surface = look.card.compositeOver(look.bgBottom)
            val (bbg, bfg) = if (pace == Pace.BEHIND) app.sakina.quran.ui.theme.warnBadgeColors(look.warn) else app.sakina.quran.ui.theme.badgeColors(col, surface, look.dark)
            Text(lbl, style = MaterialTheme.typography.labelLarge, color = bfg, maxLines = 1, modifier = Modifier.clip(CircleShape).background(bbg).padding(horizontal = 12.dp, vertical = 5.dp))
        }
        VSpace(10.dp)
        Bar(KhatmaPlan.done(k).toFloat() / KhatmaPlan.total(k))
        VSpace(4.dp)
        Muted(t("${num(KhatmaPlan.percent(k), true)}٪ • ${num(KhatmaPlan.done(k), true)} من ${num(KhatmaPlan.total(k), true)} صفحة • بقي ${num(KhatmaPlan.daysLeft(k, today), true)} يومًا",
            "${KhatmaPlan.percent(k)}% • ${KhatmaPlan.done(k)} of ${KhatmaPlan.total(k)} pages • ${KhatmaPlan.daysLeft(k, today)} days left"))
        if (!KhatmaPlan.isDone(k)) {
            val r = KhatmaPlan.todayRange(k, today)
            val left = KhatmaPlan.todayLeft(k, today)
            val target = KhatmaPlan.todayTarget(k, today)
            VSpace(12.dp)
            Column(Modifier.fillMaxWidth().surface(RoundedCornerShape(16.dp), elevated = false).padding(12.dp)) {
                Text(t("ورد اليوم", "Today's wird"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                Text(
                    t("من الصفحة ${num(r.first, true)} إلى ${num(r.last, true)} (${num(target, true)} صفحة • نحو ${num(KhatmaPlan.minutesPerDay(target), true)} دقيقة)",
                        "Pages ${r.first}–${r.last} ($target pages • about ${KhatmaPlan.minutesPerDay(target)} min)"),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (left == 0) Text(t("أتممت ورد اليوم، بارك الله فيك 🌿", "Today's wird is done — barakAllahu feek 🌿"), color = look.cta, style = MaterialTheme.typography.bodyMedium)
                if (k.afterPrayers && target > 0) {
                    VSpace(6.dp)
                    val split = KhatmaPlan.afterPrayerSplit(target)
                    var p = r.first
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        split.forEachIndexed { i, n ->
                            val from = p; p += n
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (ar) PRAYERS_AR[i] else PRAYERS_EN[i], style = MaterialTheme.typography.labelSmall, color = look.onCardMuted)
                                Text(if (n == 0) "—" else if (n == 1) num(from, ar) else "${num(from, ar)}–${num(from + n - 1, ar)}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CtaPill(if (left == 0) t("اقرأ المزيد", "Read more") else t("اقرأ الورد", "Read wird"), Modifier.weight(1f)) {
                        onOpen("mushaf/${(k.progress + 1).coerceIn(k.startPage, k.endPage)}?k=${k.id}")
                    }
                    if (left > 0) GhostPill(t("تمّ الورد", "Mark done"), Modifier.weight(1f)) { scope.launch { store.saveKhatma(KhatmaPlan.completeToday(k, today)) } }
                }
                if (pace == Pace.BEHIND) Muted(t("أُعيد توزيع ما تبقّى على الأيام المتبقية تلقائيًا.", "What's left was spread over the remaining days automatically."), Modifier.padding(top = 6.dp))
            }
            KhatmaPlan.projectedFinish(k, today)?.let { f -> Muted(t("بوتيرتك الحالية تُتمّها في ${fmtDate(f, true)}", "At your current pace you'll finish on ${fmtDate(f, false)}"), Modifier.padding(top = 6.dp)) }
        } else k.finishedOn?.let { Muted(t("أتممتها في ${fmtDate(it, true)} — تقبّل الله منك", "Completed on ${fmtDate(it, false)} — may Allah accept it"), Modifier.padding(top = 6.dp)) }
        VSpace(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onActivate != null && !KhatmaPlan.isDone(k)) Chip(t("اجعلها الحالية", "Make current"), onClick = onActivate)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, t("حذف الختمة", "Delete khatma"), tint = look.onCardMuted) }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        confirmButton = { TextButton(onClick = { confirmDelete = false; scope.launch { store.deleteKhatma(k.id) } }) { Text(t("حذف", "Delete"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t("إلغاء", "Cancel")) } },
        title = { Text(t("حذف «${k.name}»؟", "Delete \"${k.name}\"?")) },
        text = { Text(t("سيُحذف تقدّمك في هذه الختمة.", "Your progress in this khatma will be removed.")) },
        containerColor = look.bgTop,
    )
}

/** New khatma wizard (QR29): templates, by end date / pages / juz / minutes per day, start page, after-prayer split. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KhatmaNewScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) } // 0 days, 1 pages/day, 2 juz/day, 3 minutes/day
    var days by remember { mutableIntStateOf(30) }
    var perDay by remember { mutableIntStateOf(4) }
    var juz by remember { mutableIntStateOf(1) }
    var minutes by remember { mutableIntStateOf(20) }
    var startPage by remember { mutableIntStateOf(1) }
    var afterPrayers by remember { mutableStateOf(false) }
    var kind by remember { mutableIntStateOf(0) }
    val ppd = when (mode) { 0 -> 0; 1 -> perDay; 2 -> juz * 20; else -> (minutes / KhatmaPlan.MIN_PER_PAGE).toInt().coerceAtLeast(1) }
    val end = if (mode == 0) today.plusDays((days - 1).toLong()) else KhatmaPlan.endForPace(today, startPage, 604, ppd)
    val preview = Khatma("preview", name, startPage, 604, today, end)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("ختمة جديدة", "New khatma"), onBack) }
        item {
            SectionHeader(t("قوالب سريعة", "Quick templates"))
            VSpace(6.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("ختمة رمضان (٣٠ يومًا)", "Ramadan (30 days)"), onClick = { mode = 0; days = 30; kind = 1; name = if (ar) "ختمة رمضان" else "Ramadan khatma" })
                Chip(t("ختمتان في رمضان", "Two in Ramadan"), onClick = { mode = 0; days = 15; kind = 1; name = if (ar) "ختمة رمضانية سريعة" else "Fast Ramadan khatma" })
                Chip(t("ختمة شهرية", "Monthly"), onClick = { mode = 2; juz = 1; kind = 0; name = if (ar) "ختمة شهرية" else "Monthly khatma" })
                Chip(t("في أسبوع", "In a week"), onClick = { mode = 0; days = 7; kind = 0; name = if (ar) "ختمة الأسبوع" else "Weekly khatma" })
                Chip(t("في ٦٠ يومًا", "In 60 days"), onClick = { mode = 0; days = 60; kind = 0; name = if (ar) "ختمة هادئة" else "Gentle khatma" })
                Chip(t("في سنة", "In a year"), onClick = { mode = 0; days = 365; kind = 0; name = if (ar) "ختمة السنة" else "Yearly khatma" })
                Chip(t("ختمة الأسرة", "Family khatma"), onClick = { mode = 0; days = 30; kind = 2; name = if (ar) "ختمة الأسرة" else "Family khatma" })
            }
        }
        item {
            SCard {
                Text(t("اسم الختمة", "Name"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                Box(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(look.onCardMuted.copy(alpha = .08f)).padding(12.dp)) {
                    if (name.isEmpty()) Muted(t("مثال: ختمتي لهذا الشهر", "e.g. My khatma this month"))
                    BasicTextField(name, { name = it.take(40) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
                }
                VSpace(12.dp)
                Text(t("طريقة التخطيط", "Plan by"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                VSpace(6.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("عدد الأيام", "Days"), selected = mode == 0, onClick = { mode = 0 })
                    Chip(t("صفحات يوميًا", "Pages / day"), selected = mode == 1, onClick = { mode = 1 })
                    Chip(t("أجزاء يوميًا", "Juz / day"), selected = mode == 2, onClick = { mode = 2 })
                    Chip(t("دقائق يوميًا", "Minutes / day"), selected = mode == 3, onClick = { mode = 3 })
                }
                VSpace(8.dp)
                val sc = SliderDefaults.colors(thumbColor = look.cta, activeTrackColor = look.cta)
                when (mode) {
                    0 -> { Text(t("${num(days, true)} يومًا", "$days days"), style = MaterialTheme.typography.titleMedium); Slider(days.toFloat(), { days = it.toInt() }, valueRange = 3f..365f, colors = sc) }
                    1 -> { Text(t("${num(perDay, true)} صفحة يوميًا", "$perDay pages a day"), style = MaterialTheme.typography.titleMedium); Slider(perDay.toFloat(), { perDay = it.toInt() }, valueRange = 1f..60f, colors = sc) }
                    2 -> { Text(t("${num(juz, true)} جزء يوميًا", "$juz juz a day"), style = MaterialTheme.typography.titleMedium); Slider(juz.toFloat(), { juz = it.toInt() }, valueRange = 1f..10f, steps = 8, colors = sc) }
                    else -> { Text(t("${num(minutes, true)} دقيقة يوميًا", "$minutes minutes a day"), style = MaterialTheme.typography.titleMedium); Slider(minutes.toFloat(), { minutes = (it / 5).toInt() * 5 }, valueRange = 5f..180f, colors = sc) }
                }
                VSpace(8.dp)
                Text(t("أبدأ من الصفحة ${num(startPage, true)}", "Start at page $startPage"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                Slider(startPage.toFloat(), { startPage = it.toInt().coerceIn(1, 600) }, valueRange = 1f..600f, colors = sc)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("قسّم الورد على الصلوات الخمس", "Split the wird over the 5 prayers"), style = MaterialTheme.typography.bodyLarge)
                        Muted(t("جزء صغير بعد كل صلاة", "A small portion after each prayer"))
                    }
                    Switch(afterPrayers, { afterPrayers = it }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
            }
        }
        item {
            SCard(strong = true) {
                val per = KhatmaPlan.pagesPerDay(preview)
                Text(t("خطتك", "Your plan"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                Text(t("${num(per, true)} صفحة يوميًا • نحو ${num(KhatmaPlan.minutesPerDay(per), true)} دقيقة", "$per pages a day • about ${KhatmaPlan.minutesPerDay(per)} min"), style = MaterialTheme.typography.titleLarge)
                Muted(t("تنتهي بإذن الله في ${fmtDate(end, true)} (${num(KhatmaPlan.totalDays(preview), true)} يومًا)", "Finishes, in sha Allah, on ${fmtDate(end, false)} (${KhatmaPlan.totalDays(preview)} days)"))
                if (afterPrayers) Muted(t("بعد كل صلاة: ", "After each prayer: ") + KhatmaPlan.afterPrayerSplit(per).joinToString(" • ") { num(it, ar) } + t(" صفحات", " pages"))
                VSpace(12.dp)
                CtaPill(t("ابدأ الختمة", "Start khatma"), Modifier.fillMaxWidth()) {
                    val k = Khatma(
                        id = "k${System.currentTimeMillis()}", name = name.ifBlank { if (ar) "ختمتي" else "My khatma" },
                        startPage = startPage, endPage = 604, startDate = today, endDate = end, afterPrayers = afterPrayers, kind = kind,
                    )
                    scope.launch { store.saveKhatma(k, makeActive = true) }
                    onDone()
                }
            }
        }
    }
}

/** Reading stats, hasanat, goals and challenges (QR30–QR32). */
@Composable
fun QuranStatsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val quran = rememberQuran()
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val today = LocalDate.now()
    val pages = qu.pagesRead
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("إحصاءات القراءة والأهداف", "Reading stats & goals"), onBack, subtitle = t("خاصة بك وحدك، محفوظة على جهازك", "Private to you, stored on your device")) }
        item {
            SCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile(num(pages[today]?.size ?: 0, ar), t("صفحات اليوم", "Pages today"), Modifier.weight(1f))
                    StatTile(num(ReadingStats.pagesIn(pages, today.minusDays(6), today), ar), t("آخر ٧ أيام", "Last 7 days"), Modifier.weight(1f))
                    StatTile(num(ReadingStats.streak(pages, qu.prefs.dailyGoal, qu.excused, today), ar), t("أيام متتالية", "Day streak"), Modifier.weight(1f))
                    StatTile(num((qu.readSeconds[today] ?: 0) / 60, ar), t("دقائق اليوم", "Minutes today"), Modifier.weight(1f))
                }
                VSpace(8.dp)
                Muted(t("صفحات مختلفة قرأتها: ${num(ReadingStats.distinctPages(pages), true)} من ٦٠٤", "Distinct pages read: ${ReadingStats.distinctPages(pages)} of 604"))
            }
        }
        // Heatmap (last 12 weeks)
        item {
            SCard {
                Text(t("خريطة القراءة — ١٢ أسبوعًا", "Reading map — 12 weeks"), style = MaterialTheme.typography.titleSmall)
                VSpace(8.dp)
                val start = today.minusDays(83)
                LtrBox {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (w in 0 until 12) Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (d in 0 until 7) {
                                val day = start.plusDays((w * 7 + d).toLong())
                                val lv = if (day.isAfter(today)) -1 else ReadingStats.level(pages[day]?.size ?: 0)
                                val c = when (lv) { -1 -> androidx.compose.ui.graphics.Color.Transparent; 0 -> look.onCardMuted.copy(alpha = .14f); else -> look.cta.copy(alpha = .25f + lv * .18f) }
                                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(c))
                            }
                        }
                    }
                }
                VSpace(6.dp)
                Muted(t("كل مربع يوم؛ كلما اشتدّ اللون زادت الصفحات.", "Each square is a day; darker means more pages."))
            }
        }
        // Hasanat (QR31)
        if (qu.prefs.showHasanat && quran != null) item {
            SCard {
                val h = ReadingStats.hasanat(pages) { quran.pageLetters(it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.STAR, 40.dp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("تقدير الحسنات: ${digits("%,d".format(h), true)}", "Hasanat estimate: ${"%,d".format(h)}"), style = MaterialTheme.typography.titleMedium)
                        Muted(t("حرف بعشر حسنات (سنن الترمذي ٢٩١٠) — تقدير للتشجيع، والأجر عند الله.", "Ten per letter (Tirmidhi 2910) — an encouraging estimate; the reward is with Allah."))
                    }
                }
                Text(t("إخفاء العدّاد", "Hide counter"), color = look.cta, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp).press { scope.launch { store.updatePrefs { it.copy(showHasanat = false) } } })
            }
        } else item {
            Chip(t("إظهار تقدير الحسنات", "Show hasanat estimate"), onClick = { scope.launch { store.updatePrefs { it.copy(showHasanat = true) } } })
        }
        // Goal (QR32)
        item {
            SCard {
                Text(t("هدفي اليومي", "My daily goal"), style = MaterialTheme.typography.titleSmall)
                VSpace(6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 10, 20).forEach { g ->
                        Chip(t("${num(g, true)} ص", "$g p"), selected = qu.prefs.dailyGoal == g, onClick = { scope.launch { store.updatePrefs { it.copy(dailyGoal = g) } } })
                    }
                }
                VSpace(8.dp)
                val got = pages[today]?.size ?: 0
                Bar(got.toFloat() / qu.prefs.dailyGoal)
                Muted(t("اليوم ${num(got, true)} من ${num(qu.prefs.dailyGoal, true)}", "Today $got of ${qu.prefs.dailyGoal}"))
                VSpace(8.dp)
                val exc = today in qu.excused
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("إيقاف السلسلة اليوم بعذر", "Pause the streak today"), style = MaterialTheme.typography.bodyLarge)
                        Muted(t("سفر أو مرض أو عذر شرعي — لا تنقطع سلسلتك", "Travel, illness or a valid excuse — your streak is kept"))
                    }
                    Switch(exc, { scope.launch { store.toggleExcused(today) } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
            }
        }
        // Challenges
        item { SectionHeader(t("التحديات", "Challenges")) }
        item {
            SCard(onClick = { onOpen("mushaf/${ReadingStats.KAHF.first}") }) {
                Text(t("سورة الكهف كل جمعة", "Al-Kahf every Friday"), style = MaterialTheme.typography.titleSmall)
                Muted(t("آخر ٨ جُمَع", "Last 8 Fridays"))
                VSpace(6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReadingStats.kahfFridays(pages, today).forEach { (_, ok) -> Dot(if (ok) look.cta else look.onCardMuted.copy(alpha = .25f), 22) }
                }
            }
        }
        item {
            SCard(onClick = { onOpen("mushaf/${ReadingStats.MULK.first}") }) {
                Text(t("سورة الملك كل ليلة", "Al-Mulk every night"), style = MaterialTheme.typography.titleSmall)
                val n = ReadingStats.mulkNights(pages, today)
                Muted(t("${num(n, true)} من آخر ٧ ليالٍ", "$n of the last 7 nights"))
                VSpace(6.dp); Bar(n / 7f)
            }
        }
        item {
            SCard(onClick = { onOpen("khatma") }) {
                Text(t("جزء كل يوم لمدة ٣٠ يومًا", "A juz a day for 30 days"), style = MaterialTheme.typography.titleSmall)
                val n = ReadingStats.juzDays(pages, today)
                Muted(t("${num(n, true)} يومًا من آخر ٣٠", "$n of the last 30 days"))
                VSpace(6.dp); Bar(n / 30f)
            }
        }
    }
}
