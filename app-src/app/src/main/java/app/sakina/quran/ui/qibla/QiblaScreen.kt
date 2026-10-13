package app.sakina.quran.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sakina.quran.data.Prefs
import app.sakina.quran.location.Cities
import app.sakina.quran.prayer.PrayerEngine
import app.sakina.quran.ui.common.isArabic
import app.sakina.quran.ui.common.t
import app.sakina.quran.ui.theme.DeepGreen
import app.sakina.quran.ui.theme.Gold
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(prefs: Prefs, fixedHeading: Float? = null) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val ar = isArabic()
    val qibla = PrayerEngine.qibla(prefs.place).toFloat()
    val distance = Cities.distanceKm(prefs.place.lat, prefs.place.lng, 21.4225, 39.8262).roundToInt()
    var heading by remember { mutableFloatStateOf(fixedHeading ?: 0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var accuracyLow by remember { mutableStateOf(false) }

    if (fixedHeading == null) DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        hasSensor = rv != null
        val mat = FloatArray(9); val ori = FloatArray(3)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(mat, e.values)
                SensorManager.getOrientation(mat, ori)
                val deg = ((Math.toDegrees(ori[0].toDouble()) + 360) % 360).toFloat()
                heading = heading + (((deg - heading + 540) % 360) - 180) * 0.15f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) { accuracyLow = a <= SensorManager.SENSOR_STATUS_ACCURACY_LOW }
        }
        if (rv != null) sm.registerListener(l, rv, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    val dial by animateFloatAsState(-heading, tween(120), label = "dial")
    val diff = ((qibla - heading + 540) % 360) - 180
    val aligned = abs(diff) < 3
    LaunchedEffect(aligned) { if (aligned) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(t("اتجاه القبلة", "Qibla direction"), style = MaterialTheme.typography.headlineSmall)
        Text(
            t("${qibla.roundToInt()}° من الشمال • ${"%,d".format(distance)} كم إلى مكة المكرمة", "${qibla.roundToInt()}° from north • ${"%,d".format(distance)} km to Makkah"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val ring = MaterialTheme.colorScheme.outline
        val onSurface = MaterialTheme.colorScheme.onSurface
        Canvas(Modifier.fillMaxWidth(0.88f).aspectRatio(1f)) {
            val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2
            drawCircle(if (aligned) DeepGreen.copy(alpha = .18f) else ring.copy(alpha = .08f), r, c)
            drawCircle(ring.copy(alpha = .5f), r, c, style = Stroke(3f))
            rotate(dial, c) {
                for (i in 0 until 72) {
                    val a = Math.toRadians(i * 5.0)
                    val len = if (i % 18 == 0) 26f else if (i % 2 == 0) 14f else 8f
                    val o = Offset(c.x + (r - 6) * kotlin.math.sin(a).toFloat(), c.y - (r - 6) * kotlin.math.cos(a).toFloat())
                    val ii = Offset(c.x + (r - 6 - len) * kotlin.math.sin(a).toFloat(), c.y - (r - 6 - len) * kotlin.math.cos(a).toFloat())
                    drawLine(if (i == 0) Color(0xFFC0392B) else onSurface.copy(alpha = .55f), o, ii, if (i % 18 == 0) 4f else 2f)
                }
                rotate(qibla, c) {
                    val p = Path().apply {
                        moveTo(c.x, c.y - r * 0.78f); lineTo(c.x - r * 0.09f, c.y - r * 0.1f); lineTo(c.x + r * 0.09f, c.y - r * 0.1f); close()
                    }
                    drawPath(p, if (aligned) Gold else DeepGreen)
                    drawRect(Color(0xFF1B1B1B), Offset(c.x - r * 0.06f, c.y - r * 0.92f), androidx.compose.ui.geometry.Size(r * 0.12f, r * 0.12f))
                    drawRect(Gold, Offset(c.x - r * 0.06f, c.y - r * 0.89f), androidx.compose.ui.geometry.Size(r * 0.12f, r * 0.02f))
                }
            }
            drawCircle(onSurface, 8f, c)
        }
        Text(
            when {
                !hasSensor -> t("هاتفك لا يحتوي على بوصلة. وجّه هاتفك بزاوية ${qibla.roundToInt()}° من الشمال.", "No compass on this phone. Face ${qibla.roundToInt()}° from north.")
                aligned -> t("أنت متجه نحو القبلة", "You're facing the Qibla ✓")
                diff > 0 -> t("استدر يمينًا ${abs(diff).roundToInt()}°", "Turn right ${abs(diff).roundToInt()}°")
                else -> t("استدر يسارًا ${abs(diff).roundToInt()}°", "Turn left ${abs(diff).roundToInt()}°")
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (accuracyLow) Text(
            t("دقة البوصلة ضعيفة: حرّك هاتفك على شكل رقم 8 وابتعد عن المعادن.", "Compass accuracy is low: move your phone in a figure 8 away from metal."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
        )
        if (ar) Unit
    }
}
