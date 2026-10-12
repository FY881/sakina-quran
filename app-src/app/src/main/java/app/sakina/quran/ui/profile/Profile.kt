package app.sakina.quran.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import app.sakina.quran.BuildConfig
import app.sakina.quran.data.AppTheme
import app.sakina.quran.data.GlassLevel
import app.sakina.quran.data.IconPackId
import app.sakina.quran.data.Prefs
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.data.ThemeMode
import app.sakina.quran.data.UserData
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.dhikr.Stat
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.ListRow
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.Look
import app.sakina.quran.ui.theme.resolveLook
import app.sakina.quran.ui.theme.resolve
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(user: UserData, prefs: Prefs, onBack: () -> Unit, onOpen: (String) -> Unit, onName: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    var editing by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val since = if (user.memberSince > 0) Instant.ofEpochMilli(user.memberSince).atZone(ZoneId.systemDefault()).toLocalDate() else today
    val sinceTxt = digits(since.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale(ar))), ar)
    val display = user.name.ifBlank { t("ضيف سكينة", "Sakina guest") }
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader(t("ملفي", "Profile"), onBack) }
        // Gradient header
        item {
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(look.cta.copy(alpha = .85f), MaterialTheme.colorScheme.secondary.copy(alpha = .75f), look.card)))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(78.dp).clip(CircleShape).background(Color.White.copy(alpha = .22f)).border(2.dp, Color.White.copy(alpha = .7f), CircleShape), contentAlignment = Alignment.Center) {
                        val initial = user.name.trim().firstOrNull()?.uppercase()
                        if (initial != null) Text(initial, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White) else PackIcon(Ico.PROFILE, 48.dp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(display, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t("عضو منذ $sinceTxt", "Member since $sinceTxt"), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
                        VSpace(8.dp)
                        Row(
                            Modifier.clip(CircleShape).background(Color.White.copy(alpha = .2f)).press { editing = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                            Text(t("تعديل الاسم", "Edit name"), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        // Journey stats
        item { SectionHeader(t("رحلتي", "My journey"), Modifier.padding(horizontal = 12.dp)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(Modifier.weight(1f), Ico.FIRE, num(user.streak(today), ar), t("سلسلة الأيام", "Day streak"))
                    Stat(Modifier.weight(1f), Ico.MOSQUE, num(user.totalPrayed, ar), t("صلوات مسجّلة", "Prayers logged"))
                    Stat(Modifier.weight(1f), Ico.DONE, num(user.fullDays, ar), t("أيام كاملة", "Full days"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(Modifier.weight(1f), Ico.TASBIH, num(user.tasbihTotal, ar), t("تسبيحات", "Tasbih"))
                    Stat(Modifier.weight(1f), Ico.BOOKMARK, num(user.quranBookmarks.size, ar), t("علامات", "Bookmarks"))
                    Stat(Modifier.weight(1f), Ico.NAMES, num(user.namesFav.size, ar), t("أسماء مفضلة", "Fav. names"))
                }
            }
        }
        // Daily goals
        item {
            SCard(Modifier.padding(horizontal = 16.dp)) {
                Text(t("أهداف اليوم", "Today's goals"), style = MaterialTheme.typography.titleMedium)
                VSpace(8.dp)
                Goal(Ico.MOSQUE, t("الصلوات الخمس", "Five prayers"), user.prayedOn(today), 5)
                Goal(Ico.TASBIH, t("١٠٠ تسبيحة", "100 tasbih"), (user.tasbihOn(today) + user.tasbihCount).coerceAtMost(100), 100)
                Goal(Ico.QURAN, t("ورد القرآن", "Quran portion"), if (user.lastSura > 0) 1 else 0, 1)
            }
        }
        item { SectionHeader(t("التخصيص", "Personalise"), Modifier.padding(horizontal = 12.dp)) }
        item {
            SCard(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(6.dp)) {
                ListRow(Ico.THEME, t("الثيم والمظهر", "Theme & appearance"), t(prefs.appTheme.ar, prefs.appTheme.en), onClick = { onOpen("themes") })
                ListRow(Ico.ICONPACK, t("حزمة الأيقونات", "Icon pack"), t(prefs.iconPack.ar, prefs.iconPack.en), onClick = { onOpen("iconpacks") })
                ListRow(Ico.ADHAN, t("إعدادات الصلاة والأذان", "Prayer & adhan settings"), prefs.calc.method.short(ar), onClick = { onOpen("settings") })
                ListRow(Ico.LOCATION, t("المدينة", "City"), prefs.place.label(ar), onClick = { onOpen("city") })
                ListRow(Ico.LANGUAGE, t("اللغة", "Language"), if (ar) "العربية" else "English", trailing = {
                    Chip(if (ar) "English" else "العربية", onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (ar) "en" else "ar")) })
                })
                ListRow(Ico.SETTINGS, t("الإعدادات العامة", "General settings"), t("عن التطبيق والمصادر", "About & sources"), onClick = { onOpen("more") })
            }
        }
        item {
            Muted(t("سكينة ${BuildConfig.VERSION_NAME} • مجاني للأبد • بياناتك على جهازك", "Sakina ${BuildConfig.VERSION_NAME} • Free forever • Your data stays on your phone"), Modifier.fillMaxWidth().padding(8.dp))
        }
    }
    if (editing) {
        var v by remember { mutableStateOf(user.name) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(t("اسمك", "Your name")) },
            text = { OutlinedTextField(v, { v = it.take(40) }, singleLine = true, placeholder = { Text(t("اكتب اسمك", "Type your name")) }) },
            confirmButton = { TextButton(onClick = { onName(v); editing = false }) { Text(t("حفظ", "Save")) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(t("إلغاء", "Cancel")) } },
        )
    }
}

@Composable
private fun Goal(ico: Ico, label: String, done: Int, of: Int) {
    val look = LocalSakina.current.look
    val ar = isArabic()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PackIcon(ico, 26.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row { Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Muted("${num(done, ar)}/${num(of, ar)}") }
            VSpace(4.dp)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .2f))) {
                Box(Modifier.fillMaxWidth((done / of.toFloat()).coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(look.cta))
            }
        }
    }
}

