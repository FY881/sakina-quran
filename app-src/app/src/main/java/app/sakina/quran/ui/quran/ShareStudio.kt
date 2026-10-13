package app.sakina.quran.ui.quran

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.sakina.quran.R
import app.sakina.quran.quran.Editions
import app.sakina.quran.quran.Quran
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Card design for the share studio (QR25/TF12). */
data class CardStyle(val top: Int, val bottom: Int, val text: Int, val muted: Int, val accent: Int, val frame: Boolean = true)

object AyahCard {
    val STYLES = listOf(
        CardStyle(0xFF0E3B36.toInt(), 0xFF06201D.toInt(), 0xFFF5F1E6.toInt(), 0xFFB9C9C2.toInt(), 0xFFD8B46A.toInt()),
        CardStyle(0xFFF8F1E1.toInt(), 0xFFEADBB8.toInt(), 0xFF2A2118.toInt(), 0xFF6F5F48.toInt(), 0xFF8B5E1A.toInt()),
        CardStyle(0xFF101624.toInt(), 0xFF05070D.toInt(), 0xFFEDEFF7.toInt(), 0xFF9AA3B8.toInt(), 0xFF8FB3FF.toInt()),
        CardStyle(0xFF3B1F4A.toInt(), 0xFF140A1C.toInt(), 0xFFF7EEFB.toInt(), 0xFFC7B3D3.toInt(), 0xFFF0C27B.toInt()),
        CardStyle(0xFFFFFFFF.toInt(), 0xFFF1F4F2.toInt(), 0xFF1B2A26.toInt(), 0xFF5D6B66.toInt(), 0xFF0F7A6B.toInt()),
        CardStyle(0xFF1D3B1F.toInt(), 0xFF0B1A0C.toInt(), 0xFFF2F6EC.toInt(), 0xFFB4C4AE.toInt(), 0xFFE2C86E.toInt(), frame = false),
    )
    /** Output sizes: square, portrait (4:5), story (9:16). */
    val SIZES = listOf(1080 to 1080, 1080 to 1350, 1080 to 1920)

