package app.sakina.quran.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.userStore by preferencesDataStore(name = "user")

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
) {
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
    }

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
}
