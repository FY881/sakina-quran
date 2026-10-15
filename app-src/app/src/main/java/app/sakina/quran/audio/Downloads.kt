package app.sakina.quran.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.sakina.quran.quran.Quran
import app.sakina.quran.quran.Reciters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/** One download unit chosen by the user: a surah or a juz, for an ayah voice or a whole-surah moshaf. */
data class DlUnit(val mode: AudioMode, val who: String, val unit: String, val n: Int) {
    val key get() = DownloadPlan.key(mode, who, unit, n)
    companion object {
        fun parse(key: String): DlUnit? = key.split(':').takeIf { it.size == 4 }?.let { p ->
            val m = runCatching { AudioMode.valueOf(p[0].uppercase()) }.getOrNull() ?: return null
            DlUnit(m, p[1], p[2], p[3].toIntOrNull() ?: return null)
        }
    }
}

/** AU10 offline downloads on the user's device (WorkManager; resumable; Wi-Fi-only by default). */
object Downloads {
    const val TAG = "recitation_dl"

    /** Files of one unit (url → path under [AudioFiles.root]). Null if the moshaf/voice is unknown. */
    fun files(ctx: Context, q: Quran, u: DlUnit, dataSaver: Boolean): List<DlFile>? {
        val suraStarts = IntArray(q.suras.size) { q.suras[it].start }
        val range: IntRange = if (u.unit == "juz") {
            val (s, a) = q.juzStart(u.n)
            val g0 = q.global(s, a)
            val g1 = if (u.n >= 30) q.text.size - 1 else q.juzStart(u.n + 1).let { (s2, a2) -> q.global(s2, a2) - 1 }
            g0..g1
        } else {
            val su = q.suras[u.n.coerceIn(1, 114) - 1]; su.start until su.start + su.ayas
        }
        return when (u.mode) {
            AudioMode.AYAH -> DownloadPlan.ayahFiles(AyahVoices.byId(u.who), range, dataSaver)
            AudioMode.SURAH -> {
                val id = u.who.toIntOrNull() ?: return null
                val m = Reciters.load(ctx).asSequence().flatMap { it.moshafs.asSequence() }.firstOrNull { it.id == id } ?: return null
                val suras = if (u.unit == "juz") DownloadPlan.surahsInRange(suraStarts, q.text.size, range) else listOf(u.n)
                suras.filter { m.has(it) }.map { s -> DlFile(MediaRef.surahUrl(m.server, s), DownloadPlan.surahRel(m.id, s)) }
            }
        }
    }

    /** (files present, files total) for a unit. */
    fun status(ctx: Context, q: Quran, u: DlUnit): Pair<Int, Int> {
        val fs = runCatching { files(ctx, q, u, false) }.getOrNull() ?: return 0 to 0
        return fs.count { AudioFiles.file(ctx, it.rel).let { f -> f.isFile && f.length() > 0 } } to fs.size
    }

    fun enqueue(ctx: Context, u: DlUnit) {
        AudioPrefs.load(ctx)
        val p = AudioPrefs.p.value
        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("key" to u.key, "saver" to p.dataSaver))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (p.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
            .addTag(TAG).addTag("k:" + u.key)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("dl:" + u.key, ExistingWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context, u: DlUnit) { WorkManager.getInstance(ctx).cancelUniqueWork("dl:" + u.key) }

    /** Live state of every download unit WorkManager knows about. */
    fun states(ctx: Context): Flow<Map<String, DlState>> = WorkManager.getInstance(ctx).getWorkInfosByTagFlow(TAG).map { list ->
        val out = HashMap<String, DlState>()
        for (w in list) {
            val key = w.tags.firstOrNull { it.startsWith("k:") }?.removePrefix("k:") ?: continue
            val st = when (w.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> if (w.runAttemptCount > 0) DlState.Failed("retry", w.progress.getInt("done", 0), w.progress.getInt("total", 0)) else DlState.Queued
                WorkInfo.State.RUNNING -> DlState.Running(w.progress.getInt("done", 0), w.progress.getInt("total", 0), w.progress.getLong("bytes", 0))
                WorkInfo.State.SUCCEEDED -> DlState.Done(w.outputData.getInt("files", 0), w.outputData.getLong("bytes", 0))
                WorkInfo.State.FAILED -> DlState.Failed(w.outputData.getString("reason") ?: "failed", w.outputData.getInt("done", 0), w.outputData.getInt("total", 0))
                WorkInfo.State.CANCELLED -> null
            } ?: continue
            // Newest work for a key wins (a cancelled/failed run may still be listed).
            if (out[key] == null || st is DlState.Running || st is DlState.Queued) out[key] = st
        }
        out
    }

    /**
     * Resumable HTTP download into [dest] via "[dest].part": continues with a Range request when a partial file exists;
     * restarts if the server ignores the range. Returns bytes written in this call.
     */
    fun fetch(url: String, dest: File, stopped: () -> Boolean = { false }): Long {
        dest.parentFile?.mkdirs()
        val part = File(dest.path + ".part")
        var have = if (part.isFile) part.length() else 0L
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 30_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Sakina-Quran-Android")
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        try {
            val code = c.responseCode
            if (code == 416 && have > 0) { part.renameTo(dest); return 0 } // already complete
            if (code !in 200..299) error("HTTP $code")
            if (code == 200 && have > 0) { have = 0; part.delete() } // server ignored Range → restart
            var written = 0L
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(have)
                c.inputStream.use { ins ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        if (stopped()) error("stopped")
                        val n = ins.read(buf); if (n < 0) break
                        raf.write(buf, 0, n); written += n
                    }
                }
            }
            if (part.length() <= 0) error("empty")
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) error("rename")
            return written
        } finally { c.disconnect() }
    }
}

class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val key = inputData.getString("key") ?: return@withContext Result.failure()
        val u = DlUnit.parse(key) ?: return@withContext Result.failure(workDataOf("reason" to "bad key"))
        val q = runCatching { Quran.load(applicationContext) }.getOrNull() ?: return@withContext Result.retry()
        val files = Downloads.files(applicationContext, q, u, inputData.getBoolean("saver", false))
            ?: return@withContext Result.failure(workDataOf("reason" to "source"))
        val total = files.size
        var done = 0; var bytes = 0L; var failed = 0
        setProgress(workDataOf("done" to 0, "total" to total, "bytes" to 0L))
        for (f in files) {
            if (isStopped) return@withContext Result.failure(workDataOf("reason" to "cancelled", "done" to done, "total" to total))
            val dest = AudioFiles.file(applicationContext, f.rel)
            if (!(dest.isFile && dest.length() > 0)) {
                val ok = runCatching { bytes += Downloads.fetch(f.url, dest) { isStopped } }.isSuccess
                if (!ok) failed++
            }
            if (dest.isFile) done++
            setProgress(workDataOf("done" to done, "total" to total, "bytes" to bytes))
        }
        // Whole-surah units: cache the ayah timings too, so highlighting works offline.
        if (u.mode == AudioMode.SURAH) u.who.toIntOrNull()?.let { m ->
            files.forEach { f -> f.rel.substringAfterLast('/').removeSuffix(".mp3").toIntOrNull()?.let { s -> runCatching { Timings.load(applicationContext, m, s) } } }
        }
        when {
            failed == 0 -> Result.success(workDataOf("files" to done, "bytes" to bytes))
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure(workDataOf("reason" to "network", "done" to done, "total" to total))
        }
    }
}
