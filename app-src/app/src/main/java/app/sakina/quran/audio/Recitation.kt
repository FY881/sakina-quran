package app.sakina.quran.audio

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.sakina.quran.quran.Moshaf
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Reciter
import app.sakina.quran.quran.Reciters
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** What is being played: everything the service needs to describe, follow and continue the session. */
data class PlaySpec(
    val mode: AudioMode,
    /** Ayah voice id (AYAH) or moshaf id (SURAH). */
    val who: String,
    val reciterAr: String,
    val reciterEn: String,
    /** Loop / play range in globals (0-based). In SURAH mode: the ayat of the surah being looped (whole surah when plain). */
    val fromG: Int,
    val toG: Int,
    /** SURAH mode only. */
    val sura: Int = 0,
    val moshaf: Int = 0,
    val server: String = "",
    val reciterId: Int = 0,
    /** Items of one range pass (AYAH mode) so the service can count passes. */
    val passLen: Int = 1,
    val repeat: RepeatSpec = RepeatSpec(),
    val arabic: Boolean = true,
    /** Debug/CI: bundled tone instead of the network. */
    val test: Boolean = false,
)

/**
 * UI-side facade over the Media3 [MediaController] connected to [RecitationService].
 * The service resolves each media id to a downloaded file or a stream (see [AudioFiles.resolve]).
 */
object Recitation {
    private var future: ListenableFuture<MediaController>? = null
    @Volatile private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun with(ctx: Context, f: (MediaController) -> Unit) {
        controller?.takeIf { it.isConnected }?.let { f(it); return }
        synchronized(pending) { pending += f }
        if (future != null && controller == null) return
        controller = null
        val app = ctx.applicationContext
        val token = SessionToken(app, ComponentName(app, RecitationService::class.java))
        val fut = MediaController.Builder(app, token).buildAsync()
        future = fut
        fut.addListener({
            val c = runCatching { fut.get() }.getOrNull()
            controller = c
            if (c == null) { future = null; synchronized(pending) { pending.clear() }; return@addListener }
            val todo = synchronized(pending) { pending.toList().also { pending.clear() } }
            todo.forEach { runCatching { it(c) } }
        }, ContextCompat.getMainExecutor(app))
    }

    /** Item for one ayah (AYAH mode or the debug tone). */
    fun ayahItem(q: Quran, spec: PlaySpec, g: Int): MediaItem {
        val (s, a) = q.fromGlobal(g)
        val ref = if (spec.test) MediaRef.Test(g) else MediaRef.Ayah(spec.who, g)
        return MediaItem.Builder().setMediaId(ref.encode()).setMediaMetadata(meta(q, spec, s, a)).build()
    }

    fun surahItem(q: Quran, spec: PlaySpec, sura: Int): MediaItem =
        MediaItem.Builder().setMediaId(MediaRef.Surah(spec.moshaf, sura, spec.server).encode()).setMediaMetadata(meta(q, spec, sura, 0)).build()

    private fun meta(q: Quran, spec: PlaySpec, s: Int, a: Int): MediaMetadata {
        val su = q.suras[s - 1]
        val title = if (spec.arabic) "سورة ${su.name}" + (if (a > 0) " • الآية ${arabicDigits(a)}" else "") else su.tname + (if (a > 0) " • $s:$a" else "")
        return MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(if (spec.arabic) spec.reciterAr else spec.reciterEn.ifBlank { spec.reciterAr })
            .setAlbumTitle(if (spec.arabic) "سكينة — القرآن الكريم" else "Sakina — The Holy Quran")
            .setIsPlayable(true).setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(Bundle().apply { putInt("sura", s); putInt("ayah", a) })
            .build()
    }

    fun arabicDigits(n: Int) = n.toString().map { if (it in '0'..'9') '\u0660' + (it - '0') else it }.joinToString("")

    /** AYAH mode: play [fromG]..[toG] with [repeat], starting at [startG]. */
    fun playAyahs(ctx: Context, q: Quran, voice: AyahVoice, fromG: Int, toG: Int, startG: Int = fromG, repeat: RepeatSpec = RepeatSpec(), arabic: Boolean = true, test: Boolean = false) {
        val pass = QueuePlan.build(fromG, toG, repeat.copy(rangeTimes = 1))
        val queue = QueuePlan.build(fromG, toG, repeat)
        val spec = PlaySpec(AudioMode.AYAH, voice.id, voice.ar, voice.en, fromG, toG, passLen = pass.size, repeat = repeat, arabic = arabic, test = test)
        start(ctx, spec, queue.map { ayahItem(q, spec, it) }, queue.indexOf(startG).coerceAtLeast(0), 0L, if (repeat.infinite) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF)
    }

