package app.sakina.quran.ui.azkar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.sakina.quran.alarm.Scheduler
import app.sakina.quran.azkar.AzCat
import app.sakina.quran.azkar.Azkar
import app.sakina.quran.azkar.Dhikr
import app.sakina.quran.azkar.DhikrAudio
import app.sakina.quran.azkar.SESSION_META
import app.sakina.quran.azkar.Session
import app.sakina.quran.azkar.ShareImage
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.UserData
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.formatTime
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.dhikr.Stat
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Badge
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.ListRow
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/** Which session fits the current time: morning (Fajr→Dhuhr), evening (Asr→Isha), sleep (Isha→Fajr), else after-prayer. */
fun suggestedSession(prefs: Prefs, now: ZonedDateTime = ZonedDateTime.now()): String {
    val local = now.withZoneSameInstant(prefs.place.zone)
    val d = PrayerEngine.day(prefs.place, local.toLocalDate(), prefs.calc).times
    return when {
        local.isBefore(d.getValue(Prayer.FAJR)) -> "sleep"
        local.isBefore(d.getValue(Prayer.DHUHR)) -> "morning"
        local.isBefore(d.getValue(Prayer.ASR)) -> "after_prayer"
        local.isBefore(d.getValue(Prayer.ISHA)) -> "evening"
        else -> "sleep"
    }
}

private fun sessionName(id: String, ar: Boolean) = SESSION_META.firstOrNull { it.first == id }?.second?.let { if (ar) it.first else it.second } ?: id

