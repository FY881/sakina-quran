package app.sakina.quran.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import app.sakina.quran.data.AppTheme
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.ThemeMode
import app.sakina.quran.location.Cities
import app.sakina.quran.location.LocationHelper
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.profile.ThemePreview
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STEPS = 5

/** First-run flow: welcome → language → location (prominent disclosure) → notifications → theme. */
@Composable
fun OnboardingScreen(prefs: Prefs, repo: SettingsRepo, onChooseCity: () -> Unit, onFinish: () -> Unit, startStep: Int = 0) {
    var step by rememberSaveable { mutableIntStateOf(startStep.coerceIn(0, STEPS - 1)) }
    val look = LocalSakina.current.look
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        // Progress dots + skip
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(STEPS) { i ->
                    Box(Modifier.height(6.dp).width(if (i == step) 26.dp else 8.dp).clip(CircleShape).background(if (i <= step) look.cta else look.onCardMuted.copy(alpha = .3f)))
                }
            }
            if (step < STEPS - 1) Text(
                t("تخطٍّ", "Skip"), color = look.onCardMuted, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).press { step = STEPS - 1 }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        AnimatedContent(step, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ob") { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                when (s) {
                    0 -> Welcome()
                    1 -> Language()
                    2 -> LocationStep(prefs, repo, onChooseCity)
                    3 -> NotifStep()
                    else -> ThemeStep(prefs, repo)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) GhostPill(t("السابق", "Back")) { step-- }
            Spacer(Modifier.weight(1f))
            if (step < STEPS - 1) CtaPill(t("التالي", "Next"), Modifier.width(160.dp)) { step++ }
            else CtaPill(t("ابدأ رحلتك", "Start"), Modifier.width(180.dp)) { scope.launch { repo.setOnboarded(); onFinish() } }
        }
    }
}

@Composable
private fun ColumnScope.Hero(ico: Ico, title: String, body: String) {
    VSpace(28.dp)
    Box(Modifier.size(150.dp).surface(CircleShape, strong = true), contentAlignment = Alignment.Center) { PackIcon(ico, 96.dp) }
    VSpace(24.dp)
    Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    VSpace(10.dp)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = LocalSakina.current.look.onCardMuted, textAlign = TextAlign.Center)
    VSpace(20.dp)
}

