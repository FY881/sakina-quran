package app.sakina.quran.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.R
import app.sakina.quran.data.Prefs
import app.sakina.quran.prayer.Hijri
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.formatTime
import app.sakina.quran.ui.common.hms
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.prayerName
import app.sakina.quran.ui.common.rememberNow
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.prayer.TimesCard
import app.sakina.quran.ui.theme.DeepGreen
import app.sakina.quran.ui.theme.Gold
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(prefs: Prefs, onOpen: (String) -> Unit, onCycleMode: (Prayer) -> Unit, fixedNow: ZonedDateTime? = null) {
    val now = fixedNow ?: rememberNow()
    val place = prefs.place
    val next = PrayerEngine.next(place, now, prefs.calc)
    val localDate = now.withZoneSameInstant(place.zone).toLocalDate()
    val today = PrayerEngine.day(place, localDate, prefs.calc)
    val ar = isArabic()
    val greg = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale(ar)))
    val hijri = Hijri.format(now.toInstant().toEpochMilli(), ar, prefs.hijriAdj)
    val friday = next.at.dayOfWeek == DayOfWeek.FRIDAY && next.prayer == Prayer.DHUHR
    val left = Duration.between(now, next.at)
    val span = Duration.between(next.previous, next.at).seconds.coerceAtLeast(1)
    val progress = (1f - left.seconds.toFloat() / span).coerceIn(0f, 1f)

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF07322A), DeepGreen, Color(0xFF14806A))))
            ) {
                StarField(Modifier.matchParentSize())
                Column(Modifier.statusBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.greeting), color = Gold, style = MaterialTheme.typography.titleMedium)
                            Text(hijri, color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text(greg, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            Modifier.clip(CircleShape).background(Color.White.copy(alpha = .12f))
                                .clickable { onOpen("city") }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp), tint = Gold)
                            Spacer(Modifier.width(4.dp))
                            Text(place.label(ar), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SunArc(today.times, now, Modifier.fillMaxWidth().height(150.dp))
                    Column(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.next_prayer), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelLarge)
                        Text(
                            prayerName(next.prayer, friday), color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() }
                        )
                        Text(formatTime(next.at, ar), color = Gold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            hms(left, ar), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(t("متبقي على الأذان", "until the adhan"), color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        ProgressLine(progress, Modifier.fillMaxWidth(0.8f).height(6.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MiniStat(t("منتصف الليل", "Midnight"), formatTime(today.midnight, ar))
                        MiniStat(t("الثلث الأخير", "Last third"), formatTime(today.lastThird, ar))
                        MiniStat(t("الشروق", "Sunrise"), formatTime(today.times.getValue(Prayer.SUNRISE), ar))
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Quick(Icons.AutoMirrored.Outlined.MenuBook, t("المصحف", "Mushaf"), Modifier.weight(1f)) { onOpen("quran") }
                Quick(Icons.Outlined.Explore, t("القبلة", "Qibla"), Modifier.weight(1f)) { onOpen("qibla") }
                Quick(Icons.Outlined.Fingerprint, t("السبحة", "Tasbih"), Modifier.weight(1f)) { onOpen("azkar") }
                Quick(Icons.Outlined.Spa, t("الأذكار", "Azkar"), Modifier.weight(1f)) { onOpen("azkar") }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                TimesCard(today, prefs, next.prayer, ar, title = stringResource(R.string.today_times), onCycleMode = onCycleMode)
            }
        }
        item {
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NightsStay, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(t("آية اليوم", "Verse of the day"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = app.sakina.quran.ui.theme.QuranFont, fontSize = 24.sp, lineHeight = 44.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(t("الرعد ٢٨", "Ar-Ra'd 13:28"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .8f))
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.free_badge),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Composable
private fun StarField(modifier: Modifier) {
    val tw = rememberInfiniteTransition(label = "stars")
    val a by tw.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "a")
    val stars = remember8
    Canvas(modifier) {
        stars.forEachIndexed { i, (x, y, r) ->
            drawCircle(Color.White.copy(alpha = if (i % 2 == 0) a else 1.15f - a), r, Offset(x * size.width, y * size.height))
        }
    }
}

private val remember8: List<Triple<Float, Float, Float>> = run {
    val rnd = java.util.Random(7)
    List(42) { Triple(rnd.nextFloat(), rnd.nextFloat() * 0.55f, 0.8f + rnd.nextFloat() * 1.8f) }
}

/** The sun's path from sunrise to maghrib, with a marker for "now" and dots for each prayer. */
@Composable
private fun SunArc(times: Map<Prayer, ZonedDateTime>, now: ZonedDateTime, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val start = times.getValue(Prayer.SUNRISE).toEpochSecond().toFloat()
    val end = times.getValue(Prayer.MAGHRIB).toEpochSecond().toFloat()
    fun frac(t: ZonedDateTime) = ((t.toEpochSecond() - start) / (end - start)).coerceIn(-0.08f, 1.08f)
    val f = frac(now)
    val day = now.toEpochSecond() in start.toLong()..end.toLong()
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2; val cy = h * 0.95f; val r = minOf(w * 0.42f, h * 0.85f)
        fun pt(fr: Float): Offset {
            val ang = PI * (1 - fr)               // 0 → left(east in LTR), 1 → right
            val x = (cos(ang) * r).toFloat(); val y = (sin(ang) * r).toFloat()
            return Offset(cx + if (rtl) -x else x, cy - y)
        }
        drawLine(Color.White.copy(alpha = .25f), Offset(cx - r - 16, cy), Offset(cx + r + 16, cy), 2f)
        drawArc(
            Color.White.copy(alpha = .35f), 180f, 180f, false, Offset(cx - r, cy - r), Size(2 * r, 2 * r),
            style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)))
        )
        listOf(Prayer.DHUHR, Prayer.ASR).forEach { p -> drawCircle(Color.White.copy(alpha = .8f), 5f, pt(frac(times.getValue(p)))) }
        val sun = pt(f.coerceIn(0f, 1f))
        if (day) {
            drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .55f), Color.Transparent), sun, 46f), 46f, sun)
            drawCircle(Gold, 14f, sun)
        } else {
            val moon = Offset(cx, cy - r * 0.55f)
            drawCircle(Color(0xFFF4F1DE), 13f, moon)
            drawCircle(Color(0xFF0B4A3E), 11f, Offset(moon.x + (if (rtl) -6f else 6f), moon.y - 4f))
        }
    }
}

@Composable
private fun ProgressLine(p: Float, modifier: Modifier) {
    Box(modifier.clip(CircleShape).background(Color.White.copy(alpha = .18f))) {
        Box(Modifier.fillMaxWidth(p).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Gold, Color(0xFFF4D58D)))))
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Quick(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
