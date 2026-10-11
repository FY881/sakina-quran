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

    /** Umm al-Qura date for [epochMillis] shifted by [adjustDays] (user correction). */
    fun format(epochMillis: Long, arabic: Boolean, adjustDays: Int = 0, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(zone, ULocale("en@calendar=islamic-umalqura"))
        cal.setTimeInMillis(epochMillis + adjustDays * 86_400_000L)
        val d: Int = cal.get(Calendar.DAY_OF_MONTH)
        val m: Int = cal.get(Calendar.MONTH)
        val y: Int = cal.get(Calendar.YEAR)
        return if (arabic) "$d ${monthsAr[m]} $y هـ" else "$d ${monthsEn[m]} $y AH"
    }
}
