package app.sakina.quran.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.data.UserData
import app.sakina.quran.quran.AyahRef
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Sura
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.tabs.Feature
import app.sakina.quran.ui.tabs.TabTitle
import app.sakina.quran.ui.tabs.feature
import app.sakina.quran.ui.tabs.rememberFeatureOpener
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.MenuBook

@Composable
fun rememberQuran(): Quran? {
    val ctx = LocalContext.current
    val q by produceState<Quran?>(null) { value = runCatching { Quran.load(ctx) }.getOrNull() }
    return q
}

@Composable
private fun Loading() = Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalSakina.current.look.cta) }

/** Arabic name in the Quran font, always right-to-left. */
@Composable
private fun Rtl(content: @Composable () -> Unit) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)

/** Ornate end-of-ayah marker with Arabic-Indic number: ﴿١٢﴾. */
fun ayahMark(n: Int) = "\uFD3F${num(n, true)}\uFD3E"

@Composable
private fun SuraBadge(n: Int) {
    val look = LocalSakina.current.look
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(look.cta.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Text(num(n, isArabic()), style = MaterialTheme.typography.labelLarge, color = look.cta, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuranTab(user: UserData, onOpen: (String) -> Unit) {
    val quran = rememberQuran()
    val qu = rememberQUser()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val open = rememberFeatureOpener(onOpen)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TabTitle(t("القرآن الكريم", "The Holy Quran")) {
                Box(Modifier.size(42.dp).surface(CircleShape).press { onOpen("quran_search") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Search, t("بحث", "Search"), tint = look.onCard)
                }
            }
        }
        if (quran == null) { item { Loading() }; return@LazyColumn }
        // Continue reading
        item {
            val s = user.lastSura.takeIf { it in 1..114 } ?: 1
            val a = if (user.lastSura in 1..114) user.lastAyah.coerceIn(1, quran.suras[s - 1].ayas) else 1
            val sura = quran.suras[s - 1]
            SCard(onClick = { onOpen("reader/$s/$a") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Muted(if (user.lastSura in 1..114) t("تابع القراءة", "Continue reading") else t("ابدأ القراءة", "Start reading"))
                        Text(if (ar) "سورة ${sura.name}" else sura.tname, style = MaterialTheme.typography.titleLarge)
                        Muted(t("الآية ${num(a, true)} • الصفحة ${num(quran.page(s, a), true)} • الجزء ${num(quran.juz(s, a), true)}", "Ayah $a • Page ${quran.page(s, a)} • Juz ${quran.juz(s, a)}"))
                        VSpace(10.dp)
                        val progress = (quran.global(s, a) + 1f) / quran.text.size
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .2f))) {
                            Box(Modifier.fillMaxWidth(progress.coerceIn(.01f, 1f)).height(6.dp).clip(CircleShape).background(look.cta))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    PackIcon(Ico.QURAN, 64.dp)
                }
            }
        }
        // Mushaf + today's wird (QR02 / QR29)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SCard(Modifier.weight(1f), onClick = { onOpen("mushaf/${qu.prefs.lastPage}") }, padding = PaddingValues(14.dp)) {
                    PackIcon(Ico.BOOKMARK, 34.dp); VSpace(6.dp)
                    Text(t("المصحف", "Mushaf"), style = MaterialTheme.typography.titleMedium)
                    Muted(t("صفحة ${num(qu.prefs.lastPage, true)} من ٦٠٤", "Page ${qu.prefs.lastPage} of 604"))
                }
                val k = qu.active
                SCard(Modifier.weight(1f), onClick = { onOpen("khatma") }, padding = PaddingValues(14.dp)) {
                    PackIcon(Ico.KHATMA, 34.dp); VSpace(6.dp)
                    Text(t("الورد اليومي", "Daily wird"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    if (k == null) Muted(t("ابدأ خطة ختمة", "Start a khatma plan"))
                    else {
                        val today = java.time.LocalDate.now()
                        val left = app.sakina.quran.quran.KhatmaPlan.todayLeft(k, today)
                        Muted(if (app.sakina.quran.quran.KhatmaPlan.isDone(k)) t("أتممت الختمة", "Khatma complete ✓") else if (left == 0) t("أتممت ورد اليوم", "Today's wird done ✓") else t("بقي ${num(left, true)} صفحة اليوم", "$left pages left today"))
                        VSpace(6.dp)
                        Bar(app.sakina.quran.quran.KhatmaPlan.done(k).toFloat() / app.sakina.quran.quran.KhatmaPlan.total(k))
                    }
                }
            }
        }
        // Quick tools
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tools = listOf("quran_search", "tafsir", "translations", "tajweed", "quran_topics", "quran_marks", "share_ayah", "quran_stats", "reciters", "hifz")
                items(tools.mapNotNull { feature(it) }) { f -> Chip(t(f.ar, f.en), leading = { PackIcon(f.ico, 18.dp) }, onClick = { open(f) }) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("السور", "Surahs"), selected = tab == 0, onClick = { tab = 0 })
                Chip(t("الأجزاء", "Juz"), selected = tab == 1, onClick = { tab = 1 })
                Chip(t("العلامات", "Bookmarks") + " " + num(user.quranBookmarks.size, ar), selected = tab == 2, onClick = { tab = 2 })
            }
        }
        when (tab) {
            0 -> items(quran.suras, key = { it.index }) { s -> SuraRow(s, ar, { quran.page(it.index, 1) }) { onOpen("reader/${s.index}/1") } }
            1 -> items((1..30).toList(), key = { "j$it" }) { j ->
                val start = quran.juzStart(j)
                SCard(onClick = { onOpen("reader/${start.first}/${start.second}") }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuraBadge(j); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t("الجزء ${num(j, true)}", "Juz $j"), style = MaterialTheme.typography.titleMedium)
                            val s = quran.suras[start.first - 1]
                            Muted(t("يبدأ من ${s.name} ${num(start.second, true)}", "Starts at ${s.tname} ${start.second}"))
                        }
                    }
                }
            }
            else -> {
                val marks = user.quranBookmarks.mapNotNull { k -> val p = k.split(':'); p.getOrNull(0)?.toIntOrNull()?.let { s -> p.getOrNull(1)?.toIntOrNull()?.let { s to it } } }
                    .filter { it.first in 1..114 }.sortedWith(compareBy({ it.first }, { it.second }))
                if (marks.isEmpty()) item {
                    SCard { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PackIcon(Ico.BOOKMARK, 56.dp); VSpace(8.dp)
                        Text(t("لا علامات بعد", "No bookmarks yet"), style = MaterialTheme.typography.titleMedium)
                        Muted(t("اضغط رمز العلامة بجوار أي آية لحفظها هنا.", "Tap the bookmark icon next to any ayah to save it here."))
                    } }
                } else items(marks, key = { "m${it.first}:${it.second}" }) { (s, a) ->
                    SCard(onClick = { onOpen("reader/$s/$a") }, padding = PaddingValues(12.dp)) {
                        Text(t("${quran.suras[s - 1].name} • ${num(a, true)}", "${quran.suras[s - 1].tname} • $a"), style = MaterialTheme.typography.titleSmall)
                        VSpace(4.dp)
                        Rtl { Text(quran.ayah(s, a), fontFamily = QuranFont, fontSize = 20.sp, lineHeight = 38.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
        item {
            Muted(
                t("نص المصحف: مشروع تنزيل (tanzil.net) دون أي تعديل", "Quran text: Tanzil Project (tanzil.net), unaltered") +
                    if (quran.verified) t(" • تم التحقق من سلامة النص (MD5)", " • ✓ text integrity verified (MD5)") else t(" • ⚠ تعذّر التحقق من سلامة النص", " • ⚠ text integrity check failed"),
                Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

@Composable
private fun SuraRow(s: Sura, ar: Boolean, quranPage: (Sura) -> Int, onClick: () -> Unit) {
    SCard(onClick = onClick, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SuraBadge(s.index); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (ar) s.name else s.tname, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Muted(
                    if (ar) "${if (s.meccan) "مكية" else "مدنية"} • ${num(s.ayas, true)} آية"
                    else "${s.ename} • ${if (s.meccan) "Makki" else "Madani"} • ${s.ayas} ayat",
                )
            }
            Spacer(Modifier.width(10.dp))
            if (ar) Muted("ص ${num(quranPage(s), true)}") else Text(s.name, fontFamily = QuranFont, fontSize = 22.sp, color = LocalSakina.current.look.onCard)
        }
    }
}

private fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("ayah", text))
}

@OptIn(FlowPreview::class)
@Composable
fun QuranReader(sura: Int, startAyah: Int, user: UserData, onBack: () -> Unit, onSaveLast: (Int, Int) -> Unit, onBookmark: (Int, Int) -> Unit, onOpenSura: (Int) -> Unit, onOpen: (String) -> Unit = {}) {
    val quran = rememberQuran()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val qu = rememberQUser()
    val store = LocalQStore.current
    val prefs = qu.prefs
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val simple = rememberSimple(prefs.script == 1)
    val trIds = prefs.activeTranslations(ar).filter { app.sakina.quran.quran.Editions.available(ctx, it) }
    val tr = trIds.map { id -> id to rememberEdition(id) }
    val translit = rememberEdition(if (prefs.translit) "en.transliteration" else null)
    val tafsir = rememberEdition(if (prefs.inlineTafsir) prefs.tafsir else null)
    var sheet by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var scrolling by remember { mutableStateOf(false) }
    var autoBar by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.tafsir) { app.sakina.quran.quran.Editions.prewarm(ctx, listOf(prefs.tafsir)) }
    KeepScreenOn(prefs.keepOn)
    ReadingTimer()
    // QR17: hands-free auto-scroll.
    LaunchedEffect(scrolling, prefs.autoScroll) {
        if (!scrolling) return@LaunchedEffect
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            val moved = list.scrollBy(prefs.autoScroll * 0.35f)
            if (moved == 0f && !list.canScrollForward) { scrolling = false; autoBar = false; break }
        }
    }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("رجوع", "Back"), tint = MaterialTheme.colorScheme.onBackground) }
            Column(Modifier.weight(1f)) {
                val s = quran?.suras?.getOrNull(sura - 1)
                Text(if (s == null) "" else if (ar) "سورة ${s.name}" else s.tname, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                if (quran != null && s != null) {
                    val first = (list.firstVisibleItemIndex).coerceIn(1, s.ayas)
                    Muted(t("الصفحة ${num(quran.page(sura, first), true)} • الجزء ${num(quran.juz(sura, first), true)}", "Page ${quran.page(sura, first)} • Juz ${quran.juz(sura, first)}"))
                }
            }
            IconButton(onClick = { scope.launch { store.updatePrefs { it.copy(tajweed = !it.tajweed) } } }) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Palette, t("ألوان التجويد", "Tajweed colours"), tint = if (prefs.tajweed) look.cta else MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = { if (autoBar) { autoBar = false; scrolling = false } else { autoBar = true; scrolling = true } }) {
                Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardDoubleArrowDown, t("تمرير تلقائي", "Auto-scroll"), tint = if (autoBar) look.cta else MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = { showSettings = true }) { Icon(androidx.compose.material.icons.Icons.Outlined.Tune, t("إعدادات القراءة", "Reading settings"), tint = MaterialTheme.colorScheme.onBackground) }
            IconButton(onClick = {
                quran?.let { q -> val first = list.firstVisibleItemIndex.coerceIn(1, q.suras[sura - 1].ayas); onOpen("mushaf/${q.page(sura, first)}") }
            }) { Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.MenuBook, t("المصحف", "Mushaf"), tint = MaterialTheme.colorScheme.onBackground) }
        }
        if (quran == null) { Loading(); return@Column }
        val s = quran.suras[sura - 1]
        LaunchedEffect(sura) { if (startAyah > 1) list.scrollToItem(startAyah.coerceIn(1, s.ayas)) }
        LaunchedEffect(sura) {
            snapshotFlow { list.firstVisibleItemIndex }.distinctUntilChanged().debounce(800).collect { i ->
                val a = i.coerceIn(1, s.ayas); onSaveLast(sura, a); runCatching { store.logPage(quran.page(sura, a)) }
            }
        }
        // Bottom room: nav bar + (when the auto-scroll bar is up) its full height, so the last ayah never sits under it.
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(state = list, contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = navBottom + if (autoBar) 104.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SCard(strong = true) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سورة ${s.name}", fontFamily = QuranFont, fontSize = 32.sp, lineHeight = 56.sp, color = look.onCard)
                        Muted(if (ar) "${if (s.meccan) "مكية" else "مدنية"} • ${num(s.ayas, true)} آية • ترتيبها ${num(s.index, true)}" else "${s.tname} · ${s.ename} · ${if (s.meccan) "Makki" else "Madani"} · ${s.ayas} ayat")
                        VSpace(6.dp)
                        Chip(t("عن السورة وفضلها", "About this surah"), leading = { PackIcon(Ico.IDEA, 18.dp) }, onClick = { onOpen("surah_info/${s.index}") })
                        if (s.index != 1 && s.index != 9) {
                            VSpace(10.dp)
                            Text(quran.basmala, fontFamily = QuranFont, fontSize = (prefs.fontSize + 2).sp, lineHeight = ((prefs.fontSize + 2) * 1.9).sp, color = look.onCard, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            items(s.ayas, key = { it }) { i ->
                val a = i + 1
                val g = quran.global(sura, a)
                val txt = app.sakina.quran.quran.MushafLayout.ayahText(quran, sura, a, if (prefs.script == 1) simple else null)
                val marked = "$sura:$a" in user.quranBookmarks
                val isLast = user.lastSura == sura && user.lastAyah == a
                val hl = qu.highlights["$sura:$a"]
                val tint = when {
                    hl != null -> androidx.compose.ui.graphics.lerp(look.card, HL_COLORS[hl.coerceIn(0, 4)], if (look.dark) .22f else .35f)
                    isLast -> androidx.compose.ui.graphics.lerp(look.card, look.cta, .16f)
                    else -> null
                }
                SCard(padding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp), tint = tint, onClick = { sheet = a }) {
                    Rtl {
                        Text(
                            ayahText("$txt ${ayahMark(a)}", prefs.tajweed && prefs.script == 0, prefs.palette, look.dark), fontFamily = QuranFont, fontSize = prefs.fontSize.sp, lineHeight = (prefs.fontSize * 1.95).sp,
                            color = look.onCard, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    translit?.getOrNull(g)?.let { tl -> LtrBox { Text(translitText(tl), style = MaterialTheme.typography.bodyMedium, color = look.onCardMuted, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) } }
                    tr.forEach { (id, arr) ->
                        val ed = app.sakina.quran.quran.Editions.get(id)
                        arr?.getOrNull(g)?.let { line ->
                            Text(
                                line, style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (ed?.rtl == true) androidx.compose.ui.text.style.TextDirection.Rtl else androidx.compose.ui.text.style.TextDirection.Ltr),
                                color = look.onCard, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            )
                            if (tr.size > 1) Muted(if (ar) ed?.nameAr.orEmpty() else ed?.nameEn.orEmpty())
                        }
                    }
                    tafsir?.getOrNull(g)?.let { tx ->
                        Text(
                            if (tx.length > 420) tx.take(420) + "…" else tx,
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Rtl),
                            color = look.onCardMuted, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                    }
                    qu.notes["$sura:$a"]?.let { n -> Muted("✎ " + n.text.take(120) + if (n.tags.isNotEmpty()) "  " + n.tags.joinToString(" ") { "#$it" } else "", Modifier.padding(top = 4.dp)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ar) "${s.name} ${num(a, true)}" else "${s.index}:$a", style = MaterialTheme.typography.labelMedium, color = look.onCardMuted, modifier = Modifier.weight(1f))
                        if (g in quran.sajdas) Text("۩", color = look.cta, modifier = Modifier.padding(horizontal = 6.dp))
                        IconButton(onClick = { onBookmark(sura, a) }) {
                            Icon(if (marked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, t("علامة", "Bookmark"), tint = if (marked) look.cta else look.onCardMuted)
                        }
                        IconButton(onClick = { copyText(ctx, "${quran.ayah(sura, a)} [${s.name}: $a]"); Toast.makeText(ctx, if (ar) "تم النسخ" else "Copied", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Outlined.ContentCopy, t("نسخ", "Copy"), tint = look.onCardMuted)
                        }
                        IconButton(onClick = { onOpen("share/$sura/$a/$a") }) { Icon(Icons.Outlined.Share, t("مشاركة", "Share"), tint = look.onCardMuted) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (sura > 1) Chip(t("السورة السابقة", "Previous surah"), onClick = { onOpenSura(sura - 1) })
                    Spacer(Modifier.weight(1f))
                    if (sura < 114) CtaPill(if (ar) "التالية: ${quran.suras[sura].name}" else "Next: ${quran.suras[sura].tname}") { onOpenSura(sura + 1) }
                }
            }
        }
    }
    // Auto-scroll control (QR17): only shown once auto-scroll is started from the top bar, never over idle reading.
    if (autoBar) Row(
        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp).surface(CircleShape, strong = true).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { scrolling = !scrolling }) {
            Icon(if (scrolling) androidx.compose.material.icons.Icons.Outlined.Pause else androidx.compose.material.icons.Icons.Outlined.PlayArrow, t("تمرير تلقائي", "Auto-scroll"), tint = look.cta)
        }
        Text(t("تمرير تلقائي", "Auto-scroll"), style = MaterialTheme.typography.labelLarge, color = look.onCard)
        IconButton(onClick = { scope.launch { store.updatePrefs { it.copy(autoScroll = (it.autoScroll - 1).coerceAtLeast(1)) } } }) { Icon(Icons.Outlined.TextDecrease, t("أبطأ", "Slower"), tint = look.onCardMuted) }
        Text(num(prefs.autoScroll, ar), style = MaterialTheme.typography.titleSmall, color = look.onCard)
        IconButton(onClick = { scope.launch { store.updatePrefs { it.copy(autoScroll = (it.autoScroll + 1).coerceAtMost(10)) } } }) { Icon(Icons.Outlined.TextIncrease, t("أسرع", "Faster"), tint = look.onCardMuted) }
        IconButton(onClick = { scrolling = false; autoBar = false }) { Icon(androidx.compose.material.icons.Icons.Outlined.Close, t("إغلاق", "Close"), tint = look.onCardMuted) }
    }
    }
    sheet?.let { a -> quran?.let { q -> AyahSheet(q, sura, a, onDismiss = { sheet = null }, onOpen = { sheet = null; onOpen(it) }, bookmarked = "$sura:$a" in user.quranBookmarks, onBookmark = { onBookmark(sura, a) }) } }
    if (showSettings) ReaderSettingsSheet(prefs, mushaf = false, onDismiss = { showSettings = false })
}


