package app.sakina.quran

import androidx.compose.ui.graphics.Color
import app.sakina.quran.ui.theme.badgeColors
import app.sakina.quran.ui.theme.contrast
import app.sakina.quran.ui.theme.readableOn
import app.sakina.quran.ui.theme.warnBadgeColors
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    /** Every warn colour used by the themes in Looks.kt. */
    private val warns = listOf(0xFF7A1F2B, 0xFFFCE4E4, 0xFF5A1620, 0xFF8B0000, 0xFFFFE0E0).map { Color(it) }

    @Test fun warnBadgesPassAA() {
        for (w in warns) {
            val (bg, fg) = warnBadgeColors(w)
            assertTrue("warn ${w.value} → ${contrast(fg, bg)}", contrast(fg, bg) >= 4.5f)
        }
    }

    @Test fun toneBadgesPassAA() {
        val tones = listOf(0xFF22C55E, 0xFF0F766E, 0xFFF2C94C, 0xFF2E7D32, 0xFFB39DDB).map { Color(it) }
        val surfaces = listOf(0xFF153F38, 0xFF0E0A22, 0xFFFFFFFF, 0xFFF6F1E7, 0xFF1A1A1A).map { Color(it) }
        for (t in tones) for (s in surfaces) {
            val (bg, fg) = badgeColors(t, s, s.value != Color.White.value && s.value != Color(0xFFF6F1E7).value)
            assertTrue("tone ${t.value} on ${s.value}", contrast(fg, bg) >= 4.5f)
        }
    }

    @Test fun readableOnKeepsGoodColours() {
        val c = Color(0xFFFFFFFF); val bg = Color(0xFF000000)
        assertTrue(readableOn(c, bg) == c)
    }
}
