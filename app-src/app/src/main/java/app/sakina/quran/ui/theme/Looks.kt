package app.sakina.quran.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import app.sakina.quran.data.AppTheme
import app.sakina.quran.data.IconPackId

/**
 * Everything a theme decides beyond Material colors: background, card material, decoration and
 * which icon pack suits it.
 */
data class Look(
    val theme: AppTheme,
    val dark: Boolean,
    val scheme: ColorScheme,
    /** Background: [bgTop]→[bgBottom] vertical gradient plus animated colour blobs ([mesh], may be empty). */
    val bgTop: Color,
    val bgBottom: Color,
    val mesh: List<Color>,
    /** Card fill (opaque version) and its hairline border. */
    val card: Color,
    val cardBorder: Color,
    /** Card fill alpha when real blur is active (lower = more glass). */
    val glassAlpha: Float,
    /** Text/icons on cards. */
    val onCard: Color,
    val onCardMuted: Color,
    /** Bright call-to-action pill (e.g. "mark all prayed"). */
    val cta: Color,
    val onCta: Color,
    /** Selected chip (white pill in the classic theme). */
    val chipSelected: Color,
    val onChipSelected: Color,
    val pattern: Color,
    val patternAlpha: Float,
    val stars: Boolean,
    val sky: Boolean,
    val floatingBar: Boolean,
    val autoPack: IconPackId,
    /** Warning banner (health check). */
    val warn: Color,
    val seasonLabelAr: String? = null,
    val seasonLabelEn: String? = null,
)

private fun scheme(
    dark: Boolean, primary: Color, onPrimary: Color, secondary: Color, bg: Color, card: Color,
    onSurface: Color, muted: Color, outline: Color, error: Color = if (dark) Color(0xFFFF8A80) else Color(0xFFB3261E),
): ColorScheme {
    val pc = primary.copy(alpha = if (dark) .28f else .16f).compositeOver(card)
    val onPc = if (pc.luminance() > .45f) Color(0xFF0C1A16) else Color.White
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = pc, onPrimaryContainer = onPc,
        secondary = secondary, onSecondary = Color(0xFF1A1300), secondaryContainer = secondary.copy(alpha = .22f).compositeOver(card),
        onSecondaryContainer = onSurface, tertiary = secondary,
        background = bg, onBackground = onSurface, surface = card, onSurface = onSurface,
        surfaceVariant = outline.copy(alpha = .5f).compositeOver(card), onSurfaceVariant = muted,
        surfaceContainer = card, surfaceContainerHigh = Color.White.copy(alpha = .06f).compositeOver(card),
        surfaceContainerHighest = Color.White.copy(alpha = .1f).compositeOver(card), surfaceContainerLow = card,
        outline = outline, outlineVariant = outline.copy(alpha = .6f), error = error,
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = pc, onPrimaryContainer = onPc,
        secondary = secondary, onSecondary = Color.White, secondaryContainer = secondary.copy(alpha = .16f).compositeOver(card),
        onSecondaryContainer = onSurface, tertiary = secondary,
        background = bg, onBackground = onSurface, surface = card, onSurface = onSurface,
        surfaceVariant = outline.copy(alpha = .35f).compositeOver(card), onSurfaceVariant = muted,
        surfaceContainer = card, surfaceContainerHigh = Color.Black.copy(alpha = .04f).compositeOver(card),
        surfaceContainerHighest = Color.Black.copy(alpha = .07f).compositeOver(card), surfaceContainerLow = card,
        outline = outline, outlineVariant = outline.copy(alpha = .6f), error = error,
    )
}

private fun look(
    theme: AppTheme, dark: Boolean, primary: Color, onPrimary: Color, secondary: Color,
    bgTop: Color, bgBottom: Color, mesh: List<Color>, card: Color, border: Color, onCard: Color, muted: Color,
    glassAlpha: Float, cta: Color = primary, onCta: Color = onPrimary,
    chip: Color = if (dark) Color.White else primary, onChip: Color = if (dark) Color(0xFF0B2F2B) else Color.White,
    pattern: Color = secondary, patternAlpha: Float = .05f, stars: Boolean = false, sky: Boolean = true,
    floatingBar: Boolean = false, pack: IconPackId = IconPackId.FLUENT_3D,
    warn: Color = if (dark) Color(0xFF7A1F2B) else Color(0xFFFCE4E4),
) = Look(
    theme, dark, scheme(dark, primary, onPrimary, secondary, bgBottom, card, onCard, muted, border),
    bgTop, bgBottom, mesh, card, border, glassAlpha, onCard, muted, cta, onCta, chip, onChip,
    pattern, patternAlpha, stars, sky, floatingBar, pack, warn,
)