@Composable
fun QuranSearch(onBack: () -> Unit, onOpen: (String) -> Unit, initial: String = "") {
    val quran = rememberQuran()
    val look = LocalSakina.current.look
    val ar = isArabic()
    var q by rememberSaveable { mutableStateOf(initial) }
    // A Latin-script query can never match the Arabic text, so it searches the translation instead.
    fun latinOnly(x: String) = x.any { it in 'a'..'z' || it in 'A'..'Z' } && x.none { it in '\u0600'..'\u06FF' }
    var scope0 by rememberSaveable { mutableIntStateOf(if (latinOnly(initial)) 1 else 0) }
    LaunchedEffect(q) { if (scope0 == 0 && q.trim().length >= 2 && latinOnly(q)) scope0 = 1 }
    val qu = rememberQUser()
    val ctx = LocalContext.current
    val trId = qu.prefs.activeTranslations(ar).firstOrNull { app.sakina.quran.quran.Editions.available(ctx, it) } ?: "en.sahih"
    val other = rememberEdition(when (scope0) { 1 -> trId; 2 -> qu.prefs.tafsir; else -> null })
    val res = remember(q, quran, scope0, other) {
        when {
            quran == null -> 0 to emptyList<AyahRef>()
            scope0 == 0 -> quran.search(q)
            other == null -> 0 to emptyList()
            else -> searchEdition(quran, other, q)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("رجوع", "Back"), tint = MaterialTheme.colorScheme.onBackground) }
            Text(t("البحث في القرآن", "Search the Quran"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().surface(CircleShape, elevated = false).padding(start = 16.dp, end = 8.dp).height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = look.onCardMuted); Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) Muted(if (scope0 == 1) t("ابحث في الترجمة… مثل: mercy", "Search the translation… e.g. mercy") else t("اكتب كلمة دون تشكيل… مثل: الصبر", "Type an Arabic word… e.g. الصبر"))
                BasicTextField(q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard), cursorBrush = SolidColor(look.cta))
            }
            if (q.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).press { q = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, t("مسح", "Clear"), tint = look.onCardMuted) }
        }
        VSpace(8.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("نص القرآن", "Quran text"), selected = scope0 == 0, onClick = { scope0 = 0 })
            Chip(t("الترجمة", "Translation"), selected = scope0 == 1, onClick = { scope0 = 1 })
            Chip(t("التفسير", "Tafsir"), selected = scope0 == 2, onClick = { scope0 = 2 })
        }
        VSpace(10.dp)
        if (quran == null) { Loading(); return@Column }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (q.trim().length < 2) {
                item { SectionHeader(t("جرّب", "Try")) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("الصبر", "الرحمة", "الجنة", "التوبة", "نور", "الشكر")) { w -> Chip(w, onClick = { q = w }) }
                    }
                }
            } else {
                item { Muted(t("${num(res.first, true)} موضعًا", "${res.first} matches") + if (res.first > res.second.size) t(" (أول ${num(res.second.size, true)})", " (first ${res.second.size})") else "") }
                items(res.second, key = { "${it.sura}:${it.ayah}" }) { r ->
                    val s = quran.suras[r.sura - 1]
                    SCard(onClick = { onOpen("reader/${r.sura}/${r.ayah}") }, padding = PaddingValues(12.dp)) {
                        Text(if (ar) "${s.name} • ${num(r.ayah, true)}" else "${s.tname} ${r.sura}:${r.ayah}", style = MaterialTheme.typography.labelLarge, color = look.cta)
                        VSpace(4.dp)
                        if (scope0 == 0) Rtl { Text(r.text, fontFamily = QuranFont, fontSize = 21.sp, lineHeight = 40.sp, color = look.onCard, modifier = Modifier.fillMaxWidth(), maxLines = 4, overflow = TextOverflow.Ellipsis) }
                        else {
                            Rtl { Text(glueWaqf(quran.displayAyah(r.sura, r.ayah)), fontFamily = QuranFont, fontSize = 19.sp, lineHeight = 36.sp, color = look.onCard, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                            Text(r.text, style = MaterialTheme.typography.bodyMedium, color = look.onCardMuted, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

/** QR20: search a translation/tafsir edition (diacritic/alef-insensitive, case-insensitive); snippet around the first hit. */
fun searchEdition(q: Quran, arr: Array<String>, query: String, limit: Int = 200): Pair<Int, List<AyahRef>> {
    val n = app.sakina.quran.ui.tabs.normalize(query.trim())
    if (n.length < 2) return 0 to emptyList()
    val out = ArrayList<AyahRef>(); var total = 0
    for (g in arr.indices) {
        val txt = arr[g]; if (txt.isEmpty()) continue
        val nt = app.sakina.quran.ui.tabs.normalize(txt)
        val i = nt.indexOf(n)
        if (i >= 0) {
            total++
            if (out.size < limit) {
                val (s, a) = q.fromGlobal(g)
                val from = (i - 60).coerceAtLeast(0)
                val snippet = (if (from > 0) "…" else "") + nt.substring(from, (i + n.length + 140).coerceAtMost(nt.length))
                out += AyahRef(s, a, if (nt.length == txt.length) txt.substring(from, (i + n.length + 140).coerceAtMost(txt.length)).let { (if (from > 0) "…" else "") + it } else snippet)
            }
        }
    }
    return total to out
}
