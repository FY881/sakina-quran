package app.sakina.quran.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.SettingsRepo
import app.sakina.quran.location.Cities
import app.sakina.quran.location.LocationHelper
import app.sakina.quran.prayer.Place
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Location picker: one-tap approximate location (with disclosure) or offline city search. */
@Composable
fun CityScreen(repo: SettingsRepo, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ar = isArabic()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Cities.City>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val noLoc = t("تعذّر تحديد الموقع. اختر مدينتك من القائمة.", "Couldn't get your location. Pick your city from the list.")

    fun choose(p: Place) = scope.launch { repo.setPlace(p, autoMethod = true); onDone() }

    fun locate() = scope.launch {
        busy = true; error = null
        val loc = LocationHelper.current(ctx)
        if (loc == null) { error = noLoc; busy = false; return@launch }
        val city = withContext(Dispatchers.Default) { Cities.nearest(ctx, loc.latitude, loc.longitude) }
        busy = false
        if (city != null) choose(city.toPlace().copy(lat = loc.latitude, lng = loc.longitude))
    }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else error = noLoc
    }

    LaunchedEffect(q) {
        results = withContext(Dispatchers.Default) { Cities.search(ctx, q) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            t("نستخدم موقعك التقريبي مرة واحدة فقط لحساب مواقيت الصلاة واتجاه القبلة. يبقى على هاتفك ولا يُرسل لأي جهة.",
                "We use your approximate location once, only to calculate prayer times and the Qibla. It stays on your phone and is never sent anywhere."),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(10.dp))
        Button(
            onClick = { if (LocationHelper.hasPermission(ctx)) locate() else perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), enabled = !busy,
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.MyLocation, null)
            Spacer(Modifier.width(8.dp))
            Text(t("استخدم موقعي الحالي", "Use my current location"))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = q, onValueChange = { q = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text(t("ابحث عن مدينة… (مثال: الزقازيق)", "Search a city… (e.g. Cairo)")) },
            shape = RoundedCornerShape(18.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(results, key = { "${it.name}${it.lat}${it.lng}" }) { c ->
                val country = Locale("", c.cc).getDisplayCountry(if (ar) Locale("ar") else Locale.ENGLISH)
                Row(Modifier.fillMaxWidth().clickable { choose(c.toPlace()) }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (ar && c.nameAr.isNotBlank()) c.nameAr else c.name, style = MaterialTheme.typography.bodyLarge)
                        Text(country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
