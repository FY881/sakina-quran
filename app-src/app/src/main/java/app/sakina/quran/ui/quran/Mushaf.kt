package app.sakina.quran.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sakina.quran.data.ReaderPrefs
import app.sakina.quran.quran.Editions
import app.sakina.quran.quran.MushafLayout
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Seg
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.launch

/** Paper colours of the mushaf page. */
data class Paper(val bg: Color, val text: Color, val accent: Color, val muted: Color, val dark: Boolean)

@Composable
fun paperFor(p: ReaderPrefs): Paper {
    val l = LocalSakina.current.look
    return when (p.paper) {
        1 -> Paper(Color(0xFFF7EFDD), Color(0xFF2A2118), Color(0xFF8B5E1A), Color(0xFF7A6A55), false)
        2 -> Paper(Color(0xFF050505), Color(0xFFE9E3D2), Color(0xFFC9A45C), Color(0xFF9B9384), true)
        else -> Paper(l.card.copy(alpha = 1f), l.onCard, l.cta, l.onCardMuted, l.dark)
    }
}

/** Range of one ayah inside the page's annotated string. */
private data class AyahRange(val start: Int, val end: Int, val g: Int)

/** A highlighted ayah: drawn behind the text as clean rounded per-line spans (never as span backgrounds). */
private data class HlRange(val start: Int, val end: Int, val color: Color, val selected: Boolean, val playing: Boolean = false)

private class PageText(val text: AnnotatedString, val ayat: List<AyahRange>, val headers: List<IntRange>, val hls: List<HlRange>)

private fun buildPage(
    q: Quran, page: Int, prefs: ReaderPrefs, simple: Array<String>?, paper: Paper, highlights: Map<String, Int>, selected: Int?, playing: Int? = null,
): PageText {
    val segs = MushafLayout.segments(q, page)
    val ayat = ArrayList<AyahRange>(); val headers = ArrayList<IntRange>()
    val center = ParagraphStyle(textAlign = TextAlign.Center, lineHeight = 2.1.em)
    // 2.25em leaves clear room above each line for waqf marks and stacked harakat.
    val body = ParagraphStyle(textAlign = TextAlign.Justify, lineHeight = 2.25.em, textDirection = TextDirection.Rtl)
    val hls = ArrayList<HlRange>()
    val text = buildAnnotatedString {
        var i = 0
        while (i < segs.size) {
            when (val s = segs[i]) {
                is Seg.Header -> {
                    val st = length
                    withStyle(center) { withStyle(SpanStyle(color = paper.accent, fontSize = 1.15.em)) { append("سورة ${q.suras[s.sura - 1].name}") } }
                    headers += st until length; i++
                }
                is Seg.Basmala -> { withStyle(center) { append(q.basmala) }; i++ }
                is Seg.Ayah -> {
                    withStyle(body) {
                        while (i < segs.size && segs[i] is Seg.Ayah) {
                            val a = segs[i] as Seg.Ayah
                            val st = length
                            val txt = MushafLayout.ayahText(q, a.sura, a.ayah, if (prefs.script == 1) simple else null)
                            append(ayahText(txt, prefs.tajweed && prefs.script == 0, prefs.palette, paper.dark))
                            append(" ")
                            withStyle(SpanStyle(color = paper.accent)) { append(ayahMark(a.ayah)) }
                            val end = length
                            append(" ")
                            val hl = highlights["${a.sura}:${a.ayah}"]
                            if (a.g == playing) hls += HlRange(st, end, playingColor(paper.dark), selected = false, playing = true)
                            else if (a.g == selected) hls += HlRange(st, end, paper.accent, true)
                            else if (hl != null) hls += HlRange(st, end, HL_COLORS[hl.coerceIn(0, 4)], false)
                            ayat += AyahRange(st, end, a.g)
                            i++
                        }
                    }
                }
            }
        }
    }
    return PageText(text, ayat, headers, hls)
}

