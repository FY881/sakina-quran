package app.sakina.quran

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import app.sakina.quran.ui.theme.SakinaTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var ready by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !ready }
        val repo = SettingsRepo(applicationContext)
        val user = UserRepo(applicationContext)
        val startRoute = if (BuildConfig.DEBUG) intent?.getStringExtra("route") else null

        if (BuildConfig.DEBUG) applyDebugLanguage()
        lifecycleScope.launch {
            if (BuildConfig.DEBUG) applyDebugExtras(repo)
            user.ensureMemberSince()
            repo.now() // first load done → release splash
            ready = true
        }
        setContent {
            val prefs: Prefs? by repo.prefs.collectAsStateWithLifecycle(initialValue = null)
            val p = prefs
            if (ready && p != null) {
                SakinaTheme(p) { AppRoot(repo, user, p, startRoute) }
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
    }
}
