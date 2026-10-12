package app.sakina.quran.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.UserData
import app.sakina.quran.prayer.Hijri
import app.sakina.quran.prayer.NextPrayer
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.countdown
import app.sakina.quran.ui.common.hms
import app.sakina.quran.ui.common.modeLabel
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.formatTime
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.openSafely
import app.sakina.quran.ui.common.prayerName
import app.sakina.quran.ui.common.rememberHealth
import app.sakina.quran.ui.common.rememberNow
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.icons.ico
import app.sakina.quran.ui.kit.CardShape
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.ListRow
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SBox
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.tabs.feature
import app.sakina.quran.ui.tabs.rememberFeatureOpener
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** The five obligatory prayers in order. */
val FARD = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

/** The prayer whose time we are in now (the one before [next]). */
fun NextPrayer.currentPrayer(): Prayer = FARD[(FARD.indexOf(prayer) - 1 + FARD.size) % FARD.size]

private data class Ayah(val text: String, val refAr: String, val refEn: String, val en: String)

/** Rotating "ayah of the day" (Uthmani text, Tanzil; English is Sakina's own short rendering). */
private val AYAT = listOf(
    Ayah("أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ", "الرعد ٢٨", "Ar-Ra'd 13:28", "Surely, in the remembrance of Allah hearts find rest."),
    Ayah("فَٱذْكُرُونِىٓ أَذْكُرْكُمْ وَٱشْكُرُوا۟ لِى وَلَا تَكْفُرُونِ", "البقرة ١٥٢", "Al-Baqarah 2:152", "So remember Me; I will remember you. Be grateful to Me and do not deny Me."),
    Ayah("إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "الشرح ٦", "Ash-Sharh 94:6", "Indeed, with hardship comes ease."),
    Ayah("وَقَالَ رَبُّكُمُ ٱدْعُونِىٓ أَسْتَجِبْ لَكُمْ", "غافر ٦٠", "Ghafir 40:60", "Your Lord says: call upon Me, I will answer you."),
    Ayah("وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥٓ", "الطلاق ٣", "At-Talaq 65:3", "Whoever puts his trust in Allah, He is enough for him."),
)

private data class Story(val key: String, val ico: Ico, val ar: String, val en: String)

private val STORIES = listOf(
    Story("ayah", Ico.QURAN, "آية", "Ayah"), Story("hadith", Ico.HADITH, "حديث", "Hadith"),
    Story("dua", Ico.DUA, "دعاء", "Dua"), Story("dhikr", Ico.TASBIH, "ذكر", "Dhikr"),
    Story("names", Ico.NAMES, "الأسماء", "Names"), Story("idea", Ico.IDEA, "تأمّل", "Reflect"),
)

@Composable
fun HomeScreen(
    prefs: Prefs, user: UserData, onOpen: (String) -> Unit,
    onDismissHealth: (String) -> Unit, onSeenStory: (String) -> Unit, onSetFeatures: (List<String>) -> Unit = {}, fixedNow: ZonedDateTime? = null,
) {
    var editFeatures by rememberSaveable { mutableStateOf(false) }
    if (editFeatures) FeatureEditorSheet(user.features, onDismiss = { editFeatures = false }, onSave = onSetFeatures)
    val now = fixedNow ?: rememberNow()
    val ctx = LocalContext.current
    val look = LocalSakina.current.look
    val place = prefs.place
    val ar = isArabic()
    val next = PrayerEngine.next(place, now, prefs.calc)
    val cur = next.currentPrayer()
    val localDate = now.withZoneSameInstant(place.zone).toLocalDate()
    val todayKey = localDate.toString()
    val millis = now.toInstant().toEpochMilli()
    val hijri = digits(Hijri.dayMonth(millis, ar, prefs.hijriAdj), ar)
    val greg = digits(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale(ar))), ar)
    val friday = next.at.dayOfWeek == DayOfWeek.FRIDAY && next.prayer == Prayer.DHUHR
    val issues = rememberHealth()
    val openFeature = rememberFeatureOpener(onOpen)
    val ayah = AYAT[(localDate.toEpochDay() % AYAT.size).toInt()]

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // ── Top bar: profile · Hijri date · bell · stars
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).surface(CircleShape).press { onOpen("profile") }, contentAlignment = Alignment.Center) {
                    val initial = user.name.trim().firstOrNull()?.uppercase()
                    if (initial != null) Text(initial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) else PackIcon(Ico.PROFILE, 28.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(hijri, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    Muted(greg)
                }
                Box(Modifier.size(42.dp).surface(CircleShape).semantics { contentDescription = "notifications" }, contentAlignment = Alignment.Center) {
                    PackIcon(Ico.BELL, 24.dp)
                    if (issues.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(9.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                }
                Spacer(Modifier.width(8.dp))
                Row(Modifier.height(42.dp).surface(CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.STAR, 20.dp); Spacer(Modifier.width(6.dp))
                    Text(num(user.totalPrayed, ar), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        // ── Adhan reliability banner (dismissable for today)
        if (issues.isNotEmpty() && user.healthDismissedDay != todayKey) item {
            val first = issues.first()
            val fg = if (look.dark) Color.White else Color(0xFF7A1F2B)
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(CardShape).background(look.warn).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PackIcon(Ico.ADHAN, 32.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("فعّل تنبيهات الأذان المضمونة", "Make adhan alerts reliable"), color = fg, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(t(first.ar, first.en), color = fg.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    t("إصلاح", "Fix"), color = look.onCta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(look.cta).press { ctx.openSafely(first.fix) }.padding(horizontal = 14.dp, vertical = 7.dp),
                )
                IconButton(onClick = { onDismissHealth(todayKey) }) { Icon(Icons.Outlined.Close, t("إخفاء", "Dismiss"), tint = fg) }
            }
        }
        // ── Stories
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(STORIES) { s ->
                    val seen = "$todayKey:${s.key}" in user.storiesSeen
                    val ring = if (seen) Brush.linearGradient(listOf(look.onCardMuted.copy(alpha = .35f), look.onCardMuted.copy(alpha = .35f)))
                    else Brush.sweepGradient(listOf(look.cta, MaterialTheme.colorScheme.secondary, look.cta))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                        Box(
                            Modifier.size(66.dp).clip(CircleShape).border(2.5.dp, ring, CircleShape).padding(5.dp)
                                .surface(CircleShape, elevated = false).press { onSeenStory("$todayKey:${s.key}"); onOpen("community") },
                            contentAlignment = Alignment.Center,
                        ) { PackIcon(s.ico, 34.dp) }
                        VSpace(4.dp)
                        Text(t(s.ar, s.en), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    }
                }
            }
        }
        // ── Now / Next prayer cards
        item {
            Row(Modifier.padding(horizontal = 16.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val curAt = PrayerEngine.day(place, next.previous.toLocalDate(), prefs.calc).times[cur] ?: next.previous
                val total = Duration.between(curAt, next.at).seconds.coerceAtLeast(1)
                val elapsed = Duration.between(curAt, now).seconds.coerceIn(0, total)
                PrayerMiniCard(
                    Modifier.weight(1f).fillMaxHeight(), label = t("الآن", "Now"), live = true,
                    name = prayerName(cur), time = formatTime(curAt, ar), ico = cur.ico(), extra = null,
                    progress = elapsed.toFloat() / total,
                    footer = t("الأذان التالي بعد ", "Next adhan in ") + hms(Duration.between(now, next.at), ar),
                ) { onOpen("prayers") }
                PrayerMiniCard(
                    Modifier.weight(1f).fillMaxHeight(), label = t("التالي", "Next"), live = false,
                    name = prayerName(next.prayer, friday), time = formatTime(next.at, ar), ico = next.prayer.ico(),
                    extra = t("بعد ", "in ") + countdown(Duration.between(now, next.at), ar),
                    footer = modeLabel(prefs.modes[next.prayer] ?: app.sakina.quran.data.AlertMode.ADHAN),
                ) { onOpen("prayers") }
            }
        }
        // ── Daily ayah
        item {
            SCard(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.QURAN, 28.dp); Spacer(Modifier.width(10.dp))
                    Text(t("آية اليوم", "Ayah of the day"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Box(Modifier.size(36.dp).clip(CircleShape).press {
                        val body = "${ayah.text}\n(${if (ar) ayah.refAr else ayah.refEn})\n— Sakina"
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }, contentAlignment = Alignment.Center) { PackIcon(Ico.SHARE, 22.dp, contentDescription = t("مشاركة", "Share")) }
                }
                VSpace(10.dp)
                Text(
                    ayah.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = QuranFont, fontSize = 27.sp, lineHeight = 50.sp),
                )
                if (!ar) { VSpace(4.dp); Text(ayah.en, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium) }
                VSpace(6.dp)
                Muted(if (ar) ayah.refAr else ayah.refEn, Modifier.fillMaxWidth())
            }
        }
        // ── Features (user-editable order)
        item {
            SectionHeader(t("المميزات", "Features"), Modifier.padding(horizontal = 12.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).press { editFeatures = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, t("تعديل", "Edit"), tint = look.onCardMuted, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Chip(t("المزيد", "More"), onClick = { onOpen("explore") })
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(user.features.mapNotNull { feature(it) }) { f ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                        SBox(Modifier.size(70.dp), onClick = { openFeature(f) }) { PackIcon(f.ico, 42.dp, Modifier.align(Alignment.Center)) }
                        VSpace(6.dp)
                        Text(t(f.ar, f.en), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // ── For you
        item { SectionHeader(t("لك", "For you"), Modifier.padding(horizontal = 12.dp)) }
        item {
            SCard(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(6.dp)) {
                val prayedToday = user.prayedOn(localDate)
                ListRow(Ico.TRACKER, t("متتبع الصلاة", "Prayer tracker"), digits(t("صلّيت $prayedToday من ٥ اليوم", "$prayedToday of 5 prayed today"), ar), onClick = { onOpen("prayers") })
                ListRow(Ico.AZKAR, t(if (now.hour < 12) "أذكار الصباح" else "أذكار المساء", if (now.hour < 12) "Morning azkar" else "Evening azkar"), t("دقائق تطمئن بها", "A few calm minutes"), onClick = { onOpen(if (now.hour < 12) "azkar/morning" else "azkar/evening") })
                ListRow(Ico.KHATMA, t("خطة ختمة", "Khatma plan"), t("اقرأ وردك اليومي", "Read your daily portion"), onClick = { onOpen("quran") })
            }
        }
        item {
            Muted(t("مجاني للأبد • بلا إعلانات • يعمل دون إنترنت", "Free forever • No ads • Works offline"), Modifier.fillMaxWidth().padding(8.dp))
        }
    }
}

@Composable
private fun PrayerMiniCard(
    modifier: Modifier, label: String, live: Boolean, name: String, time: String, ico: Ico, extra: String?,
    progress: Float? = null, footer: String? = null, onClick: () -> Unit,
) {
    val look = LocalSakina.current.look
    SCard(modifier, onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (live) { Box(Modifier.size(7.dp).clip(CircleShape).background(look.cta)); Spacer(Modifier.width(6.dp)) }
                    Muted(label)
                }
                VSpace(2.dp)
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(time, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
            PackIcon(ico, 40.dp)
        }
        if (extra != null) { VSpace(4.dp); Text(extra, style = MaterialTheme.typography.labelLarge, color = look.cta, maxLines = 1) }
        if (progress != null) {
            VSpace(8.dp)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .2f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(.02f, 1f)).height(6.dp).clip(CircleShape).background(look.cta))
            }
        }
        if (footer != null) { Spacer(Modifier.weight(1f)); VSpace(4.dp); Text(footer, style = MaterialTheme.typography.labelMedium, color = look.onCardMuted, maxLines = 1) }
    }
}