@Composable
private fun ColumnScope.Welcome() {
    Hero(Ico.MOSQUE, t("أهلًا بك في سكينة", "Welcome to Sakina"), t("رفيقك اليومي للصلاة والقرآن والذكر. مجاني بالكامل، بلا إعلانات، ويعمل دون إنترنت.", "Your daily companion for prayer, Quran and dhikr. Completely free, no ads, works offline."))
    Text("أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ", fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 48.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    VSpace(18.dp)
    SCard {
        listOf(
            Ico.ADHAN to t("أذان دقيق وموثوق لكل مدينة", "Accurate, reliable adhan for every city"),
            Ico.QURAN to t("المصحف كاملًا بالرسم العثماني", "The full Quran in Uthmani script"),
            Ico.TASBIH to t("سبحة وأذكار وأسماء الله الحسنى", "Tasbih, azkar and the 99 Names"),
            Ico.PRIVACY to t("خصوصيتك محفوظة: بياناتك على جهازك", "Private: your data stays on your phone"),
        ).forEach { (i, s) ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { PackIcon(i, 30.dp); Spacer(Modifier.width(12.dp)); Text(s, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun ColumnScope.Language() {
    val ar = isArabic()
    Hero(Ico.LANGUAGE, t("اختر لغتك", "Choose your language"), t("يمكنك تغييرها في أي وقت من الإعدادات.", "You can change it anytime in settings."))
    LangCard("العربية", "Arabic", ar) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar")) }
    VSpace(12.dp)
    LangCard("English", "الإنجليزية", !ar) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
    VSpace(14.dp)
    Muted(t("لغات أخرى (الفرنسية، الأردية، الإندونيسية، التركية) قريبًا.", "More languages (French, Urdu, Indonesian, Turkish) coming soon."))
}

@Composable
private fun LangCard(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val look = LocalSakina.current.look
    SCard(
        Modifier.then(if (selected) Modifier.border(2.dp, look.cta, RoundedCornerShape(22.dp)) else Modifier),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Muted(sub) }
            Box(Modifier.size(26.dp).clip(CircleShape).border(2.dp, if (selected) look.cta else look.onCardMuted, CircleShape).padding(5.dp).clip(CircleShape).background(if (selected) look.cta else look.card))
        }
    }
}

@Composable
private fun ColumnScope.LocationStep(prefs: Prefs, repo: SettingsRepo, onChooseCity: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val noLoc = t("تعذّر تحديد الموقع. اختر مدينتك يدويًا.", "Couldn't get your location. Choose your city manually.")
    fun locate() = scope.launch {
        busy = true; err = null
        val loc = LocationHelper.current(ctx)
        if (loc == null) { err = noLoc; busy = false; return@launch }
        val city = withContext(Dispatchers.Default) { Cities.nearest(ctx, loc.latitude, loc.longitude) }
        if (city != null) repo.setPlace(city.toPlace().copy(lat = loc.latitude, lng = loc.longitude), autoMethod = true)
        busy = false
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) locate() else err = noLoc }
    Hero(Ico.LOCATION, t("موقعك لمواقيت دقيقة", "Your location for accurate times"), "")
    // Prominent disclosure (Google Play User Data policy): what, why, how it is used, before the system dialog.
    SCard {
        Row(verticalAlignment = Alignment.Top) {
            PackIcon(Ico.PRIVACY, 30.dp); Spacer(Modifier.width(12.dp))
            Column {
                Text(t("لماذا نطلب الموقع؟", "Why we ask for location"), style = MaterialTheme.typography.titleMedium)
                VSpace(4.dp)
                Text(
                    t("يستخدم سكينة موقعك التقريبي فقط لحساب مواقيت الصلاة واتجاه القبلة. يُستخدم على جهازك فقط، ولا يُرسل أو يُشارك مع أي جهة، ولا يُجمع في الخلفية.",
                        "Sakina uses your approximate location only to calculate prayer times and the Qibla direction. It is used on your device only, never sent or shared, and never collected in the background."),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    VSpace(14.dp)
    SCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PackIcon(Ico.MOSQUE, 28.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Muted(t("المدينة الحالية", "Current city"))
                Text(prefs.place.label(ar) + if (!prefs.placeSet) t(" (افتراضي)", " (default)") else "", style = MaterialTheme.typography.titleMedium)
            }
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalSakina.current.look.cta)
        }
    }
    VSpace(16.dp)
    CtaPill(t("استخدم موقعي التقريبي", "Use my approximate location"), Modifier.fillMaxWidth(), enabled = !busy) {
        if (LocationHelper.hasPermission(ctx)) locate() else perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    VSpace(10.dp)
    GhostPill(t("اختيار المدينة يدويًا", "Choose city manually"), Modifier.fillMaxWidth(), onClick = onChooseCity)
    err?.let { VSpace(8.dp); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ColumnScope.NotifStep() {
    val ctx = LocalContext.current
    val need = Build.VERSION.SDK_INT >= 33
    var granted by remember { mutableStateOf(!need || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    Hero(Ico.ADHAN, t("لا تفوتك صلاة", "Never miss a prayer"), t("فعّل الإشعارات ليصلك الأذان في وقته تمامًا، مع تذكير لطيف قبل الصلاة.", "Enable notifications so the adhan reaches you right on time, with a gentle reminder before."))
    SCard {
        listOf(
            Ico.BELL to t("أذان كامل أو إشعار صامت لكل صلاة على حدة", "Full adhan or silent alert per prayer"),
            Ico.CLOCK to t("تذكير قبل الأذان وتذكير صلاة الجمعة", "Pre-adhan and Jumu'ah reminders"),
            Ico.SPARKLES to t("لا رسائل مزعجة، أبدًا", "Never spammy"),
        ).forEach { (i, s) -> Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { PackIcon(i, 28.dp); Spacer(Modifier.width(12.dp)); Text(s, style = MaterialTheme.typography.bodyLarge) } }
    }
    VSpace(16.dp)
    if (granted) Row(verticalAlignment = Alignment.CenterVertically) {
        PackIcon(Ico.DONE, 26.dp); Spacer(Modifier.width(8.dp))
        Text(t("الإشعارات مفعّلة", "Notifications are on"), style = MaterialTheme.typography.titleMedium, color = LocalSakina.current.look.cta, fontWeight = FontWeight.Bold)
    } else CtaPill(t("تفعيل الإشعارات", "Enable notifications"), Modifier.fillMaxWidth()) { perm.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@Composable
private fun ColumnScope.ThemeStep(prefs: Prefs, repo: SettingsRepo) {
    val scope = rememberCoroutineScope()
    VSpace(18.dp)
    Text(t("اختر مظهرك", "Pick your look"), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    Muted(t("أحد عشر ثيمًا، ويمكنك التغيير لاحقًا.", "Eleven themes — change anytime."))
    VSpace(14.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ThemeMode.SYSTEM to t("تلقائي", "Auto"), ThemeMode.LIGHT to t("فاتح", "Light"), ThemeMode.DARK to t("داكن", "Dark")).forEach { (m, l) ->
            Chip(l, selected = prefs.theme == m, onClick = { scope.launch { repo.setTheme(m) } })
        }
    }
    VSpace(14.dp)
    val themes = listOf(AppTheme.CLASSIC_TEAL, AppTheme.LIQUID_GLASS, AppTheme.MADINAH_NIGHT, AppTheme.EMERALD_ANDALUS, AppTheme.DESERT_SAND, AppTheme.ROSE_DAWN)
    themes.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            row.forEach { th -> ThemePreview(prefs, th, prefs.appTheme == th, Modifier.weight(1f)) { scope.launch { repo.setAppTheme(th) } } }
        }
    }
}
