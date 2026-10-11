package app.sakina.quran.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Coarse, foreground-only location without Google Play Services. */
object LocationHelper {
    fun hasPermission(c: Context) =
        ContextCompat.checkSelfPermission(c, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(c: Context): Location? {
        if (!hasPermission(c)) return null
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60_000L) return last
        val provider = when {
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            else -> return last
        }
        val fresh = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                if (Build.VERSION.SDK_INT >= 30) {
                    lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(c)) { if (cont.isActive) cont.resume(it) }
                } else {
                    val l = object : LocationListener {
                        override fun onLocationChanged(loc: Location) { lm.removeUpdates(this); if (cont.isActive) cont.resume(loc) }
                        @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) {}
                    }
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, l, Looper.getMainLooper())
                    cont.invokeOnCancellation { lm.removeUpdates(l) }
                }
            }
        }
        return fresh ?: last
    }
}
