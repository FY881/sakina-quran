package app.sakina.quran.ui.tabs

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.CardShape
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.ListRow
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.surface
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont

@Composable
fun TabTitle(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

private data class Inspo(val kind: Ico, val ar: String, val en: String, val text: String, val refAr: String, val refEn: String, val quran: Boolean)

private val INSPO = listOf(
    Inspo(Ico.QURAN, "آية", "Ayah", "فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "الشرح ٥", "Ash-Sharh 94:5", true),
    Inspo(Ico.HADITH, "حديث", "Hadith", "«الكلمة الطيبة صدقة»", "متفق عليه", "Bukhari & Muslim", false),
    Inspo(Ico.DUA, "دعاء", "Dua", "«رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي»", "طه ٢٥–٢٦", "Taha 20:25–26", false),
    Inspo(Ico.TASBIH, "ذكر", "Dhikr", "«سبحان الله وبحمده، سبحان الله العظيم»", "متفق عليه", "Bukhari & Muslim", false),
)

@Composable
fun CommunityTab() {
    var cat by remember { mutableStateOf(0) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TabTitle(t("إلهام", "Inspiration")) }
        item {
            val cats = listOf(t("الكل", "All"), t("القرآن", "Quran"), t("حديث", "Hadith"), t("دعاء", "Dua"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(cats.size) { i -> Chip(cats[i], selected = cat == i, onClick = { cat = i }) } }
        }
        val shown = when (cat) { 1 -> INSPO.filter { it.quran }; 2 -> INSPO.filter { it.kind == Ico.HADITH }; 3 -> INSPO.filter { it.kind == Ico.DUA }; else -> INSPO }
        items(shown.size) { i ->
            val x = shown[i]
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(x.kind, 28.dp); Spacer(Modifier.width(10.dp))
                    Text(t(x.ar, x.en), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    PackIcon(Ico.SHARE, 22.dp)
                }
                VSpace(10.dp)
                Text(
                    x.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = if (x.quran) MaterialTheme.typography.titleLarge.copy(fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 48.sp)
                    else MaterialTheme.typography.titleMedium,
                )
                VSpace(6.dp)
                Muted(t(x.refAr, x.refEn), Modifier.fillMaxWidth())
            }
        }
        item { Muted(t("مجتمع سكينة قادم قريبًا. هذه بطاقات مختارة تعمل بدون إنترنت.", "The Sakina community is coming soon. These curated cards work offline."), Modifier.fillMaxWidth().height(48.dp)) }
    }
}
