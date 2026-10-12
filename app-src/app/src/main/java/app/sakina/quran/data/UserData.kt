package app.sakina.quran.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.userStore by preferencesDataStore(name = "user")

data class TasbihSession(val at: Long, val preset: String, val count: Int)

data class QuizResult(val at: Long, val score: Int, val total: Int)

/** A dhikr the user wrote (own azkar list, AZ10) or a custom tasbih preset (TS03). */
data class OwnDhikr(val id: String, val text: String, val count: Int)

data class JournalEntry(val id: Long, val date: LocalDate, val text: String, val mood: Int)

/** Local personal data (works without an account; synced later in wave C1 when signed in). */
data class UserData(
    val name: String = "",
    val memberSince: Long = 0L,
    /** "yyyy-MM-dd:i" where i = 0 Fajr, 1 Dhuhr, 2 Asr, 3 Maghrib, 4 Isha. */
    val prayed: Set<String> = emptySet(),
    /** Ordered feature ids for the Home "Features" row. */
    val features: List<String> = DEFAULT_FEATURES,
    val readNotifs: Set<String> = emptySet(),
    val healthDismissedDay: String = "",
    val journal: List<JournalEntry> = emptyList(),
    val deenMode: Boolean = false,
    val storiesSeen: Set<String> = emptySet(),
    val guest: Boolean = true,
    val signInSkipped: Boolean = false,
    val likedPosts: Set<String> = emptySet(),
    /** Tasbih: current counter, selected preset id, target (0 = free), lifetime total. */
    val tasbihCount: Int = 0,
    val tasbihPreset: String = "subhan",
    val tasbihTarget: Int = 33,
    val tasbihTotal: Int = 0,
    /** Completed sessions: "epochMillis|presetId|count" (newest kept, max 100). */
    val tasbihHistory: List<TasbihSession> = emptyList(),
    /** Quran: last read position "sura:ayah" and bookmarks "sura:ayah". */
    val quranLast: String = "",
    val quranBookmarks: Set<String> = emptySet(),
    val namesFav: Set<Int> = emptySet(),
    val avatar: Int = 0,
    // ── Azkar (A1)
    /** Favourite dhikr ids (see azkar.Dhikr.id). */
    val azkarFav: Set<String> = emptySet(),
    /** Today's per-session progress: sessionId → remaining-count-done per item ("date|sid|c0,c1,…"). */
    val azkarProgress: Map<String, List<Int>> = emptyMap(),
    /** Completed sessions "yyyy-MM-dd:sid". */
    val azkarDone: Set<String> = emptySet(),
    val ownAzkar: List<OwnDhikr> = emptyList(),
    val namesQuiz: List<QuizResult> = emptyList(),
    // ── Tasbih upgrades
    val tasbihSkin: Int = 0,
    val tasbihSound: Boolean = false,
    val tasbihVolumeKeys: Boolean = true,
    val tasbihCustom: List<OwnDhikr> = emptyList(),
) {
    fun azkarDoneOn(d: LocalDate, sid: String) = "$d:$sid" in azkarDone
    /** Days (ending today or yesterday) on which the morning or the evening azkar were completed. */
    fun azkarStreak(today: LocalDate): Int {
        fun ok(d: LocalDate) = azkarDoneOn(d, "morning") || azkarDoneOn(d, "evening")
        var d = if (ok(today)) today else today.minusDays(1)
        var n = 0
        while (n < 3650 && ok(d)) { n++; d = d.minusDays(1) }
        return n
    }
    fun azkarDaysInWeek(today: LocalDate) = (0..6).count { i -> val d = today.minusDays(i.toLong()); azkarDone.any { it.startsWith("$d:") } }

    val lastSura: Int get() = quranLast.substringBefore(':').toIntOrNull() ?: 0
    val lastAyah: Int get() = quranLast.substringAfter(':', "").toIntOrNull() ?: 1
    fun tasbihOn(d: LocalDate): Int {
        val z = java.time.ZoneId.systemDefault()
        return tasbihHistory.filter { java.time.Instant.ofEpochMilli(it.at).atZone(z).toLocalDate() == d }.sumOf { it.count }
    }
    fun prayedOn(d: LocalDate): Int = (0..4).count { "$d:$it" in prayed }
    fun isPrayed(d: LocalDate, i: Int) = "$d:$i" in prayed
    val totalPrayed: Int get() = prayed.size
    val fullDays: Int get() = prayed.groupBy { it.substringBefore(':') }.count { it.value.size >= 5 }
    val daysPrayed: Int get() = prayed.map { it.substringBefore(':') }.toSet().size
    /** Consecutive days (ending today or yesterday) with all 5 prayers marked. */
    fun streak(today: LocalDate): Int {
        var d = if (prayedOn(today) >= 5) today else today.minusDays(1)
        var n = 0
        while (prayedOn(d) >= 5) { n++; d = d.minusDays(1) }
        return n
    }

    companion object {
        val DEFAULT_FEATURES = listOf("qibla", "duas", "tasbih", "azkar", "inspiration", "journal", "mosques", "calendar")
    }
}

