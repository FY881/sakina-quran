package app.sakina.quran.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.sakina.quran.R
import app.sakina.quran.data.GlassLevel
import app.sakina.quran.data.IconPackId
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.ThemeMode
import app.sakina.quran.prayer.Hijri
import app.sakina.quran.ui.theme.Looks.withAccent
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

val Gold = Color(0xFFE9C46A)
val DeepGreen = Color(0xFF0E5E4E)

val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)
val QuranFont = FontFamily(Font(R.font.amiri_quran))

/** The resolved visual style available to every composable. */
@Immutable
data class SakinaStyle(
    val look: Look,
    val glass: GlassLevel,
    /** Real backdrop blur is active (glass on, API 31+ where Haze enables it). */
    val blur: Boolean,
    val haze: HazeState,
    val pack: IconPackId,
)

val LocalSakina = staticCompositionLocalOf<SakinaStyle> { error("SakinaTheme missing") }

private fun typography(): Typography {
    val b = Typography()
    fun TextStyle.p() = copy(fontFamily = Plex)
    return Typography(
        displayLarge = b.displayLarge.p(), displayMedium = b.displayMedium.p(), displaySmall = b.displaySmall.p().copy(fontWeight = FontWeight.Bold),
        headlineLarge = b.headlineLarge.p().copy(fontWeight = FontWeight.Bold), headlineMedium = b.headlineMedium.p().copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = b.headlineSmall.p().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = b.titleLarge.p().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.p().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.p().copy(fontWeight = FontWeight.Medium),
        bodyLarge = b.bodyLarge.p().copy(lineHeight = 26.sp), bodyMedium = b.bodyMedium.p().copy(lineHeight = 22.sp),
        bodySmall = b.bodySmall.p(),
        labelLarge = b.labelLarge.p().copy(fontWeight = FontWeight.SemiBold), labelMedium = b.labelMedium.p(), labelSmall = b.labelSmall.p(),
    )
}

private val Typo = typography()

/** Resolve a [Look] for [prefs] (also used by the theme picker's live previews). */
@Composable
fun resolveLook(prefs: Prefs, theme: app.sakina.quran.data.AppTheme = prefs.appTheme, mode: ThemeMode = prefs.theme): Look {
    val ctx = LocalContext.current
    val sysDark = isSystemInDarkTheme()
    val dark = when (mode) { ThemeMode.SYSTEM -> sysDark; ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val (m, d) = remember(prefs.hijriAdj) { Hijri.monthDay(System.currentTimeMillis(), prefs.hijriAdj) }
    return remember(theme, dark, prefs.accent, m, d) { Looks.of(theme, dark, ctx, m, d).withAccent(prefs.accent) }
}

fun IconPackId.resolve(look: Look): IconPackId = if (this == IconPackId.AUTO) look.autoPack else this

@Composable
fun SakinaTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val look = resolveLook(prefs)
    val blur = prefs.glass != GlassLevel.OFF && Build.VERSION.SDK_INT >= 32
    val haze = rememberHazeState(blurEnabled = blur)
    val style = SakinaStyle(look, prefs.glass, blur, haze, prefs.iconPack.resolve(look))
    CompositionLocalProvider(LocalSakina provides style) {
        MaterialTheme(colorScheme = look.scheme, typography = Typo, content = content)
    }
}
