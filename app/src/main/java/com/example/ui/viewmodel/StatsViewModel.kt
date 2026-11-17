package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RazorTuneApplication
import com.example.data.PlayEvent
import com.example.data.StatsTimeRange
import com.example.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ArtistStat(
    val name: String,
    val songCount: Int,
    val totalTimeMs: Long,
    val thumbnailUrl: String?,
    val topTrack: Track?,
)

data class SongStat(
    val track: Track,
    val playCount: Int,
    val totalTimeMs: Long,
)

data class ArtistShareStat(
    val name: String,
    val totalTimeMs: Long,
    val percentage: Float,
    val colorIndex: Int,
    val isOther: Boolean = false,
)

data class DayStat(
    val dayLabel: String,
    val totalTimeMs: Long,
)

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data object Empty : StatsUiState
    data class Success(
        val totalTimeListenedMs: Long,
        val totalPlays: Int,
        val uniqueSongs: Int,
        val uniqueArtists: Int,
        val favouriteArtist: ArtistStat?,
        val favouriteSong: SongStat?,
        val topArtistsShare: List<ArtistShareStat>,
        val listeningByDay: List<DayStat>,
        val topTracks: List<Track>,
    ) : StatsUiState
}

class StatsViewModel : ViewModel() {
    private val statsStore = RazorTuneApplication.instance.statsStore

    private val _selectedRange = MutableStateFlow(StatsTimeRange.CONTINUOUS)
    val selectedRange: StateFlow<StatsTimeRange> = _selectedRange.asStateFlow()

    val uiState: StateFlow<StatsUiState> = combine(
        statsStore.eventsFlow,
        _selectedRange,
    ) { events, range ->
        computeStatsUiState(events, range)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StatsUiState.Loading,
    )

    fun selectRange(range: StatsTimeRange) {
        _selectedRange.value = range
    }

    fun clearStats() {
        viewModelScope.launch {
            statsStore.clearStats()
        }
    }

    private fun computeStatsUiState(
        allEvents: List<PlayEvent>,
        range: StatsTimeRange,
    ): StatsUiState {
        val now = System.currentTimeMillis()
        val cutoffMs = when (range) {
            StatsTimeRange.ONE_WEEK -> now - 7L * 24L * 60L * 60L * 1000L
            StatsTimeRange.ONE_MONTH -> now - 30L * 24L * 60L * 60L * 1000L
            StatsTimeRange.CONTINUOUS, StatsTimeRange.ALL_TIME -> Long.MIN_VALUE
        }

        val filtered = if (cutoffMs == Long.MIN_VALUE) {
            allEvents
        } else {
            allEvents.filter { it.timestampMs >= cutoffMs }
        }

        if (filtered.isEmpty()) return StatsUiState.Empty

        val totalTimeListenedMs = filtered.sumOf { it.durationListenedMs }
        val totalPlays = filtered.size

        val songGroups = filtered.groupBy { it.videoId }
        val songStats = songGroups.map { (videoId, events) ->
            val latest = events.maxByOrNull { it.timestampMs } ?: events.first()
            val bestThumb = events.asReversed().firstNotNullOfOrNull { it.thumbnailUrl?.takeIf(String::isNotBlank) }
            val bestAlbum = events.asReversed().firstNotNullOfOrNull { it.album?.takeIf(String::isNotBlank) }
            val totalSongMs = events.sumOf { it.durationListenedMs }
            val track = Track(
                videoId = videoId,
                title = latest.title.ifBlank { "Untitled Track" },
                artist = latest.artist.ifBlank { "YouTube Music" },
                album = bestAlbum,
                thumbnailUrl = bestThumb,
            )
            SongStat(
                track = track,
                playCount = events.size,
                totalTimeMs = totalSongMs,
            )
        }.sortedWith(
            compareByDescending<SongStat> { it.playCount }
                .thenByDescending { it.totalTimeMs },
        )

        val uniqueSongs = songStats.size

        val artistGroups = filtered.groupBy { event ->
            event.artist.trim().ifBlank { "Unknown Artist" }
        }
        val artistStats = artistGroups.map { (artistName, events) ->
            val totalArtistMs = events.sumOf { it.durationListenedMs }
            val distinctSongCount = events.distinctBy { it.videoId }.size
            val topArtistSongEventGroup = events.groupBy { it.videoId }.values
                .maxWithOrNull(
                    compareBy<List<PlayEvent>> { it.size }
                        .thenBy { list -> list.sumOf { it.durationListenedMs } },
                )
            val repEvent = topArtistSongEventGroup?.maxByOrNull { it.timestampMs } ?: events.first()
            val bestThumb = topArtistSongEventGroup
                ?.asReversed()
                ?.firstNotNullOfOrNull { it.thumbnailUrl?.takeIf(String::isNotBlank) }
                ?: events.asReversed().firstNotNullOfOrNull { it.thumbnailUrl?.takeIf(String::isNotBlank) }
            val topTrack = Track(
                videoId = repEvent.videoId,
                title = repEvent.title.ifBlank { "Untitled Track" },
                artist = artistName,
                album = repEvent.album,
                thumbnailUrl = bestThumb,
            )
            ArtistStat(
                name = artistName,
                songCount = distinctSongCount,
                totalTimeMs = totalArtistMs,
                thumbnailUrl = bestThumb,
                topTrack = topTrack,
            )
        }.sortedWith(
            compareByDescending<ArtistStat> { it.totalTimeMs }
                .thenByDescending { it.songCount },
        )

        val uniqueArtists = artistStats.size
        val favouriteArtist = artistStats.firstOrNull()
        val favouriteSong = songStats.firstOrNull()

        val topFour = artistStats.take(4)
        val restArtists = artistStats.drop(4)
        val topArtistsShare = buildList {
            topFour.forEachIndexed { index, artist ->
                val pct = if (totalTimeListenedMs > 0L) {
                    (artist.totalTimeMs * 100f) / totalTimeListenedMs.toFloat()
                } else {
                    0f
                }
                add(
                    ArtistShareStat(
                        name = artist.name,
                        totalTimeMs = artist.totalTimeMs,
                        percentage = pct,
                        colorIndex = index,
                        isOther = false,
                    ),
                )
            }
            if (restArtists.isNotEmpty()) {
                val otherMs = restArtists.sumOf { it.totalTimeMs }
                val otherPct = if (totalTimeListenedMs > 0L) {
                    (otherMs * 100f) / totalTimeListenedMs.toFloat()
                } else {
                    0f
                }
                add(
                    ArtistShareStat(
                        name = "Other",
                        totalTimeMs = otherMs,
                        percentage = otherPct,
                        colorIndex = 4,
                        isOther = true,
                    ),
                )
            }
        }

        val dayTotals = LongArray(7)
        val calendar = Calendar.getInstance()
        for (event in filtered) {
            calendar.timeInMillis = event.timestampMs
            val index = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            dayTotals[index] += event.durationListenedMs
        }

        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val listeningByDay = dayLabels.mapIndexed { index, label ->
            DayStat(dayLabel = label, totalTimeMs = dayTotals[index])
        }

        return StatsUiState.Success(
            totalTimeListenedMs = totalTimeListenedMs,
            totalPlays = totalPlays,
            uniqueSongs = uniqueSongs,
            uniqueArtists = uniqueArtists,
            favouriteArtist = favouriteArtist,
            favouriteSong = favouriteSong,
            topArtistsShare = topArtistsShare,
            listeningByDay = listeningByDay,
            topTracks = songStats.map { it.track },
        )
    }
}
