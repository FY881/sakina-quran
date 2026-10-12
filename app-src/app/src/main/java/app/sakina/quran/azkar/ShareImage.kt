package app.sakina.quran.azkar

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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.sakina.quran.R
import java.io.File

/** Renders a dhikr / dua / ayah as a 1080×1350 card and opens the share sheet (AZ18). Fully offline. */
object ShareImage {
    data class Palette(val top: Int, val bottom: Int, val card: Int, val text: Int, val muted: Int, val accent: Int)

    fun render(ctx: Context, title: String, body: String, footer: String, pal: Palette): Bitmap {
        val w = 1080
        val quran: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.amiri_quran) }.getOrNull() ?: Typeface.SERIF
        val ui: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.plex_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val inner = w - 2 * 110
        // Shrink the body font until the card fits in a 4:5 frame (min 34px), long texts just grow taller.
        var size = 58f
        fun layout(sz: Float): StaticLayout {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = quran; textSize = sz; color = pal.text }
            return StaticLayout.Builder.obtain(body, 0, body.length, tp, inner).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(sz * .55f, 1f).setIncludePad(true).build()
        }
        var lay = layout(size)
        while (lay.height > 880 && size > 34f) { size -= 3f; lay = layout(size) }
        val tPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; textSize = 44f; color = pal.accent }
        val tLay = StaticLayout.Builder.obtain(title, 0, title.length, tPaint, inner).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        val fPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; textSize = 30f; color = pal.muted }
        val fLay = StaticLayout.Builder.obtain(footer, 0, footer.length, fPaint, inner).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        val h = maxOf(1350, lay.height + tLay.height + fLay.height + 520)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), pal.top, pal.bottom, Shader.TileMode.CLAMP) })
        // Card
        val card = RectF(60f, 150f, w - 60f, h - 170f)
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.card })
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = pal.accent and 0x66FFFFFF })
        // Ornament line
        val orn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.accent; strokeWidth = 4f }
        c.drawLine(w / 2f - 90f, 250f, w / 2f + 90f, 250f, orn)
        c.drawCircle(w / 2f, 250f, 9f, orn)
        var y = 290f
        c.save(); c.translate(110f, y); tLay.draw(c); c.restore(); y += tLay.height + 50f
        val avail = card.bottom - 140f - y
        val by = y + ((avail - lay.height) / 2f).coerceAtLeast(0f)
        c.save(); c.translate(110f, by); lay.draw(c); c.restore()
        c.save(); c.translate(110f, card.bottom - 110f); fLay.draw(c); c.restore()
        // Brand
        val bPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; textSize = 36f; color = pal.text; textAlign = Paint.Align.CENTER }
        c.drawText("سكينة • Sakina", w / 2f, h - 80f, bPaint)
        return bmp
    }

    fun share(ctx: Context, title: String, body: String, footer: String, pal: Palette) {
        val bmp = render(ctx, title, body, footer, pal)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "sakina_${System.currentTimeMillis() % 100000}.png")
        dir.listFiles()?.forEach { if (it.name != f.name) it.delete() }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "$body\n— $footer")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("", uri)
        ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}

/** Minimal streaming player for licence-cleared dhikr audio (AU22 hook). One clip at a time. */
object DhikrAudio {
    private var player: MediaPlayer? = null
    var playingUrl: String? = null; private set

    fun toggle(url: String, onState: (Boolean) -> Unit) {
        if (playingUrl == url) { stop(); onState(false); return }
        stop()
        val mp = MediaPlayer()
        player = mp; playingUrl = url
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start(); onState(true) }
            mp.setOnCompletionListener { stop(); onState(false) }
            mp.setOnErrorListener { _, _, _ -> stop(); onState(false); true }
            mp.prepareAsync()
        }.onFailure { stop(); onState(false) }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null; playingUrl = null
    }
}
