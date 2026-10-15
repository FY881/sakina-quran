package app.sakina.quran.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sakina.quran.audio.AudioBus
import app.sakina.quran.audio.AudioErr
import app.sakina.quran.audio.AudioMode
import app.sakina.quran.audio.AudioPrefs
import app.sakina.quran.audio.AudioUiState
import app.sakina.quran.audio.Recitation
import app.sakina.quran.audio.RepeatSpec
import app.sakina.quran.audio.SPEEDS
import app.sakina.quran.audio.SleepSpec
import app.sakina.quran.quran.Quran
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Badge
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.quran.RtlBox
import app.sakina.quran.ui.quran.LtrBox
import app.sakina.quran.ui.quran.rememberQuran
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.delay

@Composable
fun rememberAudio(): AudioUiState = AudioBus.state.collectAsStateWithLifecycle().value

fun mmss(ms: Long, ar: Boolean): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val txt = if (s >= 3600) "%d:%02d:%02d".format(java.util.Locale.ENGLISH, s / 3600, (s / 60) % 60, s % 60) else "%d:%02d".format(java.util.Locale.ENGLISH, s / 60, s % 60)
    return digits(txt, ar)
}

fun reciterName(st: AudioUiState, ar: Boolean) = if (ar) st.reciterAr else st.reciterEn.ifBlank { st.reciterAr }

fun nowTitle(q: Quran?, st: AudioUiState, ar: Boolean): String {
    if (q == null || st.sura !in 1..114) return if (ar) "جارٍ التحضير…" else "Preparing…"
    val su = q.suras[st.sura - 1]
    return if (ar) "سورة ${su.name}" + (if (st.ayah > 0) " • الآية ${num(st.ayah, true)}" else "")
    else su.tname + (if (st.ayah > 0) " • ${st.sura}:${st.ayah}" else "")
}

@Composable
fun errorText(e: AudioErr): String = when (e) {
    AudioErr.OFFLINE -> t("لا يوجد اتصال بالإنترنت. حمّل التلاوة لتستمع دون إنترنت.", "You're offline. Download the recitation to listen without internet.")
    AudioErr.NETWORK -> t("تعذّر الاتصال بخادم التلاوة. تحقّق من الشبكة ثم أعد المحاولة.", "Couldn't reach the recitation server. Check your network and retry.")
    AudioErr.SOURCE -> t("هذا الملف غير متاح الآن من المصدر. جرّب قارئًا آخر.", "This file isn't available from the source right now. Try another reciter.")
    AudioErr.OTHER -> t("تعذّر تشغيل التلاوة.", "Couldn't play the recitation.")
}

/** RTL-aware skip icon: "next" points in reading direction. */
@Composable
private fun DirIcon(v: ImageVector, desc: String, tint: androidx.compose.ui.graphics.Color, size: Int = 30) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Icon(v, desc, tint = tint, modifier = Modifier.size(size.dp).graphicsLayer { scaleX = if (rtl) -1f else 1f })
}

/** Mini player above the bottom bar (or at the bottom of immersive screens). */
@Composable
fun MiniPlayer(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val st = rememberAudio()
    if (!st.active) return
    val q = rememberQuran()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp)).surface(RoundedCornerShape(20.dp), strong = true).press { onOpen("player") }) {
        val frac = when {
            st.mode == AudioMode.SURAH && st.durMs > 0 -> st.posMs.toFloat() / st.durMs
            st.count > 0 -> (st.index + 1f) / st.count
            else -> 0f
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(look.onCardMuted.copy(alpha = .18f))) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(3.dp).background(look.cta))
        }
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                when {
                    st.error != null -> IconButton(onClick = { Recitation.retry(ctx) }) { Icon(Icons.Outlined.ErrorOutline, t("إعادة المحاولة", "Retry"), tint = look.warn) }
                    st.buffering -> CircularProgressIndicator(Modifier.size(24.dp), color = look.cta, strokeWidth = 2.5.dp)
                    else -> IconButton(onClick = { Recitation.toggle(ctx) }) {
                        Icon(if (st.playing || st.inGap) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (st.playing) t("إيقاف مؤقت", "Pause") else t("تشغيل", "Play"), tint = look.cta, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(nowTitle(q, st, ar), style = MaterialTheme.typography.titleSmall, color = look.onCard, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (st.error != null) t("تعذّر التشغيل — اضغط للتفاصيل", "Playback failed — tap for details") else reciterName(st, ar),
                    style = MaterialTheme.typography.bodySmall, color = if (st.error != null) look.warn else look.onCardMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { Recitation.stop(ctx) }) { Icon(Icons.Outlined.Close, t("إيقاف التلاوة", "Stop recitation"), tint = look.onCardMuted) }
        }
    }
}

