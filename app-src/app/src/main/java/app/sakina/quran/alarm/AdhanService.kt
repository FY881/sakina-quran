package app.sakina.quran.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.sakina.quran.R
import app.sakina.quran.prayer.Prayer

/** Plays the adhan with a stop action. Started from the exact alarm or from "test adhan". */
class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    companion object {
        const val STOP = "app.sakina.quran.STOP_ADHAN"
        fun intent(c: Context, p: Prayer, voice: Int, friday: Boolean) = Intent(c, AdhanService::class.java)
            .putExtra("prayer", p.name).putExtra("voice", voice).putExtra("friday", friday)
        fun stop(c: Context) = c.startService(Intent(c, AdhanService::class.java).setAction(STOP))
        fun rawFor(voice: Int) = when (voice) {
            2 -> R.raw.adhan_2; 3 -> R.raw.adhan_3; 0 -> R.raw.takbeer; else -> R.raw.adhan_1
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { finish(); return START_NOT_STICKY }
        val prayer = runCatching { Prayer.valueOf(intent?.getStringExtra("prayer")!!) }.getOrDefault(Prayer.DHUHR)
        val voice = intent?.getIntExtra("voice", 1) ?: 1
        val friday = intent?.getBooleanExtra("friday", false) ?: false
        Notifs.ensure(this)
        val stopPi = PendingIntent.getService(this, 2, Intent(this, AdhanService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, Notifs.CH_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_sakina)
            .setContentTitle(Texts.timeFor(this, prayer, friday))
            .setContentText(Texts.hint(this, prayer))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(Notifs.openApp(this))
            .setDeleteIntent(stopPi)
            .addAction(0, Texts.stop(this), stopPi)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, Notifs.ID_ADHAN, n,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
        play(voice)
        return START_NOT_STICKY
    }

    private fun play(voice: Int) {
        player?.release()
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val am = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS) finish() }.build()
            am.requestAudioFocus(focus!!)
        }
        player = runCatching {
            val afd = resources.openRawResourceFd(AdhanService.rawFor(voice))
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length); afd.close()
                setWakeMode(this@AdhanService, PowerManager.PARTIAL_WAKE_LOCK)
                setOnCompletionListener { finish() }
                setOnErrorListener { _, _, _ -> finish(); true }
                prepare(); start()
            }
        }.getOrElse { finish(); null }
    }

    private fun finish() {
        runCatching { player?.stop() }; player?.release(); player = null
        if (Build.VERSION.SDK_INT >= 26) focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
