package app.sakina.quran.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SBox
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.LocalSakina

/** The feature whose "coming in the next update" sheet is open (null = closed). Provided by AppRoot. */
val LocalSoon = compositionLocalOf<MutableState<Feature?>> { mutableStateOf(null) }

/** Opens a built feature's route, otherwise the "coming soon" sheet. Never a dead tap. */
@Composable
fun rememberFeatureOpener(onOpen: (String) -> Unit): (Feature) -> Unit {
    val soon = LocalSoon.current
    return remember(onOpen, soon) { { f -> if (f.route != null) onOpen(f.route) else soon.value = f } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonSheet(state: MutableState<Feature?>, onOpen: (String) -> Unit) {
    val f = state.value ?: return
    val look = LocalSakina.current.look
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { state.value = null }, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).surface(CircleShape), contentAlignment = Alignment.Center) { PackIcon(f.ico, 72.dp) }
            VSpace(16.dp)
            Text(t(f.ar, f.en), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            VSpace(8.dp)
            Row(
                Modifier.clip(CircleShape).background(look.cta.copy(alpha = .18f)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PackIcon(Ico.SPARKLES, 18.dp); Spacer(Modifier.width(6.dp))
                Text(t("قريبًا في التحديث القادم", "Coming in the next update"), color = look.cta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            VSpace(14.dp)
            Text(t(f.descAr, f.descEn), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = look.onCard)
            VSpace(8.dp)
            Muted(t("نعمل عليها بعناية لتكون من أفضل ما في سكينة. مجانًا وبلا إعلانات، كالعادة.", "We are crafting it carefully to be among Sakina's best. Free and ad-free, as always."), Modifier.fillMaxWidth())
            VSpace(20.dp)
            val related = FEATURES.filter { it.section == f.section && it.route != null && it.id != f.id }.take(3)
            if (related.isNotEmpty()) {
                Muted(t("جرّب الآن", "Try now"))
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    related.forEach { r -> Chip(t(r.ar, r.en), leading = { PackIcon(r.ico, 18.dp) }, onClick = { state.value = null; onOpen(r.route!!) }) }
                }
                VSpace(16.dp)
            }
            CtaPill(t("حسنًا", "Got it"), Modifier.fillMaxWidth()) { state.value = null }
        }
    }
}

private data class Suggestion(val ar: String, val en: String, val featureId: String)

private val SUGGESTIONS = listOf(
    Suggestion("أين القبلة؟ 🧭", "Where is the qibla? 🧭", "qibla"),
    Suggestion("أكمل قراءة القرآن 📖", "Continue the Quran 📖", "quran"),
    Suggestion("سبّح ٣٣ مرة 📿", "Tasbih 33 times 📿", "tasbih"),
    Suggestion("ما معنى اللطيف؟ ✨", "What does Al-Latif mean? ✨", "names"),
    Suggestion("كيف أصلي الاستخارة؟ 🤲", "How to pray Istikhara? 🤲", "istikhara"),
    Suggestion("كم زكاة مالي؟ 💰", "How much zakat do I owe? 💰", "zakat"),
)

private val RECOMMENDED = listOf("quran", "tasbih", "names", "qibla", "tracker", "themes")

@Composable
fun FeatureGrid(items: List<Feature>, open: (Feature) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { f -> FeatureTile(f, Modifier.weight(1f)) { open(f) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureTile(f: Feature, modifier: Modifier, onClick: () -> Unit) {
    val look = LocalSakina.current.look
    SCard(modifier.height(118.dp), onClick = onClick, padding = PaddingValues(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(t(f.ar, f.en), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (f.route == null) Box(Modifier.padding(top = 4.dp).size(7.dp).clip(CircleShape).background(look.onCardMuted.copy(alpha = .5f)))
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            if (f.route == null) Text(t("قريبًا", "Soon"), style = MaterialTheme.typography.labelSmall, color = look.onCardMuted, modifier = Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
            PackIcon(f.ico, 56.dp)
        }
    }
}

@Composable
fun ExploreTab(onOpen: (String) -> Unit, initialQuery: String = "") {
    val open = rememberFeatureOpener(onOpen)
    val look = LocalSakina.current.look
    val ar = isArabic()
    var q by rememberSaveable { mutableStateOf(initialQuery) }
    val results = remember(q) { FEATURES.filter { it.matches(q) } }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TabTitle(t("استكشف", "Explore")) {
                Box(Modifier.size(42.dp).surface(CircleShape).press { onOpen("more") }, contentAlignment = Alignment.Center) { PackIcon(Ico.SETTINGS, 26.dp) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().surface(CircleShape, elevated = false).padding(start = 16.dp, end = 8.dp).height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = look.onCardMuted)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Muted(t("ابحث عن أي شيء… قبلة، زكاة، سبحة", "Search anything… qibla, zakat, tasbih"))
                    BasicTextField(
                        q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.onCard),
                        cursorBrush = SolidColor(look.cta),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                }
                if (q.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).press { q = "" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, t("مسح", "Clear"), tint = look.onCardMuted)
                }
            }
        }
        if (q.isNotBlank()) {
            item {
                SectionHeader(t("النتائج", "Results")) { Muted(num(results.size, ar)) }
            }
            if (results.isEmpty()) item {
                SCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PackIcon(Ico.HELP, 56.dp); VSpace(8.dp)
                        Text(t("لا نتائج لـ «$q»", "No results for “$q”"), style = MaterialTheme.typography.titleMedium)
                        Muted(t("جرّب كلمة أخرى مثل: صلاة، قرآن، دعاء", "Try another word like: prayer, Quran, dua"))
                        VSpace(10.dp)
                        GhostPill(t("مسح البحث", "Clear search")) { q = "" }
                    }
                }
            } else item { FeatureGrid(results, open) }
        } else {
            item {
                Column {
                    Text(t("مقترحات", "Suggested"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SUGGESTIONS) { s -> Chip(t(s.ar, s.en), onClick = { feature(s.featureId)?.let(open) }) }
                    }
                }
            }
            item {
                Column {
                    Text(t("موصى به لك", "Recommended"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(RECOMMENDED.mapNotNull { feature(it) }) { f ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(78.dp)) {
                                SBox(Modifier.size(72.dp), onClick = { open(f) }, shape = RoundedCornerShape(24.dp)) { PackIcon(f.ico, 44.dp, Modifier.align(Alignment.Center)) }
                                VSpace(6.dp)
                                Text(t(f.ar, f.en), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Section.entries.forEach { sec ->
                val list = FEATURES.filter { it.section == sec && it.id != "quran_search" }
                item(key = sec.name + "_h") { SectionHeader(t(sec.ar, sec.en)) { Muted(num(list.size, ar)) } }
                item(key = sec.name) { FeatureGrid(list, open) }
            }
            item {
                Muted(
                    t("${num(FEATURES.count { it.route != null }, true)} ميزة جاهزة الآن و${num(FEATURES.count { it.route == null }, true)} قادمة قريبًا",
                        "${FEATURES.count { it.route != null }} features ready now · ${FEATURES.count { it.route == null }} coming soon"),
                    Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }
}
