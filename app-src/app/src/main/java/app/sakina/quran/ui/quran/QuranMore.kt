package app.sakina.quran.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.azkar.DhikrAudio
import app.sakina.quran.quran.Reciter
import app.sakina.quran.quran.Reciters
import app.sakina.quran.quran.SuraInfo
import app.sakina.quran.quran.TajRule
import app.sakina.quran.quran.Tajweed
import app.sakina.quran.quran.Topics
import app.sakina.quran.data.QuranUser
import app.sakina.quran.ui.azkar.SearchField
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
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

/** Reciter selection groundwork (AU1 prep): mp3quran catalogue (243 reciters) for full surahs + ayah-by-ayah voice. */
@Composable
fun RecitersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ar = isArabic()
    val look = LocalSakina.current.look
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val all by produceState<List<Reciter>>(emptyList()) { value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Reciters.load(ctx) }.getOrDefault(emptyList()) } }
    var q by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf<String?>(DhikrAudio.playingUrl) }
    var tab by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { DhikrAudio.stop() } }
    val list = remember(all, q) {
        val n = normalize(q.trim())
        if (n.isEmpty()) all else all.filter { normalize(it.ar).contains(n) || it.en.lowercase().contains(q.trim().lowercase()) }
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("القرّاء", "Reciters"), onBack, subtitle = t("${num(all.size, true)} قارئًا من mp3quran.net — بثّ مباشر", "${all.size} reciters from mp3quran.net — streamed")) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("السور كاملة", "Full surahs"), selected = tab == 0, onClick = { tab = 0 })
                Chip(t("آية بآية", "Ayah by ayah"), selected = tab == 1, onClick = { tab = 1 })
            }
        }
        if (tab == 1) {
            item { Muted(t("هذا الصوت يُستخدم عند الضغط على «استماع» في أي آية.", "This voice plays when you tap Listen on any ayah.")) }
            items(Reciters.AYAH_VOICES, key = { it.first }) { (id, nAr, nEn) ->
                val url = Reciters.ayahUrl(id, 0)
                SCard(padding = PaddingValues(12.dp), onClick = { scope.launch { store.updatePrefs { it.copy(ayahVoice = id) } } }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { DhikrAudio.toggle(url) { on -> playing = if (on) url else null }; playing = DhikrAudio.playingUrl }) {
                            Icon(if (playing == url) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, t("تجربة", "Preview"), tint = look.cta)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (ar) nAr else nEn, style = MaterialTheme.typography.titleSmall)
                            Muted(t("Islamic Network — للاستخدام غير التجاري", "Islamic Network — non-commercial use"))
                        }
                        if (qu.prefs.ayahVoice == id) Icon(Icons.Outlined.CheckCircle, null, tint = look.cta)
                    }
                }
            }
        } else {
            item { SearchField(q, { q = it }, t("ابحث عن قارئ…", "Search reciters…"), Modifier.fillMaxWidth()) }
            if (all.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = look.cta) } }
            items(list, key = { it.id }) { r ->
                val sel = qu.prefs.reciter == r.id
                SCard(padding = PaddingValues(12.dp), tint = if (sel) androidx.compose.ui.graphics.lerp(look.card, look.cta, .14f) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (ar) r.ar else r.en.ifBlank { r.ar }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!ar && r.en.isNotBlank()) Text(r.ar, style = MaterialTheme.typography.bodySmall, color = look.onCardMuted)
                        }
                        if (sel) Icon(Icons.Outlined.CheckCircle, t("القارئ المختار", "Selected reciter"), tint = look.cta)
                    }
                    r.moshafs.forEach { m ->
                        val url = m.url(if (m.has(1)) 1 else m.surahs.minOrNull() ?: 1)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).press {
                            scope.launch { store.updatePrefs { it.copy(reciter = r.id, moshaf = m.id) } }
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { DhikrAudio.toggle(url) { on -> playing = if (on) url else null }; playing = DhikrAudio.playingUrl }) {
                                Icon(if (playing == url) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, t("تجربة", "Preview"), tint = look.cta)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(if (ar) m.ar else m.en.ifBlank { m.ar }, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                Muted(t("${num(m.count, true)} سورة", "${m.count} surahs"))
                            }
                            if (sel && qu.prefs.moshaf == m.id) Text(t("مختار", "Selected"), color = look.cta, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item { Muted(t("القائمة والروابط من mp3quran.net التي تسمح بنسخ محتواها واستخدام روابطها. الاستماع الكامل مع التحميل وتظليل الآيات في التحديث القادم.", "Catalogue and links from mp3quran.net, which allows copying its material and using its links. Full player with downloads and ayah highlighting comes in the next update."), Modifier.padding(8.dp)) }
        }
    }
}

