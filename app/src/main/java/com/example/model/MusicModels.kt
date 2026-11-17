package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
    val thumbnailUrl: String? = null,
    val artistId: String? = null,
    val albumId: String? = null,
) {
    val formattedDuration: String
        get() {
            val total = durationSeconds ?: return ""
            if (total <= 0) return ""
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
}

@Immutable
data class PlaylistSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val songCountText: String? = null,
    val thumbnailUrl: String? = null,
    val isLikedSongs: Boolean = false,
)

@Immutable
data class AlbumSummary(
    val browseId: String,
    val playlistId: String?,
    val title: String,
    val artist: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
)

@Immutable
data class ArtistSummary(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
)

enum class SearchFilterOption(val label: String) {
    SONGS("Songs"),
    VIDEOS("Videos"),
    ALBUMS("Albums"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
}

sealed interface SearchResultItem {
    val key: String

    @Immutable
    data class SongResult(val track: Track) : SearchResultItem {
        override val key: String get() = "song_${track.videoId}"
    }

    @Immutable
    data class AlbumResult(val album: AlbumSummary) : SearchResultItem {
        override val key: String get() = "album_${album.browseId}"
    }

    @Immutable
    data class PlaylistResult(val playlist: PlaylistSummary) : SearchResultItem {
        override val key: String get() = "playlist_${playlist.id}"
    }

    @Immutable
    data class ArtistResult(val artist: ArtistSummary) : SearchResultItem {
        override val key: String get() = "artist_${artist.id}"
    }
}

@Immutable
data class SearchPageResult(
    val items: List<SearchResultItem>,
    val continuationToken: String? = null,
)

@Immutable
data class PlaybackUiState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface HomeCardItem {
    val key: String
    val title: String
    val subtitle: String
    val thumbnailUrl: String?

    @Immutable
    data class TrackCard(
        val track: Track,
        val subtitleOverride: String? = null,
    ) : HomeCardItem {
        override val key: String get() = "home_track_${track.videoId}"
        override val title: String get() = track.title
        override val subtitle: String get() = subtitleOverride ?: track.artist
        override val thumbnailUrl: String? get() = track.thumbnailUrl
    }

    @Immutable
    data class CollectionCard(
        val id: String,
        override val title: String,
        override val subtitle: String,
        override val thumbnailUrl: String?,
        val isAlbum: Boolean,
    ) : HomeCardItem {
        override val key: String get() = "home_collection_${if (isAlbum) "album" else "playlist"}_$id"
    }
}

@Immutable
data class HomeShelf(
    val id: String,
    val title: String,
    val strapline: String? = null,
    val items: List<HomeCardItem>,
)

@Immutable
data class HomeMoodChip(
    val title: String,
    val params: String,
)

@Immutable
data class HomeFeedResult(
    val shelves: List<HomeShelf>,
    val moodChips: List<HomeMoodChip>,
)

@Immutable
data class LyricLine(
    val text: String,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
)

@Immutable
data class LyricsData(
    val lines: List<LyricLine>,
    val source: String? = null,
    val isSynced: Boolean = false,
)

@Immutable
data class ArtistDetailData(
    val browseId: String,
    val name: String,
    val description: String? = null,
    val subscriberCount: String? = null,
    val thumbnailUrl: String? = null,
    val topSongs: List<Track> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val singles: List<AlbumSummary> = emptyList(),
    val featuredOn: List<AlbumSummary> = emptyList(),
)

@Immutable
data class TrackStreamDetails(
    val codec: String,
    val bitrateKbps: Int?,
    val mimeType: String?,
    val contentLengthBytes: Long?,
)

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Countdown(val remainingSeconds: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
}

@Immutable
@kotlinx.serialization.Serializable
data class DownloadedTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val thumbnailUrl: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack(): Track = Track(
        videoId = videoId,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = (durationMs / 1000).toInt(),
        thumbnailUrl = thumbnailUrl,
    )
}

