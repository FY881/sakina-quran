package app.sakina.quran.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.UserData
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.num
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.icons.PackIcon
import app.sakina.quran.ui.kit.CtaPill
import app.sakina.quran.ui.kit.GhostPill
import app.sakina.quran.ui.kit.Muted
import app.sakina.quran.ui.kit.VSpace
import app.sakina.quran.ui.tabs.FEATURES
import app.sakina.quran.ui.tabs.Feature
import app.sakina.quran.ui.tabs.feature
import app.sakina.quran.ui.theme.LocalSakina

private const val MAX_FEATURES = 12

/** Bottom sheet to add, remove and reorder the Home "Features" row. Saved to UserData on "Save". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureEditorSheet(current: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val look = LocalSakina.current.look
    val ar = isArabic()
    val chosen = remember { mutableStateListOf<String>().apply { addAll(current.filter { feature(it) != null }) } }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = look.bgTop, contentColor = look.onCard) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 18.dp).navigationBarsPadding()) {
            Text(t("تخصيص المميزات", "Customise features"), style = MaterialTheme.typography.headlineSmall)
            Muted(t("رتّب ما يظهر في الرئيسية (حتى ${num(MAX_FEATURES, true)}).", "Arrange what shows on Home (up to $MAX_FEATURES)."))
            VSpace(12.dp)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                item { Text(t("في الرئيسية", "On Home") + " • " + num(chosen.size, ar), style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(chosen, key = { _, id -> "c_$id" }) { i, id ->
                    val f = feature(id) ?: return@itemsIndexed
                    EditorRow(f) {
                        IconButton(onClick = { if (i > 0) { chosen.removeAt(i); chosen.add(i - 1, id) } }, enabled = i > 0) { Icon(Icons.Outlined.KeyboardArrowUp, t("لأعلى", "Move up"), tint = if (i > 0) look.onCard else look.onCardMuted.copy(alpha = .3f)) }
                        IconButton(onClick = { if (i < chosen.lastIndex) { chosen.removeAt(i); chosen.add(i + 1, id) } }, enabled = i < chosen.lastIndex) { Icon(Icons.Outlined.KeyboardArrowDown, t("لأسفل", "Move down"), tint = if (i < chosen.lastIndex) look.onCard else look.onCardMuted.copy(alpha = .3f)) }
                        IconButton(onClick = { chosen.removeAt(i) }) { Icon(Icons.Outlined.RemoveCircleOutline, t("إزالة", "Remove"), tint = Color(0xFFEF4444)) }
                    }
                }
                item { VSpace(8.dp); Text(t("أضف من المكتبة", "Add from library"), style = MaterialTheme.typography.titleMedium) }
                val rest = FEATURES.filter { it.id !in chosen && it.id != "quran_search" }
                items(rest, key = { "a_${it.id}" }) { f ->
                    EditorRow(f) {
                        val can = chosen.size < MAX_FEATURES
                        IconButton(onClick = { if (can) chosen.add(f.id) }, enabled = can) { Icon(Icons.Outlined.AddCircleOutline, t("إضافة", "Add"), tint = if (can) look.cta else look.onCardMuted.copy(alpha = .3f)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostPill(t("الافتراضي", "Reset")) { chosen.clear(); chosen.addAll(UserData.DEFAULT_FEATURES) }
                Spacer(Modifier.weight(1f))
                CtaPill(t("حفظ", "Save"), Modifier.width(150.dp)) { onSave(chosen.toList()); onDismiss() }
            }
        }
    }
}

@Composable
private fun EditorRow(f: Feature, actions: @Composable () -> Unit) {
    val look = LocalSakina.current.look
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(look.card).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PackIcon(f.ico, 32.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t(f.ar, f.en), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (f.route == null) Muted(t("قريبًا", "Coming soon"))
        }
        Box { Row(verticalAlignment = Alignment.CenterVertically) { actions() } }
    }
}