/** Topic index (QR21). */
@Composable
fun TopicsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val ar = isArabic()
    val topics = remember { runCatching { Topics.load(ctx) }.getOrDefault(emptyList()) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("موضوعات القرآن", "Quran topics"), onBack, subtitle = t("${num(topics.size, true)} موضوعًا مختارًا بآياتها", "${topics.size} curated topics with their ayat")) }
        items(topics, key = { it.id }) { tp ->
            SCard(padding = PaddingValues(14.dp), onClick = { onOpen("topic/${tp.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.QURAN, 30.dp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (ar) tp.ar else tp.en, style = MaterialTheme.typography.titleMedium)
                        Muted(t("${num(tp.refs.size, true)} آيات", "${tp.refs.size} ayat"))
                    }
                }
            }
        }
    }
}

@Composable
fun TopicScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val ar = isArabic()
    val look = LocalSakina.current.look
    val quran = rememberQuran()
    val qu = rememberQUser()
    val tp = remember(id) { runCatching { Topics.load(ctx) }.getOrDefault(emptyList()).firstOrNull { it.id == id } }
    val trId = qu.prefs.activeTranslations(ar).firstOrNull { app.sakina.quran.quran.Editions.available(ctx, it) }
    val tr = rememberEdition(trId)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(tp?.let { if (ar) it.ar else it.en } ?: "", onBack) }
        if (quran == null || tp == null) return@LazyColumn
        items(tp.refs, key = { "${it.first}:${it.second}" }) { (s, a) ->
            SCard(padding = PaddingValues(14.dp), onClick = { onOpen("reader/$s/$a") }) {
                Text(suraRef(quran, s, a, ar), style = MaterialTheme.typography.labelLarge, color = look.cta)
                VSpace(4.dp)
                RtlBox { Text(glueWaqf(quran.displayAyah(s, a)), fontFamily = QuranFont, fontSize = 22.sp, lineHeight = 42.sp, modifier = Modifier.fillMaxWidth()) }
                tr?.getOrNull(quran.global(s, a))?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (app.sakina.quran.quran.Editions.get(trId ?: "")?.rtl == true) androidx.compose.ui.text.style.TextDirection.Rtl else androidx.compose.ui.text.style.TextDirection.Ltr), color = look.onCardMuted, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        item { Muted(t("فهرس من إعداد سكينة؛ كل مرجع تم التحقق منه آليًا مقابل نص المصحف.", "Index prepared by Sakina; every reference was machine-checked against the Quran text."), Modifier.padding(8.dp)) }
    }
}