/** "Continue listening" / "now playing" card on the Quran tab. */
@Composable
fun ContinueListeningCard(q: Quran, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { AudioPrefs.load(ctx) }
    val st = rememberAudio()
    val last by AudioPrefs.last.collectAsStateWithLifecycle()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val l = last
    if (!st.active && l == null) return
    SCard(onClick = { if (st.active) onOpen("player") else if (l != null) { Recitation.resume(ctx, q, l, ar); onOpen("player") } }, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(look.cta.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Icon(if (st.active) Icons.Outlined.GraphicEq else Icons.Outlined.Headphones, null, tint = look.cta)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Muted(if (st.active) t("يُتلى الآن", "Now playing") else t("تابع الاستماع", "Continue listening"))
                if (st.active) {
                    Text(nowTitle(q, st, ar), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Muted(reciterName(st, ar))
                } else if (l != null) {
                    val su = q.suras[l.sura.coerceIn(1, 114) - 1]
                    Text(if (ar) "سورة ${su.name} • الآية ${num(l.ayah, true)}" else "${su.tname} • ${l.sura}:${l.ayah}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Muted(if (l.mode == AudioMode.AYAH) t("آية بآية", "Ayah by ayah") + " • " + app.sakina.quran.audio.AyahVoices.byId(l.who).let { if (ar) it.ar else it.en }
                        else t("السورة كاملة", "Whole surah") + if (l.posMs > 0) " • " + mmss(l.posMs, ar) else "")
                }
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(look.cta), contentAlignment = Alignment.Center) {
                Icon(if (st.active && st.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (st.active && st.playing) t("إيقاف مؤقت", "Pause") else t("تشغيل", "Play"), tint = look.onCta,
                    modifier = Modifier.press { if (st.active) Recitation.toggle(ctx) else if (l != null) Recitation.resume(ctx, q, l, ar) })
            }
        }
    }
}

private fun repeatLabel(r: RepeatSpec, ar: Boolean): String = when {
    r.isPlain -> if (ar) "بدون تكرار" else "No repeat"
    else -> buildList {
        if (r.ayahTimes > 1) add(if (ar) "الآية ×${num(r.ayahTimes, true)}" else "Ayah ×${r.ayahTimes}")
        if (r.infinite) add(if (ar) "المقطع ∞" else "Range ∞") else if (r.rangeTimes > 1) add(if (ar) "المقطع ×${num(r.rangeTimes, true)}" else "Range ×${r.rangeTimes}")
        if (r.gapMs > 0) add(if (ar) "توقف ${num((r.gapMs / 1000).toInt(), true)}ث" else "${r.gapMs / 1000}s gap")
        if (r.ladder) add(if (ar) "تدرّج" else "Ladder")
    }.joinToString(" • ")
}

@Composable
private fun sleepLabel(st: AudioUiState, ar: Boolean): String {
    val s = st.sleep ?: return t("مؤقّت النوم", "Sleep timer")
    return when (s) {
        is SleepSpec.Minutes -> {
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(st.sleepEndsAt) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
            t("نوم بعد ", "Sleep in ") + mmss(st.sleepEndsAt - now, ar)
        }
        SleepSpec.EndOfAyah -> t("نوم بعد هذه الآية", "Sleep after this ayah")
        SleepSpec.EndOfSurah -> t("نوم بعد السورة", "Sleep after the surah")
    }
}

/** Full player (AU06–AU09): ayah text, progress, transport, repeat / sleep / speed sheets, follow-along. */
@Composable
fun PlayerScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val st = rememberAudio()
    val q = rememberQuran()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { AudioPrefs.load(ctx) }
    val prefs by AudioPrefs.p.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(0) } // 1 repeat, 2 sleep, 3 speed
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp).navigationBarsPadding()) {
        BackHeader(t("مشغّل التلاوة", "Recitation player"), onBack, subtitle = if (st.active) (if (st.mode == AudioMode.AYAH) t("آية بآية", "Ayah by ayah") else t("السورة كاملة", "Whole surah")) else null) {
            IconButton(onClick = { onOpen("downloads") }) { Icon(Icons.Outlined.Download, t("التحميلات", "Downloads"), tint = MaterialTheme.colorScheme.onBackground) }
        }
        if (!st.active || q == null) {
            SCard(Modifier.padding(16.dp)) {
                Text(t("لا تُتلى سورة الآن", "Nothing is playing"), style = MaterialTheme.typography.titleMedium)
                Muted(t("اختر سورة من المصحف أو اضغط «استمع من هنا» على أي آية.", "Open a surah in the mushaf or tap “Listen from here” on any ayah."))
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CtaPill(t("القرّاء", "Reciters")) { onOpen("reciters") }
                    GhostPill(t("المصحف", "Mushaf")) { onOpen("mushaf") }
                }
            }
            return@Column
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Reciter card
            SCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.RECITERS, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reciterName(st, ar), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        VSpace(4.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val (bg, fg) = app.sakina.quran.ui.theme.badgeColors(look.cta, look.card, dark = look.dark)
                            Badge(if (st.mode == AudioMode.AYAH) t("آية بآية", "Ayah by ayah") else t("السورة كاملة", "Whole surah"), bg, fg)
                            if (st.mode == AudioMode.AYAH || st.timing) Badge(t("تظليل متزامن", "Synced highlight"), bg, fg)
                            Badge(if (st.local) t("من الجهاز", "On device") else t("بثّ", "Streaming"), bg, fg)
                        }
                    }
                }
            }
            // Now reciting
            SCard(strong = true) {
                val su = q.suras.getOrNull(st.sura - 1)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (su != null) "سورة ${su.name}" else "", fontFamily = QuranFont, fontSize = 30.sp, lineHeight = 52.sp, color = look.onCard)
                    if (su != null) Muted(
                        if (st.ayah > 0) t("الآية ${num(st.ayah, true)} من ${num(su.ayas, true)} • الصفحة ${num(q.page(st.sura, st.ayah), true)}", "Ayah ${st.ayah} of ${su.ayas} • Page ${q.page(st.sura, st.ayah)}")
                        else t("الاستعاذة والبسملة", "Isti'adha & basmala"),
                    )
                    VSpace(10.dp)
                    if (su != null && st.ayah > 0) RtlBox {
                        Text(
                            app.sakina.quran.quran.Tajweed.glueWaqf(q.displayAyah(st.sura, st.ayah)) + " " + app.sakina.quran.ui.quran.ayahMark(st.ayah),
                            fontFamily = QuranFont, fontSize = 24.sp, lineHeight = 48.sp, color = look.onCard, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                    if (st.inGap) Muted(t("توقّف بين الآيات…", "Pause between ayat…"))
                }
            }
            st.error?.let { e ->
                SCard(tint = androidx.compose.ui.graphics.lerp(look.card, look.warn, .25f), padding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (e == AudioErr.OFFLINE) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline, null, tint = app.sakina.quran.ui.theme.readableOn(look.warn, look.card))
                        Spacer(Modifier.width(10.dp))
                        Text(errorText(e), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    VSpace(10.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CtaPill(t("إعادة المحاولة", "Retry")) { Recitation.retry(ctx) }
                        GhostPill(t("التحميلات", "Downloads")) { onOpen("downloads") }
                    }
                }
            }
            // Progress
            if (st.mode == AudioMode.SURAH) {
                var drag by remember { mutableStateOf<Float?>(null) }
                val dur = st.durMs.coerceAtLeast(1)
                Slider(
                    value = drag ?: (st.posMs.toFloat() / dur).coerceIn(0f, 1f), onValueChange = { drag = it },
                    onValueChangeFinished = { drag?.let { Recitation.seekTo(ctx, (it * dur).toLong()) }; drag = null },
                    colors = SliderDefaults.colors(thumbColor = look.cta, activeTrackColor = look.cta),
                    modifier = Modifier.semantics { contentDescription = if (ar) "موضع التلاوة" else "Playback position" },
                )
                Row(Modifier.fillMaxWidth()) {
                    Muted(mmss(st.posMs, ar)); Spacer(Modifier.weight(1f)); Muted(if (st.durMs > 0) mmss(st.durMs, ar) else "—")
                }
            } else {
                val fromA = q.fromGlobal(st.fromG).second; val toA = q.fromGlobal(st.toG).second
                val total = (st.toG - st.fromG + 1).coerceAtLeast(1)
                val doneN = if (st.sura > 0 && st.ayah > 0) (q.global(st.sura, st.ayah) - st.fromG + 1).coerceIn(0, total) else 0
                app.sakina.quran.ui.quran.Bar(doneN.toFloat() / total, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth()) {
                    Muted(t("الآيات ${num(fromA, true)}–${num(toA, true)}", "Ayat $fromA–$toA"))
                    Spacer(Modifier.weight(1f))
                    if (!st.repeat.isPlain) Muted(t("الدورة ${num(st.pass + 1, true)}", "Pass ${st.pass + 1}") + if (st.repeat.infinite) " / ∞" else " / ${num(st.repeat.rangeTimes, ar)}")
                }
            }
            // Transport
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Recitation.step(ctx, false) }, modifier = Modifier.size(56.dp)) { DirIcon(Icons.Outlined.SkipPrevious, t("الآية السابقة", "Previous ayah"), look.onCard) }
                Box(Modifier.size(78.dp).clip(CircleShape).background(look.cta).press { Recitation.toggle(ctx) }, contentAlignment = Alignment.Center) {
                    if (st.buffering) CircularProgressIndicator(Modifier.size(34.dp), color = look.onCta, strokeWidth = 3.dp)
                    else Icon(if (st.playing || st.inGap) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (st.playing) t("إيقاف مؤقت", "Pause") else t("تشغيل", "Play"), tint = look.onCta, modifier = Modifier.size(42.dp))
                }
                IconButton(onClick = { Recitation.step(ctx, true) }, modifier = Modifier.size(56.dp)) { DirIcon(Icons.Outlined.SkipNext, t("الآية التالية", "Next ayah"), look.onCard) }
            }
            // Options
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(repeatLabel(st.repeat, ar), selected = !st.repeat.isPlain, leading = { Icon(Icons.Outlined.Repeat, null, Modifier.size(18.dp)) }, onClick = { sheet = 1 })
                Chip(sleepLabel(st, ar), selected = st.sleep != null, leading = { Icon(Icons.Outlined.Bedtime, null, Modifier.size(18.dp)) }, onClick = { sheet = 2 })
                Chip(digits(fmtSpeed(st.speed), ar) + "×", selected = st.speed != 1f, leading = { Icon(Icons.Outlined.Speed, null, Modifier.size(18.dp)) }, onClick = { sheet = 3 })
                Chip(t("تتبّع الآية", "Follow along"), selected = prefs.follow, leading = { Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp)) }, onClick = { AudioPrefs.update(ctx) { it.copy(follow = !it.follow) } })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                if (st.sura > 0) {
                    Chip(t("في المصحف", "In the mushaf"), leading = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) }, onClick = { onOpen("mushaf/${q.page(st.sura, st.ayah.coerceAtLeast(1))}") })
                    Chip(t("عرض الآيات", "Verse view"), leading = { Icon(Icons.AutoMirrored.Outlined.ViewList, null, Modifier.size(18.dp)) }, onClick = { onOpen("reader/${st.sura}/${st.ayah.coerceAtLeast(1)}") })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                GhostPill(t("القرّاء", "Reciters")) { onOpen("reciters") }
                GhostPill(t("إيقاف التلاوة", "Stop")) { Recitation.stop(ctx); onBack() }
            }
            Muted(
                if (st.mode == AudioMode.AYAH) t("الصوت: Islamic Network (cdn.islamic.network) — مجاني للاستخدام غير التجاري.", "Audio: Islamic Network (cdn.islamic.network) — free for non-commercial use.")
                else t("الصوت وتوقيت الآيات: mp3quran.net.", "Audio and ayah timings: mp3quran.net."),
                Modifier.fillMaxWidth(),
            )
        }
    }
    when (sheet) {
        1 -> q?.let { RepeatSheet(it, st) { sheet = 0 } }
        2 -> SleepSheet(st) { sheet = 0 }
        3 -> SpeedSheet(st) { sheet = 0 }
    }
}

