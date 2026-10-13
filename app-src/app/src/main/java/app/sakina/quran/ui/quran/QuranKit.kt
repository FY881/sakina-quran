package app.sakina.quran.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sakina.quran.data.QuranStore
import app.sakina.quran.data.QuranUser
import app.sakina.quran.data.ReaderPrefs
import app.sakina.quran.quran.Editions
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Tajweed
import app.sakina.quran.quran.parseTranslit
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.kit.press

/** The Quran store, provided by AppRoot. */
val LocalQStore = staticCompositionLocalOf<QuranStore> { error("QuranStore missing") }

@Composable
fun rememberQUser(): QuranUser {
    val store = LocalQStore.current
    val q by store.data.collectAsStateWithLifecycle(initialValue = QuranUser())
    return q
}

/** Loading state of an edition. */
sealed interface EdState {
    data object Loading : EdState
    data object Error : EdState
    data class Ready(val data: Array<String>) : EdState
}

/** Loads an edition off the main thread with an explicit error state; [retry] bumps to reload. */
@Composable
fun rememberEditionState(id: String?, retry: Int = 0): EdState? {
    val ctx = LocalContext.current
    if (id == null) return null
    val v by produceState<EdState>(Editions.cached(id)?.let { EdState.Ready(it) } ?: EdState.Loading, id, retry) {
        if (value is EdState.Ready) return@produceState
        value = EdState.Loading
        val r = runCatching { kotlinx.coroutines.withTimeout(20_000) { Editions.load(ctx, id) } }.getOrNull()
        value = if (r != null) EdState.Ready(r) else EdState.Error
    }
    return v
}

/** Loads a tafsir/translation edition (null while loading, on error, or when not downloaded). */
@Composable
fun rememberEdition(id: String?): Array<String>? = (rememberEditionState(id) as? EdState.Ready)?.data

/** Error line with a retry action, used where an edition failed to load. */
@Composable
fun EditionError(onRetry: () -> Unit) {
    val look = LocalSakina.current.look
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            app.sakina.quran.ui.common.t("تعذّر تحميل التفسير.", "Couldn't load this edition."),
            style = MaterialTheme.typography.bodyMedium, color = look.onCardMuted, modifier = Modifier.weight(1f),
        )
        Text(
            app.sakina.quran.ui.common.t("إعادة المحاولة", "Retry"), style = MaterialTheme.typography.labelLarge, color = look.cta,
            modifier = Modifier.clip(CircleShape).border(1.dp, look.cta.copy(alpha = .5f), CircleShape).press { onRetry() }.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** Simple (Imlaei) script, loaded only when [enabled]. */
@Composable
fun rememberSimple(enabled: Boolean): Array<String>? {
    val ctx = LocalContext.current
    val v by produceState<Array<String>?>(null, enabled) { value = if (!enabled) null else runCatching { Quran.simple(ctx) }.getOrNull() }
    return v
}

/** Translation ids to show: user choice, or automatic (EN UI → Saheeh International, AR UI → none). */
fun ReaderPrefs.activeTranslations(arabicUi: Boolean): List<String> = translations ?: if (arabicUi) emptyList() else listOf("en.sahih")

/** The five highlight colours (QR09). */
val HL_COLORS = listOf(Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFF48FB1), Color(0xFFFFB74D))
val HL_DEFAULT_AR = listOf("مفضلة", "رجاء ورحمة", "تدبّر", "دعاء", "للحفظ")
val HL_DEFAULT_EN = listOf("Favourite", "Hope & mercy", "Reflect", "Dua", "To memorise")
fun legendLabel(p: ReaderPrefs, i: Int, ar: Boolean) = p.legend.getOrNull(i)?.takeIf { it.isNotBlank() } ?: if (ar) HL_DEFAULT_AR[i] else HL_DEFAULT_EN[i]

/** Ayah text with optional tajweed colours (text unchanged; only colour spans are added). */
fun ayahText(text: String, tajweed: Boolean, palette: Int, dark: Boolean): AnnotatedString = buildAnnotatedString {
    append(text)
    if (tajweed) for (s in Tajweed.spans(text)) addStyle(SpanStyle(color = Color(Tajweed.color(s.rule, palette, dark))), s.start, s.end)
}

/** Tanzil transliteration with long vowels underlined and merged letters bold. */
fun translitText(raw: String): AnnotatedString = buildAnnotatedString {
    for (r in parseTranslit(raw)) {
        val st = SpanStyle(
            fontStyle = FontStyle.Italic,
            textDecoration = if (r.long) TextDecoration.Underline else null,
            fontWeight = if (r.bold) FontWeight.Bold else null,
        )
        pushStyle(st); append(r.text); pop()
    }
}

@Composable
fun RtlBox(content: @Composable () -> Unit) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)

@Composable
fun LtrBox(content: @Composable () -> Unit) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

/** Keeps the screen awake while [on] (QR18). */
@Composable
fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) { view.keepScreenOn = on; onDispose { view.keepScreenOn = false } }
}

/** Counts reading time while the screen is shown (30-second slices). */
@Composable
fun ReadingTimer() {
    val store = LocalQStore.current
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); runCatching { store.addSeconds(30) } } }
}

/** Small colour dot. */
@Composable
fun Dot(c: Color, size: Int = 18, selected: Boolean = false, modifier: Modifier = Modifier) {
    val look = LocalSakina.current.look
    Box(modifier.size(size.dp).clip(CircleShape).background(c).then(if (selected) Modifier.border(2.dp, look.onCard, CircleShape) else Modifier))
}

/** Progress bar used by khatma / stats. */
@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = LocalSakina.current.look.cta) {
    val look = LocalSakina.current.look
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .2f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(color))
    }
}

/** Big number + label tile. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val look = LocalSakina.current.look
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = look.onCard, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = look.onCardMuted, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

fun suraRef(q: Quran, s: Int, a: Int, ar: Boolean) = if (ar) "${q.suras[s - 1].name} ${num(a, true)}" else "${q.suras[s - 1].tname} $s:$a"