/** Surah info card (QR22). */
@Composable
fun SurahInfoScreen(sura: Int, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val quran = rememberQuran()
    val ar = isArabic()
    val look = LocalSakina.current.look
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val s = quran?.suras?.getOrNull(sura - 1)
        item { BackHeader(if (s == null) "" else if (ar) "سورة ${s.name}" else "${s.tname} — ${s.ename}", onBack) }
        if (quran == null || s == null) return@LazyColumn
        item {
            SCard(strong = true) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("سورة ${s.name}", fontFamily = QuranFont, fontSize = 34.sp, lineHeight = 60.sp, color = look.onCard)
                    VSpace(6.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile(if (s.meccan) t("مكية", "Makki") else t("مدنية", "Madani"), t("النزول", "Revealed"), Modifier.weight(1f))
                        StatTile(num(s.ayas, ar), t("آية", "Ayat"), Modifier.weight(1f))
                        StatTile(num(s.index, ar), t("ترتيب المصحف", "Mushaf order"), Modifier.weight(1f))
                        StatTile(num(s.order, ar), t("ترتيب النزول", "Revelation order"), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile(num(quran.page(s.index, 1), ar), t("تبدأ في الصفحة", "Starts on page"), Modifier.weight(1f))
                        StatTile(num(quran.juz(s.index, 1), ar), t("الجزء", "Juz"), Modifier.weight(1f))
                        StatTile(num(s.rukus, ar), t("الركوعات", "Rukus"), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SCard {
                Text(t("محاورها", "Main themes"), style = MaterialTheme.typography.titleSmall, color = look.cta)
                VSpace(4.dp)
                Text(if (ar) SuraInfo.themeAr(s.index) else SuraInfo.themeEn(s.index), style = MaterialTheme.typography.bodyLarge)
                VSpace(4.dp)
                Muted(t("ملخص موجز من إعداد سكينة.", "A brief summary by Sakina."))
            }
        }
        val v = SuraInfo.VIRTUES[s.index].orEmpty()
        if (v.isNotEmpty()) {
            item { SectionHeader(t("من فضلها (أحاديث صحيحة)", "Virtues (authentic narrations)")) }
            items(v) { x ->
                SCard(padding = PaddingValues(14.dp)) {
                    Text(if (ar) x.ar else x.en, style = MaterialTheme.typography.bodyLarge)
                    Muted(if (ar) x.refAr else x.refEn, Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CtaPill(t("اقرأ السورة", "Read the surah"), Modifier.weight(1f)) { onOpen("reader/${s.index}/1") }
                CtaPill(t("في المصحف", "In the mushaf"), Modifier.weight(1f)) { onOpen("mushaf/${quran.page(s.index, 1)}") }
            }
        }
    }
}

/** Favourites, collections, highlights and notes (QR08/QR09/QR10). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranMarksScreen(startTab: Int, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val quran = rememberQuran()
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    val look = LocalSakina.current.look
    var tab by remember { mutableIntStateOf(startTab.coerceIn(0, 2)) }
    var color by remember { mutableStateOf<Int?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var editLegend by remember { mutableStateOf(false) }
    fun parse(r: String) = r.split(':').let { p -> p.getOrNull(0)?.toIntOrNull()?.let { s -> p.getOrNull(1)?.toIntOrNull()?.let { s to it } } }?.takeIf { it.first in 1..114 }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("آياتي", "My ayat"), onBack, subtitle = t("المفضلة والمجموعات والتظليل والملاحظات", "Favourites, collections, highlights and notes")) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("المفضلة والمجموعات", "Collections"), selected = tab == 0, onClick = { tab = 0 })
                Chip(t("التظليل", "Highlights"), selected = tab == 1, onClick = { tab = 1 })
                Chip(t("الملاحظات", "Notes"), selected = tab == 2, onClick = { tab = 2 })
            }
        }
        if (quran == null) return@LazyColumn
        when (tab) {
            0 -> {
                val cols = listOf(QuranUser.FAV) + (qu.collections.keys - QuranUser.FAV).sorted()
                cols.forEach { name ->
                    val refs = qu.collections[name].orEmpty()
                    item(key = "h$name") {
                        SectionHeader(if (name == QuranUser.FAV) t("★ المفضلة", "★ Favourites") else name) {
                            Muted(num(refs.size, ar))
                            if (name != QuranUser.FAV) TextButton(onClick = { scope.launch { store.deleteCollection(name) } }) { Text(t("حذف", "Delete"), color = look.onCardMuted) }
                        }
                    }
                    if (refs.isEmpty()) item(key = "e$name") { Muted(t("اضغط على أي آية ثم «المفضلة» أو «مجموعة».", "Tap any ayah, then Favourite or Collection."), Modifier.padding(horizontal = 6.dp)) }
                    items(refs, key = { "$name|$it" }) { MarkRow(quran, it, null, onOpen) }
                }
            }
            1 -> {
                item {
                    SCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t("دليل الألوان", "Colour legend"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { editLegend = !editLegend }) { Text(if (editLegend) t("تم", "Done") else t("تعديل", "Edit"), color = look.cta) }
                        }
                        HL_COLORS.forEachIndexed { i, c ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).press { color = if (color == i) null else i }, verticalAlignment = Alignment.CenterVertically) {
                                Dot(c, 22, selected = color == i); Spacer(Modifier.width(10.dp))
                                if (editLegend) {
                                    var v by remember(i) { mutableStateOf(qu.prefs.legend.getOrElse(i) { "" }) }
                                    Box(Modifier.weight(1f).surface(CircleShape, elevated = false).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        if (v.isEmpty()) Muted(legendLabel(qu.prefs, i, ar))
                                        BasicTextField(v, { nv -> v = nv.take(24); scope.launch { store.updatePrefs { p -> p.copy(legend = p.legend.toMutableList().also { l -> while (l.size < 5) l.add(""); l[i] = v }) } } },
                                            singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
                                    }
                                } else Text(legendLabel(qu.prefs, i, ar), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Muted(num(qu.highlights.values.count { it == i }, ar))
                            }
                        }
                        Muted(t("اضغط لونًا لتصفية الآيات به.", "Tap a colour to filter by it."))
                    }
                }
                val hl = qu.highlights.filter { color == null || it.value == color }.keys.mapNotNull { k -> parse(k)?.let { k to it } }.sortedWith(compareBy({ it.second.first }, { it.second.second })).map { it.first }
                if (hl.isEmpty()) item { Muted(t("لا آيات مظللة بعد — اضغط على آية واختر لونًا.", "No highlighted ayat yet — tap an ayah and pick a colour."), Modifier.padding(6.dp)) }
                items(hl, key = { "hl$it" }) { r -> MarkRow(quran, r, legendLabel(qu.prefs, qu.highlights[r] ?: 0, ar), onOpen) }
            }
            else -> {
                val tags = qu.allTags
                if (tags.isNotEmpty()) item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(t("الكل", "All"), selected = tag == null, onClick = { tag = null })
                        tags.forEach { tg -> Chip("#$tg", selected = tag == tg, onClick = { tag = if (tag == tg) null else tg }) }
                    }
                }
                val notes = qu.notes.filter { tag == null || tag in it.value.tags }.entries.sortedByDescending { it.value.at }
                if (notes.isEmpty()) item { Muted(t("لا ملاحظات بعد — اضغط على آية ثم «ملاحظة».", "No notes yet — tap an ayah, then Note."), Modifier.padding(6.dp)) }
                items(notes, key = { "n${it.key}" }) { e -> MarkRow(quran, e.key, e.value.text.take(200) + if (e.value.tags.isNotEmpty()) "\n" + e.value.tags.joinToString(" ") { "#$it" } else "", onOpen) }
            }
        }
    }
}

@Composable
private fun MarkRow(quran: app.sakina.quran.quran.Quran, r: String, extra: String?, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val p = r.split(':')
    val s = p.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..114 } ?: return
    val a = p.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..quran.suras[s - 1].ayas } ?: return
    SCard(padding = PaddingValues(12.dp), onClick = { onOpen("reader/$s/$a") }) {
        Text(suraRef(quran, s, a, ar), style = MaterialTheme.typography.labelLarge, color = look.cta)
        RtlBox { Text(glueWaqf(quran.displayAyah(s, a)), fontFamily = QuranFont, fontSize = 20.sp, lineHeight = 38.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) }
        if (!extra.isNullOrBlank()) Muted(extra)
    }
}

/** Tajweed guide with legend and palettes (HZ12 / AC03). */
@Composable
fun TajweedGuideScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val quran = rememberQuran()
    val examples = mapOf(
        TajRule.GHUNNAH to (4 to 1), TajRule.IKHFA to (2 to 255), TajRule.IDGHAM_GHUNNAH to (2 to 255), TajRule.IDGHAM_NO_GHUNNAH to (2 to 2),
        TajRule.IQLAB to (2 to 27), TajRule.QALQALAH to (112 to 1), TajRule.MADD_NECESSARY to (1 to 7), TajRule.MADD_OBLIGATORY to (2 to 255),
        TajRule.MADD_PERMISSIBLE to (2 to 255), TajRule.SILENT to (2 to 255), TajRule.MADD_NATURAL to (2 to 2), TajRule.IKHFA_SHAFAWI to (2 to 8), TajRule.IDGHAM_SHAFAWI to (4 to 1),
    )
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("التجويد الملوّن", "Colour-coded tajweed"), onBack, subtitle = t("فعّل الألوان في المصحف وتعرّف على كل حكم", "Turn colours on in the mushaf and learn each rule")) }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("تلوين التجويد في المصحف", "Tajweed colours in the mushaf"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(qu.prefs.tajweed, { v -> scope.launch { store.updatePrefs { it.copy(tajweed = v) } } }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("كلاسيكية", "Classic"), selected = qu.prefs.palette == 0, onClick = { scope.launch { store.updatePrefs { it.copy(palette = 0) } } })
                    Chip(t("تباين عالٍ", "High contrast"), selected = qu.prefs.palette == 1, onClick = { scope.launch { store.updatePrefs { it.copy(palette = 1) } } })
                    Chip(t("لعمى الألوان", "Colour-blind"), selected = qu.prefs.palette == 2, onClick = { scope.launch { store.updatePrefs { it.copy(palette = 2) } } })
                }
                VSpace(8.dp)
                CtaPill(t("افتح المصحف الملوّن", "Open the coloured mushaf"), Modifier.fillMaxWidth()) {
                    scope.launch { store.updatePrefs { it.copy(tajweed = true) } }; onOpen("mushaf/${qu.prefs.lastPage}")
                }
            }
        }
        items(TajRule.entries.toList(), key = { it.name }) { r ->
            SCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(Color(Tajweed.color(r, qu.prefs.palette, look.dark)), 22); Spacer(Modifier.width(10.dp))
                    Text(if (ar) r.ar else r.en, style = MaterialTheme.typography.titleMedium, color = Color(Tajweed.color(r, qu.prefs.palette, look.dark)))
                }
                Text(if (ar) r.descAr else r.descEn, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                val ex = examples[r]
                if (quran != null && ex != null) {
                    val txt = quran.displayAyah(ex.first, ex.second)
                    // Show the first word group that carries this rule.
                    val sp = Tajweed.spans(txt).firstOrNull { it.rule == r }
                    if (sp != null) {
                        val st = txt.lastIndexOf(' ', (sp.start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
                        val en = txt.indexOf(' ', (sp.end + 1).coerceAtMost(txt.length)).let { if (it < 0) txt.length else it }
                        val st2 = txt.lastIndexOf(' ', (st - 2).coerceAtLeast(0)).let { if (it < 0 || st == 0) st else it + 1 }
                        // Trim any waqf mark / space left at either edge of the cut (they belong to the neighbouring word).
                        val snippet = txt.substring(st2, en).trim { it == ' ' || it == '\u00A0' || it in '\u06D6'..'\u06DC' }
                        VSpace(6.dp)
                        RtlBox {
                            Text(ayahText(snippet, true, qu.prefs.palette, look.dark), fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 48.sp, modifier = Modifier.fillMaxWidth())
                        }
                        Muted(t("مثال: ", "Example: ") + suraRef(quran, ex.first, ex.second, ar))
                    }
                }
            }
        }
        item { Muted(t("التلوين يحسبه سكينة على النص العثماني دون تغييره، وفق رواية حفص عن عاصم. للتعلّم المتقن ارجع إلى معلّم مُجاز.", "Colours are computed by Sakina on the unaltered Uthmani text (Hafs 'an 'Asim). For mastery, learn with a certified teacher."), Modifier.padding(8.dp)) }
    }
}
