package app.sakina.quran

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.UserRepo
import app.sakina.quran.ui.AppRoot
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.SakinaTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Tasbih registers a handler while open so the volume keys count (TS01). */
object VolumeKeys {
    @Volatile var handler: (() -> Unit)? = null
}

class MainActivity : AppCompatActivity() {
    private var ready by mutableStateOf(false)
    /** In-app route requested by a notification (allow-listed). */
    private var openRoute by mutableStateOf<String?>(null)

    private fun routeFrom(i: Intent?): String? = i?.getStringExtra("open")?.takeIf { r ->
        r.startsWith("azkar/") || r in setOf("azkar", "tasbih", "prayers", "quran", "duas", "ruqyah", "khatma", "mushaf")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFrom(intent)?.let { openRoute = it }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val h = VolumeKeys.handler
        if (h != null && (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) h()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !ready }
        val repo = SettingsRepo(applicationContext)
        val user = UserRepo(applicationContext)
        val startRoute = if (BuildConfig.DEBUG) intent?.getStringExtra("route") else null
        if (savedInstanceState == null) openRoute = routeFrom(intent)

        // Debug/CI only: write the extras synchronously so an activity recreation (e.g. from the
        // language switch below) can never cancel them half-way.
        if (BuildConfig.DEBUG) { runBlocking { applyDebugExtras(repo) }; applyDebugLanguage() }
        lifecycleScope.launch {
            user.ensureMemberSince()
            repo.now() // first load done → release splash
            ready = true
        }
        setContent {
            val prefs: Prefs? by repo.prefs.collectAsStateWithLifecycle(initialValue = null)
            val p = prefs
            if (ready && p != null) {
                SakinaTheme(p) {
                    val dark = LocalSakina.current.look.dark
                    LaunchedEffect(dark) {
                        val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                    }
                    AppRoot(repo, user, p, startRoute, openRoute = openRoute, onOpened = { openRoute = null })
                }
            }
        }
    }

    /** Debug/CI only: `--es lang ar|en`. Applied once; AppCompat recreates the activity if it changes. */
    private fun applyDebugLanguage() {
        val lang = intent?.getStringExtra("lang")?.takeIf { it.isNotBlank() } ?: return
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != lang) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }

    /** Debug/CI only: `--es theme CLASSIC_TEAL --es mode DARK --es glass FULL --es icons AUTO --es onboarded true`. */
    private suspend fun applyDebugExtras(repo: SettingsRepo) {
        val i = intent ?: return
        val onboarded = i.extras?.let { b ->
            when {
                !b.containsKey("onboarded") -> null
                b.getString("onboarded") != null -> b.getString("onboarded")!!.toBooleanStrictOrNull()
                else -> b.getBoolean("onboarded")
            }
        }
        val theme = i.getStringExtra("theme"); val mode = i.getStringExtra("mode")
        val glass = i.getStringExtra("glass"); val icons = i.getStringExtra("icons")
        if (theme != null || mode != null || glass != null || icons != null || onboarded != null) {
            repo.debugApply(theme, mode, glass, icons, onboarded)
        }
        // Q2 CI: sample Quran data and reader settings ("qseed=1", "qset=tajweed=1,paper=1").
        val qs = app.sakina.quran.data.QuranStore(applicationContext)
        if (i.getStringExtra("qseed") == "1") runCatching { app.sakina.quran.debug.DebugSeed.seed(qs, i.getStringExtra("lang") != "en") }
        i.getStringExtra("qset")?.let { spec -> runCatching { app.sakina.quran.debug.DebugSeed.applyPrefs(qs, spec) } }
    }
}
