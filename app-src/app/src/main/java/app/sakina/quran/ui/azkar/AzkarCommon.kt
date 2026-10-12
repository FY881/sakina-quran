package app.sakina.quran.ui.azkar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.sakina.quran.azkar.Azkar
import app.sakina.quran.azkar.ShareImage
import app.sakina.quran.quran.Quran
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.Look
import app.sakina.quran.ui.theme.LocalSakina

@Composable
fun rememberAzkar(): State<Azkar?> {
    val ctx = LocalContext.current
    return produceState<Azkar?>(null) { value = runCatching { Azkar.load(ctx) }.getOrNull() }
}

@Composable
fun rememberQuran(): State<Quran?> {
    val ctx = LocalContext.current
    return produceState<Quran?>(null) { value = runCatching { Quran.load(ctx) }.getOrNull() }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalSakina.current.look.cta)
    }
}

/** "مرة واحدة / مرتان / ٣ مرات / ١٠٠ مرة" or "Once / Twice / 3 times". */
fun countLabel(n: Int, ar: Boolean): String = if (ar) when {
    n <= 1 -> "مرة واحدة"
    n == 2 -> "مرتان"
    n in 3..10 -> "${num(n, true)} مرات"
    else -> "${num(n, true)} مرة"
} else when (n) { 1 -> "Once"; 2 -> "Twice"; else -> "$n times" }

fun palette(l: Look) = ShareImage.Palette(
    l.bgTop.copy(alpha = 1f).toArgb(), l.bgBottom.copy(alpha = 1f).toArgb(), l.card.copy(alpha = 1f).toArgb(),
    l.onCard.toArgb(), l.onCardMuted.toArgb(), l.cta.toArgb(),
)

fun copyText(ctx: Context, text: String, ar: Boolean) {
    runCatching { ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("dhikr", text)) }
    Toast.makeText(ctx, if (ar) "نُسخ النص" else "Copied", Toast.LENGTH_SHORT).show()
}

/** Arabic content (dhikr references, virtues, titles) always laid out right-to-left, also inside the English UI. */
@Composable
fun ArabicText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall, color: androidx.compose.ui.graphics.Color = LocalSakina.current.look.onCardMuted, maxLines: Int = Int.MAX_VALUE) {
    androidx.compose.material3.Text(
        text, modifier.fillMaxWidth(), style = style.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Rtl),
        color = color, textAlign = androidx.compose.ui.text.style.TextAlign.Right, maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

@Composable
fun switchColors(): androidx.compose.material3.SwitchColors {
    val l = LocalSakina.current.look
    return androidx.compose.material3.SwitchDefaults.colors(
        checkedTrackColor = l.cta, checkedThumbColor = l.onCta, checkedBorderColor = l.cta,
        uncheckedTrackColor = l.onCardMuted.copy(alpha = .18f), uncheckedThumbColor = l.onCardMuted, uncheckedBorderColor = l.onCardMuted.copy(alpha = .8f),
    )
}

/** "دعاء واحد / دعاءان / ٣ أدعية" or "1 dua / 3 duas". */
fun duasLabel(n: Int, ar: Boolean): String = if (ar) when {
    n == 1 -> "دعاء واحد"; n == 2 -> "دعاءان"; n in 3..10 -> "${num(n, true)} أدعية"; else -> "${num(n, true)} دعاءً"
} else if (n == 1) "1 dua" else "$n duas"

/** English reference for a Quran range, e.g. "Al-Baqara 2:255–257". */
fun enRef(q: app.sakina.quran.quran.Quran, s: Int, a: Int, b: Int): String =
    "${q.suras[s - 1].tname} $s:$a" + if (b != a) "–$b" else ""

@Composable
fun SearchField(q: String, onQ: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val look = LocalSakina.current.look
    Row(modifier.surface(CircleShape, elevated = false).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, tint = look.onCardMuted); Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Muted(hint)
            BasicTextField(q, onQ, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard), cursorBrush = SolidColor(look.cta), modifier = Modifier.fillMaxWidth())
        }
        if (q.isNotEmpty()) Icon(Icons.Outlined.Close, null, tint = look.onCardMuted, modifier = Modifier.size(20.dp).press { onQ("") })
    }
}