/** A miniature phone rendering of [theme] using its real colours. */
@Composable
fun ThemePreview(prefs: Prefs, theme: AppTheme, selected: Boolean, modifier: Modifier = Modifier, mode: ThemeMode = prefs.theme, onClick: () -> Unit) {
    val l: Look = resolveLook(prefs, theme, mode)
    val cur = LocalSakina.current.look
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(.56f).clip(RoundedCornerShape(18.dp))
                .border(if (selected) 3.dp else 1.dp, if (selected) cur.cta else cur.cardBorder, RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(l.bgTop, l.bgBottom))).press(onClick = onClick).padding(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(l.onCard.copy(alpha = .8f)))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(2) { Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(l.card).border(1.dp, l.cardBorder, RoundedCornerShape(8.dp))) }
                }
                Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp)).background(l.card).border(1.dp, l.cardBorder, RoundedCornerShape(8.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(l.card)) } }
                Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(l.cta))
            }
            if (selected) Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(cur.cta), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = cur.onCta, modifier = Modifier.size(16.dp))
            }
        }
        VSpace(6.dp)
        Text(t(theme.ar, theme.en), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun ThemePickerScreen(prefs: Prefs, repo: SettingsRepo, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader(t("الثيم والمظهر", "Theme & appearance"), onBack, subtitle = t("المعاينة حيّة بألوان كل ثيم", "Live previews in each theme's colours")) }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(t("الوضع", "Mode"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to t("تلقائي", "Auto"), ThemeMode.LIGHT to t("فاتح", "Light"), ThemeMode.DARK to t("داكن", "Dark")).forEach { (m, l) ->
                        Chip(l, selected = prefs.theme == m, onClick = { scope.launch { repo.setTheme(m) } })
                    }
                }
                VSpace(12.dp)
                Text(t("الزجاج", "Glass"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassLevel.entries.forEach { g -> Chip(t(g.ar, g.en), selected = prefs.glass == g, onClick = { scope.launch { repo.setGlass(g) } }) }
                }
            }
        }
        item { SectionHeader(t("الثيمات", "Themes"), Modifier.padding(horizontal = 12.dp)) }
        AppTheme.entries.chunked(3).forEach { row ->
            item(key = row.first().name) {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { th -> ThemePreview(prefs, th, prefs.appTheme == th, Modifier.weight(1f)) { scope.launch { repo.setAppTheme(th) } } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Muted(t("بعض الثيمات داكنة فقط. «موسمي» يتغير تلقائيًا مع رمضان والأعياد.", "Some themes are dark-only. “Seasonal” changes automatically for Ramadan and Eid."), Modifier.fillMaxWidth().padding(horizontal = 20.dp)) }
    }
}

@Composable
fun IconPackPickerScreen(prefs: Prefs, repo: SettingsRepo, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val look = LocalSakina.current.look
    val sample = listOf(Ico.MOSQUE, Ico.QURAN, Ico.TASBIH, Ico.QIBLA, Ico.DUA, Ico.CALENDAR, Ico.ZAKAT, Ico.NAMES)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("حزمة الأيقونات", "Icon pack"), onBack, subtitle = t("كل الأيقونات في التطبيق تتغير فورًا", "Every icon in the app changes instantly")) }
        IconPackId.entries.forEach { p ->
            item(key = p.name) {
                val sel = prefs.iconPack == p
                val shown = p.resolve(look)
                SCard(
                    Modifier.then(if (sel) Modifier.border(2.dp, look.cta, RoundedCornerShape(22.dp)) else Modifier),
                    onClick = { scope.launch { repo.setIconPack(p) } },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t(p.ar, p.en), style = MaterialTheme.typography.titleMedium)
                            if (p == IconPackId.AUTO) Muted(t("الحالي: ${shown.ar}", "Now: ${shown.en}"))
                        }
                        if (sel) Box(Modifier.size(26.dp).clip(CircleShape).background(look.cta), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = look.onCta, modifier = Modifier.size(18.dp)) }
                    }
                    VSpace(10.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        sample.forEach { PackIcon(it, 34.dp, pack = shown) }
                    }
                }
            }
        }
        item { Muted(t("الأيقونات: Fluent Emoji من مايكروسوفت (MIT) وNoto Emoji من جوجل (Apache 2.0).", "Icons: Microsoft Fluent Emoji (MIT) and Google Noto Emoji (Apache 2.0)."), Modifier.fillMaxWidth().padding(8.dp)) }
    }
}
