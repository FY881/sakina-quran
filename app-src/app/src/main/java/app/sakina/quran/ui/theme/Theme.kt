package app.sakina.quran.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.sakina.quran.R
import app.sakina.quran.data.ThemeMode

val Gold = Color(0xFFE9C46A)
val DeepGreen = Color(0xFF0E5E4E)

val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)
val QuranFont = FontFamily(Font(R.font.amiri_quran))

private val Light = lightColorScheme(
    primary = DeepGreen, onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE2), onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF9A7B2F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E7BE), onSecondaryContainer = Color(0xFF2E2300),
    background = Color(0xFFF6F3EA), onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFBF9F3), onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE6E2D6), onSurfaceVariant = Color(0xFF4A4740),
    surfaceContainer = Color(0xFFF0ECE1), surfaceContainerHigh = Color(0xFFEAE6DA),
    outline = Color(0xFF7B776C),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF7FD6BF), onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF0E5E4E), onPrimaryContainer = Color(0xFFCDEBE2),
    secondary = Gold, onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF574400), onSecondaryContainer = Color(0xFFFFE08E),
    background = Color(0xFF0F1614), onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF141C1A), onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF2B3532), onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceContainer = Color(0xFF1A2321), surfaceContainerHigh = Color(0xFF212B28),
    outline = Color(0xFF89938F),
)

private fun typography(): Typography {
    val b = Typography()
    fun TextStyle.p() = copy(fontFamily = Plex)
    return Typography(
        displayLarge = b.displayLarge.p(), displayMedium = b.displayMedium.p(), displaySmall = b.displaySmall.p(),
        headlineLarge = b.headlineLarge.p(), headlineMedium = b.headlineMedium.p(), headlineSmall = b.headlineSmall.p(),
        titleLarge = b.titleLarge.p().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.p().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.p(),
        bodyLarge = b.bodyLarge.p().copy(lineHeight = 26.sp), bodyMedium = b.bodyMedium.p().copy(lineHeight = 22.sp),
        bodySmall = b.bodySmall.p(),
        labelLarge = b.labelLarge.p(), labelMedium = b.labelMedium.p(), labelSmall = b.labelSmall.p(),
    )
}

@Composable
fun SakinaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = typography(), content = content)
}
