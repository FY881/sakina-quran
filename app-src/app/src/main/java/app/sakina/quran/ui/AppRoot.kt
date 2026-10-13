package app.sakina.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import app.sakina.quran.ui.tabs.ComingSoonSheet
import app.sakina.quran.ui.tabs.Feature
import app.sakina.quran.ui.tabs.LocalSoon
import app.sakina.quran.ui.tabs.feature
import app.sakina.quran.ui.quran.QuranTab
import app.sakina.quran.ui.quran.QuranReader
import app.sakina.quran.ui.quran.QuranSearch
import app.sakina.quran.ui.dhikr.NamesScreen
import app.sakina.quran.ui.dhikr.NamesQuizScreen
import app.sakina.quran.ui.azkar.AzkarHome
import app.sakina.quran.ui.azkar.AzkarSessionScreen
import app.sakina.quran.ui.azkar.HisnScreen
import app.sakina.quran.ui.azkar.AzkarSearchScreen
import app.sakina.quran.ui.azkar.AzkarMineScreen
import app.sakina.quran.ui.azkar.DuasScreen
import app.sakina.quran.ui.azkar.RuqyahScreen
import app.sakina.quran.ui.azkar.IstikharaScreen
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import app.sakina.quran.ui.dhikr.TasbihScreen
import app.sakina.quran.ui.onboarding.OnboardingScreen
import app.sakina.quran.ui.profile.IconPackPickerScreen
import app.sakina.quran.ui.profile.ProfileScreen
import app.sakina.quran.ui.profile.ThemePickerScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.sakina.quran.ui.theme.LocalSakina
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

enum class Tab(val route: String, val ar: String, val en: String, val icon: ImageVector) {
    HOME("home", "الرئيسية", "Home", Icons.Outlined.Home),
    PRAYERS("prayers", "الصلوات", "Prayers", Icons.Outlined.AccessTime),
    QURAN("quran", "القرآن", "Quran", Icons.AutoMirrored.Outlined.MenuBook),
    COMMUNITY("community", "إلهام", "Community", Icons.Outlined.Groups),
    EXPLORE("explore", "استكشف", "Explore", Icons.Outlined.Explore),
}

private val KNOWN_ROUTES = Tab.entries.map { it.route }.toSet() +
    setOf("settings", "city", "qibla", "more", "profile", "themes", "iconpacks", "tasbih", "names", "quran_search", "onboarding",
        "azkar", "hisn", "duas", "ruqyah", "istikhara", "names_quiz", "azkar_search", "azkar_mine",
        "mushaf", "tafsir", "quran_library", "reciters", "khatma", "khatma_new", "tajweed", "topics", "quran_marks", "quran_stats", "share_ayah")

/** Route prefixes with path arguments that are navigated as-is. */
private val PATH_ROUTES = listOf("reader/", "mushaf/", "tafsir/", "topic/", "surah_info/", "share/")

/** Routes that hide the bottom bar (immersive / first-run). */
private fun hidesBar(route: String?) = route == "onboarding" || route?.startsWith("reader/") == true || route?.startsWith("azkar/") == true ||
    route == "mushaf" || route?.startsWith("mushaf/") == true

