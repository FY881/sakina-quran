package app.sakina.quran.location

import android.content.Context
import app.sakina.quran.prayer.Place
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Offline city database (GeoNames cities15000, CC BY 4.0), sorted by population. */
object Cities {
    data class City(val name: String, val nameAr: String, val cc: String, val lat: Double, val lng: Double, val tz: String, val pop: Long) {
        fun toPlace() = Place(name, nameAr, cc, lat, lng, runCatching { ZoneId.of(tz) }.getOrDefault(ZoneId.systemDefault()))
        val key: String by lazy { normalize(name) + " " + normalize(nameAr) }
    }

    @Volatile private var cache: List<City>? = null

    fun all(context: Context): List<City> = cache ?: synchronized(this) {
        cache ?: context.assets.open("cities.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull { l ->
                val f = l.split('\t')
                if (f.size < 7) null else runCatching {
                    City(f[0], f[1], f[2], f[3].toDouble(), f[4].toDouble(), f[5], f[6].toLong())
                }.getOrNull()
            }.toList()
        }.also { cache = it }
    }

    /** Arabic-aware normalization: strip tashkeel/tatweel, unify alef/yaa/taa marbuta, lowercase. */
    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) {
            when {
                c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u0640' -> {}
                c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ' -> sb.append('ا')
                c == 'ى' -> sb.append('ي')
                c == 'ة' -> sb.append('ه')
                c == 'ؤ' -> sb.append('و')
                c == 'ئ' -> sb.append('ي')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun search(context: Context, q: String, limit: Int = 60): List<City> {
        val n = normalize(q.trim())
        val list = all(context)
        if (n.isEmpty()) return list.take(limit)
        val starts = ArrayList<City>(); val contains = ArrayList<City>()
        for (c in list) {
            val nn = normalize(c.name); val na = normalize(c.nameAr)
            if (nn.startsWith(n) || na.startsWith(n)) starts += c
            else if (c.key.contains(n)) contains += c
            if (starts.size >= limit) break
        }
        return (starts + contains).take(limit)
    }

    fun distanceKm(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(bLat - aLat); val dLng = Math.toRadians(bLng - aLng)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }

    fun nearest(context: Context, lat: Double, lng: Double): City? =
        all(context).minByOrNull { distanceKm(lat, lng, it.lat, it.lng) }
}