    /** SURAH mode: whole-surah file of [moshaf]; seeks to [fromAyah] once the ayah timings are known. */
    fun playSurah(ctx: Context, q: Quran, reciter: Reciter, moshaf: Moshaf, sura: Int, fromAyah: Int = 1, posMs: Long = 0, repeat: RepeatSpec = RepeatSpec(), loopFrom: Int = 1, loopTo: Int = 0, arabic: Boolean = true) {
        val su = q.suras[sura - 1]
        val lt = if (loopTo <= 0) su.ayas else loopTo.coerceIn(1, su.ayas)
        val lf = loopFrom.coerceIn(1, lt)
        val spec = PlaySpec(
            AudioMode.SURAH, moshaf.id.toString(), reciter.ar, reciter.en, su.start + lf - 1, su.start + lt - 1,
            sura = sura, moshaf = moshaf.id, server = moshaf.server, reciterId = reciter.id, repeat = repeat, arabic = arabic,
        )
        io.launch {
            var pos = posMs
            if (pos == 0L && fromAyah > 1) {
                val tm = runCatching { Timings.load(ctx, moshaf.id, sura) }.getOrNull()
                pos = tm?.startOf(fromAyah) ?: 0L
            }
            start(ctx, spec, listOf(surahItem(q, spec, sura)), 0, pos, Player.REPEAT_MODE_OFF)
        }
    }

    private fun start(ctx: Context, spec: PlaySpec, items: List<MediaItem>, index: Int, posMs: Long, repeatMode: Int) {
        AudioPrefs.load(ctx)
        AudioBus.spec.value = spec
        AudioBus.set { it.copy(active = true, error = null, errorDetail = "", buffering = true, mode = spec.mode, who = spec.who, reciterAr = spec.reciterAr, reciterEn = spec.reciterEn, fromG = spec.fromG, toG = spec.toG, repeat = spec.repeat, pass = 0) }
        with(ctx) { c ->
            c.setMediaItems(items, index, posMs)
            c.repeatMode = repeatMode
            c.setPlaybackSpeed(AudioPrefs.p.value.speed)
            c.prepare()
            c.play()
        }
    }

    /** "Listen from here" (AyahSheet / mushaf): uses the listening mode and reciter the user chose. */
    fun listenFrom(ctx: Context, q: Quran, ayahVoice: String, reciterId: Int, moshafId: Int, sura: Int, ayah: Int, arabic: Boolean, onlyThis: Boolean = false) {
        AudioPrefs.load(ctx)
        val mode = AudioPrefs.p.value.mode
        if (mode == AudioMode.SURAH && !onlyThis) {
            val r = runCatching { Reciters.load(ctx) }.getOrNull()?.firstOrNull { it.id == reciterId }
            val m = r?.moshafs?.firstOrNull { it.id == moshafId } ?: r?.moshafs?.firstOrNull()
            if (r != null && m != null && m.has(sura)) { playSurah(ctx, q, r, m, sura, ayah, arabic = arabic); return }
        }
        val su = q.suras[sura - 1]
        val g = q.global(sura, ayah)
        playAyahs(ctx, q, AyahVoices.byId(ayahVoice), g, if (onlyThis) g else su.start + su.ayas - 1, g, RepeatSpec(), arabic)
    }

    /** Continue the last saved session ("continue listening"). */
    fun resume(ctx: Context, q: Quran, last: AudioPrefs.Last, arabic: Boolean) {
        if (last.mode == AudioMode.SURAH) {
            val id = last.who.toIntOrNull() ?: return
            val all = runCatching { Reciters.load(ctx) }.getOrNull() ?: return
            val r = all.firstOrNull { rr -> rr.moshafs.any { it.id == id } } ?: return
            val m = r.moshafs.first { it.id == id }
            playSurah(ctx, q, r, m, last.sura, posMs = last.posMs, arabic = arabic)
        } else {
            val su = q.suras[last.sura.coerceIn(1, 114) - 1]
            val g = q.global(su.index, last.ayah.coerceIn(1, su.ayas))
            playAyahs(ctx, q, AyahVoices.byId(last.who), g, su.start + su.ayas - 1, g, RepeatSpec(), arabic)
        }
    }

