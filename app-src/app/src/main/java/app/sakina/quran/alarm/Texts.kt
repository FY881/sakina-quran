package app.sakina.quran.alarm

import android.content.Context
import app.sakina.quran.prayer.Prayer

/** Localized strings usable outside Compose (receivers/services/widgets). */
object Texts {
    fun ar(c: Context) = c.resources.configuration.locales[0].language == "ar"
    fun name(c: Context, p: Prayer, friday: Boolean = false): String = if (ar(c)) when (p) {
        Prayer.FAJR -> "الفجر"; Prayer.SUNRISE -> "الشروق"; Prayer.DHUHR -> if (friday) "الجمعة" else "الظهر"
        Prayer.ASR -> "العصر"; Prayer.MAGHRIB -> "المغرب"; Prayer.ISHA -> "العشاء"
    } else when (p) {
        Prayer.FAJR -> "Fajr"; Prayer.SUNRISE -> "Sunrise"; Prayer.DHUHR -> if (friday) "Jumu'ah" else "Dhuhr"
        Prayer.ASR -> "Asr"; Prayer.MAGHRIB -> "Maghrib"; Prayer.ISHA -> "Isha"
    }
    fun timeFor(c: Context, p: Prayer, friday: Boolean) =
        if (ar(c)) (if (p == Prayer.SUNRISE) "حان وقت الشروق" else "حان الآن موعد صلاة ${name(c, p, friday)}")
        else (if (p == Prayer.SUNRISE) "It's sunrise" else "It's time for ${name(c, p, friday)}")
    fun hint(c: Context, p: Prayer) = when {
        p == Prayer.SUNRISE -> if (ar(c)) "انتهى وقت الفجر. لا تنسَ أذكار الصباح." else "Fajr time has ended. Remember the morning azkar."
        ar(c) -> "حيّ على الصلاة، حيّ على الفلاح"
        else -> "Come to prayer, come to success"
    }
    fun before(c: Context, p: Prayer, min: Int, friday: Boolean) =
        if (ar(c)) "باقي $min دقيقة على ${name(c, p, friday)}" else "${name(c, p, friday)} in $min min"
    fun stop(c: Context) = if (ar(c)) "إيقاف الأذان" else "Stop adhan"
    fun azkarTitle(c: Context, kind: String) = if (ar(c)) when (kind) {
        "morning" -> "أذكار الصباح"; "evening" -> "أذكار المساء"; else -> "أذكار النوم"
    } else when (kind) { "morning" -> "Morning azkar"; "evening" -> "Evening azkar"; else -> "Azkar before sleep" }
    fun azkarText(c: Context, kind: String) = if (ar(c)) when (kind) {
        "morning" -> "ابدأ يومك بذكر الله — دقائق قليلة تحفظك بإذن الله"
        "evening" -> "حان وقت أذكار المساء — اضغط لتبدأ بالعدّاد"
        else -> "اختم يومك بأذكار النوم وآية الكرسي"
    } else when (kind) {
        "morning" -> "Start your day remembering Allah — a few protective minutes"
        "evening" -> "Time for the evening azkar — tap to start with the counter"
        else -> "End your day with the sleep azkar and Ayat al-Kursi"
    }
}
