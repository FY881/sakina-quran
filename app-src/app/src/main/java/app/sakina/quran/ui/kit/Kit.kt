package app.sakina.quran.ui.kit

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.GlassLevel
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.theme.LocalSakina
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val CardShape = RoundedCornerShape(22.dp)

/** Spring "press" feedback + haptic tick. */
@Composable
fun Modifier.press(enabled: Boolean = true, haptic: Boolean = true, onClick: (() -> Unit)?): Modifier {
    if (onClick == null || !enabled) return this
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .965f else 1f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow), label = "press")
    val view = LocalView.current
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(src, indication = androidx.compose.material3.ripple()) {
            if (haptic) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
}

/**
 * The card material. Classic theme: solid teal card with hairline border (Muslim-Pro-like).
 * Glass themes: real backdrop blur (Haze) + tint + specular sheen + inner highlight stroke + soft shadow.
 * API < 32 or glass OFF: tinted near-opaque fallback.
 */
@Composable
fun Modifier.surface(shape: Shape = CardShape, tint: Color? = null, strong: Boolean = false, elevated: Boolean = true): Modifier {
    val st = LocalSakina.current
    val l = st.look
    val base = tint ?: l.card
    val glassy = st.blur && l.glassAlpha < .99f
    val alpha = when {
        !glassy -> if (st.glass == GlassLevel.OFF || l.glassAlpha >= .99f) 1f else (l.glassAlpha + .3f).coerceAtMost(.96f)
        strong -> (l.glassAlpha + .2f).coerceAtMost(.95f)
        st.glass == GlassLevel.LIGHT -> (l.glassAlpha + .15f).coerceAtMost(.95f)
        else -> l.glassAlpha
    }
    val blurR = if (st.glass == GlassLevel.LIGHT) 18.dp else 30.dp
    val sheen = l.glassAlpha < .7f && st.glass != GlassLevel.OFF
    var m = this
    if (elevated && !l.dark) m = m.shadow(10.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
    m = m.clip(shape)
    m = if (glassy) m.hazeEffect(st.haze) {
        blurRadius = blurR
        tints = listOf(HazeTint(base.copy(alpha = alpha)))
        noiseFactor = if (l.dark) .06f else .04f
        backgroundColor = l.bgBottom
    } else m.background(base.copy(alpha = alpha))
    if (sheen) m = m.drawWithContent {
        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (l.dark) .10f else .28f), Color.Transparent, Color.Transparent), Offset.Zero, Offset(size.width * .7f, size.height)))
        drawContent()
    }
    val borderBrush = if (sheen) Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (l.dark) .30f else .85f), Color.White.copy(alpha = .05f), Color.White.copy(alpha = if (l.dark) .12f else .4f))
    ) else Brush.linearGradient(listOf(l.cardBorder, l.cardBorder))
    return m.border(1.dp, borderBrush, shape)
}

@Composable
fun SCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp),
    shape: Shape = CardShape, tint: Color? = null, strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val l = LocalSakina.current.look
    CompositionLocalProvider(LocalContentColor provides l.onCard) {
        Column(modifier.fillMaxWidth().press(onClick = onClick).surface(shape, tint, strong).padding(padding), content = content)
    }
}

@Composable
fun SBox(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, shape: Shape = CardShape, content: @Composable BoxScope.() -> Unit) {
    val l = LocalSakina.current.look
    CompositionLocalProvider(LocalContentColor provides l.onCard) {
        Box(modifier.press(onClick = onClick).surface(shape), content = content)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** Chip: selected = solid contrast pill (white in classic dark), otherwise card-coloured with hairline. */
@Composable
fun Chip(text: String, selected: Boolean = false, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val l = LocalSakina.current.look
    Row(
        modifier.clip(CircleShape)
            .then(if (selected) Modifier.background(l.chipSelected) else Modifier.surface(CircleShape, elevated = false))
            .press(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) l.onChipSelected else l.onCard, maxLines = 1)
    }
}

/** Bright call-to-action pill. */
@Composable
fun CtaPill(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val l = LocalSakina.current.look
    Row(
        modifier.clip(CircleShape).background(if (enabled) l.cta else l.cta.copy(alpha = .4f)).press(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        CompositionLocalProvider(LocalContentColor provides l.onCta) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = l.onCta, maxLines = 1)
        }
    }
}

/** Secondary outlined pill. */
@Composable
fun GhostPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val l = LocalSakina.current.look
    Box(modifier.clip(CircleShape).border(1.dp, l.onCard.copy(alpha = .5f), CircleShape).press(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = l.onCard, maxLines = 1)
    }
}

@Composable
fun Chevron(tint: Color = LocalSakina.current.look.onCardMuted) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Icon(if (rtl) Icons.AutoMirrored.Outlined.KeyboardArrowLeft else Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = tint, modifier = Modifier.size(22.dp))
}

/** Settings-style row with a 3D icon, title, optional subtitle and trailing content (chevron by default). */
@Composable
fun ListRow(
    ico: Ico?, title: String, subtitle: String? = null, modifier: Modifier = Modifier, iconSize: Dp = 30.dp,
    trailing: (@Composable () -> Unit)? = { Chevron() }, onClick: (() -> Unit)? = null,
) {
    val l = LocalSakina.current.look
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).press(onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ico != null) { PackIcon(ico, iconSize); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = l.onCard, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = l.onCardMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = LocalSakina.current.look.onCardMuted)

/** Rounded badge (e.g. "now", counters). */
@Composable
fun Badge(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) { Box(Modifier.size(6.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(5.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

/** Screen header for pushed screens: back arrow, title (+ optional subtitle) and trailing actions. */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, subtitle: String? = null, trailing: (@Composable RowScope.() -> Unit)? = null) {
    val l = LocalSakina.current.look
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = l.onCardMuted, maxLines = 1)
        }
        trailing?.invoke(this)
    }
}