// ─────────────────────────────────────────────── Azkar home
@Composable
fun AzkarHome(prefs: Prefs, repo: SettingsRepo, user: UserData, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val azkar by rememberAzkar()
    val today = LocalDate.now()
    val sug = remember(prefs.place, prefs.calc) { suggestedSession(prefs) }
    val counts = remember(azkar) {
        val a = azkar
        if (a == null) emptyMap() else SESSION_META.associate { (id, _, _) ->
            id to when (id) { "ruqyah" -> 0; else -> a.session(id)?.items?.size ?: 0 }
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            BackHeader(t("الأذكار", "Azkar"), onBack, subtitle = t("سلسلتك: ${num(user.azkarStreak(today), true)} يوم • حصن المسلم كاملًا", "Streak: ${user.azkarStreak(today)} days • complete Hisn al-Muslim")) {
                IconButton(onClick = { onOpen("azkar_search") }) { Icon(Icons.Outlined.Search, t("بحث", "Search"), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { onOpen("azkar_mine") }) { Icon(Icons.Outlined.FavoriteBorder, t("المفضلة", "Favourites"), tint = MaterialTheme.colorScheme.onBackground) }
            }
        }
        // Hero: the session that fits now
        item {
            val total = counts[sug] ?: 0
            val prog = user.azkarProgress[sug]
            val doneItems = prog?.size?.let { _ -> azkar?.session(sug)?.items?.withIndex()?.count { (i, d) -> (prog.getOrNull(i) ?: 0) >= d.count } } ?: 0
            val finished = user.azkarDoneOn(today, sug)
            SCard(Modifier.padding(horizontal = 16.dp), strong = true, onClick = { onOpen("azkar/$sug") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted(t("المناسب الآن", "Right now"))
                        Text(sessionName(sug, ar), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        VSpace(2.dp)
                        Muted(if (finished) t("أتممتها اليوم — تقبّل الله", "Completed today — may Allah accept") else digits(t("$doneItems من $total ذكرًا", "$doneItems of $total done"), ar))
                    }
                    PackIcon(SESSION_META.first { it.first == sug }.third, 64.dp)
                }
                VSpace(10.dp)
                LinearProgressIndicator(
                    progress = { if (finished) 1f else if (total == 0) 0f else doneItems / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = look.cta, trackColor = look.onCardMuted.copy(alpha = .2f),
                    drawStopIndicator = {},
                )
                VSpace(12.dp)
                CtaPill(if (finished) t("اقرأها مرة أخرى", "Read again") else if (doneItems > 0) t("تابع", "Continue") else t("ابدأ الآن", "Start now"), Modifier.fillMaxWidth()) { onOpen("azkar/$sug") }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(Modifier.weight(1f), Ico.FIRE, num(user.azkarStreak(today), ar), t("أيام متتالية", "Day streak"))
                Stat(Modifier.weight(1f), Ico.CALENDAR, digits("${user.azkarDaysInWeek(today)}/7", ar), t("هذا الأسبوع", "This week"))
                Stat(Modifier.weight(1f), Ico.STAR, num(user.azkarFav.size, ar), t("المفضلة", "Favourites"))
            }
        }
        item { SectionHeader(t("الأوراد اليومية", "Daily sets"), Modifier.padding(horizontal = 12.dp)) }
        SESSION_META.chunked(2).forEach { pair ->
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (id, names, ico) ->
                        val done = user.azkarDoneOn(today, id)
                        SCard(Modifier.weight(1f), onClick = { onOpen(if (id == "ruqyah") "ruqyah" else "azkar/$id") }, padding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PackIcon(ico, 40.dp)
                                Spacer(Modifier.weight(1f))
                                if (done) Icon(Icons.Outlined.CheckCircle, t("تمّت", "Done"), tint = look.cta, modifier = Modifier.size(22.dp))
                            }
                            VSpace(8.dp)
                            Text(if (ar) names.first else names.second, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val n = counts[id] ?: 0
                            Muted(if (id == "ruqyah") t("آيات وأدعية", "Ayat & duas") else if (done) t("أُتمّت اليوم", "Done today") else digits(t("$n ذكرًا", "$n adhkar"), ar))
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { SectionHeader(t("المكتبة", "Library"), Modifier.padding(horizontal = 12.dp)) }
        item {
            SCard(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(6.dp)) {
                ListRow(Ico.SHAHADA, t("حصن المسلم", "Hisn al-Muslim"), t("١٣٣ بابًا لكل المواقف مع المصادر", "133 chapters for every situation, with sources"), onClick = { onOpen("hisn") })
                ListRow(Ico.DUA, t("الأدعية", "Duas"), t("أدعية من القرآن والسنة", "From the Quran and the Sunnah"), onClick = { onOpen("duas") })
                ListRow(Ico.PRIVACY, t("الرقية الشرعية", "Ruqyah"), t("آيات وأدعية الرقية مع آدابها", "Ruqyah ayat and duas with etiquette"), onClick = { onOpen("ruqyah") })
                ListRow(Ico.IDEA, t("صلاة الاستخارة", "Istikhara"), t("الخطوات والدعاء", "Steps and the dua"), onClick = { onOpen("istikhara") })
                ListRow(Ico.SEEDLING, t("عيادة المريض والدعاء للميت", "Visiting the sick & the deceased"), t("أدعية المرض والتعزية والجنازة", "Duas for illness, condolence and funerals"), onClick = { onOpen("hisn?cat=SICK") })
                ListRow(Ico.JOURNAL, t("مفضلتي وأذكاري الخاصة", "Favourites & my own azkar"), digits(t("${user.azkarFav.size} مفضلة • ${user.ownAzkar.size} خاصة", "${user.azkarFav.size} favourites • ${user.ownAzkar.size} own"), ar), onClick = { onOpen("azkar_mine") })
                ListRow(Ico.TASBIH, t("السبحة", "Tasbih"), t("عدّاد حرّ بأهداف وسجل", "Free counter with targets and history"), onClick = { onOpen("tasbih") })
            }
        }
        item { SectionHeader(t("التذكير", "Reminders"), Modifier.padding(horizontal = 12.dp)) }
        item { ReminderCard(prefs, repo) }
        item {
            Muted(
                t("نصوص الأذكار من مجموعتي بيانات مفتوحتين بترخيص MIT (asellam/HisnElMuslim و Seen-Arabic/Morning-And-Evening-Adhkar-DB) دون تعديل، والآيات من مشروع تنزيل. التلاوة الصوتية متاحة حيث يسمح الترخيص فقط.",
                    "Arabic texts from two MIT-licensed open datasets (asellam/HisnElMuslim, Seen-Arabic/Morning-And-Evening-Adhkar-DB), unchanged; ayat from the Tanzil Project. English translations will be added once their licence is cleared; audio only where licensed."),
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun ReminderCard(prefs: Prefs, repo: SettingsRepo) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<String?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        pending?.let { k -> scope.launch { repo.setAzkarReminder(k, true) } }; pending = null
    }
    fun toggle(kind: String, on: Boolean) {
        val need = on && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (need) { pending = kind; perm.launch(Manifest.permission.POST_NOTIFICATIONS) } else scope.launch { repo.setAzkarReminder(kind, on) }
    }
    val next = remember(prefs) { Scheduler.azkarUpcoming(prefs, ZonedDateTime.now()).firstOrNull() }
    SCard(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)) {
        @Composable
        fun row(kind: String, ico: Ico, title: String, sub: String, on: Boolean) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PackIcon(ico, 30.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Muted(sub)
                }
                Switch(on, { toggle(kind, it) }, colors = SwitchDefaults.colors(checkedTrackColor = look.cta, checkedThumbColor = look.onCta))
            }
        }
        row("morning", Ico.SUNRISE, t("أذكار الصباح", "Morning azkar"), digits(t("بعد الفجر بـ ${prefs.azMorningMin} دقيقة (قبل الشروق)", "${prefs.azMorningMin} min after Fajr (before sunrise)"), ar), prefs.azMorning)
        if (prefs.azMorning) Row(Modifier.padding(start = 42.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10, 20, 30, 45).forEach { m -> Chip(num(m, ar), selected = prefs.azMorningMin == m, onClick = { scope.launch { repo.setAzkarOffsets(morningMin = m) } }) }
        }
        row("evening", Ico.MAGHRIB, t("أذكار المساء", "Evening azkar"), digits(t("بعد العصر بـ ${prefs.azEveningMin} دقيقة (قبل المغرب)", "${prefs.azEveningMin} min after Asr (before Maghrib)"), ar), prefs.azEvening)
        if (prefs.azEvening) Row(Modifier.padding(start = 42.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 30, 60, 90).forEach { m -> Chip(num(m, ar), selected = prefs.azEveningMin == m, onClick = { scope.launch { repo.setAzkarOffsets(eveningMin = m) } }) }
        }
        val sleepLabel = formatTime(LocalDate.now().atStartOfDay(prefs.place.zone).plusMinutes(prefs.azSleepAt.toLong()), ar)
        row("sleep", Ico.NIGHT, t("أذكار النوم", "Sleep azkar"), t("الساعة $sleepLabel", "At $sleepLabel"), prefs.azSleep)
        if (prefs.azSleep) LazyRow(Modifier.padding(start = 42.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(21 * 60, 21 * 60 + 30, 22 * 60, 22 * 60 + 30, 23 * 60, 23 * 60 + 30, 0)) { m ->
                Chip(formatTime(LocalDate.now().atStartOfDay(prefs.place.zone).plusMinutes(m.toLong()), ar), selected = prefs.azSleepAt == m, onClick = { scope.launch { repo.setAzkarOffsets(sleepAt = m) } })
            }
        }
        VSpace(4.dp)
        Muted(
            if (next != null) t("التذكير القادم: ${sessionName(next.kind, true)} — ${formatTime(next.at, true)}", "Next: ${sessionName(next.kind, false)} — ${formatTime(next.at, false)}")
            else t("فعّل التذكير ليصلك إشعار هادئ في وقت الأذكار، محسوب من مواقيت مدينتك.", "Turn on a gentle notification at azkar time, computed from your city's prayer times."),
        )
    }
}

// ─────────────────────────────────────────────── Session (counter)
@Composable
fun AzkarSessionScreen(
    sid: String, startIndex: Int, user: UserData, onBack: () -> Unit, onOpen: (String) -> Unit,
    onProgress: (String, List<Int>) -> Unit, onComplete: (String) -> Unit, onFav: (String) -> Unit,
) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val azkar by rememberAzkar()
    val quran by rememberQuran()
    val own = remember(user.ownAzkar) { user.ownAzkar.map { Dhikr(it.id, it.text, it.count) } }
    val session: Session? = remember(azkar, quran, sid, own, if (sid == "fav") user.azkarFav else null) {
        val a = azkar ?: return@remember null
        if (sid == "ruqyah" && quran == null) null else a.session(sid, quran, own, user.azkarFav.toList())
    }
    DisposableEffect(Unit) { onDispose { DhikrAudio.stop() } }
    if (session == null) {
        Column { BackHeader(t("الأذكار", "Azkar"), onBack); Loading() }
        return
    }
    val items = session.items
    if (items.isEmpty()) {
        Column {
            BackHeader(if (ar) session.ar else session.en, onBack)
            SCard(Modifier.padding(16.dp)) {
                Muted(if (sid == "fav") t("لم تُضف أذكارًا إلى المفضلة بعد. اضغط ♡ على أي ذكر لإضافته.", "No favourites yet. Tap ♡ on any dhikr to add it.") else t("لا توجد أذكار هنا بعد. أضف أذكارك الخاصة من «مفضلتي وأذكاري الخاصة».", "Nothing here yet. Add your own azkar from “Favourites & my own azkar”."))
                VSpace(10.dp)
                CtaPill(t("أذكاري", "My azkar")) { onOpen("azkar_mine") }
            }
        }
        return
    }
    val done = remember(session) {
        val saved = user.azkarProgress[sid]
        mutableStateListOf(*Array(items.size) { i -> (saved?.getOrNull(i) ?: 0).coerceAtMost(items[i].count) })
    }
    val firstOpen = remember(session) { if (startIndex > 0) startIndex.coerceAtMost(items.size - 1) else done.indexOfFirst { it == 0 }.let { if (it < 0) items.size else it } }
    val pager = rememberPagerState(initialPage = firstOpen.coerceIn(0, items.size)) { items.size + 1 }
    var completedNow by remember(session) { mutableStateOf(false) }
    val finishedItems = items.indices.count { done[it] >= items[it].count }
    var playing by remember { mutableStateOf<String?>(null) }

    fun count(i: Int) {
        val d = items[i]
        if (done[i] >= d.count) { scope.launch { if (i < items.size) pager.animateScrollToPage(i + 1) }; return }
        done[i] = done[i] + 1
        onProgress(sid, done.toList())
        val finished = done[i] >= d.count
        view.performHapticFeedback(if (finished) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
        if (finished) {
            val all = items.indices.all { done[it] >= items[it].count }
            if (all && !completedNow) { completedNow = true; onComplete(sid) }
            scope.launch { delay(380); pager.animateScrollToPage(i + 1) }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        BackHeader(if (ar) session.ar else session.en, onBack, subtitle = digits(t("${finishedItems} من ${items.size} • اضغط على البطاقة للعدّ", "$finishedItems of ${items.size} • tap the card to count"), ar)) {
            IconButton(onClick = {
                for (i in done.indices) done[i] = 0
                onProgress(sid, done.toList()); completedNow = false
                scope.launch { pager.scrollToPage(0) }
            }) { Icon(Icons.Outlined.RestartAlt, t("إعادة البدء", "Restart"), tint = MaterialTheme.colorScheme.onBackground) }
        }
        LinearProgressIndicator(
            progress = { finishedItems / items.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(5.dp).clip(CircleShape),
            color = look.cta, trackColor = look.onCardMuted.copy(alpha = .2f), drawStopIndicator = {},
        )
        VSpace(10.dp)
        HorizontalPager(pager, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { page ->
            if (page >= items.size) {
                CompletionPage(session, user, ar, onBack = onBack, onRestart = {
                    for (i in done.indices) done[i] = 0
                    onProgress(sid, done.toList()); completedNow = false
                    scope.launch { pager.scrollToPage(0) }
                }, onOpen = onOpen)
                return@HorizontalPager
            }
            val d = items[page]
            val fav = d.id in user.azkarFav
            SCard(Modifier.fillMaxSize(), onClick = { count(page) }, padding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(digits("${page + 1}/${items.size}", ar), look.cta.copy(alpha = .18f), look.onCard)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ar && d.countLabel.isNotBlank()) d.countLabel else countLabel(d.count, ar), style = MaterialTheme.typography.labelLarge, color = look.cta, maxLines = 1, modifier = Modifier.weight(1f))
                    val audio = d.audio
                    if (audio != null) IconButton(onClick = { DhikrAudio.toggle(audio) { on -> playing = if (on) audio else null } }) {
                        Icon(if (playing == audio) Icons.Outlined.StopCircle else Icons.AutoMirrored.Outlined.VolumeUp, t("استماع", "Listen"), tint = look.onCardMuted)
                    }
                    IconButton(onClick = { onFav(d.id) }) {
                        Icon(if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, t("مفضلة", "Favourite"), tint = if (fav) androidx.compose.ui.graphics.Color(0xFFEF4444) else look.onCardMuted)
                    }
                    IconButton(onClick = { ShareImage.share(ctx, if (ar) session.ar else session.en, d.text, d.ref.ifBlank { session.ar }, palette(look)) }) {
                        Icon(Icons.Outlined.Share, t("مشاركة كصورة", "Share as image"), tint = look.onCardMuted)
                    }
                    IconButton(onClick = { copyText(ctx, d.text + if (d.ref.isNotBlank()) "\n— ${d.ref}" else "", ar) }) {
                        Icon(Icons.Outlined.ContentCopy, t("نسخ", "Copy"), tint = look.onCardMuted)
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(end = 8.dp)) {
                    VSpace(6.dp)
                    Text(d.text, fontFamily = QuranFont, fontSize = if (d.text.length > 700) 20.sp else 23.sp, lineHeight = if (d.text.length > 700) 38.sp else 44.sp, color = look.onCard, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    if (d.virtue.isNotBlank()) {
                        VSpace(10.dp)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(look.cta.copy(alpha = .12f)).padding(12.dp)) {
                            Text(t("الفضل", "Virtue"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                            Text(d.virtue, style = MaterialTheme.typography.bodyMedium, color = look.onCard)
                        }
                    }
                    if (d.ref.isNotBlank()) {
                        VSpace(8.dp)
                        Muted(t("المصدر: ", "Source: ") + d.ref)
                    }
                    if (d.sura > 0) {
                        VSpace(6.dp)
                        Chip(t("افتح في المصحف", "Open in the mushaf"), leading = { PackIcon(Ico.QURAN, 18.dp) }, onClick = { onOpen("reader/${d.sura}/${d.ayah}") })
                    }
                }
            }
        }
        // Counter button
        val page = pager.currentPage
        if (page < items.size) {
            val d = items[page]
            val prog by animateFloatAsState(done[page] / d.count.toFloat(), label = "c")
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostPill(t("السابق", "Back"), Modifier.width(96.dp)) { scope.launch { pager.animateScrollToPage((page - 1).coerceAtLeast(0)) } }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(104.dp).clip(CircleShape).surface(CircleShape, strong = true).press { count(page) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                        val w = 7.dp.toPx()
                        drawArc(look.onCardMuted.copy(alpha = .2f), 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                        if (prog > 0f) drawArc(look.cta, -90f, 360f * prog, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
                    }
                    val left = d.count - done[page]
                    if (left <= 0) Icon(Icons.Outlined.CheckCircle, null, tint = look.cta, modifier = Modifier.size(44.dp))
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num(left, ar), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = look.onCard)
                        Muted(t("متبقٍ", "left"))
                    }
                }
                Spacer(Modifier.weight(1f))
                GhostPill(t("التالي", "Next"), Modifier.width(96.dp)) { scope.launch { pager.animateScrollToPage(page + 1) } }
            }
        } else VSpace(12.dp)
    }
}

@Composable
private fun CompletionPage(session: Session, user: UserData, ar: Boolean, onBack: () -> Unit, onRestart: () -> Unit, onOpen: (String) -> Unit) {
    val look = LocalSakina.current.look
    val today = LocalDate.now()
    SCard(Modifier.fillMaxSize(), strong = true) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PackIcon(Ico.DONE, 96.dp)
            VSpace(14.dp)
            Text(t("تقبّل الله منك", "May Allah accept from you"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            VSpace(6.dp)
            Muted(digits(t("أتممت ${session.ar} — ${session.items.size} ذكرًا", "You completed ${session.en} — ${session.items.size} adhkar"), ar))
            VSpace(18.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Badge(digits(t("السلسلة ${user.azkarStreak(today)} يوم", "Streak ${user.azkarStreak(today)} days"), ar), look.cta.copy(alpha = .2f), look.onCard, dot = look.cta)
                Badge(digits(t("${user.azkarDaysInWeek(today)}/7 هذا الأسبوع", "${user.azkarDaysInWeek(today)}/7 this week"), ar), look.cta.copy(alpha = .2f), look.onCard)
            }
            VSpace(24.dp)
            CtaPill(t("تم", "Done"), Modifier.fillMaxWidth(.7f), onClick = onBack)
            VSpace(10.dp)
            GhostPill(t("من البداية", "Start over"), Modifier.fillMaxWidth(.7f), onClick = onRestart)
            VSpace(10.dp)
            GhostPill(t("السبحة", "Open tasbih"), Modifier.fillMaxWidth(.7f)) { onOpen("tasbih") }
        }
    }
}

// ─────────────────────────────────────────────── Hisn al-Muslim index
@Composable
fun HisnScreen(initialCat: String?, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val azkar by rememberAzkar()
    var cat by rememberSaveable { mutableStateOf(initialCat?.let { c -> AzCat.entries.firstOrNull { it.name == c }?.name }) }
    var q by rememberSaveable { mutableStateOf("") }
    val a = azkar
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BackHeader(t("حصن المسلم", "Hisn al-Muslim"), onBack, subtitle = if (a == null) null else digits(t("${a.chapters.size} بابًا • ${a.chapters.sumOf { it.items.size }} ذكرًا ودعاءً", "${a.chapters.size} chapters • ${a.chapters.sumOf { it.items.size }} adhkar & duas"), ar))
        }
        item { SearchField(q, { q = it }, t("ابحث في عناوين الأبواب", "Search chapter titles")) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(t("الكل", "All"), selected = cat == null, onClick = { cat = null }) }
                items(AzCat.entries) { c -> Chip(if (ar) c.ar else c.en, selected = cat == c.name, leading = { PackIcon(c.ico, 18.dp) }, onClick = { cat = c.name }) }
            }
        }
        if (a == null) { item { Loading() }; return@LazyColumn }
        val n = app.sakina.quran.ui.tabs.normalize(q.trim())
        val list = a.chapters.filter { c -> (cat == null || AzCat.of(c.index)?.name == cat) && (n.isEmpty() || app.sakina.quran.ui.tabs.normalize(c.title).contains(n)) }
        if (list.isEmpty()) item { Muted(t("لا نتائج", "No results"), Modifier.fillMaxWidth().padding(20.dp)) }
        items(list, key = { it.index }) { c ->
            SCard(onClick = { onOpen("azkar/h${c.index}") }, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).border(1.5.dp, look.cta.copy(alpha = .6f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(num(c.index + 1, ar), style = MaterialTheme.typography.labelLarge, color = look.cta, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Muted(digits(t("${c.items.size} • ${AzCat.of(c.index)?.ar ?: ""}", "${c.items.size} • ${AzCat.of(c.index)?.en ?: ""}"), ar))
                    }
                    AzCat.of(c.index)?.let { PackIcon(it.ico, 28.dp) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────── Search
@Composable
fun AzkarSearchScreen(initial: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val azkar by rememberAzkar()
    var q by rememberSaveable { mutableStateOf(initial) }
    val results = remember(azkar, q) { azkar?.search(q) ?: emptyList() }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("البحث في الأذكار", "Search azkar"), onBack, subtitle = t("في حصن المسلم كاملًا وأذكار الصباح والمساء", "Across all of Hisn al-Muslim and the morning/evening set")) }
        item { SearchField(q, { q = it }, t("مثال: الكرب، السفر، المطر", "Arabic word, e.g. السفر")) }
        if (q.trim().length >= 2) item { Muted(digits(t("${results.size} نتيجة", "${results.size} results"), ar)) }
        items(results, key = { it.first.id }) { (d, where) ->
            SCard(onClick = { onOpen(openRouteFor(azkar!!, d)) }, padding = PaddingValues(12.dp)) {
                Text(where, style = MaterialTheme.typography.labelLarge, color = look.cta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                VSpace(4.dp)
                Text(d.text, fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (q.trim().length < 2) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Muted(t("اقتراحات", "Suggestions"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("الكرب", "السفر", "المطر", "الطعام", "المريض").forEach { s -> Chip(s, onClick = { q = s }) }
                }
            }
        }
    }
}

/** Route that opens a dhikr inside its session at the right position ("azkar/<sid>@<index>"). */
fun openRouteFor(a: Azkar, d: Dhikr): String = when {
    d.id.startsWith("h") -> { val c = d.chapter; "azkar/h$c@${d.id.substringAfter('.').toIntOrNull() ?: 0}" }
    d.id.startsWith("m") -> { val m = a.morning.indexOfFirst { it.id == d.id }; if (m >= 0) "azkar/morning@$m" else "azkar/evening@${a.evening.indexOfFirst { it.id == d.id }.coerceAtLeast(0)}" }
    d.sura > 0 -> "reader/${d.sura}/${d.ayah}"
    else -> "azkar_mine"
}

// ─────────────────────────────────────────────── Favourites + own azkar
@Composable
fun AzkarMineScreen(user: UserData, onBack: () -> Unit, onOpen: (String) -> Unit, onFav: (String) -> Unit, onAdd: (String, Int) -> Unit, onDelete: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val azkar by rememberAzkar()
    val quran by rememberQuran()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var cnt by rememberSaveable { mutableIntStateOf(3) }
    val favs = remember<List<Dhikr>>(azkar, quran, user.azkarFav) {
        val a = azkar ?: return@remember emptyList()
        user.azkarFav.mapNotNull { id -> a.dhikr(id) ?: quran?.let { Azkar.quranDhikr(it, id) } }
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("مفضلتي وأذكاري", "Favourites & my azkar"), onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("المفضلة", "Favourites") + " " + num(user.azkarFav.size, ar), selected = tab == 0, onClick = { tab = 0 })
                Chip(t("أذكاري الخاصة", "My own") + " " + num(user.ownAzkar.size, ar), selected = tab == 1, onClick = { tab = 1 })
            }
        }
        if (tab == 0) {
            if (favs.isNotEmpty()) item { CtaPill(t("اقرأها كلها بالعدّاد", "Read all with the counter"), Modifier.fillMaxWidth()) { onOpen("azkar/fav") } }
            else item { SCard { Muted(t("اضغط ♡ على أي ذكر أو دعاء ليظهر هنا.", "Tap ♡ on any dhikr or dua to keep it here.")) } }
            items(favs, key = { it.id }) { d ->
                SCard(onClick = { azkar?.let { onOpen(openRouteFor(it, d)) } }, padding = PaddingValues(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(d.text, fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (d.ref.isNotBlank()) Muted(d.ref, Modifier.padding(top = 2.dp))
                        }
                        IconButton(onClick = { onFav(d.id) }) { Icon(Icons.Outlined.Favorite, t("إزالة", "Remove"), tint = androidx.compose.ui.graphics.Color(0xFFEF4444)) }
                    }
                }
            }
        } else {
            item {
                SCard {
                    Text(t("أضف ذكرًا خاصًا", "Add your own dhikr"), style = MaterialTheme.typography.titleMedium)
                    VSpace(8.dp)
                    SearchField(text, { text = it.take(400) }, t("اكتب الذكر أو الدعاء…", "Type the dhikr or dua…"))
                    VSpace(8.dp)
                    Muted(t("عدد المرات", "Repeat"))
                    VSpace(4.dp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(1, 3, 7, 10, 33, 100)) { n -> Chip(num(n, ar), selected = cnt == n, onClick = { cnt = n }) }
                    }
                    VSpace(10.dp)
                    CtaPill(t("إضافة", "Add"), Modifier.fillMaxWidth(), enabled = text.isNotBlank()) { onAdd(text, cnt); text = "" }
                }
            }
            if (user.ownAzkar.isNotEmpty()) item { CtaPill(t("اقرأ أذكاري بالعدّاد", "Read mine with the counter"), Modifier.fillMaxWidth()) { onOpen("azkar/custom") } }
            itemsIndexed(user.ownAzkar, key = { _, o -> o.id }) { _, o ->
                SCard(padding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.text, fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp)
                            Muted(countLabel(o.count, ar))
                        }
                        IconButton(onClick = { onDelete(o.id) }) { Icon(Icons.Outlined.Delete, t("حذف", "Delete"), tint = look.onCardMuted) }
                    }
                }
            }
        }
    }
}
