package app.sakina.quran.ui.azkar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.azkar.Azkar
import app.sakina.quran.azkar.ISTIKHARA_CHAPTER
import app.sakina.quran.azkar.PROPHETIC_CHAPTERS
import app.sakina.quran.azkar.QURANIC_DUAS
import app.sakina.quran.azkar.RUQYAH_AYAT
import app.sakina.quran.azkar.RUQYAH_CHAPTERS
import app.sakina.quran.azkar.ShareImage
import app.sakina.quran.data.UserData
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont

// ─────────────────────────────────────────────── Duas: Quranic + Prophetic
@Composable
fun DuasScreen(user: UserData, initialTab: Int, onBack: () -> Unit, onOpen: (String) -> Unit, onFav: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val azkar by rememberAzkar()
    val quran by rememberQuran()
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val qd = remember(quran) { quran?.let { q -> QURANIC_DUAS.map { it to Azkar.ayatDhikr(q, it.sura, it.from, it.to) } } ?: emptyList() }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("الأدعية", "Duas"), onBack, subtitle = t("من القرآن الكريم والسنة النبوية الصحيحة", "From the Quran and the authentic Sunnah")) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("أدعية قرآنية", "Quranic duas") + " " + num(QURANIC_DUAS.size, ar), selected = tab == 0, leading = { PackIcon(Ico.QURAN, 18.dp) }, onClick = { tab = 0 })
                Chip(t("أدعية نبوية", "Prophetic duas"), selected = tab == 1, leading = { PackIcon(Ico.HADITH, 18.dp) }, onClick = { tab = 1 })
            }
        }
        if (tab == 0) {
            if (quran == null) item { Loading() }
            items(qd, key = { it.second.id }) { (meta, d) ->
                val fav = d.id in user.azkarFav
                SCard(padding = PaddingValues(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (ar) meta.topicAr else meta.topicEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = look.cta)
                            Muted(if (ar) d.refAr else quran?.let { enRef(it, meta.sura, meta.from, meta.to) } ?: d.refAr)
                        }
                        IconButton(onClick = { onFav(d.id) }) {
                            Icon(if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, t("مفضلة", "Favourite"), tint = if (fav) Color(0xFFEF4444) else look.onCardMuted)
                        }
                        IconButton(onClick = { ShareImage.share(ctx, if (ar) meta.topicAr else meta.topicEn, d.text, d.refAr, palette(look)) }) {
                            Icon(Icons.Outlined.Share, t("مشاركة كصورة", "Share as image"), tint = look.onCardMuted)
                        }
                    }
                    Text(d.text, fontFamily = QuranFont, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), fontSize = 21.sp, lineHeight = 42.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(end = 10.dp, top = 4.dp))
                    VSpace(6.dp)
                    Chip(t("افتح في المصحف", "Open in the mushaf"), leading = { PackIcon(Ico.QURAN, 18.dp) }, onClick = { onOpen("reader/${meta.sura}/${meta.from}") })
                }
            }
        } else {
            val a = azkar
            if (a == null) item { Loading() } else items(PROPHETIC_CHAPTERS.mapNotNull { a.chapter(it) }, key = { it.index }) { c ->
                SCard(onClick = { onOpen("azkar/h${c.index}") }, padding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = look.cta, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Muted(duasLabel(c.items.size, ar))
                    }
                    VSpace(4.dp)
                    Text(c.items.first().text, fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth())
                    ArabicText(c.items.first().refAr, maxLines = 2)
                }
            }
        }
        item { Muted(t("الآيات بنص مشروع تنزيل دون تعديل؛ الأدعية النبوية من حصن المسلم مع مصادرها.", "Ayat are Tanzil's text, unchanged; prophetic duas come from Hisn al-Muslim with their references."), Modifier.fillMaxWidth().padding(8.dp)) }
    }
}

