package app.sakina.quran.ui.icons

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import app.sakina.quran.data.IconPackId
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.ui.theme.LocalSakina

/**
 * Semantic icon names. Every pack provides every name as res/drawable-nodpi/ip_<pack>_<name>.webp
 * (built by tools/icons/build_icons.py from Fluent Emoji [MIT] and Noto Emoji [Apache-2.0]).
 */
enum class Ico {
    MOSQUE, KAABA, QURAN, FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, NIGHT, TASBIH, QIBLA, DUA, CALENDAR, ZAKAT,
    STAR, BELL, ADHAN, WUDU, DATES, AZKAR, HADITH, NAMES, RAMADAN, FULLMOON, SETTINGS, LOCATION, LANGUAGE, THEME,
    FREE, PRIVACY, CLOCK, HOURGLASS, RECITERS, DONE, SPARKLES, BOOKMARK, IDEA, HIJRI, ICONPACK, TOOLS,
    KHATMA, JOURNAL, INSPIRATION, TRACKER, SHAHADA, HALAL, LIVE, MAP, DEEN, GREETING, HELP, PROFILE, BACKUP,
    SHARE, TROPHY, FIRE, COMMUNITY, RADIO, GIFT, VIDEO, PHONE, FAMILY, SEEDLING, BRAIN, LABEL, SCALE, KIDS, ROBOT;

    val key: String get() = name.lowercase()
}

fun Prayer.ico(): Ico = when (this) {
    Prayer.FAJR -> Ico.FAJR; Prayer.SUNRISE -> Ico.SUNRISE; Prayer.DHUHR -> Ico.DHUHR
    Prayer.ASR -> Ico.ASR; Prayer.MAGHRIB -> Ico.MAGHRIB; Prayer.ISHA -> Ico.ISHA
}

object IconRes {
    private val cache = HashMap<String, Int>()

    /** Drawable id for [ico] in [pack] (falls back to the 3D pack, then 0). */
    @SuppressLint("DiscouragedApi")
    fun id(ctx: Context, pack: IconPackId, ico: Ico): Int {
        val dir = pack.dir.ifEmpty { IconPackId.FLUENT_3D.dir }
        val k = "ip_${dir}_${ico.key}"
        return cache.getOrPut(k) {
            ctx.resources.getIdentifier(k, "drawable", ctx.packageName).takeIf { it != 0 }
                ?: ctx.resources.getIdentifier("ip_fluent3d_${ico.key}", "drawable", ctx.packageName)
        }
    }

    fun bitmap(ctx: Context, pack: IconPackId, ico: Ico): ImageBitmap? =
        id(ctx, pack, ico).takeIf { it != 0 }?.let { BitmapFactory.decodeResource(ctx.resources, it)?.asImageBitmap() }
}

/** Draw a semantic icon in the user's icon pack. Mono pack is tinted with [tint] (default: theme accent). */
@Composable
fun PackIcon(ico: Ico, size: Dp = 32.dp, modifier: Modifier = Modifier, pack: IconPackId? = null, tint: Color? = null, contentDescription: String? = null) {
    val ctx = LocalContext.current
    val p = pack ?: LocalSakina.current.pack
    val id = remember(p, ico) { IconRes.id(ctx, p, ico) }
    if (id == 0) return
    val mono = p == IconPackId.FLUENT_MONO
    Image(
        painterResource(id), contentDescription, modifier.size(size),
        colorFilter = if (mono) ColorFilter.tint(tint ?: MaterialTheme.colorScheme.primary) else null,
    )
}