fun fmtSpeed(s: Float): String = if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString().trimEnd('0')

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepeatSheet(q: Quran, st: AudioUiState, onDismiss: () -> Unit) {
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val ar = isArabic()
    val (s0, fromA0) = q.fromGlobal(st.fromG)
    val su = q.suras[s0 - 1]
    val toA0 = if (q.fromGlobal(st.toG).first == s0) q.fromGlobal(st.toG).second else su.ayas
    var r by remember { mutableStateOf(if (st.repeat.isPlain) AudioPrefs.p.value.lastRepeat else st.repeat) }
    var fromA by remember { mutableIntStateOf(fromA0) }
    var toA by remember { mutableIntStateOf(toA0) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canLoop = st.mode == AudioMode.AYAH || st.timing
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("التكرار والحفظ", "Repeat & memorise"), style = MaterialTheme.typography.titleLarge)
            if (!canLoop) Muted(t("هذا المصحف بلا توقيت للآيات؛ التكرار متاح في وضع «آية بآية» أو مع قارئ عليه شارة «تظليل متزامن».", "This recording has no ayah timings; repeat works in Ayah-by-ayah mode or with a reciter marked “Synced highlight”."))
            Text(if (ar) "سورة ${su.name}" else su.tname, style = MaterialTheme.typography.titleMedium, color = look.cta)
            Stepper(t("من الآية", "From ayah"), fromA, 1, toA, ar) { fromA = it }
            Stepper(t("إلى الآية", "To ayah"), toA, fromA, su.ayas, ar) { toA = it }
            Muted(t("تكرار كل آية", "Repeat each ayah"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5, 7, 10, 20).forEach { n -> Chip("×" + num(n, ar), selected = r.ayahTimes == n, onClick = { r = r.copy(ayahTimes = n) }) }
            }
            Muted(t("تكرار المقطع كله", "Repeat the whole range"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5, 10, 0).forEach { n -> Chip(if (n == 0) "∞" else "×" + num(n, ar), selected = r.rangeTimes == n, onClick = { r = r.copy(rangeTimes = n) }) }
            }
            Muted(t("توقّف بعد كل آية (للترديد)", "Pause after each ayah (to repeat after the reciter)"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L, 1000L, 2000L, 3000L, 5000L, 8000L).forEach { ms -> Chip(if (ms == 0L) t("بلا توقف", "None") else t("${num((ms / 1000).toInt(), true)} ث", "${ms / 1000} s"), selected = r.gapMs == ms, onClick = { r = r.copy(gapMs = ms) }) }
            }
            if (st.mode == AudioMode.AYAH) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("التكرار المتدرّج", "Ladder repeat"), style = MaterialTheme.typography.bodyLarge)
                    Muted(t("كل آية جديدة، ثم من بداية المقطع حتى هي — طريقة مجرّبة للحفظ", "Each new ayah, then from the start of the range up to it — a proven memorisation method"))
                }
                Switch(r.ladder, { r = r.copy(ladder = it) }, colors = app.sakina.quran.ui.azkar.switchColors())
            }
            Muted(t("إعدادات جاهزة", "Presets"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("استماع عادي", "Plain listening"), onClick = { r = RepeatSpec() })
                Chip(t("حفظ: ٣× مع توقف", "Memorise: 3× + pause"), onClick = { r = RepeatSpec(3, 1, 2000) })
                Chip(t("مراجعة: المقطع ×٣", "Review: range ×3"), onClick = { r = RepeatSpec(1, 3, 0) })
                Chip(t("تلقين: ٥× وتوقف ٥ث", "Talqin: 5× + 5 s"), onClick = { r = RepeatSpec(5, 1, 5000) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CtaPill(t("تطبيق", "Apply"), enabled = canLoop || r.isPlain) { Recitation.setRepeat(ctx, q, r, fromA, toA); onDismiss() }
                GhostPill(t("بدون تكرار", "No repeat")) { Recitation.setRepeat(ctx, q, RepeatSpec(), fromA, toA); onDismiss() }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, ar: Boolean, onChange: (Int) -> Unit) {
    val look = LocalSakina.current.look
    Row(Modifier.fillMaxWidth().surface(RoundedCornerShape(16.dp), elevated = false).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box(Modifier.size(40.dp).clip(CircleShape).press(enabled = value > min) { onChange((value - 1).coerceAtLeast(min)) }, contentAlignment = Alignment.Center) { Text("−", style = MaterialTheme.typography.titleLarge, color = if (value > min) look.cta else look.onCardMuted) }
        Text(num(value, ar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 44.dp), textAlign = TextAlign.Center)
        Box(Modifier.size(40.dp).clip(CircleShape).press(enabled = value < max) { onChange((value + 1).coerceAtMost(max)) }, contentAlignment = Alignment.Center) { Text("+", style = MaterialTheme.typography.titleLarge, color = if (value < max) look.cta else look.onCardMuted) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepSheet(st: AudioUiState, onDismiss: () -> Unit) {
    val look = LocalSakina.current.look
    val ar = isArabic()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("مؤقّت النوم", "Sleep timer"), style = MaterialTheme.typography.titleLarge)
            Muted(t("يخفت الصوت تدريجيًا في آخر ٨ ثوانٍ ثم يتوقف.", "The sound fades out over the last 8 seconds, then stops."))
            if (st.sleep != null) Text(sleepLabel(st, ar), style = MaterialTheme.typography.titleMedium, color = look.cta)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("إيقاف المؤقّت", "Off"), selected = st.sleep == null, onClick = { Recitation.setSleep(null); onDismiss() })
                listOf(5, 10, 15, 30, 45, 60, 90).forEach { m ->
                    Chip(t("${num(m, true)} دقيقة", "$m min"), selected = (st.sleep as? SleepSpec.Minutes)?.minutes == m, onClick = { Recitation.setSleep(SleepSpec.Minutes(m)); onDismiss() })
                }
                Chip(t("نهاية الآية", "End of ayah"), selected = st.sleep == SleepSpec.EndOfAyah, onClick = { Recitation.setSleep(SleepSpec.EndOfAyah); onDismiss() })
                Chip(t("نهاية السورة", "End of surah"), selected = st.sleep == SleepSpec.EndOfSurah, onClick = { Recitation.setSleep(SleepSpec.EndOfSurah); onDismiss() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeedSheet(st: AudioUiState, onDismiss: () -> Unit) {
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val ar = isArabic()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("سرعة التلاوة", "Playback speed"), style = MaterialTheme.typography.titleLarge)
            Muted(t("تتغير السرعة مع بقاء طبقة صوت القارئ كما هي.", "Speed changes while the reciter's pitch stays natural."))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEEDS.forEach { s -> Chip(digits(fmtSpeed(s), ar) + "×", selected = st.speed == s, onClick = { Recitation.setSpeed(ctx, s); onDismiss() }) }
            }
        }
    }
}
