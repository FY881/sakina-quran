package app.sakina.quran.prayer

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale

object Hijri {
    private val monthsAr = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    private val monthsEn = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Akhir", "Jumada al-Ula", "Jumada al-Akhirah",
        "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
    )

    /** (month 1..12, day) of the Umm al-Qura date. */
    fun monthDay(epochMillis: Long, adjustDays: Int = 0): Pair<Int, Int> {
        val cal = Calendar.getInstance(TimeZone.getDefault(), ULocale("en@calendar=islamic-umalqura"))
        cal.setTimeInMillis(epochMillis + adjustDays * 86_400_000L)
        return (cal.get(Calendar.MONTH) + 1) to cal.get(Calendar.DAY_OF_MONTH)
    }

    /** Umm al-Qura date for [epochMillis] shifted by [adjustDays] (user correction). */
    fun format(epochMillis: Long, arabic: Boolean, adjustDays: Int = 0, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(zone, ULocale("en@calendar=islamic-umalqura"))
        cal.setTimeInMillis(epochMillis + adjustDays * 86_400_000L)
        val d: Int = cal.get(Calendar.DAY_OF_MONTH)
        val m: Int = cal.get(Calendar.MONTH)
        val y: Int = cal.get(Calendar.YEAR)
        return if (arabic) "$d ${monthsAr[m]} $y هـ" else "$d ${monthsEn[m]} $y AH"
    }

    /** "28 Rabi al-Akhir" / "28 ربيع الآخر" (Western digits; localize in UI). */
    fun dayMonth(epochMillis: Long, arabic: Boolean, adjustDays: Int = 0): String {
        val (m, d) = monthDay(epochMillis, adjustDays)
        return if (arabic) "$d ${monthsAr[m - 1]}" else "$d ${monthsEn[m - 1]}"
    }
}
