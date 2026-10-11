package app.sakina.quran.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.SunnahTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

data class Place(
    val name: String,
    val nameAr: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val zone: ZoneId,
) {
    fun label(arabic: Boolean) = if (arabic && nameAr.isNotBlank()) nameAr else name
}

enum class Method(val ar: String, val en: String, val adhan: CalculationMethod) {
    EGYPTIAN("الهيئة المصرية العامة للمساحة", "Egyptian General Authority of Survey", CalculationMethod.EGYPTIAN),
    UMM_AL_QURA("أم القرى (مكة المكرمة)", "Umm al-Qura, Makkah", CalculationMethod.UMM_AL_QURA),
    MWL("رابطة العالم الإسلامي", "Muslim World League", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", "University of Islamic Sciences, Karachi", CalculationMethod.KARACHI),
    DUBAI("دبي", "Dubai", CalculationMethod.DUBAI),
    KUWAIT("الكويت", "Kuwait", CalculationMethod.KUWAIT),
    QATAR("قطر", "Qatar", CalculationMethod.QATAR),
    SINGAPORE("سنغافورة / ماليزيا / إندونيسيا", "Singapore / Malaysia / Indonesia", CalculationMethod.SINGAPORE),
    NORTH_AMERICA("أمريكا الشمالية (ISNA)", "North America (ISNA)", CalculationMethod.NORTH_AMERICA),
    MOONSIGHTING("لجنة رؤية الهلال", "Moonsighting Committee", CalculationMethod.MOON_SIGHTING_COMMITTEE);

    companion object {
        fun forCountry(cc: String): Method = when (cc.uppercase()) {
            "EG", "SD", "LY", "SY", "LB", "IQ", "JO", "PS" -> EGYPTIAN
            "SA", "YE", "BH", "OM" -> UMM_AL_QURA
            "AE" -> DUBAI
            "KW" -> KUWAIT
            "QA" -> QATAR
            "PK", "IN", "BD", "AF" -> KARACHI
            "SG", "MY", "ID", "BN" -> SINGAPORE
            "US", "CA" -> NORTH_AMERICA
            "GB", "IE" -> MOONSIGHTING
            else -> MWL
        }
    }
}

enum class AsrMadhab(val ar: String, val en: String) { SHAFI("الجمهور (شافعي، مالكي، حنبلي)", "Standard (Shafi, Maliki, Hanbali)"), HANAFI("حنفي", "Hanafi") }

enum class HighLat(val ar: String, val en: String, val rule: HighLatitudeRule) {
    MIDDLE("منتصف الليل", "Middle of the night", HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
    SEVENTH("سُبع الليل", "Seventh of the night", HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    ANGLE("زاوية الشفق", "Twilight angle", HighLatitudeRule.TWILIGHT_ANGLE),
}

data class Calc(
    val method: Method = Method.EGYPTIAN,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val highLat: HighLat = HighLat.MIDDLE,
    /** minutes per prayer, indexed by Prayer.ordinal */
    val adj: List<Int> = List(6) { 0 },
) {
    fun params(): CalculationParameters = method.adhan.parameters.also { p ->
        p.madhab = if (madhab == AsrMadhab.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        p.highLatitudeRule = highLat.rule
        p.adjustments.fajr += adj[0]; p.adjustments.sunrise += adj[1]; p.adjustments.dhuhr += adj[2]
        p.adjustments.asr += adj[3]; p.adjustments.maghrib += adj[4]; p.adjustments.isha += adj[5]
    }
}

data class DayTimes(
    val date: LocalDate,
    val times: Map<Prayer, ZonedDateTime>,
    val midnight: ZonedDateTime,
    val lastThird: ZonedDateTime,
)

data class NextPrayer(val prayer: Prayer, val at: ZonedDateTime, val previous: ZonedDateTime)

object PrayerEngine {
    val CAIRO = Place("Cairo", "القاهرة", "EG", 30.0444, 31.2357, ZoneId.of("Africa/Cairo"))

    fun day(place: Place, date: LocalDate, calc: Calc = Calc()): DayTimes {
        val coords = Coordinates(place.lat, place.lng)
        val pt = PrayerTimes(coords, DateComponents(date.year, date.monthValue, date.dayOfMonth), calc.params())
        val st = SunnahTimes(pt)
        fun z(d: java.util.Date) = Instant.ofEpochMilli(d.time).atZone(place.zone)
        return DayTimes(
            date,
            linkedMapOf(
                Prayer.FAJR to z(pt.fajr), Prayer.SUNRISE to z(pt.sunrise), Prayer.DHUHR to z(pt.dhuhr),
                Prayer.ASR to z(pt.asr), Prayer.MAGHRIB to z(pt.maghrib), Prayer.ISHA to z(pt.isha),
            ),
            z(st.middleOfTheNight), z(st.lastThirdOfTheNight),
        )
    }

    /** Next obligatory prayer (sunrise skipped) after [now]. */
    fun next(place: Place, now: ZonedDateTime, calc: Calc = Calc()): NextPrayer {
        val local = now.withZoneSameInstant(place.zone)
        val yesterday = day(place, local.toLocalDate().minusDays(1), calc)
        val today = day(place, local.toLocalDate(), calc)
        var prev = yesterday.times.getValue(Prayer.ISHA)
        today.times.filterKeys { it != Prayer.SUNRISE }.forEach { (k, v) ->
            if (v.isAfter(local)) return NextPrayer(k, v, prev)
            prev = v
        }
        val tomorrow = day(place, local.toLocalDate().plusDays(1), calc)
        return NextPrayer(Prayer.FAJR, tomorrow.times.getValue(Prayer.FAJR), prev)
    }

    /** Qibla bearing in degrees from true north. */
    fun qibla(place: Place): Double = com.batoulapps.adhan.Qibla(Coordinates(place.lat, place.lng)).direction
}
