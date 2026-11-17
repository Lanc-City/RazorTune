package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PlayEvent(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationListenedMs: Long,
    val timestampMs: Long,
)

enum class StatsTimeRange(val label: String) {
    CONTINUOUS("Continuous"),
    ONE_WEEK("1 week"),
    ONE_MONTH("1 month"),
    ALL_TIME("All time"),
}

private val Context.listeningStatsDataStore by preferencesDataStore(name = "razortune_listening_stats")

class ListeningStatsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val writeMutex = Mutex()

    private object Keys {
        val PLAY_EVENTS_JSON = stringPreferencesKey("play_events_json")
    }

    val eventsFlow: Flow<List<PlayEvent>> = context.listeningStatsDataStore.data.map { prefs ->
        decodeEvents(prefs[Keys.PLAY_EVENTS_JSON])
    }

    suspend fun recordPlayEvent(event: PlayEvent) {
        if (event.videoId.isBlank() || event.durationListenedMs <= 0L) return
        writeMutex.withLock {
            context.listeningStatsDataStore.edit { prefs ->
                val current = decodeEvents(prefs[Keys.PLAY_EVENTS_JSON])
                val updated = (current + event).takeLast(MAX_STORED_EVENTS)
                prefs[Keys.PLAY_EVENTS_JSON] = json.encodeToString(updated)
            }
        }
    }

    suspend fun clearStats() {
        writeMutex.withLock {
            context.listeningStatsDataStore.edit { prefs ->
                prefs.remove(Keys.PLAY_EVENTS_JSON)
            }
        }
    }

    private fun decodeEvents(raw: String?): List<PlayEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<PlayEvent>>(raw) }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_STORED_EVENTS = 10_000
    }
}
