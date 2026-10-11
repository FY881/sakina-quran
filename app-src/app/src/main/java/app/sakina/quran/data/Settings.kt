package app.sakina.quran.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.sakina.quran.prayer.AsrMadhab
import app.sakina.quran.prayer.Calc
import app.sakina.quran.prayer.HighLat
import app.sakina.quran.prayer.Method
import app.sakina.quran.prayer.Place
import app.sakina.quran.prayer.Prayer
import app.sakina.quran.prayer.PrayerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.ZoneId

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How a prayer is announced. */
enum class AlertMode { OFF, SILENT, NOTIFY, ADHAN }

data class Voice(val id: Int, val ar: String, val en: String, val credit: String)

val VOICES = listOf(
    Voice(1, "أذان خاشع", "Serene adhan", "\"Beautiful adhan\" — Adam-synagda, Wikimedia Commons, CC0"),
    Voice(2, "أذان بصوت ممتد", "Long adhan", "\"Azan\" — Andrewler, Wikimedia Commons, CC BY-SA 4.0"),
    Voice(3, "أذان عاقب عزيز", "Adhan by Aaqib Azeez", "\"The Adhan\" — Aaqib Azeez / Atcovi, Wikimedia Commons, CC BY-SA 4.0"),
    Voice(0, "تكبير فقط (قصير)", "Takbeer only (short)", "Excerpt of \"Beautiful adhan\" (CC0)"),
)

data class Prefs(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val place: Place = PrayerEngine.CAIRO,
    val placeSet: Boolean = false,
    val calc: Calc = Calc(),
    val hijriAdj: Int = 0,
    val modes: Map<Prayer, AlertMode> = defaultModes,
    val voice: Int = 1,
    val fajrVoice: Int = 1,
    val preMin: Int = 10,
    val jumuahReminder: Boolean = true,
) {
    companion object {
        val defaultModes = mapOf(
            Prayer.FAJR to AlertMode.ADHAN, Prayer.SUNRISE to AlertMode.OFF, Prayer.DHUHR to AlertMode.ADHAN,
            Prayer.ASR to AlertMode.ADHAN, Prayer.MAGHRIB to AlertMode.ADHAN, Prayer.ISHA to AlertMode.ADHAN,
        )
    }
}

class SettingsRepo(context: Context) {
    private val ds = context.applicationContext.dataStore

    private object K {
        val theme = stringPreferencesKey("theme")
        val onboarded = booleanPreferencesKey("onboarded")
        val place = stringPreferencesKey("place")
        val method = stringPreferencesKey("method")
        val madhab = stringPreferencesKey("madhab")
        val highLat = stringPreferencesKey("highLat")
        val adj = stringPreferencesKey("adj")
        val hijriAdj = intPreferencesKey("hijriAdj")
        val modes = stringPreferencesKey("modes")
        val voice = intPreferencesKey("voice")
        val fajrVoice = intPreferencesKey("fajrVoice")
        val preMin = intPreferencesKey("preMin")
        val jumuah = booleanPreferencesKey("jumuah")
    }

    private inline fun <reified E : Enum<E>> en(v: String?, d: E): E = runCatching { enumValueOf<E>(v!!) }.getOrDefault(d)

    private fun decode(p: Preferences): Prefs {
        val place = p[K.place]?.split('|')?.takeIf { it.size == 6 }?.let {
            runCatching { Place(it[0], it[1], it[2], it[3].toDouble(), it[4].toDouble(), ZoneId.of(it[5])) }.getOrNull()
        }
        val adj = p[K.adj]?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 6 } ?: List(6) { 0 }
        val modes = Prefs.defaultModes.toMutableMap()
        p[K.modes]?.split(',')?.forEachIndexed { i, s -> if (i < 6) modes[Prayer.entries[i]] = en(s, AlertMode.ADHAN) }
        return Prefs(
            theme = en(p[K.theme], ThemeMode.SYSTEM),
            onboarded = p[K.onboarded] ?: false,
            place = place ?: PrayerEngine.CAIRO,
            placeSet = place != null,
            calc = Calc(en(p[K.method], Method.EGYPTIAN), en(p[K.madhab], AsrMadhab.SHAFI), en(p[K.highLat], HighLat.MIDDLE), adj),
            hijriAdj = p[K.hijriAdj] ?: 0,
            modes = modes,
            voice = p[K.voice] ?: 1,
            fajrVoice = p[K.fajrVoice] ?: 1,
            preMin = p[K.preMin] ?: 10,
            jumuahReminder = p[K.jumuah] ?: true,
        )
    }

    val prefs: Flow<Prefs> = ds.data.map(::decode)
    val theme: Flow<ThemeMode> = prefs.map { it.theme }

    suspend fun now(): Prefs = prefs.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) { ds.edit { block(it) } }

    suspend fun setTheme(mode: ThemeMode) = edit { it[K.theme] = mode.name }
    suspend fun setOnboarded() = edit { it[K.onboarded] = true }
    suspend fun setPlace(pl: Place, autoMethod: Boolean) = edit {
        it[K.place] = listOf(pl.name.replace("|", " "), pl.nameAr.replace("|", " "), pl.country, pl.lat, pl.lng, pl.zone.id).joinToString("|")
        if (autoMethod) {
            it[K.method] = Method.forCountry(pl.country).name
            if (pl.country in setOf("PK", "IN", "BD", "AF", "TR")) it[K.madhab] = AsrMadhab.HANAFI.name
        }
    }
    suspend fun setMethod(m: Method) = edit { it[K.method] = m.name }
    suspend fun setMadhab(m: AsrMadhab) = edit { it[K.madhab] = m.name }
    suspend fun setHighLat(h: HighLat) = edit { it[K.highLat] = h.name }
    suspend fun setAdj(prayer: Prayer, minutes: Int) {
        val cur = now().calc.adj.toMutableList(); cur[prayer.ordinal] = minutes.coerceIn(-30, 30)
        edit { it[K.adj] = cur.joinToString(",") }
    }
    suspend fun setHijriAdj(d: Int) = edit { it[K.hijriAdj] = d.coerceIn(-2, 2) }
    suspend fun setMode(prayer: Prayer, mode: AlertMode) {
        val m = now().modes.toMutableMap(); m[prayer] = mode
        edit { it[K.modes] = Prayer.entries.joinToString(",") { p -> m.getValue(p).name } }
    }
    suspend fun setVoice(id: Int) = edit { it[K.voice] = id }
    suspend fun setFajrVoice(id: Int) = edit { it[K.fajrVoice] = id }
    suspend fun setPreMin(m: Int) = edit { it[K.preMin] = m }
    suspend fun setJumuah(b: Boolean) = edit { it[K.jumuah] = b }
}
