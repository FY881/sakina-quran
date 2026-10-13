package app.sakina.quran.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.quran.EdKind
import app.sakina.quran.quran.Edition
import app.sakina.quran.quran.Editions
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.launch

/** Tafsir as a book (TF11): every ayah of the surah with the chosen tafsir, anchored at [startAyah]. */
@Composable
fun TafsirScreen(sura: Int, startAyah: Int, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val quran = rememberQuran()
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val tafsirs = Editions.ALL.filter { it.kind == EdKind.TAFSIR && Editions.available(ctx, it.id) }
    var ed by remember(qu.prefs.tafsir) { mutableStateOf(qu.prefs.tafsir.takeIf { id -> tafsirs.any { it.id == id } } ?: "ar.muyassar") }
    var retry by remember { mutableIntStateOf(0) }
    val state = rememberEditionState(ed, retry)
    val data = (state as? EdState.Ready)?.data
    val list = rememberLazyListState()
    var s by remember { mutableIntStateOf(sura.coerceIn(1, 114)) }
    Column(Modifier.fillMaxSize()) {
        val su = quran?.suras?.getOrNull(s - 1)
        BackHeader(
            t("التفسير", "Tafsir") + if (su != null) (if (ar) " — سورة ${su.name}" else " — ${su.tname}") else "", onBack,
            subtitle = Editions.get(ed)?.let { if (ar) "${it.nameAr} • ${it.author}" else "${it.nameEn} • ${it.author}" },
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tafsirs, key = { it.id }) { e ->
                Chip(if (ar) e.nameAr else e.nameEn, selected = e.id == ed, onClick = { ed = e.id; scope.launch { store.updatePrefs { p -> p.copy(tafsir = e.id) } } })
            }
            item { Chip(t("+ تفاسير أخرى", "+ More tafsir"), onClick = { onOpen("quran_library") }) }
        }
        VSpace(8.dp)
        if (state is EdState.Error) { Box(Modifier.fillMaxWidth().padding(24.dp)) { EditionError { Editions.failed -= ed; retry++ } }; return@Column }
        if (quran == null || data == null) { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = look.cta) }; return@Column }
        val sur = quran.suras[s - 1]
        LaunchedEffect(s) { if (s == sura && startAyah > 1) list.scrollToItem(startAyah - 1) }
        LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sur.ayas, key = { "$s:$it" }) { i ->
                val a = i + 1
                val g = quran.global(s, a)
                SCard(padding = PaddingValues(14.dp), onClick = { onOpen("reader/$s/$a") }) {
                    RtlBox {
                        Text(glueWaqf("${quran.displayAyah(s, a)} ${ayahMark(a)}"), fontFamily = QuranFont, fontSize = 22.sp, lineHeight = 42.sp, color = look.onCard, modifier = Modifier.fillMaxWidth())
                    }
                    VSpace(6.dp)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(look.cta.copy(alpha = .08f)).padding(10.dp)) {
                        Text(
                            data.getOrNull(g).orEmpty().ifBlank { t("لا يوجد تفسير لهذه الآية في هذا الكتاب.", "No tafsir for this ayah in this book.") },
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl, lineHeight = 26.sp), color = look.onCard,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Muted(suraRef(quran, s, a, ar), Modifier.padding(top = 4.dp))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s > 1) Chip(t("السورة السابقة", "Previous surah"), onClick = { s -= 1; scope.launch { list.scrollToItem(0) } })
                    Spacer(Modifier.weight(1f))
                    if (s < 114) Chip(if (ar) "التالية: ${quran.suras[s].name}" else "Next: ${quran.suras[s].tname}", selected = true, onClick = { s += 1; scope.launch { list.scrollToItem(0) } })
                }
            }
            item { Muted(Editions.get(ed)?.let { e -> t("المصدر: ${if (e.source == "Tanzil") "مشروع تنزيل tanzil.net" else "Al Quran Cloud (alquran.cloud)"} — ${e.author}", "Source: ${if (e.source == "Tanzil") "Tanzil (tanzil.net)" else "Al Quran Cloud (alquran.cloud)"} — ${e.author}") } ?: "") }
        }
    }
}

