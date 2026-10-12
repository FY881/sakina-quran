package app.sakina.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.sakina.quran.alarm.Scheduler
import app.sakina.quran.data.AlertMode
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.UserData
import app.sakina.quran.data.UserRepo
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.ui.common.nextMode
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.home.HomeScreen
import app.sakina.quran.ui.more.MoreScreen
import app.sakina.quran.ui.prayer.PrayerScreen
import app.sakina.quran.ui.qibla.QiblaScreen
import app.sakina.quran.ui.settings.CityScreen
import app.sakina.quran.ui.settings.PrayerSettingsScreen
import app.sakina.quran.ui.tabs.CommunityTab
import app.sakina.quran.ui.tabs.ExploreTab
import app.sakina.quran.ui.tabs.QuranTab
import app.sakina.quran.ui.theme.LocalSakina
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

enum class Tab(val route: String, val ar: String, val en: String, val icon: ImageVector) {
    HOME("home", "الرئيسية", "Home", Icons.Outlined.Home),
    PRAYERS("prayers", "الصلوات", "Prayers", Icons.Outlined.AccessTime),
    QURAN("quran", "القرآن", "Quran", Icons.AutoMirrored.Outlined.MenuBook),
    COMMUNITY("community", "إلهام", "Community", Icons.Outlined.Groups),
    EXPLORE("explore", "استكشف", "Explore", Icons.Outlined.Explore),
}

private val KNOWN_ROUTES = Tab.entries.map { it.route }.toSet() + setOf("settings", "city", "qibla", "more")

@Composable
fun AppRoot(repo: SettingsRepo, user: UserRepo, prefs: Prefs, startRoute: String? = null) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val userData by user.data.collectAsStateWithLifecycle(initialValue = UserData())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val st = LocalSakina.current
    val look = st.look
    LaunchedEffect(prefs) { runCatching { Scheduler.reschedule(ctx.applicationContext, prefs) } }
    val cycle: (Prayer) -> Unit = { p ->
        val m = prefs.modes[p] ?: AlertMode.OFF
        scope.launch { repo.setMode(p, m.nextMode()) }
    }
    fun go(route: String) {
        if (Tab.entries.any { it.route == route }) nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        } else if (route in KNOWN_ROUTES) nav.navigate(route)
    }
    LaunchedEffect(startRoute) { if (startRoute != null && startRoute != Tab.HOME.route) go(startRoute) }

    Box(Modifier.fillMaxSize()) {
        // Backdrop: the layer every glass card blurs.
        Box(Modifier.fillMaxSize().hazeSource(st.haze).background(Brush.verticalGradient(listOf(look.bgTop, look.bgBottom))))
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                NavigationBar(containerColor = look.bgBottom, tonalElevation = 0.dp) {
                    Tab.entries.forEach { tb ->
                        val sel = current == tb.route
                        NavigationBarItem(
                            selected = sel,
                            onClick = { go(tb.route) },
                            icon = { Icon(tb.icon, contentDescription = null) },
                            label = { Text(t(tb.ar, tb.en), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                unselectedIconColor = look.onCardMuted,
                                unselectedTextColor = look.onCardMuted,
                                indicatorColor = look.card,
                            ),
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(bottom = pad.calculateBottomPadding())) {
                composable(Tab.HOME.route) {
                    HomeScreen(
                        prefs = prefs, user = userData, onOpen = ::go,
                        onDismissHealth = { d -> scope.launch { user.dismissHealth(d) } },
                        onSeenStory = { k -> scope.launch { user.markSeen(k) } },
                    )
                }
                composable(Tab.PRAYERS.route) {
                    PrayerScreen(
                        prefs = prefs, user = userData, onCycleMode = cycle,
                        onToggle = { d, i -> scope.launch { user.togglePrayed(d, i) } },
                        onMarkAll = { d -> scope.launch { user.markAll(d) } },
                        onSettings = { go("settings") }, onCity = { go("city") },
                    )
                }
                composable(Tab.QURAN.route) { QuranTab(onOpen = ::go) }
                composable(Tab.COMMUNITY.route) { CommunityTab() }
                composable(Tab.EXPLORE.route) { ExploreTab(onOpen = ::go) }
                composable("settings") { Box(Modifier.statusBarsPadding()) { PrayerSettingsScreen(prefs = prefs, repo = repo, onCity = { go("city") }) } }
                composable("city") { Box(Modifier.statusBarsPadding()) { CityScreen(repo = repo, onDone = { nav.popBackStack() }) } }
                composable("qibla") { Box(Modifier.statusBarsPadding()) { QiblaScreen(prefs = prefs) } }
                composable("more") { Box(Modifier.statusBarsPadding()) { MoreScreen(repo) } }
            }
        }
    }
}