    /**
     * Renders ayat [from]..[to] of [sura] with optional translation / tafsir snippet. Quran text is drawn exactly as Tanzil
     * provides it; the reference and attribution are always added (Tanzil licence).
     */
    fun render(
        ctx: Context, q: Quran, sura: Int, from: Int, to: Int, style: CardStyle, size: Pair<Int, Int>,
        translation: String?, trName: String?, tafsir: String?, tafsirName: String?, scale: Float = 1f,
    ): Bitmap {
        val (w, h) = size
        val quranTf: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.amiri_quran) }.getOrNull() ?: Typeface.SERIF
        val ui: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.plex_medium) }.getOrNull() ?: Typeface.DEFAULT
        val uiBold: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.plex_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val inner = w - 2 * 120
        val body = (from..to).joinToString(" ") { a -> q.displayAyah(sura, a) + " \uFD3F" + num(a, true) + "\uFD3E" }
        val refText = "﴿${q.suras[sura - 1].name}: ${if (from == to) num(from, true) else num(from, true) + "–" + num(to, true)}﴾"
        fun lay(text: String, tf: Typeface, sz: Float, color: Int, rtl: Boolean, spacing: Float = .45f): StaticLayout {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = sz; this.color = color }
            return StaticLayout.Builder.obtain(text, 0, text.length, tp, inner).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setLineSpacing(sz * spacing, 1f).setIncludePad(true).build()
        }
        val extraLines = listOfNotNull(translation, tafsir)
        val budget = h - 520f - (if (extraLines.isEmpty()) 0f else h * .28f)
        var sz = 74f * scale
        var main = lay(body, quranTf, sz, style.text, true, .6f)
        while (main.height > budget && sz > 30f) { sz -= 3f; main = lay(body, quranTf, sz, style.text, true, .6f) }
        val trLay = translation?.let { lay(it, ui, (36f * scale).coerceAtLeast(26f), style.muted, false, .25f) }
        val tfLay = tafsir?.let { lay(if (it.length > 300) it.take(300) + "…" else it, ui, (32f * scale).coerceAtLeast(24f), style.muted, true, .3f) }
        val refLay = lay(refText, uiBold, 40f, style.accent, true)
        val credit = buildString {
            append("نص القرآن: مشروع تنزيل tanzil.net")
            if (trName != null) append(" • ").append(trName)
            if (tafsirName != null) append(" • ").append(tafsirName)
        }
        val credLay = lay(credit, ui, 24f, style.muted, true, .1f)
        val contentH = main.height + refLay.height + 40 + (trLay?.height?.plus(36) ?: 0) + (tfLay?.height?.plus(36) ?: 0)
        val bmp = Bitmap.createBitmap(w, maxOf(h, contentH + 420), Bitmap.Config.ARGB_8888)
        val H = bmp.height.toFloat()
        val c = Canvas(bmp)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, H, style.top, style.bottom, Shader.TileMode.CLAMP) })
        if (style.frame) {
            val fr = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 3f; color = (style.accent and 0x00FFFFFF) or 0x99000000.toInt() }
            c.drawRoundRect(RectF(50f, 50f, w - 50f, H - 50f), 48f, 48f, fr)
            c.drawRoundRect(RectF(64f, 64f, w - 64f, H - 64f), 40f, 40f, fr.apply { strokeWidth = 1.5f })
        }
        // ornament
        val orn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accent; strokeWidth = 4f }
        var y = (H - contentH - 120) / 2f
        c.drawLine(w / 2f - 80f, y, w / 2f + 80f, y, orn); c.drawCircle(w / 2f, y, 8f, orn)
        y += 50f
        c.save(); c.translate(120f, y); main.draw(c); c.restore(); y += main.height + 20
        c.save(); c.translate(120f, y); refLay.draw(c); c.restore(); y += refLay.height + 20
        trLay?.let { c.save(); c.translate(120f, y + 16); it.draw(c); c.restore(); y += it.height + 36 }
        tfLay?.let { c.save(); c.translate(120f, y + 16); it.draw(c); c.restore(); y += it.height + 36 }
        c.save(); c.translate(120f, H - 150f - credLay.height); credLay.draw(c); c.restore()
        val bPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = uiBold; textSize = 34f; color = style.text; textAlign = Paint.Align.CENTER }
        c.drawText("سكينة • Sakina", w / 2f, H - 100f, bPaint)
        return bmp
    }

    fun share(ctx: Context, bmp: Bitmap, text: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "ayah_${System.currentTimeMillis() % 100000}.png")
        dir.listFiles()?.forEach { if (it.name != f.name) it.delete() }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("", uri)
        ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}

