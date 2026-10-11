package app.sakina.quran.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.sakina.quran.R
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.AlertMode
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.alarm.Scheduler
import app.sakina.quran.ui.common.nextMode
import app.sakina.quran.ui.qibla.QiblaScreen
import app.sakina.quran.ui.settings.CityScreen
import app.sakina.quran.ui.settings.PrayerSettingsScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import app.sakina.quran.ui.common.ComingSoon
import app.sakina.quran.ui.home.HomeScreen
import app.sakina.quran.ui.more.MoreScreen
import app.sakina.quran.ui.prayer.PrayerScreen

enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Outlined.Home),
    QURAN("quran", R.string.tab_quran, Icons.AutoMirrored.Outlined.MenuBook),
    PRAYER("prayer", R.string.tab_prayer, Icons.Outlined.AccessTime),
    AZKAR("azkar", R.string.tab_azkar, Icons.Outlined.Spa),
    MORE("more", R.string.tab_more, Icons.Outlined.MoreHoriz),
}

@Composable
fun AppRoot(repo: SettingsRepo) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val prefs by repo.prefs.collectAsStateWithLifecycle(initialValue = Prefs())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(prefs) { runCatching { Scheduler.reschedule(ctx.applicationContext, prefs) } }
    val cycle: (Prayer) -> Unit = { p ->
        val m = prefs.modes[p] ?: AlertMode.OFF
        scope.launch { repo.setMode(p, m.nextMode()) }
    }
    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { go(t.route) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(pad)) {
            composable(Tab.HOME.route) { HomeScreen(prefs = prefs, onOpen = { r -> if (Tab.entries.any { it.route == r }) go(r) else nav.navigate(r) }, onCycleMode = cycle) }
            composable(Tab.QURAN.route) { ComingSoon(R.string.tab_quran) }
            composable(Tab.PRAYER.route) { PrayerScreen(prefs = prefs, onCycleMode = cycle, onSettings = { nav.navigate("settings") }, onCity = { nav.navigate("city") }) }
            composable("settings") { PrayerSettingsScreen(prefs = prefs, repo = repo, onCity = { nav.navigate("city") }) }
            composable("city") { CityScreen(repo = repo, onDone = { nav.popBackStack() }) }
            composable("qibla") { QiblaScreen(prefs = prefs) }
            composable(Tab.AZKAR.route) { ComingSoon(R.string.tab_azkar) }
            composable(Tab.MORE.route) { MoreScreen(repo) }
        }
    }
}