    fun toggle(ctx: Context) = with(ctx) { c ->
        if (c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING)) c.pause()
        else { if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0); if (c.playbackState == Player.STATE_IDLE) c.prepare(); c.play() }
    }
    fun pause(ctx: Context) { if (controller != null) with(ctx) { it.pause() } }
    fun stop(ctx: Context) {
        with(ctx) { c -> c.stop(); c.clearMediaItems() }
        AudioBus.spec.value = null
        AudioBus.set { AudioUiState() }
    }
    fun retry(ctx: Context) = with(ctx) { c -> AudioBus.set { it.copy(error = null, errorDetail = "", buffering = true) }; c.prepare(); c.play() }
    fun seekTo(ctx: Context, ms: Long) = with(ctx) { it.seekTo(ms.coerceAtLeast(0)) }
    fun setSpeed(ctx: Context, s: Float) { AudioPrefs.update(ctx) { it.copy(speed = s) }; with(ctx) { it.setPlaybackSpeed(s) } }

    /** Next / previous ayah: next queue item in AYAH mode, timing seek in SURAH mode. */
    fun step(ctx: Context, forward: Boolean) = with(ctx) { c ->
        val st = AudioBus.state.value
        val spec = AudioBus.spec.value
        if (spec?.mode == AudioMode.SURAH) {
            val tm = Timings.cached(spec.moshaf, spec.sura)
            val target = if (forward) st.ayah + 1 else (if (c.currentPosition - (tm?.startOf(st.ayah) ?: 0L) > 2500) st.ayah else st.ayah - 1)
            val ms = tm?.startOf(target.coerceAtLeast(0)) ?: (if (target <= 0) 0L else null)
            if (ms != null) c.seekTo(ms) else if (!forward) c.seekTo(0)
        } else {
            if (forward) { if (c.hasNextMediaItem()) c.seekToNextMediaItem() }
            else if (c.currentPosition > 2500 || !c.hasPreviousMediaItem()) c.seekTo(0) else c.seekToPreviousMediaItem()
        }
    }

    /** Apply a new repeat setting; restarts the queue from the ayah playing now. [fromAyah]/[toAyah] are ayah numbers in the current surah. */
    fun setRepeat(ctx: Context, q: Quran, repeat: RepeatSpec, fromAyah: Int? = null, toAyah: Int? = null) {
        AudioPrefs.update(ctx) { it.copy(lastRepeat = repeat) }
        val spec = AudioBus.spec.value ?: return
        val st = AudioBus.state.value
        val (s0, _) = q.fromGlobal(spec.fromG)
        val su = q.suras[s0 - 1]
        val from = fromAyah?.let { su.start + it.coerceIn(1, su.ayas) - 1 } ?: spec.fromG
        val to = (toAyah?.let { su.start + it.coerceIn(1, su.ayas) - 1 } ?: spec.toG).coerceAtLeast(from)
        val cur = if (st.sura > 0 && st.ayah > 0) q.global(st.sura, st.ayah).coerceIn(from, to) else from
        if (spec.mode == AudioMode.SURAH) {
            val all = runCatching { Reciters.load(ctx) }.getOrNull() ?: return
            val r = all.firstOrNull { it.id == spec.reciterId } ?: all.firstOrNull { rr -> rr.moshafs.any { it.id == spec.moshaf } } ?: return
            val m = r.moshafs.firstOrNull { it.id == spec.moshaf } ?: return
            playSurah(ctx, q, r, m, spec.sura, cur - su.start + 1, repeat = repeat, loopFrom = from - su.start + 1, loopTo = to - su.start + 1, arabic = spec.arabic)
        } else {
            playAyahs(ctx, q, AyahVoices.byId(spec.who), from, to, cur, repeat, spec.arabic, spec.test)
        }
    }

    fun setSleep(s: SleepSpec?) {
        AudioBus.config.value = AudioBus.config.value.copy(sleep = s, sleepSetAt = System.currentTimeMillis())
        val ends = if (s is SleepSpec.Minutes) System.currentTimeMillis() + s.minutes * 60_000L else 0L
        AudioBus.set { it.copy(sleep = s, sleepEndsAt = ends) }
        // The service collects AudioBus.config in-process and applies it (fade timer, pause at end of ayah/surah).
    }

    /** Release the UI's controller (the service keeps playing in the background). */
    fun release() {
        controller?.release(); controller = null
        future?.let { MediaController.releaseFuture(it) }; future = null
    }
}
