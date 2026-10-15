package app.sakina.quran.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.audio.Recitation
import app.sakina.quran.data.QuranUser
import app.sakina.quran.quran.Editions
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Reciters
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.launch

fun copyAyah(ctx: Context, q: Quran, s: Int, a: Int, ar: Boolean) {
    runCatching { ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ayah", "${q.ayah(s, a)} [${q.suras[s - 1].name}: $a]")) }
    Toast.makeText(ctx, if (ar) "تم النسخ" else "Copied", Toast.LENGTH_SHORT).show()
}

@Composable
private fun Action(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color = LocalSakina.current.look.onCard, onClick: () -> Unit) {
    val look = LocalSakina.current.look
    Column(Modifier.width(74.dp).clip(RoundedCornerShape(14.dp)).press(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(42.dp).surface(CircleShape, elevated = false), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
        VSpace(4.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = look.onCardMuted, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/** Everything one ayah can do: tafsir, translations, audio, bookmark, favourite, highlight, note & tags, collections, copy, share. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahSheet(q: Quran, s: Int, a: Int, onDismiss: () -> Unit, onOpen: (String) -> Unit, bookmarked: Boolean? = null, onBookmark: (() -> Unit)? = null) {
    val look = LocalSakina.current.look
    val ar = isArabic()
    val ctx = LocalContext.current
    val qu = rememberQUser()
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val prefs = qu.prefs
    val ref = "$s:$a"
    val g = q.global(s, a)
    var tafRetry by remember { mutableIntStateOf(0) }
    val tafState = rememberEditionState(prefs.tafsir, tafRetry)
    val tafsir = (tafState as? EdState.Ready)?.data
    val trIds = prefs.activeTranslations(ar).filter { Editions.available(ctx, it) }
    // AU1: recitation runs through the media service (notification, lock screen, background), not a preview clip.
    val audio = app.sakina.quran.ui.audio.rememberAudio()
    val playingHere = audio.active && audio.sura == s && audio.ayah == a
    var editNote by remember { mutableStateOf(false) }
    var showCollections by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            Text(
                if (ar) "سورة ${q.suras[s - 1].name} • الآية ${num(a, true)}" else "${q.suras[s - 1].tname} • Ayah $s:$a",
                style = MaterialTheme.typography.titleLarge,
            )
            Muted(t("الصفحة ${num(q.page(s, a), true)} • الجزء ${num(q.juz(s, a), true)}", "Page ${q.page(s, a)} • Juz ${q.juz(s, a)}") + if (g in q.sajdas) t(" • موضع سجدة ۩", " • Sajdah ۩") else "")
            VSpace(10.dp)
            RtlBox {
                Text(
                    ayahText(q.displayAyah(s, a), prefs.tajweed, prefs.palette, look.dark), fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 50.sp,
                    color = look.onCard, modifier = Modifier.fillMaxWidth(),
                )
            }
            VSpace(8.dp)
            // Actions
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Action(Icons.Outlined.PlayArrow, t("استمع من هنا", "Listen from here"), look.cta) {
                    Recitation.listenFrom(ctx, q, prefs.ayahVoice, prefs.reciter, prefs.moshaf, s, a, ar)
                    onDismiss()
                }
                Action(Icons.Outlined.Headphones, t("هذه الآية فقط", "This ayah only"), look.cta) {
                    Recitation.listenFrom(ctx, q, prefs.ayahVoice, prefs.reciter, prefs.moshaf, s, a, ar, onlyThis = true)
                    onDismiss()
                }
                Action(Icons.AutoMirrored.Outlined.MenuBook, t("التفسير", "Tafsir")) { onOpen("tafsir/$s/$a") }
                if (onBookmark != null) Action(if (bookmarked == true) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, t("علامة", "Bookmark"), if (bookmarked == true) look.cta else look.onCard) { onBookmark() }
                Action(if (qu.isFav(s, a)) Icons.Outlined.Star else Icons.Outlined.StarBorder, t("المفضلة", "Favourite"), if (qu.isFav(s, a)) look.cta else look.onCard) {
                    scope.launch { store.toggleInCollection(QuranUser.FAV, ref) }
                }
                Action(Icons.AutoMirrored.Outlined.PlaylistAdd, t("مجموعة", "Collection")) { showCollections = !showCollections }
                Action(Icons.Outlined.EditNote, t("ملاحظة", "Note"), if (qu.notes[ref] != null) look.cta else look.onCard) { editNote = !editNote }
                Action(Icons.Outlined.ContentCopy, t("نسخ", "Copy")) { copyAyah(ctx, q, s, a, ar) }
                Action(Icons.Outlined.Share, t("صورة للمشاركة", "Share image")) { onOpen("share/$s/$a/$a") }
                Action(Icons.Outlined.Info, t("عن السورة", "Surah info")) { onOpen("surah_info/$s") }
                Action(Icons.AutoMirrored.Outlined.ViewList, t("عرض الآيات", "Verse view")) { onOpen("reader/$s/$a") }
            }
            if (playingHere) Muted(t("تُتلى الآن بصوت ", "Now reciting: ") + (if (ar) audio.reciterAr else audio.reciterEn.ifBlank { audio.reciterAr }))
            VSpace(8.dp)
            // Highlight colours (QR09)
            Muted(t("تظليل بلون", "Highlight"))
            VSpace(6.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val cur = qu.highlights[ref]
                HL_COLORS.forEachIndexed { i, c -> Dot(c, 30, selected = cur == i, modifier = Modifier.press { scope.launch { store.setHighlight(s, a, if (cur == i) null else i) } }) }
                if (cur != null) Text(legendLabel(prefs, cur, ar), style = MaterialTheme.typography.labelLarge, color = look.onCardMuted, maxLines = 1)
            }
            if (showCollections) {
                VSpace(12.dp)
                CollectionsPicker(qu, ref)
            }
            if (editNote) {
                VSpace(12.dp)
                NoteEditor(qu, s, a) { editNote = false }
            } else qu.notes[ref]?.let { n ->
                VSpace(12.dp)
                Column(Modifier.fillMaxWidth().surface(RoundedCornerShape(16.dp), elevated = false).padding(12.dp)) {
                    Text(t("ملاحظتي", "My note"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                    if (n.text.isNotBlank()) Text(n.text, style = MaterialTheme.typography.bodyMedium)
                    if (n.tags.isNotEmpty()) Muted(n.tags.joinToString("  ") { "#$it" })
                }
            }
            // Tafsir snippet
            VSpace(14.dp)
            val ted = Editions.get(prefs.tafsir)
            Text(t("التفسير — ", "Tafsir — ") + (ted?.let { if (ar) it.nameAr else it.nameEn } ?: ""), style = MaterialTheme.typography.titleSmall, color = look.cta)
            VSpace(4.dp)
            val tx = tafsir?.getOrNull(g).orEmpty()
            if (tafState is EdState.Error) EditionError { Editions.failed -= prefs.tafsir; tafRetry++ }
            else if (tafsir == null) Muted(t("جارٍ التحميل…", "Loading…"))
            else Text(
                if (tx.length > 700) tx.take(700) + "…" else tx, style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (ted?.rtl != false) TextDirection.Rtl else TextDirection.Ltr),
                color = look.onCard, modifier = Modifier.fillMaxWidth(),
            )
            if (tx.length > 700) Text(t("اقرأ التفسير كاملًا", "Read the full tafsir"), color = look.cta, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp).press { onOpen("tafsir/$s/$a") })
            // Translations
            trIds.forEach { id -> key(id) {
                val ed = Editions.get(id)
                val arr = rememberEdition(id)
                if (ed != null) {
                    VSpace(12.dp)
                    Text(if (ar) ed.nameAr else ed.nameEn, style = MaterialTheme.typography.titleSmall, color = look.cta)
                    Text(arr?.getOrNull(g).orEmpty(), style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (ed.rtl) TextDirection.Rtl else TextDirection.Ltr), modifier = Modifier.fillMaxWidth())
                }
            } }
            if (trIds.isEmpty()) {
                VSpace(12.dp)
                Text(t("أضف ترجمة لمعاني الآيات", "Add a translation"), color = look.cta, style = MaterialTheme.typography.labelLarge, modifier = Modifier.press { onOpen("quran_library") })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionsPicker(qu: QuranUser, ref: String) {
    val look = LocalSakina.current.look
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().surface(RoundedCornerShape(16.dp), elevated = false).padding(12.dp)) {
        Text(t("أضف إلى مجموعة", "Add to a collection"), style = MaterialTheme.typography.labelLarge, color = look.cta)
        VSpace(6.dp)
        val names = (qu.collections.keys - QuranUser.FAV).sorted()
        if (names.isEmpty()) Muted(t("أنشئ مجموعتك الأولى، مثل: آيات الصبر", "Create your first collection, e.g. Verses of patience"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            names.forEach { n -> Chip(n, selected = ref in (qu.collections[n] ?: emptyList()), onClick = { scope.launch { store.toggleInCollection(n, ref) } }) }
        }
        VSpace(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).surface(CircleShape, elevated = false).padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (newName.isEmpty()) Muted(t("اسم مجموعة جديدة", "New collection name"))
                BasicTextField(newName, { newName = it.take(40) }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(8.dp))
            Chip(t("إضافة", "Add"), selected = newName.isNotBlank(), onClick = {
                val n = newName.trim(); if (n.isNotEmpty() && n != QuranUser.FAV) { scope.launch { store.createCollection(n); store.toggleInCollection(n, ref) }; newName = "" }
            })
        }
    }
}

@Composable
private fun NoteEditor(qu: QuranUser, s: Int, a: Int, onDone: () -> Unit) {
    val look = LocalSakina.current.look
    val store = LocalQStore.current
    val scope = rememberCoroutineScope()
    val cur = qu.notes["$s:$a"]
    var text by remember { mutableStateOf(cur?.text ?: "") }
    var tags by remember { mutableStateOf(cur?.tags?.joinToString(", ") ?: "") }
    Column(Modifier.fillMaxWidth().surface(RoundedCornerShape(16.dp), elevated = false).padding(12.dp)) {
        Text(t("ملاحظة ووسوم", "Note & tags"), style = MaterialTheme.typography.labelLarge, color = look.cta)
        VSpace(6.dp)
        Box(Modifier.fillMaxWidth().heightIn(min = 90.dp).clip(RoundedCornerShape(12.dp)).background(look.onCardMuted.copy(alpha = .08f)).padding(10.dp)) {
            if (text.isEmpty()) Muted(t("اكتب تدبّرك أو فائدة من هذه الآية…", "Write your reflection on this ayah…"))
            BasicTextField(text, { text = it.take(4000) }, textStyle = MaterialTheme.typography.bodyMedium.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
        }
        VSpace(8.dp)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(look.onCardMuted.copy(alpha = .08f)).padding(10.dp)) {
            if (tags.isEmpty()) Muted(t("وسوم مفصولة بفاصلة: صبر، دعاء", "Tags separated by commas: patience, dua"))
            BasicTextField(tags, { tags = it.take(200) }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
        }
        VSpace(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CtaPill(t("حفظ", "Save")) {
                scope.launch { store.setNote(s, a, text, tags.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }) }; onDone()
            }
            if (cur != null) Chip(t("حذف", "Delete"), onClick = { scope.launch { store.setNote(s, a, "", emptyList()) }; onDone() })
        }
    }
}