class UserRepo(context: Context) {
    private val ds = context.applicationContext.userStore

    private object K {
        val name = stringPreferencesKey("name")
        val since = longPreferencesKey("since")
        val prayed = stringSetPreferencesKey("prayed")
        val features = stringPreferencesKey("features")
        val read = stringSetPreferencesKey("readNotifs")
        val health = stringPreferencesKey("healthDismissed")
        val journal = stringSetPreferencesKey("journal")
        val deen = booleanPreferencesKey("deen")
        val seen = stringSetPreferencesKey("storiesSeen")
        val skipped = booleanPreferencesKey("signInSkipped")
        val liked = stringSetPreferencesKey("liked")
        val tCount = intPreferencesKey("tCount")
        val tPreset = stringPreferencesKey("tPreset")
        val tTarget = intPreferencesKey("tTarget")
        val tTotal = intPreferencesKey("tTotal")
        val tHist = stringSetPreferencesKey("tHist")
        val qLast = stringPreferencesKey("qLast")
        val qMarks = stringSetPreferencesKey("qMarks")
        val namesFav = stringSetPreferencesKey("namesFav")
        val avatar = intPreferencesKey("avatar")
        val azFav = stringSetPreferencesKey("azFav")
        val azProg = stringSetPreferencesKey("azProg")
        val azDone = stringSetPreferencesKey("azDone")
        val azOwn = stringSetPreferencesKey("azOwn")
        val quiz = stringSetPreferencesKey("namesQuiz")
        val tSkin = intPreferencesKey("tSkin")
        val tSound = booleanPreferencesKey("tSound")
        val tVol = booleanPreferencesKey("tVol")
        val tCustom = stringSetPreferencesKey("tCustom")
    }

    private fun decodeOwn(s: Set<String>?): List<OwnDhikr> = (s ?: emptySet()).mapNotNull { r ->
        val x = r.split('\u001F'); if (x.size < 3) null else x[1].toIntOrNull()?.let { OwnDhikr(x[0], x[2], it) }
    }.sortedBy { it.id }

    private fun decodeJournal(s: Set<String>): List<JournalEntry> = s.mapNotNull { raw ->
        val p = raw.split('\u001F')
        if (p.size < 4) null else runCatching { JournalEntry(p[0].toLong(), LocalDate.parse(p[1]), p[3], p[2].toInt()) }.getOrNull()
    }.sortedByDescending { it.id }