/** Library of tafsir / translation packs (TF03/TF04/TF05/TF06): choose, download on device, delete. */
@Composable
fun QuranLibraryScreen(onBack: () -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val progress = remember { mutableStateMapOf<String, Float>() }
    val errors = remember { mutableStateMapOf<String, Boolean>() }
    var refresh by remember { mutableIntStateOf(0) }
    val selected = qu.prefs.activeTranslations(ar)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("مكتبة التفاسير والترجمات", "Tafsir & translations"), onBack, subtitle = t("اختر حتى ٣ ترجمات تظهر تحت الآيات معًا", "Pick up to 3 translations to show side by side")) }
        item { SectionHeader(t("التفسير", "Tafsir")) }
        items(Editions.ALL.filter { it.kind == EdKind.TAFSIR }, key = { it.id }) { EditionRow(it, progress, errors, refresh) { refresh++ } }
        item { SectionHeader(t("النطق اللاتيني", "Transliteration")) }
        items(Editions.ALL.filter { it.kind == EdKind.TRANSLIT }, key = { it.id }) { EditionRow(it, progress, errors, refresh) { refresh++ } }
        item { SectionHeader(t("ترجمات المعاني", "Translations")) }
        items(Editions.ALL.filter { it.kind == EdKind.TRANSLATION }, key = { it.id }) { EditionRow(it, progress, errors, refresh) { refresh++ } }
        item {
            Muted(
                t("التراجم والتفاسير من مشروع تنزيل (tanzil.net/trans) لاستخدام غير تجاري، وتفسيرا القرطبي والبغوي من Al Quran Cloud. ابن كثير والسعدي والمختصر لم تُضمَّن لعدم وضوح ترخيصها.",
                    "Translations and tafsir from Tanzil (tanzil.net/trans) for non-commercial use; al-Qurtubi and al-Baghawi from Al Quran Cloud. Ibn Kathir, as-Sa'di and al-Mukhtasar are not included because their licences are unclear."),
                Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun EditionRow(e: Edition, progress: MutableMap<String, Float>, errors: MutableMap<String, Boolean>, refresh: Int, onRefresh: () -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val selected = qu.prefs.activeTranslations(ar)
    key(refresh) {
        val have = Editions.available(ctx, e.id)
        val p = progress[e.id]
        val isTr = e.kind == EdKind.TRANSLATION
        val on = when (e.kind) { EdKind.TAFSIR -> qu.prefs.tafsir == e.id; EdKind.TRANSLATION -> e.id in selected; EdKind.TRANSLIT -> qu.prefs.translit }
        val click: (() -> Unit)? = if (!have) null else {
    {
            scope.launch {
                store.updatePrefs { pr ->
                    when (e.kind) {
                        EdKind.TAFSIR -> pr.copy(tafsir = e.id)
                        EdKind.TRANSLIT -> pr.copy(translit = !pr.translit)
                        EdKind.TRANSLATION -> {
                            val cur = pr.activeTranslations(ar)
                            pr.copy(translations = if (e.id in cur) cur - e.id else (cur + e.id).takeLast(3))
                        }
                    }
                }
            }
        Unit
    }
}
        SCard(padding = PaddingValues(12.dp), onClick = click) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (ar) e.nameAr else e.nameEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Muted(e.author + " • " + when {
                        e.bundled -> t("مضمَّن، يعمل دون إنترنت", "Built in, works offline")
                        have -> t("محمَّل على جهازك", "Downloaded on your device")
                        else -> t("نحو ${num(e.approxMb, true)} م.ب — تنزيل مرة واحدة", "About ${e.approxMb} MB — one-time download")
                    })
                    if (errors[e.id] == true) Text(t("تعذّر التنزيل، تحقّق من الاتصال ثم أعد المحاولة.", "Download failed — check your connection and try again."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (p != null && p < 1f) { VSpace(6.dp); Bar(p) }
                }
                Spacer(Modifier.width(8.dp))
                when {
                    p != null && p < 1f -> CircularProgressIndicator(Modifier.size(26.dp), color = look.cta, strokeWidth = 3.dp)
                    !have -> IconButton(onClick = {
                        errors.remove(e.id); progress[e.id] = 0f
                        scope.launch {
                            val r = Editions.download(ctx, e.id) { v -> progress[e.id] = v }
                            if (r.isFailure) { errors[e.id] = true; progress.remove(e.id) } else { progress[e.id] = 1f; onRefresh() }
                        }
                    }) { Icon(Icons.Outlined.CloudDownload, t("تنزيل", "Download"), tint = look.cta) }
                    else -> {
                        if (on) Icon(Icons.Outlined.CheckCircle, t("مُختار", "Selected"), tint = look.cta)
                        else Box(Modifier.size(22.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .25f)))
                        if (!e.bundled) IconButton(onClick = {
                            Editions.delete(ctx, e.id); progress.remove(e.id); onRefresh()
                            if (isTr) scope.launch { store.updatePrefs { pr -> pr.copy(translations = pr.activeTranslations(ar) - e.id) } }
                            if (e.kind == EdKind.TAFSIR && qu.prefs.tafsir == e.id) scope.launch { store.updatePrefs { pr -> pr.copy(tafsir = "ar.muyassar") } }
                        }) { Icon(Icons.Outlined.Delete, t("حذف", "Delete"), tint = look.onCardMuted) }
                    }
                }
            }
        }
    }
}
