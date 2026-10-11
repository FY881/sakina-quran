package app.sakina.quran

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.ThemeMode
import app.sakina.quran.ui.AppRoot
import app.sakina.quran.ui.theme.SakinaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = SettingsRepo(applicationContext)
        setContent {
            val theme by repo.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            SakinaTheme(theme) { AppRoot(repo) }
        }
    }
}