/** 604-page mushaf (QR02) with Madinah page boundaries, tajweed colours, highlights and ayah actions. */
@Composable
fun MushafScreen(startPage: Int, khatmaId: String?, onBack: () -> Unit, onOpen: (String) -> Unit, onSaveLast: (Int, Int) -> Unit) {
    val quran = rememberQuran()
    val qu = rememberQUser()
    val store = LocalQStore.current
    val prefs = qu.prefs
    val ar = isArabic()
    val look = LocalSakina.current.look
    val paper = paperFor(prefs)
    val simple = rememberSimple(prefs.script == 1)
    KeepScreenOn(prefs.keepOn)
    ReadingTimer()
    val ctxW = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(prefs.tafsir) { Editions.prewarm(ctxW, listOf(prefs.tafsir) + prefs.activeTranslations(ar)) }
    val pager = rememberPagerState(initialPage = (startPage.coerceIn(1, 604)) - 1) { 604 }
    val scope = rememberCoroutineScope()
    var chrome by remember { mutableStateOf(true) }
    var sheetAyah by remember { mutableStateOf<Int?>(null) }
    var showIndex by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // AU1: the ayah being recited is highlighted; with "follow" on, the page turns with the recitation.
    val playingG = rememberPlayingAyah(quran)
    val follow = rememberAudioFollow()
    LaunchedEffect(playingG, follow) {
        val q = quran ?: return@LaunchedEffect
        val g = playingG ?: return@LaunchedEffect
        if (!follow) return@LaunchedEffect
        val (ps, pa) = q.fromGlobal(g)
        val target = q.page(ps, pa).coerceIn(1, 604) - 1
        if (target != pager.currentPage && !pager.isScrollInProgress) runCatching { pager.animateScrollToPage(target) }
    }
    LaunchedEffect(pager.settledPage, quran) {
        val p = pager.settledPage + 1
        runCatching { store.logPage(p, khatmaId) }
        runCatching { store.updatePrefs { it.copy(lastPage = p) } }
        quran?.let { q -> val (s, a) = q.fromGlobal(q.pageStart(p)); onSaveLast(s, a) }
    }
    Column(Modifier.fillMaxSize().background(if (prefs.paper == 0) Color.Transparent else paper.bg)) {
        if (chrome) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("رجوع", "Back"), tint = if (prefs.paper == 0) MaterialTheme.colorScheme.onBackground else paper.text) }
                Column(Modifier.weight(1f)) {
                    val p = pager.currentPage + 1
                    if (quran != null) {
                        val s = MushafLayout.surasOn(quran, p).first()
                        Text(if (ar) "سورة ${quran.suras[s - 1].name}" else quran.suras[s - 1].tname, style = MaterialTheme.typography.titleMedium, color = if (prefs.paper == 0) MaterialTheme.colorScheme.onBackground else paper.text, maxLines = 1)
                        val g = quran.pageStart(p)
                        val rub = quran.quarterOfGlobal(g)
                        Text(
                            t("الجزء ${num(quran.juzOfGlobal(g), true)} • الحزب ${num((rub - 1) / 4 + 1, true)} • الصفحة ${num(p, true)}", "Juz ${quran.juzOfGlobal(g)} • Hizb ${(rub - 1) / 4 + 1} • Page $p"),
                            style = MaterialTheme.typography.bodySmall, color = paper.muted, maxLines = 1,
                        )
                    }
                }
                val tint = if (prefs.paper == 0) MaterialTheme.colorScheme.onBackground else paper.text
                IconButton(onClick = { scope.launch { store.updatePrefs { it.copy(tajweed = !it.tajweed) } } }) {
                    Icon(Icons.Outlined.Palette, t("ألوان التجويد", "Tajweed colours"), tint = if (prefs.tajweed) paper.accent else tint)
                }
                IconButton(onClick = { showIndex = true }) { Icon(Icons.Outlined.FormatListNumbered, t("الفهرس", "Index"), tint = tint) }
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Outlined.Tune, t("إعدادات القراءة", "Reading settings"), tint = tint) }
                IconButton(onClick = {
                    quran?.let { q -> val (s, a) = q.fromGlobal(q.pageStart(pager.currentPage + 1)); onOpen("reader/$s/$a") }
                }) { Icon(Icons.AutoMirrored.Outlined.ViewList, t("عرض الآيات", "Verse view"), tint = tint) }
            }
        } else Spacer(Modifier.statusBarsPadding())
        if (quran == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = look.cta) }; return@Column }
        RtlBox {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1, key = { it }) { idx ->
                MushafPage(
                    quran, idx + 1, prefs, simple, paper, qu.highlights, sheetAyah, playingG,
                    onTap = { g -> if (g == null) chrome = !chrome else sheetAyah = g },
                )
            }
        }
        // Footer: khatma wird status or page hint.
        val k = qu.khatmas.firstOrNull { it.id == khatmaId }
        if (chrome) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (k != null) {
                val today = java.time.LocalDate.now()
                val r = app.sakina.quran.quran.KhatmaPlan.todayRange(k, today)
                val left = app.sakina.quran.quran.KhatmaPlan.todayLeft(k, today)
                Text(
                    if (left == 0) t("أتممت ورد اليوم — ${k.name}", "Today's wird complete ✓ — ${k.name}")
                    else t("ورد اليوم: ص ${num(r.first, true)}–${num(r.last, true)} • بقي ${num(left, true)}", "Wird: p. ${r.first}–${r.last} • $left left"),
                    style = MaterialTheme.typography.labelLarge, color = paper.accent, modifier = Modifier.weight(1f), maxLines = 1,
                )
            } else Text(
                t("اضغط على آية لخياراتها، وعلى الهامش لإخفاء الأدوات", "Tap an ayah for options, the margin to hide controls"),
                style = MaterialTheme.typography.bodySmall, color = app.sakina.quran.ui.theme.readableOn(paper.muted, paper.bg), modifier = Modifier.weight(1f),
            )
        }
    }
    sheetAyah?.let { g -> quran?.let { q -> val (s, a) = q.fromGlobal(g); AyahSheet(q, s, a, onDismiss = { sheetAyah = null }, onOpen = { sheetAyah = null; onOpen(it) }) } }
    if (showIndex && quran != null) MushafIndexSheet(quran, pager.currentPage + 1, onDismiss = { showIndex = false }) { p -> showIndex = false; scope.launch { pager.scrollToPage(p - 1) } }
    if (showSettings) ReaderSettingsSheet(prefs, mushaf = true, onDismiss = { showSettings = false })
}

