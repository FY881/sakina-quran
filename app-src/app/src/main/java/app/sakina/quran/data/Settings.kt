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

/** The visual theme family (DESIGN_PLAN §2). Light/dark is chosen separately by [ThemeMode]. */
enum class AppTheme(val ar: String, val en: String, val hasLight: Boolean = true, val hasDark: Boolean = true) {
    CLASSIC_TEAL("سكينة الكلاسيكي", "Sakina Classic"),
    LIQUID_GLASS("زجاج سائل", "Liquid Glass"),
    MADINAH_NIGHT("ليل المدينة", "Madinah Night", hasLight = false),
    EMERALD_ANDALUS("زمرد الأندلس", "Emerald Andalus"),
    DESERT_SAND("رمال الصحراء", "Desert Sand"),
    OTTOMAN_RUBY("ياقوت عثماني", "Ottoman Ruby"),
    ROSE_DAWN("فجر وردي", "Rose Dawn"),
    AMOLED("أسود AMOLED", "AMOLED Black", hasLight = false),
    HIGH_CONTRAST("تباين عالٍ", "High Contrast"),
    DYNAMIC("ألوان جهازك", "Material You"),
    SEASONAL("موسمي تلقائي", "Seasonal (auto)"),
}

/** How much glass: OFF = solid surfaces (fastest), LIGHT = soft blur, FULL = deep frosted glass. */
enum class GlassLevel(val ar: String, val en: String) { OFF("بدون", "Off"), LIGHT("خفيف", "Light"), FULL("كامل", "Full") }

/** Selectable icon packs. AUTO picks the best pack for the current theme. */
enum class IconPackId(val ar: String, val en: String, val dir: String) {
    AUTO("تلقائي حسب الثيم", "Auto (match theme)", ""),
    FLUENT_3D("ثلاثي الأبعاد واقعي", "Realistic 3D", "fluent3d"),
    FLUENT_COLOR("ملوّن ناعم", "Soft color", "fluentcolor"),
    FLUENT_FLAT("مسطّح", "Flat", "fluentflat"),
    FLUENT_MONO("أحادي بلون الثيم", "Mono (theme tint)", "fluentmono"),
    NOTO("نوتو", "Noto", "noto"),
}

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
    val theme: ThemeMode = ThemeMode.DARK,
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
    val appTheme: AppTheme = AppTheme.CLASSIC_TEAL,
    /** ARGB accent override; 0 = theme default. */
    val accent: Int = 0,
    val glass: GlassLevel = GlassLevel.FULL,
    val iconPack: IconPackId = IconPackId.AUTO,
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
        val appTheme = stringPreferencesKey("appTheme")
        val accent = intPreferencesKey("accent")
        val glass = stringPreferencesKey("glass")
        val iconPack = stringPreferencesKey("iconPack")
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
            theme = en(p[K.theme], ThemeMode.DARK),
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
            appTheme = en(p[K.appTheme], AppTheme.CLASSIC_TEAL),
            accent = p[K.accent] ?: 0,
            glass = en(p[K.glass], GlassLevel.FULL),
            iconPack = en(p[K.iconPack], IconPackId.AUTO),
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
    suspend fun setAppTheme(t: AppTheme) = edit { it[K.appTheme] = t.name }
    suspend fun setAccent(argb: Int) = edit { it[K.accent] = argb }
    suspend fun setGlass(g: GlassLevel) = edit { it[K.glass] = g.name }
    suspend fun setIconPack(p: IconPackId) = edit { it[K.iconPack] = p.name }

    /** Debug/CI only: apply several values at once (used by screenshot automation). */
    suspend fun debugApply(theme: String?, mode: String?, glass: String?, icons: String?, onboarded: Boolean?) = edit {
        theme?.let { v -> it[K.appTheme] = en(v, AppTheme.CLASSIC_TEAL).name }
        mode?.let { v -> it[K.theme] = en(v, ThemeMode.SYSTEM).name }
        glass?.let { v -> it[K.glass] = en(v, GlassLevel.FULL).name }
        icons?.let { v -> it[K.iconPack] = en(v, IconPackId.AUTO).name }
        onboarded?.let { v -> it[K.onboarded] = v }
    }
}
