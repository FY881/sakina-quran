package app.sakina.quran.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG 2.x contrast ratio between two opaque colours (1..21). */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance(); val lb = b.luminance()
    return (maxOf(la, lb) + .05f) / (minOf(la, lb) + .05f)
}

/**
 * Returns [fg] nudged toward white or black (keeping its hue as long as possible) until it reaches
 * [min] contrast on [bg] (WCAG AA = 4.5 for normal text).
 */
fun readableOn(fg: Color, bg: Color, min: Float = 4.5f): Color {
    val solidBg = if (bg.alpha < 1f) bg.compositeOver(Color.Black) else bg
    val f = if (fg.alpha < 1f) fg.compositeOver(solidBg) else fg
    if (contrast(f, solidBg) >= min) return f
    val target = if (solidBg.luminance() < .4f) Color.White else Color.Black
    var t = 0f
    while (t < 1f) { t += .05f; val c = lerp(f, target, t); if (contrast(c, solidBg) >= min) return c }
    return target
}

/** Badge colours (background, text) for a tone on a surface, always ≥ 4.5:1. */
fun badgeColors(tone: Color, surface: Color, dark: Boolean): Pair<Color, Color> {
    val bg = tone.copy(alpha = if (dark) .22f else .16f).compositeOver(surface)
    return bg to readableOn(tone, bg)
}

/** Warning badge on [surface]: solid warn fill with a light/dark red text that passes AA. */
fun warnBadgeColors(warn: Color): Pair<Color, Color> {
    val dark = warn.luminance() < .3f
    val fg = if (dark) Color(0xFFFFE1E1) else Color(0xFF8C1D18)
    return warn to readableOn(fg, warn)
}