@Composable
private fun MushafPage(q: Quran, page: Int, prefs: ReaderPrefs, simple: Array<String>?, paper: Paper, highlights: Map<String, Int>, selected: Int?, playing: Int?, onTap: (Int?) -> Unit) {
    // Only pages that hold the playing ayah rebuild when it moves.
    val playHere = playing?.takeIf { g -> val (ps, pa) = q.fromGlobal(g); q.page(ps, pa) == page }
    val pt = remember(page, prefs.script, prefs.tajweed, prefs.palette, simple, paper, highlights, selected, playHere) { buildPage(q, page, prefs, simple, paper, highlights, selected, playHere) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(paper.bg)
            .border(1.dp, paper.accent.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val g0 = q.pageStart(page)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(MushafLayout.surasOn(q, page).joinToString(" • ") { q.suras[it - 1].name }, fontFamily = QuranFont, fontSize = 15.sp, color = paper.muted, modifier = Modifier.weight(1f), maxLines = 1)
            Text("الجزء ${num(q.juzOfGlobal(g0), true)}", fontFamily = QuranFont, fontSize = 15.sp, color = paper.muted)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val wPx = constraints.maxWidth; val hPx = constraints.maxHeight
            val base = TextStyle(fontFamily = QuranFont, color = paper.text, textDirection = TextDirection.Rtl, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
            val maxSize = (prefs.fontSize + 6).coerceIn(20, 40).toFloat()
            val fit = remember(pt, wPx, hPx, maxSize) {
                var lo = 11f; var hi = maxSize; var best = lo
                repeat(9) {
                    val mid = (lo + hi) / 2
                    val r = measurer.measure(pt.text, base.copy(fontSize = mid.sp), constraints = Constraints(maxWidth = wPx.coerceAtLeast(1)), density = density)
                    if (r.size.height <= hPx) { best = mid; lo = mid } else hi = mid
                }
                best
            }
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            val frame = paper.accent.copy(alpha = .55f)
            Text(
                pt.text, style = base.copy(fontSize = fit.sp),
                onTextLayout = { layout = it },
                modifier = Modifier.fillMaxWidth()
                    .drawBehind {
                        val l = layout ?: return@drawBehind
                        // Highlights: one rounded pill per line the ayah covers, inset inside the line box so
                        // neighbouring lines never touch; soft fill + thin edge keeps tajweed colours readable.
                        val n = l.layoutInput.text.length
                        for (h in pt.hls) {
                            if (h.start >= n) continue
                            val e = h.end.coerceAtMost(n)
                            val first = l.getLineForOffset(h.start); val last = l.getLineForOffset((e - 1).coerceAtLeast(h.start))
                            for (line in first..last) {
                                val ls = l.getLineStart(line); val le = l.getLineEnd(line, visibleEnd = true)
                                val a = maxOf(h.start, ls); val b = minOf(e, le)
                                if (b <= a) continue
                                val xa = if (a == ls) l.getLineRight(line) else l.getHorizontalPosition(a, usePrimaryDirection = true)
                                val xb = if (b >= le) l.getLineLeft(line) else l.getHorizontalPosition(b, usePrimaryDirection = true)
                                val left = minOf(xa, xb) - 6f; val right = maxOf(xa, xb) + 6f
                                val top = l.getLineTop(line); val bottom = l.getLineBottom(line); val lh = bottom - top
                                val t0 = top + lh * .14f; val b0 = bottom - lh * .10f
                                val r = CornerRadius(lh * .22f, lh * .22f)
                                val fill = h.color.copy(alpha = when { h.playing -> if (paper.dark) .20f else .16f; h.selected -> if (paper.dark) .16f else .20f; else -> if (paper.dark) .17f else .24f })
                                drawRoundRect(fill, Offset(left.coerceAtLeast(0f), t0), Size((right - left).coerceAtMost(size.width), b0 - t0), r)
                                drawRoundRect(
                                    h.color.copy(alpha = when { h.playing -> .9f; h.selected -> .75f; else -> .45f }), Offset(left.coerceAtLeast(0f), t0), Size((right - left).coerceAtMost(size.width), b0 - t0), r,
                                    style = Stroke(width = when { h.playing -> 2.8f; h.selected -> 2.4f; else -> 1.4f }),
                                )
                            }
                        }
                        for (h in pt.headers) {
                            if (h.first >= l.layoutInput.text.length) continue
                            val line = l.getLineForOffset(h.first)
                            val top = l.getLineTop(line) + 2f; val bottom = l.getLineBottom(line) - 2f
                            drawRoundRect(frame, Offset(4f, top), Size(size.width - 8f, bottom - top), CornerRadius(18f, 18f), style = Stroke(width = 2.2f))
                        }
                    }
                    .pointerInput(pt) {
                        detectTapGestures { pos ->
                            val l = layout
                            if (l == null) { onTap(null); return@detectTapGestures }
                            val line = l.getLineForVerticalPosition(pos.y)
                            if (pos.y > l.getLineBottom(l.lineCount - 1) || pos.x < l.getLineLeft(line) - 8f || pos.x > l.getLineRight(line) + 8f) { onTap(null); return@detectTapGestures }
                            val off = l.getOffsetForPosition(pos)
                            onTap(pt.ayat.firstOrNull { off >= it.start && off <= it.end }?.g)
                        }
                    },
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("﴿ ${num(page, true)} ﴾", fontFamily = QuranFont, fontSize = 15.sp, color = paper.accent)
        }
    }
}

/** Jump sheet: surahs, juz, hizb quarters and a page number box (QR04/QR05). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafIndexSheet(q: Quran, current: Int, onDismiss: () -> Unit, onPage: (Int) -> Unit) {
    val look = LocalSakina.current.look
    val ar = isArabic()
    var tab by remember { mutableIntStateOf(0) }
    var pageText by remember { mutableStateOf("") }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(horizontal = 16.dp)) {
            Text(t("الانتقال في المصحف", "Go to"), style = MaterialTheme.typography.titleLarge)
            VSpace(8.dp)
            Row(Modifier.fillMaxWidth().surface(CircleShape, elevated = false).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (pageText.isEmpty()) Muted(t("رقم الصفحة (١–٦٠٤) — الحالية ${num(current, true)}", "Page number (1–604) — now $current"))
                    BasicTextField(pageText, { v -> pageText = v.filter { it.isDigit() || it in '\u0660'..'\u0669' }.take(3) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard),
                        cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
                }
                val n = pageText.map { if (it in '\u0660'..'\u0669') '0' + (it - '\u0660') else it }.joinToString("").toIntOrNull()
                Chip(t("اذهب", "Go"), selected = n != null && n in 1..604, onClick = { if (n != null && n in 1..604) onPage(n) })
            }
            VSpace(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("السور", "Surahs"), selected = tab == 0, onClick = { tab = 0 })
                Chip(t("الأجزاء", "Juz"), selected = tab == 1, onClick = { tab = 1 })
                Chip(t("الأحزاب", "Hizb"), selected = tab == 2, onClick = { tab = 2 })
            }
            VSpace(8.dp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                when (tab) {
                    0 -> items(q.suras, key = { "s${it.index}" }) { s ->
                        val p = q.page(s.index, 1)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).press { onPage(p) }.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(num(s.index, ar), style = MaterialTheme.typography.labelLarge, color = look.cta, modifier = Modifier.width(36.dp))
                            Text(if (ar) s.name else "${s.tname} · ${s.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                            Muted(t("ص ${num(p, true)}", "p. $p"))
                        }
                    }
                    1 -> items((1..30).toList(), key = { "j$it" }) { j ->
                        val p = q.juzPage(j)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).press { onPage(p) }.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t("الجزء ${num(j, true)}", "Juz $j"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Muted(t("ص ${num(p, true)}", "p. $p"))
                        }
                    }
                    else -> items((1..60).toList(), key = { "h$it" }) { h ->
                        val qn = (h - 1) * 4 + 1
                        val p = q.quarterPage(qn)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).press { onPage(p) }.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t("الحزب ${num(h, true)}", "Hizb $h"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (0..3).forEach { r ->
                                    val pp = q.quarterPage(qn + r)
                                    Box(Modifier.clip(CircleShape).background(look.cta.copy(alpha = .14f)).press { onPage(pp) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(if (r == 0) num(pp, ar) else listOf("¼", "½", "¾")[r - 1], style = MaterialTheme.typography.labelMedium, color = look.cta)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Reading settings shared by the mushaf and the verse view. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsSheet(prefs: ReaderPrefs, mushaf: Boolean, onDismiss: () -> Unit) {
    val look = LocalSakina.current.look
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun set(f: (ReaderPrefs) -> ReaderPrefs) { scope.launch { store.updatePrefs(f) } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { Text(t("إعدادات القراءة", "Reading settings"), style = MaterialTheme.typography.titleLarge) }
            item {
                Muted(t("حجم الخط: ${num(prefs.fontSize, true)}", "Font size: ${prefs.fontSize}"))
                Slider(prefs.fontSize.toFloat(), { v -> set { it.copy(fontSize = v.toInt()) } }, valueRange = 18f..40f, steps = 10,
                    colors = SliderDefaults.colors(thumbColor = look.cta, activeTrackColor = look.cta))
                if (mushaf) Muted(t("في المصحف تُضبط الصفحة لتملأ الشاشة، وهذا هو الحد الأعلى للخط.", "In the mushaf each page is fitted to the screen; this is the maximum size."))
            }
            item {
                Muted(t("الخط", "Script"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("العثماني", "Uthmani"), selected = prefs.script == 0, onClick = { set { it.copy(script = 0) } })
                    Chip(t("الإملائي", "Imlaei (simple)"), selected = prefs.script == 1, onClick = { set { it.copy(script = 1) } })
                }
                if (prefs.script == 1) Muted(t("ألوان التجويد تعمل مع الرسم العثماني فقط.", "Tajweed colours work with the Uthmani script only."))
            }
            item {
                Muted(t("لون الصفحة", "Page colour"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("حسب السمة", "Theme"), selected = prefs.paper == 0, onClick = { set { it.copy(paper = 0) } })
                    Chip(t("ورقي", "Sepia"), selected = prefs.paper == 1, onClick = { set { it.copy(paper = 1) } })
                    Chip(t("ليلي أسود", "Night"), selected = prefs.paper == 2, onClick = { set { it.copy(paper = 2) } })
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("ألوان التجويد", "Tajweed colours"), style = MaterialTheme.typography.bodyLarge)
                        Muted(t("تلوين أحكام التجويد دون أي تغيير في النص", "Colour-codes the rules without changing the text"))
                    }
                    Switch(prefs.tajweed, { v -> set { it.copy(tajweed = v) } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("ألوان كلاسيكية", "Classic"), selected = prefs.palette == 0, onClick = { set { it.copy(palette = 0) } })
                    Chip(t("تباين عالٍ", "High contrast"), selected = prefs.palette == 1, onClick = { set { it.copy(palette = 1) } })
                    Chip(t("مناسبة لعمى الألوان", "Colour-blind safe"), selected = prefs.palette == 2, onClick = { set { it.copy(palette = 2) } })
                }
            }
            if (!mushaf) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("النطق اللاتيني", "Transliteration"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(prefs.translit, { v -> set { it.copy(translit = v) } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("التفسير تحت كل آية", "Tafsir under each ayah"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(prefs.inlineTafsir, { v -> set { it.copy(inlineTafsir = v) } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("إبقاء الشاشة مضاءة", "Keep screen on"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(prefs.keepOn, { v -> set { it.copy(keepOn = v) } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
            }
            item { Muted(t("الترجمات والتفاسير تُختار من «مكتبة التفاسير والترجمات».", "Choose translations and tafsir in the Tafsir & translations library.")) }
        }
    }
}