/** Share studio: range, design, size, translation and tafsir toggles, live preview (QR25, TF12). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareStudioScreen(sura: Int, from: Int, to: Int, onBack: () -> Unit) {
    val quran = rememberQuran()
    val ctx = LocalContext.current
    val ar = isArabic()
    val look = LocalSakina.current.look
    val qu = rememberQUser()
    var end by remember { mutableIntStateOf(to.coerceAtLeast(from)) }
    var styleI by remember { mutableIntStateOf(0) }
    var sizeI by remember { mutableIntStateOf(1) }
    var withTr by remember { mutableStateOf(!ar) }
    var withTafsir by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    val trId = qu.prefs.activeTranslations(ar).firstOrNull { Editions.available(ctx, it) } ?: "en.sahih"
    val tr = rememberEdition(if (withTr) trId else null)
    val tf = rememberEdition(if (withTafsir) qu.prefs.tafsir else null)
    if (quran == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = look.cta) }; return }
    val maxEnd = minOf(quran.suras[sura - 1].ayas, from + 9)
    val trText = tr?.let { arr -> (from..end).joinToString(" ") { a -> arr[quran.global(sura, a)] + " ($a)" } }
    val tfText = tf?.let { arr -> arr[quran.global(sura, from)] }
    val trEd = Editions.get(trId); val tfEd = Editions.get(qu.prefs.tafsir)
    val bmp by produceState<Bitmap?>(null, end, styleI, sizeI, trText, tfText, scale) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                AyahCard.render(ctx, quran, sura, from, end, AyahCard.STYLES[styleI], AyahCard.SIZES[sizeI], trText, trEd?.nameEn?.takeIf { trText != null }, tfText, tfEd?.nameAr?.takeIf { tfText != null }, scale)
            }.getOrNull()
        }
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("صورة الآية للمشاركة", "Ayah share studio"), onBack, subtitle = suraRef(quran, sura, from, ar) + if (end > from) (if (ar) "–${num(end, true)}" else "–$end") else "") }
        item {
            Box(Modifier.fillMaxWidth().heightIn(max = 460.dp), contentAlignment = Alignment.Center) {
                val b = bmp
                if (b == null) CircularProgressIndicator(color = look.cta)
                else Image(b.asImageBitmap(), t("معاينة الصورة", "Preview"), contentScale = ContentScale.Fit, modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, look.cardBorder, RoundedCornerShape(16.dp)))
            }
        }
        item {
            SCard {
                Text(t("التصميم", "Design"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                VSpace(6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AyahCard.STYLES.forEachIndexed { i, st ->
                        Box(Modifier.size(38.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(st.top), Color(st.bottom))))
                            .border(if (i == styleI) 3.dp else 1.dp, if (i == styleI) look.cta else look.cardBorder, CircleShape).press { styleI = i })
                    }
                }
                VSpace(10.dp)
                Text(t("المقاس", "Size"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                VSpace(6.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("مربع", "Square"), selected = sizeI == 0, onClick = { sizeI = 0 })
                    Chip(t("منشور ٤:٥", "Post 4:5"), selected = sizeI == 1, onClick = { sizeI = 1 })
                    Chip(t("قصة ٩:١٦", "Story 9:16"), selected = sizeI == 2, onClick = { sizeI = 2 })
                }
                VSpace(10.dp)
                Text(t("الآيات: حتى الآية ${num(end, true)}", "Ayat: up to $end"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                if (maxEnd > from) Slider(end.toFloat(), { end = it.toInt().coerceIn(from, maxEnd) }, valueRange = from.toFloat()..maxEnd.toFloat(), steps = (maxEnd - from - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = look.cta, activeTrackColor = look.cta))
                Text(t("حجم الخط", "Text size"), style = MaterialTheme.typography.labelLarge, color = look.cta)
                Slider(scale, { scale = it }, valueRange = .7f..1.3f, colors = SliderDefaults.colors(thumbColor = look.cta, activeTrackColor = look.cta))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("إضافة الترجمة", "Add translation") + (trEd?.let { " — " + if (ar) it.nameAr else it.nameEn } ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(withTr, { withTr = it }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("إضافة مقتطف من التفسير", "Add a tafsir snippet") + (tfEd?.let { " — " + if (ar) it.nameAr else it.nameEn } ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(withTafsir, { withTafsir = it }, colors = app.sakina.quran.ui.azkar.switchColors())
                }
            }
        }
        item {
            CtaPill(t("مشاركة الصورة", "Share image"), Modifier.fillMaxWidth(), enabled = bmp != null) {
                bmp?.let { b ->
                    val txt = (from..end).joinToString(" ") { quran.ayah(sura, it) } + "\n﴿${quran.suras[sura - 1].name}: ${digits(if (from == end) "$from" else "$from-$end", true)}﴾"
                    AyahCard.share(ctx, b, txt)
                }
            }
        }
        item { Muted(t("يُضاف المرجع ومصدر النص تلقائيًا، ولا يُغيَّر نص القرآن.", "The reference and text source are added automatically; the Quran text is never altered."), Modifier.padding(8.dp)) }
    }
}
