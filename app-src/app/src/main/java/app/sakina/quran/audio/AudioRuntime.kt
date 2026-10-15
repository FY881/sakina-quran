package app.sakina.quran.audio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Why playback stopped with an error (AU1 error/offline states). */
enum class AudioErr { OFFLINE, NETWORK, SOURCE, OTHER }

/** Everything the UI shows about the recitation player. Written by [RecitationService], read by Compose. */
data class AudioUiState(
    val active: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val mode: AudioMode = AudioMode.AYAH,
    /** Ayah voice id (AYAH) or mp3quran moshaf id as text (SURAH). */
    val who: String = "",
    val reciterAr: String = "",
    val reciterEn: String = "",
    /** Surah + ayah being recited now (ayah 0 = intro / basmala of a whole-surah file). */
    val sura: Int = 0,
    val ayah: Int = 0,
    /** Selected range in globals (0-based) — the loop range in AYAH mode, the surah in SURAH mode. */
    val fromG: Int = 0,
    val toG: Int = 0,
    val index: Int = 0,
    val count: Int = 0,
    val posMs: Long = 0,
    val durMs: Long = 0,
    val speed: Float = 1f,
    val repeat: RepeatSpec = RepeatSpec(),
    /** Completed range passes (for the "2 / 5" loop counter). */
    val pass: Int = 0,
    val sleep: SleepSpec? = null,
    val sleepEndsAt: Long = 0,
    val error: AudioErr? = null,
    val errorDetail: String = "",
    /** Whole-surah file has an ayah timing map (synchronised highlight available). */
    val timing: Boolean = false,
    /** Current item plays from a downloaded file. */
    val local: Boolean = false,
    /** Waiting in a "pause between ayat" gap. */
    val inGap: Boolean = false,
)

/** Player configuration shared in-process between the UI facade and the service. */
data class AudioConfig(val repeat: RepeatSpec = RepeatSpec(), val sleep: SleepSpec? = null, val sleepSetAt: Long = 0, val autoNext: Boolean = true)

object AudioBus {
    val state = MutableStateFlow(AudioUiState())
    /** The session being played, set by [Recitation] before it hands the queue to the service. */
    val spec = MutableStateFlow<PlaySpec?>(null)
    val config = MutableStateFlow(AudioConfig())
    fun set(f: (AudioUiState) -> AudioUiState) = state.update(f)
}

/** Persisted audio preferences (AU07/AU10) + the last session for "continue listening". */
object AudioPrefs {
    data class P(
        val mode: AudioMode = AudioMode.AYAH,
        val speed: Float = 1f,
        val dataSaver: Boolean = false,
        val wifiOnly: Boolean = true,
        val autoNext: Boolean = true,
        val follow: Boolean = true,
        val lastRepeat: RepeatSpec = RepeatSpec(ayahTimes = 3, rangeTimes = 1, gapMs = 1000),
    )
    data class Last(val mode: AudioMode, val who: String, val sura: Int, val ayah: Int, val posMs: Long, val at: Long)

    private val _p = MutableStateFlow(P())
    val p: StateFlow<P> = _p.asStateFlow()
    private val _last = MutableStateFlow<Last?>(null)
    val last: StateFlow<Last?> = _last.asStateFlow()
    @Volatile private var loaded = false

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("audio", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        if (loaded) return
        val s = sp(ctx)
        _p.value = P(
            mode = runCatching { AudioMode.valueOf(s.getString("mode", "AYAH")!!) }.getOrDefault(AudioMode.AYAH),
            speed = s.getFloat("speed", 1f),
            dataSaver = s.getBoolean("saver", false),
            wifiOnly = s.getBoolean("wifi", true),
            autoNext = s.getBoolean("next", true),
            follow = s.getBoolean("follow", true),
            lastRepeat = RepeatSpec(s.getInt("r_ay", 3), s.getInt("r_rg", 1), s.getLong("r_gap", 1000), s.getBoolean("r_lad", false)),
        )
        val lm = s.getString("l_mode", null)
        _last.value = if (lm == null) null else Last(
            runCatching { AudioMode.valueOf(lm) }.getOrDefault(AudioMode.AYAH), s.getString("l_who", "") ?: "",
            s.getInt("l_s", 1), s.getInt("l_a", 1), s.getLong("l_pos", 0), s.getLong("l_at", 0),
        )
        loaded = true
    }

    fun update(ctx: Context, f: (P) -> P) {
        load(ctx)
        val n = f(_p.value); _p.value = n
        sp(ctx).edit().putString("mode", n.mode.name).putFloat("speed", n.speed).putBoolean("saver", n.dataSaver)
            .putBoolean("wifi", n.wifiOnly).putBoolean("next", n.autoNext).putBoolean("follow", n.follow)
            .putInt("r_ay", n.lastRepeat.ayahTimes).putInt("r_rg", n.lastRepeat.rangeTimes).putLong("r_gap", n.lastRepeat.gapMs).putBoolean("r_lad", n.lastRepeat.ladder)
            .apply()
    }