object Looks {
    /** Hijri-season skin for [AppTheme.SEASONAL]. month 1..12. */
    fun seasonal(dark: Boolean, month: Int, day: Int): Look = when {
        month == 9 -> look(
            AppTheme.SEASONAL, true, Color(0xFFF2C94C), Color(0xFF241A00), Color(0xFFB39DDB),
            Color(0xFF1B1036), Color(0xFF0E0A22), listOf(Color(0xFF3A1E6E), Color(0xFF6B4A12), Color(0xFF1A2F6B), Color(0xFF4A2A7A)),
            Color(0xFF221844), Color(0xFF3B2D66), Color(0xFFFFF6E0), Color(0xFFC9BEE6), .55f,
            pattern = Color(0xFFF2C94C), patternAlpha = .07f, stars = true,
        ).copy(seasonLabelAr = "رمضان كريم", seasonLabelEn = "Ramadan")
        (month == 10 && day <= 3) || (month == 12 && day in 10..13) -> look(
            AppTheme.SEASONAL, dark, if (dark) Color(0xFF7FE0B0) else Color(0xFF0F7A4F), if (dark) Color(0xFF00301C) else Color.White, Color(0xFFD4A62A),
            if (dark) Color(0xFF0A2A20) else Color(0xFFF4FBF6), if (dark) Color(0xFF051A14) else Color(0xFFFFF8E6),
            listOf(Color(0xFF2E8B57), Color(0xFFD4A62A), Color(0xFF1F6F8B), Color(0xFFE07A5F)).map { it.copy(alpha = if (dark) .55f else .35f) },
            if (dark) Color(0xFF103A2E) else Color.White, if (dark) Color(0xFF1F5A47) else Color(0xFFE5EFE7),
            if (dark) Color.White else Color(0xFF0E2A20), if (dark) Color(0xFFB5D5C8) else Color(0xFF52665D), .6f,
        ).copy(seasonLabelAr = "عيد مبارك", seasonLabelEn = "Eid Mubarak")
        month == 12 && day <= 13 -> look(
            AppTheme.SEASONAL, dark, if (dark) Color(0xFFE6C27A) else Color(0xFF7A5A1E), if (dark) Color(0xFF2A1E00) else Color.White, Color(0xFF3E6B5A),
            if (dark) Color(0xFF14110C) else Color(0xFFFBF8F1), if (dark) Color(0xFF0A0906) else Color(0xFFF1E9D8),
            listOf(Color(0xFFEDE3CC), Color(0xFFD9C7A0), Color(0xFFFFFFFF), Color(0xFFCBB68A)).map { it.copy(alpha = if (dark) .12f else .7f) },
            if (dark) Color(0xFF1F1A12) else Color.White, if (dark) Color(0xFF3A3122) else Color(0xFFE8DFCC),
            if (dark) Color(0xFFF6EEDC) else Color(0xFF2A2214), if (dark) Color(0xFFBFB39A) else Color(0xFF6E624B), .6f,
        ).copy(seasonLabelAr = "أيام الحج", seasonLabelEn = "Hajj season")
        else -> of(AppTheme.CLASSIC_TEAL, dark, null, 1, 1).copy(theme = AppTheme.SEASONAL, seasonLabelAr = "بلا موسم الآن", seasonLabelEn = "No season now")
    }