    private fun decode(p: Preferences) = UserData(
        name = p[K.name] ?: "",
        memberSince = p[K.since] ?: 0L,
        prayed = p[K.prayed] ?: emptySet(),
        features = p[K.features]?.split(',')?.filter { it.isNotBlank() } ?: UserData.DEFAULT_FEATURES,
        readNotifs = p[K.read] ?: emptySet(),
        healthDismissedDay = p[K.health] ?: "",
        journal = decodeJournal(p[K.journal] ?: emptySet()),
        deenMode = p[K.deen] ?: false,
        storiesSeen = p[K.seen] ?: emptySet(),
        signInSkipped = p[K.skipped] ?: false,
        likedPosts = p[K.liked] ?: emptySet(),
        tasbihCount = p[K.tCount] ?: 0,
        tasbihPreset = p[K.tPreset] ?: "subhan",
        tasbihTarget = p[K.tTarget] ?: 33,
        tasbihTotal = p[K.tTotal] ?: 0,
        tasbihHistory = (p[K.tHist] ?: emptySet()).mapNotNull { r ->
            val x = r.split('|'); if (x.size < 3) null else runCatching { TasbihSession(x[0].toLong(), x[1], x[2].toInt()) }.getOrNull()
        }.sortedByDescending { it.at },
        quranLast = p[K.qLast] ?: "",
        quranBookmarks = p[K.qMarks] ?: emptySet(),
        namesFav = (p[K.namesFav] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet(),
        avatar = p[K.avatar] ?: 0,
        azkarFav = p[K.azFav] ?: emptySet(),
        azkarProgress = (p[K.azProg] ?: emptySet()).mapNotNull { r ->
            val x = r.split('|'); if (x.size < 3 || x[0] != LocalDate.now().toString()) null
            else x[1] to x[2].split(',').mapNotNull { it.toIntOrNull() }
        }.toMap(),
        azkarDone = p[K.azDone] ?: emptySet(),
        ownAzkar = decodeOwn(p[K.azOwn]),
        namesQuiz = (p[K.quiz] ?: emptySet()).mapNotNull { r ->
            val x = r.split('|'); if (x.size < 3) null else runCatching { QuizResult(x[0].toLong(), x[1].toInt(), x[2].toInt()) }.getOrNull()
        }.sortedByDescending { it.at },
        tasbihSkin = p[K.tSkin] ?: 0,
        tasbihSound = p[K.tSound] ?: false,
        tasbihVolumeKeys = p[K.tVol] ?: true,
        tasbihCustom = decodeOwn(p[K.tCustom]),
    )

    val data: Flow<UserData> = ds.data.map(::decode)

    suspend fun ensureMemberSince() = ds.edit { if (it[K.since] == null) it[K.since] = System.currentTimeMillis() }
    suspend fun setName(n: String) = ds.edit { it[K.name] = n.trim().take(40) }
    suspend fun togglePrayed(d: LocalDate, i: Int) = ds.edit {
        val k = "$d:$i"; val cur = it[K.prayed] ?: emptySet()
        it[K.prayed] = if (k in cur) cur - k else cur + k
    }
    suspend fun markAll(d: LocalDate) = ds.edit { it[K.prayed] = (it[K.prayed] ?: emptySet()) + (0..4).map { i -> "$d:$i" } }
    suspend fun setFeatures(ids: List<String>) = ds.edit { it[K.features] = ids.joinToString(",") }
    suspend fun markRead(ids: Collection<String>) = ds.edit { it[K.read] = (it[K.read] ?: emptySet()) + ids }
    suspend fun dismissHealth(day: String) = ds.edit { it[K.health] = day }
    suspend fun addJournal(text: String, mood: Int, date: LocalDate = LocalDate.now()) = ds.edit {
        val id = System.currentTimeMillis()
        val clean = text.replace('\u001F', ' ').trim()
        if (clean.isNotEmpty()) it[K.journal] = (it[K.journal] ?: emptySet()) + "$id\u001F$date\u001F$mood\u001F$clean"
    }
    suspend fun deleteJournal(id: Long) = ds.edit { p -> p[K.journal] = (p[K.journal] ?: emptySet()).filterNot { it.startsWith("$id\u001F") }.toSet() }
    suspend fun setDeen(b: Boolean) = ds.edit { it[K.deen] = b }
    suspend fun markSeen(key: String) = ds.edit { it[K.seen] = ((it[K.seen] ?: emptySet()) + key).toSortedSet().toList().takeLast(60).toSet() }
    suspend fun skipSignIn() = ds.edit { it[K.skipped] = true }
    suspend fun toggleLike(id: String) = ds.edit { val c = it[K.liked] ?: emptySet(); it[K.liked] = if (id in c) c - id else c + id }

    // ── Tasbih
    suspend fun tasbihTap() = ds.edit { it[K.tCount] = (it[K.tCount] ?: 0) + 1; it[K.tTotal] = (it[K.tTotal] ?: 0) + 1 }
    /** Saves the current count as a finished session (if > 0) and resets the counter. */
    suspend fun tasbihSave() = ds.edit {
        val c = it[K.tCount] ?: 0
        if (c > 0) {
            val rec = "${System.currentTimeMillis()}|${it[K.tPreset] ?: "subhan"}|$c"
            it[K.tHist] = ((it[K.tHist] ?: emptySet()) + rec).sortedByDescending { r -> r.substringBefore('|').toLongOrNull() ?: 0 }.take(100).toSet()
        }
        it[K.tCount] = 0
    }
    suspend fun tasbihUndo() = ds.edit { val c = it[K.tCount] ?: 0; if (c > 0) { it[K.tCount] = c - 1; it[K.tTotal] = ((it[K.tTotal] ?: 1) - 1).coerceAtLeast(0) } }
    suspend fun tasbihPreset(id: String, target: Int) = ds.edit { it[K.tPreset] = id; it[K.tTarget] = target }
    suspend fun tasbihTarget(n: Int) = ds.edit { it[K.tTarget] = n }
    suspend fun clearTasbihHistory() = ds.edit { it[K.tHist] = emptySet() }
    // ── Quran
    suspend fun setQuranLast(sura: Int, ayah: Int) = ds.edit { it[K.qLast] = "$sura:$ayah" }
    suspend fun toggleBookmark(sura: Int, ayah: Int) = ds.edit { val k = "$sura:$ayah"; val c = it[K.qMarks] ?: emptySet(); it[K.qMarks] = if (k in c) c - k else c + k }
    // ── Names / profile
    suspend fun toggleNameFav(i: Int) = ds.edit { val c = it[K.namesFav] ?: emptySet(); val k = i.toString(); it[K.namesFav] = if (k in c) c - k else c + k }
    suspend fun setAvatar(i: Int) = ds.edit { it[K.avatar] = i }
    // ── Azkar
    suspend fun toggleAzkarFav(id: String) = ds.edit { val c = it[K.azFav] ?: emptySet(); it[K.azFav] = if (id in c) c - id else c + id }
    /** Saves today's progress for a session (counts done per item). Older days are dropped. */
    suspend fun setAzkarProgress(sid: String, done: List<Int>) = ds.edit { p ->
        val today = LocalDate.now().toString()
        val keep = (p[K.azProg] ?: emptySet()).filter { it.startsWith("$today|") && !it.startsWith("$today|$sid|") }
        p[K.azProg] = (keep + "$today|$sid|${done.joinToString(",")}").toSet()
    }
    suspend fun azkarComplete(sid: String) = ds.edit { p ->
        val k = "${LocalDate.now()}:$sid"
        p[K.azDone] = ((p[K.azDone] ?: emptySet()) + k).sortedDescending().take(800).toSet()
    }
    suspend fun addOwnAzkar(text: String, count: Int) = ds.edit {
        val clean = text.replace('\u001F', ' ').trim(); if (clean.isEmpty()) return@edit
        it[K.azOwn] = (it[K.azOwn] ?: emptySet()) + "c${System.currentTimeMillis()}\u001F${count.coerceIn(1, 10000)}\u001F$clean"
    }
    suspend fun deleteOwnAzkar(id: String) = ds.edit { p -> p[K.azOwn] = (p[K.azOwn] ?: emptySet()).filterNot { it.startsWith("$id\u001F") }.toSet() }
    suspend fun addQuizResult(score: Int, total: Int) = ds.edit { p ->
        p[K.quiz] = ((p[K.quiz] ?: emptySet()) + "${System.currentTimeMillis()}|$score|$total").sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0 }.take(50).toSet()
    }
    // ── Tasbih upgrades
    suspend fun setTasbihSkin(i: Int) = ds.edit { it[K.tSkin] = i }
    suspend fun setTasbihSound(b: Boolean) = ds.edit { it[K.tSound] = b }
    suspend fun setTasbihVolumeKeys(b: Boolean) = ds.edit { it[K.tVol] = b }
    suspend fun addTasbihCustom(text: String, target: Int) = ds.edit {
        val clean = text.replace('\u001F', ' ').trim(); if (clean.isEmpty()) return@edit
        it[K.tCustom] = (it[K.tCustom] ?: emptySet()) + "t${System.currentTimeMillis()}\u001F${target.coerceIn(0, 100000)}\u001F$clean"
    }
    suspend fun deleteTasbihCustom(id: String) = ds.edit { p -> p[K.tCustom] = (p[K.tCustom] ?: emptySet()).filterNot { it.startsWith("$id\u001F") }.toSet() }
}