// ─────────────────────────────────────────────── Ruqyah
@Composable
fun RuqyahScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val azkar by rememberAzkar()
    val quran by rememberQuran()
    var open by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("الرقية الشرعية", "Ruqyah"), onBack, subtitle = t("من كتاب الله وما صحّ عن النبي ﷺ", "From the Book of Allah and the authentic Sunnah")) }
        item {
            SCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("آداب الرقية", "Etiquette"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        VSpace(4.dp)
                        listOf(
                            t("ارقِ نفسك أو غيرك بيقين أن الشفاء من الله وحده.", "Recite for yourself or others, certain that healing is from Allah alone."),
                            t("اقرأ بتدبّر، ويُستحب النفث (نفخ خفيف مع ريق يسير) ووضع اليد على موضع الألم.", "Recite attentively; gently blowing and placing the hand on the place of pain is from the Sunnah."),
                            t("لا تتعارض الرقية مع التداوي؛ راجع الطبيب واستشر أهل العلم.", "Ruqyah does not replace medical care — see a doctor and ask people of knowledge."),
                        ).forEach { s -> Row(Modifier.padding(vertical = 2.dp)) { Text("•  ", color = look.cta); Text(s, style = MaterialTheme.typography.bodyMedium) } }
                    }
                }
                VSpace(10.dp)
                CtaPill(t("اقرأ الرقية كاملة بالعدّاد", "Read the full ruqyah with the counter"), Modifier.fillMaxWidth()) { onOpen("azkar/ruqyah") }
            }
        }
        item { SectionHeader(t("من القرآن الكريم", "From the Quran")) }
        val q = quran
        if (q == null) item { Loading() } else items(RUQYAH_AYAT, key = { "${it[0]}:${it[1]}" }) { r ->
            val key = "${r[0]}:${r[1]}"
            val d = remember(key) { Azkar.ayatDhikr(q, r[0], r[1], r[2], r[3]) }
            val expanded = open == key
            SCard(onClick = { open = if (expanded) "" else key }, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackIcon(Ico.QURAN, 26.dp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (ar) d.refAr else enRef(q, r[0], r[1], r[2]), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Muted(if (r[3] > 1) countLabel(r[3], ar) else t("اضغط لعرض النص", "Tap to show the text"))
                    }
                }
                if (expanded) {
                    VSpace(8.dp)
                    Text(d.text, fontFamily = QuranFont, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), fontSize = 21.sp, lineHeight = 42.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    VSpace(6.dp)
                    Chip(t("افتح في المصحف", "Open in the mushaf"), leading = { PackIcon(Ico.QURAN, 18.dp) }, onClick = { onOpen("reader/${r[0]}/${r[1]}") })
                }
            }
        }
        item { SectionHeader(t("من السنة النبوية", "From the Sunnah")) }
        val a = azkar
        if (a == null) item { Loading() } else items(RUQYAH_CHAPTERS.mapNotNull { a.chapter(it) }, key = { "c${it.index}" }) { c ->
            SCard(onClick = { onOpen("azkar/h${c.index}") }, padding = PaddingValues(14.dp)) {
                Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = look.cta)
                VSpace(4.dp)
                Text(c.items.first().text, fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth())
                ArabicText(c.items.first().refAr, maxLines = 2)
            }
        }
        item { Muted(t("التلاوة الصوتية للرقية ستُضاف من مصدر مرخّص في تحديث قادم.", "Recited audio for the ruqyah will be added from a licensed source in a later update."), Modifier.fillMaxWidth().padding(8.dp)) }
    }
}

// ─────────────────────────────────────────────── Istikhara
@Composable
fun IstikharaScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val ctx = LocalContext.current
    val azkar by rememberAzkar()
    val steps = listOf(
        t("أخلص النية، واعلم أن الاستخارة تكون في الأمور المباحة التي تتردد فيها، لا في واجب أو محرم.", "Make your intention sincere. Istikhara is for permissible matters you are unsure about, not for an obligation or a sin."),
        t("صلِّ ركعتين من غير الفريضة، في غير أوقات النهي.", "Pray two rak'ahs other than an obligatory prayer, outside the disliked times."),
        t("بعد الصلاة ادعُ بدعاء الاستخارة، وسمِّ حاجتك عند قولك «هذا الأمر».", "After the prayer, say the Istikhara dua and name your matter where it says “this matter”."),
        t("امضِ فيما ييسّره الله لك واطمئن؛ لا يُشترط رؤيا في المنام، ويحسن أن تستشير أهل الخبرة.", "Proceed with what Allah makes easy and be at peace; no dream is required. It is good to also consult people of experience."),
    )
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader(t("صلاة الاستخارة", "Istikhara prayer"), onBack, subtitle = t("اطلب من الله الخيرة في أمرك ثم امضِ مطمئنًا", "Ask Allah to choose what is best, then proceed at peace")) }
        items(steps.size) { i ->
            SCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(look.cta), contentAlignment = Alignment.Center) {
                        Text(num(i + 1, ar), color = look.onCta, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(steps[i], style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        val d = azkar?.chapter(ISTIKHARA_CHAPTER)?.items?.firstOrNull()
        item {
            if (d == null) Loading() else SCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("دعاء الاستخارة", "The Istikhara dua"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { ShareImage.share(ctx, if (ar) "دعاء الاستخارة" else "Istikhara dua", d.text, d.refAr, palette(look)) }) {
                        Icon(Icons.Outlined.Share, t("مشاركة كصورة", "Share as image"), tint = look.onCardMuted)
                    }
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(look.cta.copy(alpha = .08f)).padding(12.dp)) {
                    Text(d.text, fontFamily = QuranFont, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), fontSize = 20.sp, lineHeight = 40.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                VSpace(6.dp)
                Text(t("المصدر", "Source"), style = MaterialTheme.typography.labelMedium, color = look.cta)
                ArabicText(d.refAr)
                VSpace(10.dp)
                CtaPill(t("افتح الدعاء بالعدّاد", "Open the dua with the counter"), Modifier.fillMaxWidth()) { onOpen("azkar/h$ISTIKHARA_CHAPTER") }
            }
        }
        item { Muted(t("الخطوات بصياغة مختصرة للتيسير؛ للتفاصيل الفقهية استشر أهل العلم.", "Steps are a short summary for ease; for fiqh details, consult a scholar."), Modifier.fillMaxWidth().padding(8.dp)) }
    }
}