    fun saveLast(ctx: Context, l: Last) {
        _last.value = l
        sp(ctx).edit().putString("l_mode", l.mode.name).putString("l_who", l.who).putInt("l_s", l.sura).putInt("l_a", l.ayah)
            .putLong("l_pos", l.posMs).putLong("l_at", l.at).apply()
    }
}

/** On-device recitation files (user downloads only; never bundled, never re-hosted). */
object AudioFiles {
    fun root(ctx: Context) = File(ctx.applicationContext.filesDir, "recitations")
    fun file(ctx: Context, rel: String) = File(root(ctx), rel)

    /** Local file when downloaded, otherwise the streaming URL. Debug test refs use a bundled tone clip. */
    fun resolve(ctx: Context, ref: MediaRef, dataSaver: Boolean): Pair<Uri, Boolean> = when (ref) {
        is MediaRef.Ayah -> {
            val f = file(ctx, DownloadPlan.ayahRel(ref.edition, ref.g))
            if (f.isFile && f.length() > 0) Uri.fromFile(f) to true else Uri.parse(AyahVoices.byId(ref.edition).url(ref.g, dataSaver)) to false
        }
        is MediaRef.Surah -> {
            val f = file(ctx, DownloadPlan.surahRel(ref.moshaf, ref.sura))
            if (f.isFile && f.length() > 0) Uri.fromFile(f) to true else Uri.parse(MediaRef.surahUrl(ref.server, ref.sura)) to false
        }
        is MediaRef.Test -> Uri.parse("asset:///audio_test/tone.mp3") to true
    }

    fun dirSize(f: File): Long = if (!f.exists()) 0 else if (f.isFile) f.length() else (f.listFiles() ?: emptyArray()).sumOf { dirSize(it) }

    /** Bytes used per reciter folder: key "ayah/<edition>" or "surah/<moshaf>". */
    fun usage(ctx: Context): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        for (kind in listOf("ayah", "surah")) {
            (File(root(ctx), kind).listFiles() ?: emptyArray()).filter { it.isDirectory }.forEach { d ->
                val n = dirSize(d); if (n > 0) out["$kind/${d.name}"] = n
            }
        }
        return out
    }

    fun deleteReciter(ctx: Context, key: String) {
        File(root(ctx), key).deleteRecursively()
        if (key.startsWith("surah/")) File(root(ctx), "timing/" + key.removePrefix("surah/")).deleteRecursively()
    }
}

/** mp3quran ayah timings for whole-surah files (AU03). Cached on disk; bundled list of reads that have them. */
object Timings {
    @Volatile private var reads: Map<Int, Int>? = null
    private val mem = HashMap<String, TimingMap>()

    /** moshaf id → number of surahs with timings. */
    fun reads(ctx: Context): Map<Int, Int> = reads ?: runCatching {
        val txt = ctx.assets.open("quran/timing_reads.json").bufferedReader().readText()
        Regex("""\[(\d+),(\d+)]""").findAll(txt).associate { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    }.getOrDefault(emptyMap()).also { reads = it }

    fun has(ctx: Context, moshaf: Int) = reads(ctx).containsKey(moshaf)

    /** In-memory map only (no IO), for the player's step/seek. */
    fun cached(moshaf: Int, sura: Int): TimingMap? = synchronized(mem) { mem["$moshaf:$sura"] }

    fun url(moshaf: Int, sura: Int) = "https://www.mp3quran.net/api/v3/ayat_timing?surah=$sura&read=$moshaf"

    /** Disk cache → network. Null when this moshaf has no timings or the network is down and nothing is cached. */
    suspend fun load(ctx: Context, moshaf: Int, sura: Int): TimingMap? = withContext(Dispatchers.IO) {
        val k = "$moshaf:$sura"
        synchronized(mem) { mem[k] }?.let { return@withContext it }
        if (!has(ctx, moshaf)) return@withContext null
        val f = AudioFiles.file(ctx, DownloadPlan.timingRel(moshaf, sura))
        val cached = if (f.isFile) TimingMap.parseCompact(f.readText()) else null
        val map = cached ?: runCatching { fetch(url(moshaf, sura)) }.getOrNull()?.let { TimingMap.parseMp3Quran(it) }?.also { m ->
            runCatching { f.parentFile?.mkdirs(); f.writeText(m.serialize()) }
        }
        if (map != null) synchronized(mem) { mem[k] = map }
        map
    }

    fun fetch(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 20_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Sakina-Quran-Android")
        }
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().readText()
        } finally { c.disconnect() }
    }
}

object Net {
    fun online(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
}