    fun of(theme: AppTheme, darkWanted: Boolean, ctx: Context?, hijriMonth: Int, hijriDay: Int): Look {
        val dark = when {
            !theme.hasLight -> true
            !theme.hasDark -> false
            else -> darkWanted
        }
        return when (theme) {
            AppTheme.CLASSIC_TEAL -> if (dark) look(
                theme, true, Color(0xFF2BD47D), Color(0xFF02240F), Color(0xFFE9C46A),
                Color(0xFF0F3F39), Color(0xFF0A2B27), emptyList(),
                Color(0xFF164A44), Color(0xFF2A5E57), Color.White, Color(0xFFA9C7C1), .82f,
                cta = Color(0xFF22C55E), onCta = Color(0xFF032912), pattern = Color(0xFF7FD6BF), patternAlpha = .035f,
                warn = Color(0xFF7A1F2B),
            ) else look(
                theme, false, Color(0xFF0E7C5A), Color.White, Color(0xFFA9791F),
                Color(0xFFE6F2EE), Color(0xFFF4F8F6), emptyList(),
                Color.White, Color(0xFFD3E5DF), Color(0xFF0B2F2B), Color(0xFF52706A), .82f,
                cta = Color(0xFF16A34A), onCta = Color.White, chip = Color(0xFF0B2F2B), onChip = Color.White,
                pattern = Color(0xFF0E7C5A), patternAlpha = .04f,
            )
            AppTheme.LIQUID_GLASS -> if (dark) look(
                theme, true, Color(0xFF6FE0C6), Color(0xFF00382D), Color(0xFFE9C46A),
                Color(0xFF0D1018), Color(0xFF07090E),
                listOf(Color(0xFF1E3A5F), Color(0xFF3B2A5C), Color(0xFF0F4C46), Color(0xFF4A2E3A)),
                Color(0xFF1A1F29), Color(0x33FFFFFF), Color(0xFFEEF1F5), Color(0xFFA9B1BE), .42f,
                pattern = Color.White, patternAlpha = .035f, floatingBar = true,
            ) else look(
                theme, false, Color(0xFF0B7A66), Color.White, Color(0xFFB08A2E),
                Color(0xFFE9EEF7), Color(0xFFF4F1F8),
                listOf(Color(0xFF9CC5FF), Color(0xFFD9C4FF), Color(0xFFAEEBD8), Color(0xFFFFD2B3)),
                Color.White, Color(0x99FFFFFF), Color(0xFF10141A), Color(0xFF4A5361), .5f,
                pattern = Color(0xFF0B7A66), patternAlpha = .04f, floatingBar = true,
            )
            AppTheme.MADINAH_NIGHT -> look(
                theme, true, Color(0xFFE9C46A), Color(0xFF2A1F00), Color(0xFF8FB7FF),
                Color(0xFF0A1530), Color(0xFF050A1A),
                listOf(Color(0xFF0B1E4A), Color(0xFF1A2F6B), Color(0xFF2B1E52), Color(0xFF0E3A5A)),
                Color(0xFF111C3F), Color(0xFF2A3766), Color(0xFFF3EEDF), Color(0xFFB9B5A8), .55f,
                chip = Color(0xFFE9C46A), onChip = Color(0xFF2A1F00), pattern = Color(0xFFE9C46A), patternAlpha = .06f, stars = true,
            )
            AppTheme.EMERALD_ANDALUS -> if (dark) look(
                theme, true, Color(0xFF7FD6BF), Color(0xFF00382D), Color(0xFFE9C46A),
                Color(0xFF062219), Color(0xFF03130E),
                listOf(Color(0xFF0A3A2C), Color(0xFF0E5E4E), Color(0xFF1E3B20), Color(0xFF3D3315)),
                Color(0xFF0B2A22), Color(0xFF22493D), Color(0xFFEAF3EE), Color(0xFFA8C3B8), .55f,
                pattern = Color(0xFFE9C46A), patternAlpha = .06f,
            ) else look(
                theme, false, Color(0xFF0E5E4E), Color.White, Color(0xFFA07A2C),
                Color(0xFFF2EEDF), Color(0xFFF8F5EC),
                listOf(Color(0xFFCFE8DA), Color(0xFFF3E7C6), Color(0xFFB9DCC9), Color(0xFFEAD9B0)),
                Color(0xFFFFFBF0), Color(0xFFE3DCC6), Color(0xFF172420), Color(0xFF55645E), .6f,
                pattern = Color(0xFF0E5E4E), patternAlpha = .07f,
            )
            AppTheme.DESERT_SAND -> if (dark) look(
                theme, true, Color(0xFFE8B56A), Color(0xFF2E1C00), Color(0xFF7FD6BF),
                Color(0xFF221A10), Color(0xFF140F08),
                listOf(Color(0xFF3A2A16), Color(0xFF4F3920), Color(0xFF2E2414), Color(0xFF5A4022)),
                Color(0xFF2A2015), Color(0xFF4A3B27), Color(0xFFF3E6CC), Color(0xFFC2B194), .55f,
                chip = Color(0xFFE8B56A), onChip = Color(0xFF2E1C00), pattern = Color(0xFFE8B56A), patternAlpha = .05f,
            ) else look(
                theme, false, Color(0xFF8A5A1F), Color.White, Color(0xFF0E5E4E),
                Color(0xFFF0DFC0), Color(0xFFF7EBD5),
                listOf(Color(0xFFF7E3BE), Color(0xFFEBCB9A), Color(0xFFF2D8B0), Color(0xFFE6C08A)),
                Color(0xFFFFF6E4), Color(0xFFE6D3B0), Color(0xFF2E2215), Color(0xFF6B5A44), .6f,
                chip = Color(0xFF8A5A1F), pattern = Color(0xFF8A5A1F), patternAlpha = .06f,
            )
            AppTheme.OTTOMAN_RUBY -> if (dark) look(
                theme, true, Color(0xFFE3B758), Color(0xFF2A1C00), Color(0xFFF2A3AE),
                Color(0xFF2A070D), Color(0xFF160306),
                listOf(Color(0xFF5A0E1A), Color(0xFF8B1A2B), Color(0xFF3A0A12), Color(0xFF6B4A12)),
                Color(0xFF2E0B12), Color(0xFF5A2430), Color(0xFFFBEFE6), Color(0xFFD9B9B0), .55f,
                chip = Color(0xFFE3B758), onChip = Color(0xFF2A1C00), pattern = Color(0xFFE3B758), patternAlpha = .07f,
            ) else look(
                theme, false, Color(0xFF8B1A2B), Color.White, Color(0xFFA87B1E),
                Color(0xFFF8E6E1), Color(0xFFFBF1EC),
                listOf(Color(0xFFF6D0D3), Color(0xFFF3E1B5), Color(0xFFEFC1C6), Color(0xFFF8E6D6)),
                Color.White, Color(0xFFEBD3D0), Color(0xFF2A0F14), Color(0xFF6E4A4F), .6f,
                pattern = Color(0xFF8B1A2B), patternAlpha = .05f,
            )
            AppTheme.ROSE_DAWN -> if (dark) look(
                theme, true, Color(0xFFFFB1C6), Color(0xFF4A1026), Color(0xFFFFCF99),
                Color(0xFF241520), Color(0xFF140B11),
                listOf(Color(0xFF4A2235), Color(0xFF5A3328), Color(0xFF33244A), Color(0xFF4F2A2A)),
                Color(0xFF2A1820), Color(0xFF4A3038), Color(0xFFFBE9EE), Color(0xFFD1B5BE), .5f,
                chip = Color(0xFFFFB1C6), onChip = Color(0xFF4A1026), pattern = Color(0xFFFFB1C6), patternAlpha = .05f,
            ) else look(
                theme, false, Color(0xFFB4466A), Color.White, Color(0xFFC98A3A),
                Color(0xFFFFE6E4), Color(0xFFFFF5F0),
                listOf(Color(0xFFFFC7D0), Color(0xFFFFDDBD), Color(0xFFE3CCFF), Color(0xFFFFC2AE)),
                Color.White, Color(0x99FFFFFF), Color(0xFF2B1A20), Color(0xFF6E5560), .5f,
                pattern = Color(0xFFB4466A), patternAlpha = .04f, floatingBar = true,
            )
            AppTheme.AMOLED -> look(
                theme, true, Color(0xFF5EE0C0), Color(0xFF002A20), Color(0xFFE9C46A),
                Color.Black, Color.Black, emptyList(),
                Color(0xFF0B0B0B), Color(0xFF242424), Color.White, Color(0xFFB0B0B0), .8f,
                chip = Color.White, onChip = Color.Black, pattern = Color.White, patternAlpha = .0f, sky = false,
                pack = IconPackId.FLUENT_MONO, warn = Color(0xFF5A1620),
            )
            AppTheme.HIGH_CONTRAST -> if (dark) look(
                theme, true, Color(0xFFFFD54F), Color.Black, Color(0xFF80D8FF),
                Color.Black, Color.Black, emptyList(),
                Color.Black, Color.White, Color.White, Color(0xFFEDEDED), 1f,
                chip = Color(0xFFFFD54F), onChip = Color.Black, patternAlpha = 0f, sky = false,
                pack = IconPackId.FLUENT_MONO, warn = Color(0xFF8B0000),
            ) else look(
                theme, false, Color(0xFF003D33), Color.White, Color(0xFF5A3D00),
                Color.White, Color.White, emptyList(),
                Color.White, Color.Black, Color.Black, Color(0xFF1A1A1A), 1f,
                chip = Color.Black, onChip = Color.White, patternAlpha = 0f, sky = false,
                pack = IconPackId.FLUENT_MONO, warn = Color(0xFFFFE0E0),
            )
            AppTheme.DYNAMIC -> {
                if (ctx != null && Build.VERSION.SDK_INT >= 31) {
                    val s = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                    Look(
                        theme, dark, s, s.surfaceContainerLow, s.background,
                        listOf(s.primaryContainer, s.tertiaryContainer, s.secondaryContainer, s.surfaceVariant).map { it.copy(alpha = .7f) },
                        s.surfaceContainer, s.outlineVariant, .6f, s.onSurface, s.onSurfaceVariant,
                        s.primary, s.onPrimary, s.onSurface, s.surface, s.primary, .04f, false, true, false,
                        IconPackId.FLUENT_COLOR, s.errorContainer,
                    )
                } else of(AppTheme.CLASSIC_TEAL, dark, ctx, hijriMonth, hijriDay).copy(theme = AppTheme.DYNAMIC)
            }
            AppTheme.SEASONAL -> seasonal(dark, hijriMonth, hijriDay)
        }
    }

    /** Apply an accent override to a look. */
    fun Look.withAccent(argb: Int): Look {
        if (argb == 0) return this
        val a = Color(argb)
        val onA = if (a.luminance() > .5f) Color(0xFF0B1512) else Color.White
        return copy(
            scheme = scheme.copy(
                primary = a, onPrimary = onA,
                primaryContainer = a.copy(alpha = if (dark) .28f else .16f).compositeOver(card),
            ),
            cta = a, onCta = onA,
        )
    }
}