@Composable
fun AppRoot(repo: SettingsRepo, user: UserRepo, prefs: Prefs, startRoute: String? = null, openRoute: String? = null, onOpened: () -> Unit = {}) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val userData by user.data.collectAsStateWithLifecycle(initialValue = UserData())
    val appCtx = LocalContext.current.applicationContext
    val qstore = remember { app.sakina.quran.data.QuranStore(appCtx) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val st = LocalSakina.current
    val look = st.look
    val isAr = app.sakina.quran.ui.common.isArabic()
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
        } else if (route.startsWith("azkar/")) {
            // "azkar/<sid>@<index>" → session route with optional start index.
            val sid = route.removePrefix("azkar/").substringBefore('@'); val i = route.substringAfter('@', "0").toIntOrNull() ?: 0
            nav.navigate("azkar/$sid/$i") { launchSingleTop = true }
        } else if (route.substringBefore('?') in KNOWN_ROUTES) nav.navigate(route) { launchSingleTop = true }
        else if (PATH_ROUTES.any { route.startsWith(it) }) nav.navigate(route) { launchSingleTop = true }
    }
    LaunchedEffect(openRoute) { if (openRoute != null) { go(openRoute); onOpened() } }
    val snack = remember { SnackbarHostState() }
    // Debug/CI start route, e.g. "explore?q=zakat", "explore?soon=hajj", "reader/2/255", "onboarding?step=2".
    val baseStart = startRoute?.substringBefore('?')
    val startArgs = remember(startRoute) {
        startRoute?.substringAfter('?', "")?.split('&')?.mapNotNull { kv -> kv.split('=').takeIf { it.size == 2 }?.let { it[0] to java.net.URLDecoder.decode(it[1], "UTF-8") } }?.toMap() ?: emptyMap()
    }
    val soon = remember { mutableStateOf<Feature?>(null) }
    LaunchedEffect(startRoute) {
        if (baseStart != null && baseStart != Tab.HOME.route && baseStart != "onboarding") go(baseStart)
        startArgs["soon"]?.let { id -> soon.value = feature(id) }
    }
    val startDest = remember { if (!prefs.onboarded || baseStart == "onboarding") "onboarding" else Tab.HOME.route }

    Box(Modifier.fillMaxSize()) {
        // Backdrop: the layer every glass card blurs.
        Box(Modifier.fillMaxSize().hazeSource(st.haze).background(Brush.verticalGradient(listOf(look.bgTop, look.bgBottom))))
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (!hidesBar(current)) NavigationBar(containerColor = look.bgBottom, tonalElevation = 0.dp) {
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
            CompositionLocalProvider(LocalSoon provides soon, app.sakina.quran.ui.quran.LocalQStore provides qstore) {
            NavHost(nav, startDestination = startDest, modifier = Modifier.padding(bottom = pad.calculateBottomPadding())) {
                composable(Tab.HOME.route) {
                    HomeScreen(
                        prefs = prefs, user = userData, onOpen = ::go,
                        onDismissHealth = { d -> scope.launch { user.dismissHealth(d) } },
                        onSeenStory = { k -> scope.launch { user.markSeen(k) } },
                        onSetFeatures = { ids -> scope.launch { user.setFeatures(ids) } },
                    )
                }
                composable(Tab.PRAYERS.route) {
                    PrayerScreen(
                        prefs = prefs, user = userData, onCycleMode = cycle,
                        onToggle = { d, i ->
                            val wasPrayed = userData.isPrayed(d, i)
                            scope.launch { user.togglePrayed(d, i) }
                            // AZ03: after marking a prayer, offer the after-prayer azkar.
                            if (!wasPrayed) scope.launch {
                                snack.currentSnackbarData?.dismiss()
                                val r = snack.showSnackbar(
                                    if (isAr) "تقبّل الله — أذكار ما بعد الصلاة؟" else "May Allah accept — after-prayer azkar?",
                                    actionLabel = if (isAr) "افتح" else "Open", duration = SnackbarDuration.Short,
                                )
                                if (r == SnackbarResult.ActionPerformed) go("azkar/after_prayer")
                            }
                        },
                        onMarkAll = { d -> scope.launch { user.markAll(d) } },
                        onSettings = { go("settings") }, onCity = { go("city") },
                    )
                }
                composable(Tab.QURAN.route) { QuranTab(user = userData, onOpen = ::go) }
                composable(Tab.COMMUNITY.route) { CommunityTab() }
                composable(Tab.EXPLORE.route) { ExploreTab(onOpen = ::go, initialQuery = startArgs["q"] ?: "") }
                composable("settings") { Box(Modifier.statusBarsPadding()) { PrayerSettingsScreen(prefs = prefs, repo = repo, onCity = { go("city") }) } }
                composable("city") { Box(Modifier.statusBarsPadding()) { CityScreen(repo = repo, onDone = { nav.popBackStack() }) } }
                composable("qibla") { Box(Modifier.statusBarsPadding()) { QiblaScreen(prefs = prefs) } }
                composable("more") { Box(Modifier.statusBarsPadding()) { MoreScreen(repo) } }
                composable("onboarding") {
                    OnboardingScreen(
                        prefs = prefs, repo = repo, onChooseCity = { go("city") }, startStep = startArgs["step"]?.toIntOrNull() ?: 0,
                        onFinish = { nav.navigate(Tab.HOME.route) { popUpTo("onboarding") { inclusive = true } } },
                    )
                }
                composable("profile") {
                    ProfileScreen(userData, prefs, onBack = { nav.popBackStack() }, onOpen = ::go, onName = { n -> scope.launch { user.setName(n) } })
                }
                composable("themes") { ThemePickerScreen(prefs, repo, onBack = { nav.popBackStack() }) }
                composable("iconpacks") { IconPackPickerScreen(prefs, repo, onBack = { nav.popBackStack() }) }
                composable("tasbih") {
                    TasbihScreen(
                        userData, onBack = { nav.popBackStack() },
                        onTap = { scope.launch { user.tasbihTap() } }, onUndo = { scope.launch { user.tasbihUndo() } },
                        onSave = { scope.launch { user.tasbihSave() } }, onPreset = { id, n -> scope.launch { user.tasbihPreset(id, n) } },
                        onTarget = { n -> scope.launch { user.tasbihTarget(n) } }, onClearHistory = { scope.launch { user.clearTasbihHistory() } },
                        onSkin = { i -> scope.launch { user.setTasbihSkin(i) } }, onSound = { b -> scope.launch { user.setTasbihSound(b) } },
                        onVolumeKeys = { b -> scope.launch { user.setTasbihVolumeKeys(b) } },
                        onAddCustom = { txt, n -> scope.launch { user.addTasbihCustom(txt, n) } }, onDeleteCustom = { id -> scope.launch { user.deleteTasbihCustom(id) } },
                    )
                }
                composable("names") { NamesScreen(userData, onBack = { nav.popBackStack() }, onFav = { i -> scope.launch { user.toggleNameFav(i) } }, onTasbih = { go("tasbih") }, onQuiz = { go("names_quiz") }) }
                composable("names_quiz") { NamesQuizScreen(userData, onBack = { nav.popBackStack() }, onResult = { sc, tot -> scope.launch { user.addQuizResult(sc, tot) } }, onNames = { go("names") }) }
                composable("azkar") { AzkarHome(prefs, repo, userData, onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("azkar/{sid}/{i}") { e ->
                    val sid = e.arguments?.getString("sid") ?: "morning"
                    val i = e.arguments?.getString("i")?.toIntOrNull() ?: 0
                    AzkarSessionScreen(
                        sid, i, userData, onBack = { nav.popBackStack() }, onOpen = ::go,
                        onProgress = { s2, d -> scope.launch { user.setAzkarProgress(s2, d) } },
                        onComplete = { s2 -> scope.launch { user.azkarComplete(s2) } },
                        onFav = { id -> scope.launch { user.toggleAzkarFav(id) } },
                    )
                }
                composable("hisn?cat={cat}", arguments = listOf(androidx.navigation.navArgument("cat") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null })) { e ->
                    HisnScreen(e.arguments?.getString("cat") ?: startArgs["cat"], onBack = { nav.popBackStack() }, onOpen = ::go)
                }
                composable("azkar_search") { AzkarSearchScreen(startArgs["q"] ?: "", onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("azkar_mine") {
                    AzkarMineScreen(
                        userData, onBack = { nav.popBackStack() }, onOpen = ::go, onFav = { id -> scope.launch { user.toggleAzkarFav(id) } },
                        onAdd = { txt, n -> scope.launch { user.addOwnAzkar(txt, n) } }, onDelete = { id -> scope.launch { user.deleteOwnAzkar(id) } },
                    )
                }
                composable("duas") { DuasScreen(userData, startArgs["tab"]?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }, onOpen = ::go, onFav = { id -> scope.launch { user.toggleAzkarFav(id) } }) }
                composable("ruqyah") { RuqyahScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("istikhara") { IstikharaScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("quran_search") { QuranSearch(onBack = { nav.popBackStack() }, onOpen = ::go, initial = startArgs["q"] ?: "") }
                composable("reader/{sura}/{ayah}") { e ->
                    val s = e.arguments?.getString("sura")?.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    val a = e.arguments?.getString("ayah")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    QuranReader(
                        s, a, userData, onBack = { nav.popBackStack() },
                        onSaveLast = { ss, aa -> scope.launch { user.setQuranLast(ss, aa) } },
                        onBookmark = { ss, aa -> scope.launch { user.toggleBookmark(ss, aa) } },
                        onOpenSura = { n -> nav.navigate("reader/$n/1") { popUpTo("reader/{sura}/{ayah}") { inclusive = true } } },
                        onOpen = ::go,
                    )
                }
                // ── Q2: mushaf, tafsir, translations, khatma, share studio, topics, marks, reciters, tajweed
                val saveLast: (Int, Int) -> Unit = { ss, aa -> scope.launch { user.setQuranLast(ss, aa) } }
                composable("mushaf") {
                    val lp by androidx.compose.runtime.produceState<Int?>(null) { value = runCatching { qstore.now().prefs.lastPage }.getOrDefault(1) }
                    lp?.let { p -> app.sakina.quran.ui.quran.MushafScreen(p, null, onBack = { nav.popBackStack() }, onOpen = ::go, onSaveLast = saveLast) }
                }
                composable(
                    "mushaf/{page}?k={k}",
                    arguments = listOf(
                        androidx.navigation.navArgument("page") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("k") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { e ->
                    val p = e.arguments?.getString("page")?.toIntOrNull()?.coerceIn(1, 604) ?: 1
                    app.sakina.quran.ui.quran.MushafScreen(p, e.arguments?.getString("k"), onBack = { nav.popBackStack() }, onOpen = ::go, onSaveLast = saveLast)
                }
                composable("tafsir") {
                    val last by androidx.compose.runtime.produceState<Pair<Int, Int>?>(null) {
                        val u = runCatching { user.data.first() }.getOrNull()
                        value = if (u != null && u.lastSura in 1..114) u.lastSura to u.lastAyah else 1 to 1
                    }
                    last?.let { (ss, aa) -> app.sakina.quran.ui.quran.TafsirScreen(ss, aa, onBack = { nav.popBackStack() }, onOpen = ::go) }
                }
                composable("tafsir/{sura}/{ayah}") { e ->
                    val ss = e.arguments?.getString("sura")?.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    val aa = e.arguments?.getString("ayah")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    app.sakina.quran.ui.quran.TafsirScreen(ss, aa, onBack = { nav.popBackStack() }, onOpen = ::go)
                }
                composable("quran_library") { app.sakina.quran.ui.quran.QuranLibraryScreen(onBack = { nav.popBackStack() }) }
                composable("reciters") { app.sakina.quran.ui.quran.RecitersScreen(onBack = { nav.popBackStack() }) }
                composable("khatma") { app.sakina.quran.ui.quran.KhatmaScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("khatma_new") { app.sakina.quran.ui.quran.KhatmaNewScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }) }
                composable("quran_stats") { app.sakina.quran.ui.quran.QuranStatsScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("tajweed") { app.sakina.quran.ui.quran.TajweedGuideScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("topics") { app.sakina.quran.ui.quran.TopicsScreen(onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("topic/{id}") { e -> app.sakina.quran.ui.quran.TopicScreen(e.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("surah_info/{sura}") { e ->
                    app.sakina.quran.ui.quran.SurahInfoScreen(e.arguments?.getString("sura")?.toIntOrNull()?.coerceIn(1, 114) ?: 1, onBack = { nav.popBackStack() }, onOpen = ::go)
                }
                composable("quran_marks") { app.sakina.quran.ui.quran.QuranMarksScreen(startArgs["tab"]?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }, onOpen = ::go) }
                composable("share/{s}/{a}/{b}") { e ->
                    val ss = e.arguments?.getString("s")?.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    val aa = e.arguments?.getString("a")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val bb = e.arguments?.getString("b")?.toIntOrNull()?.coerceAtLeast(aa) ?: aa
                    app.sakina.quran.ui.quran.ShareStudioScreen(ss, aa, bb, onBack = { nav.popBackStack() })
                }
                composable("share_ayah") {
                    val last by androidx.compose.runtime.produceState<Pair<Int, Int>?>(null) {
                        val u = runCatching { user.data.first() }.getOrNull()
                        value = if (u != null && u.lastSura in 1..114) u.lastSura to u.lastAyah else 2 to 255
                    }
                    last?.let { (ss, aa) -> app.sakina.quran.ui.quran.ShareStudioScreen(ss, aa, aa, onBack = { nav.popBackStack() }) }
                }
            }
            ComingSoonSheet(soon, onOpen = ::go)
            }
        }
        // Status-bar scrim so scrolled content never runs under the clock/icons.
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(look.bgTop.copy(alpha = .94f)))
    }
}
