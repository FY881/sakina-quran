package app.sakina.quran.audio

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.sakina.quran.MainActivity
import app.sakina.quran.R
import app.sakina.quran.quran.Quran
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * AU1 recitation player: ExoPlayer in a Media3 [MediaSessionService] (AU09 background + lock screen + headset buttons).
 * - audio focus handled by ExoPlayer; pauses when headphones are unplugged ("becoming noisy").
 * - media ids are resolved here: downloaded file first, otherwise the stream (AU10).
 * - pause between ayat (talqin gaps) via pauseAtEndOfMediaItems; whole-surah range/ayah loops via the TimingMap.
 * - sleep timer with an 8 s fade (AU08), "continue listening" saved, errors mapped to offline/network/source.
 */
@OptIn(UnstableApi::class)
class RecitationService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var quran: Quran? = null
    private var timing: TimingMap? = null
    private var timingKey = ""

    // Whole-surah loop state.
    private var lastAyah = -1
    private var ayahCount = 1
    private var pass = 0
    // Sleep "end of ayah / surah" anchors.
    private var sleepAyah = -1
    private var sleepSura = -1
    private var lastSave = 0L
    private var gapPending: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        AudioPrefs.load(this)
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(5_000).setSeekForwardIncrementMs(10_000)
            .build()
        player.addListener(listener)
        val open = Intent(this, MainActivity::class.java).putExtra("open", "player").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(this, 20, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setId("sakina_recitation").setSessionActivity(pi).setCallback(Callback()).build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).setChannelId(CHANNEL).setChannelName(R.string.ch_recitation).setNotificationId(4401).build()
                .also { it.setSmallIcon(R.drawable.ic_stat_sakina) },
        )
        scope.launch { quran = runCatching { Quran.load(applicationContext) }.getOrNull(); publish() }
        scope.launch { AudioBus.spec.collect { onSpec(it) } }
        scope.launch { AudioBus.config.collect { applyConfig() } }
        main.post(ticker)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) { saveLast(true); stopSelf() }
    }

    override fun onDestroy() {
        saveLast(true)
        main.removeCallbacksAndMessages(null)
        scope.cancel()
        session?.run { player.release(); release() }
        session = null
        AudioBus.set { AudioUiState() }
        super.onDestroy()
    }

    private inner class Callback : MediaSession.Callback {
        /** Controllers send ids only; the URI is resolved here (local download → stream). */
        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
            val out = mediaItems.map { resolve(it) }.toMutableList()
            return Futures.immediateFuture(out)
        }
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val spec = AudioBus.spec.value
            val ref = MediaRef.decode(mediaItem?.mediaId)
            // Sleep "end of surah" in ayah mode: stop when the next surah begins.
            val cfg = AudioBus.config.value
            if (cfg.sleep == SleepSpec.EndOfSurah && spec?.mode != AudioMode.SURAH && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val g = (ref as? MediaRef.Ayah)?.g ?: (ref as? MediaRef.Test)?.g
                val s = g?.let { quran?.fromGlobal(it)?.first }
                if (s != null && sleepSura > 0 && s != sleepSura) { player.pause(); clearSleep() }
            }
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) pass++
            if (ref is MediaRef.Surah) loadTiming(ref.moshaf, ref.sura)
            publish()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) { gapPending?.let { main.removeCallbacks(it) }; gapPending = null; AudioBus.set { it.copy(inGap = false) } }
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                val cfg = AudioBus.config.value
                if (cfg.sleep == SleepSpec.EndOfAyah) { clearSleep(); applyConfig(); if (player.hasNextMediaItem()) player.seekToNextMediaItem(); publish(); return }
                val gap = AudioBus.spec.value?.repeat?.gapMs ?: 0L
                if (gap > 0 && (player.hasNextMediaItem() || player.repeatMode == Player.REPEAT_MODE_ALL)) {
                    AudioBus.set { it.copy(inGap = true) }
                    val r = Runnable { gapPending = null; AudioBus.set { it.copy(inGap = false) }; player.play() }
                    gapPending = r; main.postDelayed(r, gap)
                }
            }
            if (!playWhenReady) saveLast(true)
            publish()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) { publish() }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) onEnded()
            if (state == Player.STATE_READY) AudioBus.set { it.copy(error = null, errorDetail = "") }
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            val ref = MediaRef.decode(player.currentMediaItem?.mediaId)
            val local = ref != null && AudioFiles.resolve(this@RecitationService, ref, false).second
            val code = error.errorCode
            val kind = when {
                !local && !Net.online(this@RecitationService) -> AudioErr.OFFLINE
                code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> AudioErr.NETWORK
                code == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS || code == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    code == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE || code in 3000..4999 -> AudioErr.SOURCE
                else -> AudioErr.OTHER
            }
            android.util.Log.w("SakinaAudio", "error $kind ${error.errorCodeName} ${player.currentMediaItem?.localConfiguration?.uri}")
            AudioBus.set { it.copy(error = kind, errorDetail = "${error.errorCodeName} (${error.message ?: ""})".take(160), playing = false, buffering = false) }
        }
    }

    private fun onSpec(spec: PlaySpec?) {
        lastAyah = -1; ayahCount = 1; pass = 0
        gapPending?.let { main.removeCallbacks(it) }; gapPending = null
        if (spec?.mode == AudioMode.SURAH) loadTiming(spec.moshaf, spec.sura) else { timing = null; timingKey = "" }
        applyConfig()
        publish()
    }

    private fun applyConfig() {
        val spec = AudioBus.spec.value
        val cfg = AudioBus.config.value
        val st = AudioBus.state.value
        player.pauseAtEndOfMediaItems = spec?.mode == AudioMode.AYAH && ((spec.repeat.gapMs > 0) || cfg.sleep == SleepSpec.EndOfAyah)
        sleepAyah = st.ayah; sleepSura = st.sura
        if (cfg.sleep == null) player.volume = 1f
    }

    private fun clearSleep() {
        AudioBus.config.value = AudioBus.config.value.copy(sleep = null)
        AudioBus.set { it.copy(sleep = null, sleepEndsAt = 0) }
        player.volume = 1f
    }

    private fun loadTiming(moshaf: Int, sura: Int) {
        val k = "$moshaf:$sura"
        if (k == timingKey && timing != null) return
        timingKey = k; timing = Timings.cached(moshaf, sura)
        if (timing == null) scope.launch {
            val m = runCatching { Timings.load(applicationContext, moshaf, sura) }.getOrNull()
            if (timingKey == k) { timing = m; publish() }
        }
    }

    /** Whole surah finished, or the ayah queue ran out: continue with the next surah when that is the plain listening flow. */
    private fun onEnded() {
        val spec = AudioBus.spec.value ?: return
        val q = quran ?: return
        val cfg = AudioBus.config.value
        if (cfg.sleep == SleepSpec.EndOfSurah) { clearSleep(); saveLast(true); return }
        if (!AudioPrefs.p.value.autoNext || !spec.repeat.isPlain || spec.test) { saveLast(true); return }
        if (spec.mode == AudioMode.SURAH) {
            val next = spec.sura + 1
            if (next > 114) return
            val su = q.suras[next - 1]
            val ns = spec.copy(sura = next, fromG = su.start, toG = su.start + su.ayas - 1)
            AudioBus.spec.value = ns
            player.setMediaItem(resolve(Recitation.surahItem(q, ns, next))); player.prepare(); player.play()
        } else {
            val (s, _) = q.fromGlobal(spec.toG)
            val su = q.suras[s - 1]
            // Only continue when the range reached the end of its surah (listening, not a hifz range).
            if (spec.toG != su.start + su.ayas - 1 || s >= 114) return
            val n = q.suras[s]
            val ns = spec.copy(fromG = n.start, toG = n.start + n.ayas - 1, passLen = n.ayas)
            AudioBus.spec.value = ns
            player.setMediaItems((ns.fromG..ns.toG).map { resolve(Recitation.ayahItem(q, ns, it)) }); player.prepare(); player.play()
        }
    }

    private fun resolve(item: MediaItem): MediaItem {
        val ref = MediaRef.decode(item.mediaId) ?: return item
        return item.buildUpon().setUri(AudioFiles.resolve(this, ref, AudioPrefs.p.value.dataSaver).first).build()
    }

    private val ticker = object : Runnable {
        override fun run() {
            runCatching { tick() }
            main.postDelayed(this, if (player.isPlaying) 120 else 500)
        }
    }

    private fun tick() {
        val spec = AudioBus.spec.value
        val cfg = AudioBus.config.value
        // Sleep timer with fade-out.
        val sl = cfg.sleep
        if (sl is SleepSpec.Minutes && player.playWhenReady) {
            val remaining = cfg.sleepSetAt + sl.minutes * 60_000L - System.currentTimeMillis()
            if (remaining <= 0) { player.pause(); clearSleep() } else player.volume = SleepSpec.volumeAt(remaining)
        }
        if (spec?.mode == AudioMode.SURAH && player.isPlaying) surahLoop(spec, cfg)
        if (player.isPlaying) publish()
        if (player.isPlaying && System.currentTimeMillis() - lastSave > 5_000) saveLast(false)
    }

    /** Ayah repeat / range loop / gaps inside one whole-surah file, driven by the ayah timings. */
    private fun surahLoop(spec: PlaySpec, cfg: AudioConfig) {
        val tm = timing ?: return
        val q = quran ?: return
        val pos = player.currentPosition
        val cur = tm.ayahAt(pos)
        val su = q.suras[spec.sura - 1]
        val lf = spec.fromG - su.start + 1; val lt = spec.toG - su.start + 1
        if (cfg.sleep == SleepSpec.EndOfAyah && sleepAyah >= 0 && cur != sleepAyah && cur > 0) { player.pause(); clearSleep(); return }
        val rp = spec.repeat
        if (rp.isPlain) { lastAyah = cur; return }
        if (lastAyah < 0) {
            lastAyah = cur.coerceIn(lf, lt)
            if (cur < lf || cur > lt) tm.startOf(lf)?.let { player.seekTo(it) }
            return
        }
        val end = tm.endOf(lastAyah) ?: return
        if (lastAyah !in lf..lt || pos < end - 60) { if (cur != lastAyah && cur in lf..lt && pos < end - 60) lastAyah = cur; return }
        val times = rp.ayahTimes.coerceAtLeast(1)
        val target: Int? = when {
            ayahCount < times -> { ayahCount++; lastAyah }
            lastAyah >= lt -> {
                pass++
                ayahCount = 1
                if (rp.infinite || pass < rp.rangeTimes.coerceAtLeast(1)) lf else null
            }
            else -> { ayahCount = 1; lastAyah + 1 }
        }
        if (target == null) { player.pause(); pass = 0; lastAyah = -1; publish(); return }
        lastAyah = target
        tm.startOf(target)?.let { player.seekTo(it) }
        if (rp.gapMs > 0) {
            player.pause()
            AudioBus.set { it.copy(inGap = true) }
            val r = Runnable { gapPending = null; AudioBus.set { it.copy(inGap = false) }; player.play() }
            gapPending = r; main.postDelayed(r, rp.gapMs)
        }
    }

    private fun publish() {
        val spec = AudioBus.spec.value
        val item = player.currentMediaItem
        val ref = MediaRef.decode(item?.mediaId)
        val q = quran
        var sura = 0; var ayah = 0
        when (ref) {
            is MediaRef.Ayah -> q?.fromGlobal(ref.g)?.let { sura = it.first; ayah = it.second }
            is MediaRef.Test -> q?.fromGlobal(ref.g)?.let { sura = it.first; ayah = it.second }
            is MediaRef.Surah -> { sura = ref.sura; ayah = timing?.ayahAt(player.currentPosition) ?: 0 }
            null -> {}
        }
        val local = ref != null && ref !is MediaRef.Test && AudioFiles.resolve(this, ref, false).second
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        val passLen = spec?.passLen?.coerceAtLeast(1) ?: 1
        val cfg = AudioBus.config.value
        AudioBus.set {
            it.copy(
                active = item != null,
                playing = player.isPlaying,
                buffering = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady,
                mode = spec?.mode ?: it.mode, who = spec?.who ?: it.who,
                reciterAr = spec?.reciterAr ?: it.reciterAr, reciterEn = spec?.reciterEn ?: it.reciterEn,
                sura = sura, ayah = ayah,
                fromG = spec?.fromG ?: 0, toG = spec?.toG ?: 0,
                index = player.currentMediaItemIndex, count = player.mediaItemCount,
                posMs = player.currentPosition.coerceAtLeast(0), durMs = dur,
                speed = player.playbackParameters.speed,
                repeat = spec?.repeat ?: RepeatSpec(),
                pass = if (spec?.mode == AudioMode.SURAH) pass else (player.currentMediaItemIndex / passLen) + pass,
                sleep = cfg.sleep,
                sleepEndsAt = (cfg.sleep as? SleepSpec.Minutes)?.let { s -> cfg.sleepSetAt + s.minutes * 60_000L } ?: 0L,
                timing = spec?.mode == AudioMode.SURAH && timing != null,
                local = local,
            )
        }
    }

    private fun saveLast(force: Boolean) {
        val st = AudioBus.state.value
        val spec = AudioBus.spec.value ?: return
        if (spec.test || st.sura !in 1..114) return
        lastSave = System.currentTimeMillis()
        if (!force && !player.isPlaying) return
        AudioPrefs.saveLast(this, AudioPrefs.Last(spec.mode, spec.who, st.sura, st.ayah.coerceAtLeast(1), if (spec.mode == AudioMode.SURAH) player.currentPosition.coerceAtLeast(0) else 0L, System.currentTimeMillis()))
    }

    companion object {
        const val CHANNEL = "recitation"
    }
}
