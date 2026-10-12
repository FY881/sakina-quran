package app.sakina.quran.ui.dhikr

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sakina.quran.data.UserData
import app.sakina.quran.ui.common.digits
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.locale
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.Ico
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.BackHeader
import app.sakina.quran.ui.kit.Chip
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.SCard
import app.sakina.quran.ui.kit.SectionHeader
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.kit.press
import app.sakina.quran.ui.theme.LocalSakina
import app.sakina.quran.ui.theme.QuranFont
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NamesQuizScreen(user: UserData, onBack: () -> Unit, onResult: (Int, Int) -> Unit, onNames: () -> Unit) {
    val ar = isArabic()
    val look = LocalSakina.current.look
    val view = LocalView.current
    var mode by rememberSaveable { mutableIntStateOf(2) }
    var seed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var started by rememberSaveable { mutableStateOf(false) }
    val quiz = remember(seed, mode) { makeNamesQuiz(seed, 10, mode) }
    var idx by rememberSaveable(seed, mode) { mutableIntStateOf(0) }
    var score by rememberSaveable(seed, mode) { mutableIntStateOf(0) }
    var chosen by rememberSaveable(seed, mode, idx) { mutableIntStateOf(-1) }
    var saved by rememberSaveable(seed, mode) { mutableStateOf(false) }
    val finished = started && idx >= quiz.size
    LaunchedEffect(chosen) {
        if (chosen >= 0) { delay(950); idx++ }
    }
    LaunchedEffect(finished) { if (finished && !saved) { saved = true; onResult(score, quiz.size) } }
    val best = user.namesQuiz.maxOfOrNull { it.score * 100 / it.total.coerceAtLeast(1) } ?: 0

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(t("اختبار الأسماء الحسنى", "99 Names quiz"), onBack, subtitle = digits(t("أفضل نتيجة: $best٪ • ${user.namesQuiz.size} محاولة", "Best: $best% • ${user.namesQuiz.size} attempts"), ar)) }
        if (!started) {
            item {
                SCard(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t("عشرة أسئلة", "Ten questions"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Muted(t("اختر المعنى الصحيح للاسم، أو الاسم الصحيح للمعنى.", "Pick the right meaning for a Name, or the right Name for a meaning."))
                        }
                        PackIcon(Ico.TROPHY, 64.dp)
                    }
                    VSpace(12.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(t("مختلط", "Mixed"), selected = mode == 2, onClick = { mode = 2 })
                        Chip(t("الاسم ← المعنى", "Name → meaning"), selected = mode == 0, onClick = { mode = 0 })
                        Chip(t("المعنى ← الاسم", "Meaning → name"), selected = mode == 1, onClick = { mode = 1 })
                    }
                    VSpace(14.dp)
                    CtaPill(t("ابدأ الاختبار", "Start the quiz"), Modifier.fillMaxWidth()) { seed = System.currentTimeMillis(); started = true }
                    VSpace(8.dp)
                    GhostPill(t("راجع الأسماء أولًا", "Review the Names first"), Modifier.fillMaxWidth(), onClick = onNames)
                }
            }
        } else if (!finished) {
            val q = quiz[idx]
            val ans = NAMES_99[q.answer]
            item {
                LinearProgressIndicator(
                    progress = { (idx + 1) / quiz.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = look.cta, trackColor = look.onCardMuted.copy(alpha = .2f), drawStopIndicator = {},
                )
                VSpace(6.dp)
                Muted(digits(t("السؤال ${idx + 1} من ${quiz.size} • النتيجة $score", "Question ${idx + 1} of ${quiz.size} • Score $score"), ar))
            }
            item {
                SCard(strong = true) {
                    Muted(if (q.kind == 0) t("ما معنى هذا الاسم؟", "What does this Name mean?") else t("أيّ الأسماء معناه:", "Which Name means:"))
                    VSpace(6.dp)
                    if (q.kind == 0) Text(ans.ar, fontFamily = QuranFont, fontSize = 44.sp, lineHeight = 76.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    else Text(if (ar) ans.meaningAr else ans.en, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp))
                }
            }
            items(q.options) { o ->
                val nm = NAMES_99[o]
                val isAns = o == q.answer
                val bg = when {
                    chosen < 0 -> look.card
                    isAns -> Color(0xFF16A34A)
                    o == chosen -> Color(0xFFDC2626)
                    else -> look.card
                }
                val fg = if (chosen >= 0 && (isAns || o == chosen)) Color.White else look.onCard
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, look.cardBorder, RoundedCornerShape(18.dp))
                        .press(enabled = chosen < 0) {
                            chosen = o
                            if (isAns) score++
                            view.performHapticFeedback(if (isAns) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.LONG_PRESS)
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (q.kind == 0) Text(if (ar) nm.meaningAr else "${nm.en}", style = MaterialTheme.typography.bodyLarge, color = fg)
                    else Text(nm.ar + if (!ar) "  ·  ${nm.tr}" else "", fontFamily = QuranFont, fontSize = 24.sp, lineHeight = 44.sp, color = fg)
                }
            }
            if (chosen >= 0) item {
                Muted(if (chosen == q.answer) t("أحسنت!", "Well done!") else t("الإجابة الصحيحة: ${ans.ar} — ${ans.meaningAr}", "Correct answer: ${ans.tr} — ${ans.en}"))
            }
        } else {
            item {
                SCard(strong = true) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PackIcon(if (score >= 8) Ico.TROPHY else Ico.STAR, 80.dp)
                        VSpace(8.dp)
                        Text(digits("$score / ${quiz.size}", ar), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Muted(when {
                            score == quiz.size -> t("ممتاز! أحصيتها حفظًا وفهمًا.", "Excellent — full marks!")
                            score >= 7 -> t("رائع، واصل المراجعة.", "Great — keep reviewing.")
                            else -> t("لا بأس، راجع الأسماء وحاول مرة أخرى.", "Review the Names and try again.")
                        })
                        VSpace(14.dp)
                        CtaPill(t("اختبار جديد", "New quiz"), Modifier.fillMaxWidth()) { seed = System.currentTimeMillis() }
                        VSpace(8.dp)
                        GhostPill(t("راجع الأسماء", "Review the Names"), Modifier.fillMaxWidth(), onClick = onNames)
                    }
                }
            }
        }
        if (user.namesQuiz.isNotEmpty()) {
            item { SectionHeader(t("السجل", "History")) }
            item {
                SCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    val fmt = DateTimeFormatter.ofPattern("d MMM • h:mm a", locale(ar))
                    user.namesQuiz.take(8).forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Muted(digits(Instant.ofEpochMilli(r.at).atZone(ZoneId.systemDefault()).format(fmt), ar), Modifier.weight(1f))
                            Text(digits("${r.score}/${r.total}", ar), style = MaterialTheme.typography.titleMedium, color = look.cta)
                        }
                    }
                }
            }
        }
    }
}
