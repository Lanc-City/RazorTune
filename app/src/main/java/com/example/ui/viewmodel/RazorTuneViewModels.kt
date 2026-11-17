package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RazorTuneApplication
import com.example.model.ArtistDetailData
import com.example.model.ArtworkPaletteColors
import com.example.model.ContentLayoutStyle
import com.example.model.DarkThemeMode
import com.example.model.DownloadStatus
import com.example.model.DownloadedTrack
import com.example.model.HomeShelf
import com.example.model.HomeMoodChip
import com.example.model.LyricsData
import com.example.model.MiniPlayerStyle
import com.example.model.NavBarStyle
import com.example.model.NowPlayingStyle
import com.example.model.PlayerBackgroundStyle
import com.example.model.PlayerButtonsLayout
import com.example.model.PlayerDesignStyle
import com.example.model.PlayerSliderStyle
import com.example.model.PlaylistSummary
import com.example.model.SearchFilterOption
import com.example.model.SearchResultItem
import com.example.model.SearchBarStyle
import com.example.model.ThemeSettings
import com.example.model.Track
import com.example.model.TrackStreamDetails
import com.example.model.VisualizerStyle
import com.example.ui.theme.ArtworkPaletteExtractor
import com.metrolist.innertubex.models.AccountInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val results: List<SearchResultItem>,
        val continuationToken: String? = null,
        val isLoadingMore: Boolean = false,
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val shelves: List<HomeShelf>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class SearchViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val repository = app.repository
    private val authStore = app.authStore

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilterOption.SONGS)
    val selectedFilter: StateFlow<SearchFilterOption> = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var homeJob: Job? = null

    init {
        viewModelScope.launch {
            combine(app.isAuthInitialized, authStore.cookieFlow) { initialized, cookie ->
                initialized to cookie
            }
                .filter { (initialized, _) -> initialized }
                .map { (_, cookie) -> cookie }
                .distinctUntilChanged()
                .collect { cookie ->
                    repository.setCookie(cookie)
                    loadHomeFeed()
                }
        }
    }

    private val _moodChips = MutableStateFlow<List<HomeMoodChip>>(emptyList())
    val moodChips: StateFlow<List<HomeMoodChip>> = _moodChips.asStateFlow()

    private val _selectedMood = MutableStateFlow<HomeMoodChip?>(null)
    val selectedMood: StateFlow<HomeMoodChip?> = _selectedMood.asStateFlow()

    private val _isLoadingMood = MutableStateFlow(false)
    val isLoadingMood: StateFlow<Boolean> = _isLoadingMood.asStateFlow()

    private var moodJob: Job? = null

    fun refreshHome() {
        loadHomeFeed(_selectedMood.value?.params)
    }

    private fun loadHomeFeed(params: String? = null) {
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            if (params == null) {
                _homeState.value = HomeUiState.Loading
            } else {
                _isLoadingMood.value = true
            }
            try {
                val feedResult = repository.fetchHomeFeed(params)
                _moodChips.value = feedResult.moodChips
                _homeState.value = HomeUiState.Success(feedResult.shelves)
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                if (params == null) {
                    _homeState.value = HomeUiState.Error(
                        err.localizedMessage ?: "Couldn't load YouTube Music home feed.",
                    )
                }
            } finally {
                _isLoadingMood.value = false
            }
        }
    }

    fun selectMoodChip(chip: HomeMoodChip) {
        if (_selectedMood.value?.title == chip.title) {
            _selectedMood.value = null
            loadHomeFeed(null)
        } else {
            _selectedMood.value = chip
            moodJob?.cancel()
            moodJob = viewModelScope.launch {
                _isLoadingMood.value = true
                try {
                    val feedResult = repository.fetchHomeFeed(chip.params)
                    _homeState.value = HomeUiState.Success(feedResult.shelves)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    // Keep existing shelves on failure
                } finally {
                    _isLoadingMood.value = false
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        loadMoreJob?.cancel()
        val trimmed = newQuery.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            delay(400)
            executeSearch(trimmed, _selectedFilter.value)
        }
    }

    fun selectFilter(filter: SearchFilterOption) {
        if (_selectedFilter.value == filter) return
        _selectedFilter.value = filter
        loadMoreJob?.cancel()
        if (_query.value.isNotBlank()) {
            submitSearch()
        }
    }

    fun submitSearch() {
        val currentQuery = _query.value.trim()
        searchJob?.cancel()
        loadMoreJob?.cancel()
        if (currentQuery.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        val activeFilter = _selectedFilter.value
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            executeSearch(currentQuery, activeFilter)
        }
    }

    fun loadMoreResults() {
        val currentState = _uiState.value as? SearchUiState.Success ?: return
        val token = currentState.continuationToken?.takeIf { it.isNotBlank() } ?: return
        if (currentState.isLoadingMore) return

        val currentQuery = _query.value.trim()
        val activeFilter = _selectedFilter.value

        _uiState.update { state ->
            if (state is SearchUiState.Success && state.continuationToken == token && !state.isLoadingMore) {
                state.copy(isLoadingMore = true)
            } else {
                state
            }
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            try {
                val nextPage = repository.search(
                    query = currentQuery,
                    filter = activeFilter,
                    continuationToken = token,
                )
                _uiState.update { state ->
                    if (state !is SearchUiState.Success || state.continuationToken != token) {
                        state
                    } else {
                        val existingKeys = state.results.mapTo(HashSet(state.results.size)) { it.key }
                        val newItems = nextPage.items
                            .distinctBy { it.key }
                            .filter { it.key !in existingKeys }
                        val nextToken = nextPage.continuationToken
                            ?.takeIf { it.isNotBlank() && it != token && newItems.isNotEmpty() }
                        state.copy(
                            results = state.results + newItems,
                            continuationToken = nextToken,
                            isLoadingMore = false,
                        )
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                _uiState.update { state ->
                    if (state is SearchUiState.Success && state.continuationToken == token) {
                        state.copy(isLoadingMore = false)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private suspend fun executeSearch(currentQuery: String, activeFilter: SearchFilterOption) {
        try {
            val page = repository.search(
                query = currentQuery,
                filter = activeFilter,
                continuationToken = null,
            )
            _uiState.value = SearchUiState.Success(
                results = page.items.distinctBy { it.key },
                continuationToken = page.continuationToken,
                isLoadingMore = false,
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (err: Exception) {
            _uiState.value = SearchUiState.Error(
                err.localizedMessage ?: "Search failed. Check your connection and try again.",
            )
        }
    }
}

sealed interface PlaylistsUiState {
    data object SignedOut : PlaylistsUiState
    data object Loading : PlaylistsUiState
    data class Success(val playlists: List<PlaylistSummary>) : PlaylistsUiState
    data class Error(val message: String) : PlaylistsUiState
}

class PlaylistsViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val repository = app.repository
    private val authStore = app.authStore

    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.SignedOut)
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(app.isAuthInitialized, authStore.cookieFlow) { initialized, cookie ->
                initialized to cookie
            }.collect { (initialized, cookie) ->
                if (!initialized) return@collect
                repository.setCookie(cookie)
                if (!hasSapCookie(cookie)) {
                    _uiState.value = PlaylistsUiState.SignedOut
                } else {
                    loadPlaylists()
                }
            }
        }
    }

    fun refresh() {
        if (!repository.isSignedIn()) {
            _uiState.value = PlaylistsUiState.SignedOut
            return
        }
        viewModelScope.launch {
            loadPlaylists()
        }
    }

    private suspend fun loadPlaylists() {
        _uiState.value = PlaylistsUiState.Loading
        runCatching {
            repository.syncAccountContext()
            repository.fetchLibraryPlaylists()
        }.onSuccess { playlists ->
            _uiState.value = PlaylistsUiState.Success(playlists)
        }.onFailure { err ->
            _uiState.value = PlaylistsUiState.Error(
                err.localizedMessage ?: "Couldn't sync playlists from YouTube Music.",
            )
        }
    }
}

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Success(
        val title: String,
        val subtitle: String?,
        val thumbnailUrl: String?,
        val tracks: List<Track>,
    ) : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
}

class PlaylistDetailViewModel : ViewModel() {
    private val repository = RazorTuneApplication.instance.repository

    private val _uiState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private var loadedId: String? = null

    fun load(playlistId: String, fallbackTitle: String) {
        if (loadedId == playlistId && _uiState.value is PlaylistDetailUiState.Success) return
        loadedId = playlistId
        viewModelScope.launch {
            _uiState.value = PlaylistDetailUiState.Loading
            runCatching {
                repository.fetchPlaylistTracks(playlistId)
            }.onSuccess { detail ->
                _uiState.value = PlaylistDetailUiState.Success(
                    title = detail.title ?: fallbackTitle,
                    subtitle = detail.subtitle,
                    thumbnailUrl = detail.thumbnailUrl,
                    tracks = detail.tracks,
                )
            }.onFailure { err ->
                _uiState.value = PlaylistDetailUiState.Error(
                    message = err.localizedMessage ?: "Failed to load tracks.",
                )
            }
        }
    }

    fun reload(playlistId: String, fallbackTitle: String) {
        loadedId = null
        load(playlistId, fallbackTitle)
    }
}

data class AccountUiState(
    val isSignedIn: Boolean = false,
    val isLoadingProfile: Boolean = false,
    val accountInfo: AccountInfo? = null,
    val statusMessage: String? = null,
)

class AccountViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val repository = app.repository
    private val authStore = app.authStore

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authStore.cookieFlow.collect { cookie ->
                val signedIn = hasSapCookie(cookie)
                repository.setCookie(cookie)
                if (signedIn) {
                    _uiState.update { it.copy(isSignedIn = true, isLoadingProfile = true) }
                    val profile = repository.syncAccountContext()
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            isLoadingProfile = false,
                            accountInfo = profile,
                        )
                    }
                } else {
                    _uiState.value = AccountUiState(isSignedIn = false)
                }
            }
        }
    }

    fun onCookieCaptured(cookie: String) {
        if (!hasSapCookie(cookie)) return
        if (_uiState.value.isSignedIn) return
        viewModelScope.launch {
            repository.setCookie(cookie)
            _uiState.update {
                it.copy(
                    isSignedIn = true,
                    isLoadingProfile = true,
                    statusMessage = "YouTube Music session synced.",
                )
            }
            val profile = repository.syncAccountContext()
            authStore.saveCookie(cookie)
            _uiState.update {
                it.copy(
                    isLoadingProfile = false,
                    accountInfo = profile,
                )
            }
        }
    }

    fun signOut(onCookiesCleared: () -> Unit) {
        viewModelScope.launch {
            onCookiesCleared()
            repository.setCookie(null)
            authStore.clearCookie()
            _uiState.value = AccountUiState(
                isSignedIn = false,
                statusMessage = "Signed out of YouTube Music.",
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

class ThemeViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val themeStore = app.themeStore
    private val playbackController = app.playbackController

    val settings: StateFlow<ThemeSettings> = themeStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeSettings(),
        )

    private val _artworkColors = MutableStateFlow<ArtworkPaletteColors?>(null)
    val artworkColors: StateFlow<ArtworkPaletteColors?> = _artworkColors.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.playbackState
                .map { it.currentTrack?.thumbnailUrl }
                .distinctUntilChanged()
                .collect { thumbnailUrl ->
                    _artworkColors.value = ArtworkPaletteExtractor.extract(app, thumbnailUrl)
                }
        }
    }

    fun setDynamicArtworkTheme(enabled: Boolean) {
        viewModelScope.launch { themeStore.setDynamicArtworkTheme(enabled) }
    }

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { themeStore.setDarkThemeMode(mode) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { themeStore.setPureBlack(enabled) }
    }

    fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle) {
        viewModelScope.launch { themeStore.setPlayerBackgroundStyle(style) }
    }

    fun setLiquidGlass(enabled: Boolean) {
        viewModelScope.launch { themeStore.setLiquidGlass(enabled) }
    }

    fun setGlassIntensity(intensity: Float) {
        viewModelScope.launch { themeStore.setGlassIntensity(intensity) }
    }

    fun setPlayerButtonsLayout(layout: PlayerButtonsLayout) {
        viewModelScope.launch { themeStore.setPlayerButtonsLayout(layout) }
    }

    fun setNowPlayingStyle(style: NowPlayingStyle) {
        viewModelScope.launch { themeStore.setNowPlayingStyle(style) }
    }

    fun setPlayerDesignStyle(style: PlayerDesignStyle) {
        viewModelScope.launch { themeStore.setPlayerDesignStyle(style) }
    }

    fun setPlayerSliderStyle(style: PlayerSliderStyle) {
        viewModelScope.launch { themeStore.setPlayerSliderStyle(style) }
    }

    fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        viewModelScope.launch { themeStore.setMiniPlayerStyle(style) }
    }

    fun setNavBarStyle(style: NavBarStyle) {
        viewModelScope.launch { themeStore.setNavBarStyle(style) }
    }

    fun setContentLayoutStyle(style: ContentLayoutStyle) {
        viewModelScope.launch { themeStore.setContentLayoutStyle(style) }
    }

    fun setSearchBarStyle(style: SearchBarStyle) {
        viewModelScope.launch { themeStore.setSearchBarStyle(style) }
    }

    fun setMiniPlayerCornerRadiusDp(radius: Int) {
        viewModelScope.launch { themeStore.setMiniPlayerCornerRadiusDp(radius) }
    }

    fun setMiniPlayerShowLike(show: Boolean) {
        viewModelScope.launch { themeStore.setMiniPlayerShowLike(show) }
    }

    fun setMiniPlayerShowPrevious(show: Boolean) {
        viewModelScope.launch { themeStore.setMiniPlayerShowPrevious(show) }
    }

    fun setMiniPlayerSwipeToSkip(swipe: Boolean) {
        viewModelScope.launch { themeStore.setMiniPlayerSwipeToSkip(swipe) }
    }

    fun setThumbnailCornerRadius(radius: Int) {
        viewModelScope.launch { themeStore.setThumbnailCornerRadius(radius) }
    }

    fun setShowSpectralVisualizer(show: Boolean) {
        viewModelScope.launch { themeStore.setShowSpectralVisualizer(show) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch { themeStore.setVisualizerStyle(style) }
    }
}

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Loaded(val videoId: String, val lyrics: LyricsData) : LyricsUiState
    data class Unavailable(val videoId: String, val reason: String) : LyricsUiState
}

sealed interface TrackStreamDetailsUiState {
    data object Loading : TrackStreamDetailsUiState
    data class Loaded(val details: TrackStreamDetails) : TrackStreamDetailsUiState
    data class Error(val message: String) : TrackStreamDetailsUiState
}

class TrackActionsViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val repository = app.repository
    private val downloadManager = app.downloadManager
    private val playbackController = app.playbackController

    val downloadStates: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStates
    val downloadProgress: StateFlow<Map<String, Float>> = downloadManager.downloadProgress
    val downloadedTracks: StateFlow<List<DownloadedTrack>> = downloadManager.downloadedTracks

    val likedVideoIds: StateFlow<Set<String>> = playbackController.likedVideoIds

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _streamDetailsState = MutableStateFlow<TrackStreamDetailsUiState>(TrackStreamDetailsUiState.Loading)
    val streamDetailsState: StateFlow<TrackStreamDetailsUiState> = _streamDetailsState.asStateFlow()

    private var lyricsJob: Job? = null

    fun toggleLike(track: Track, onFeedback: (String) -> Unit) {
        val currentlyLiked = playbackController.isTrackLiked(track.videoId)
        val targetLike = !currentlyLiked
        viewModelScope.launch {
            runCatching {
                repository.setLikeStatus(track.videoId, targetLike)
            }.onSuccess { liked ->
                playbackController.updateTrackLikedState(track.videoId, liked)
                onFeedback(if (liked) "Added \"${track.title}\" to Liked Music" else "Removed from Liked Music")
            }.onFailure { err ->
                onFeedback(err.localizedMessage ?: "Couldn't update like status")
            }
        }
    }

    fun addToLibrary(track: Track, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.setLikeStatus(track.videoId, true)
            }.onSuccess {
                playbackController.updateTrackLikedState(track.videoId, true)
                onFeedback("Added \"${track.title}\" to your library")
            }.onFailure { err ->
                onFeedback(err.localizedMessage ?: "Sign in on Settings > Account & Sync to add to library")
            }
        }
    }

    fun addToPlaylist(
        playlist: PlaylistSummary,
        track: Track,
        onFeedback: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addTrackToPlaylist(playlist.id, track.videoId)
            }.onSuccess {
                onFeedback("Added \"${track.title}\" to ${playlist.title}")
            }.onFailure { err ->
                onFeedback(err.localizedMessage ?: "Couldn't add track to ${playlist.title}")
            }
        }
    }

    fun downloadOrRemove(track: Track, onFeedback: (String) -> Unit) {
        when (downloadManager.getStatus(track.videoId)) {
            DownloadStatus.DOWNLOADING -> {
                onFeedback("Downloading \"${track.title}\"…")
            }
            DownloadStatus.DOWNLOADED -> {
                downloadManager.removeDownload(track.videoId)
                onFeedback("Removed downloaded audio for \"${track.title}\"")
            }
            DownloadStatus.NOT_DOWNLOADED, DownloadStatus.FAILED -> {
                onFeedback("Downloading \"${track.title}\" for offline playback…")
                downloadManager.downloadTrack(track) { res ->
                    res.onSuccess {
                        onFeedback("Downloaded \"${track.title}\"")
                    }.onFailure { err ->
                        onFeedback(err.localizedMessage ?: "Download failed")
                    }
                }
            }
        }
    }

    fun downloadAll(tracks: List<Track>, onFeedback: (String) -> Unit) {
        val pending = tracks.filter { downloadManager.getStatus(it.videoId) != DownloadStatus.DOWNLOADED }
        if (pending.isEmpty()) {
            onFeedback("All tracks in this playlist are already downloaded")
            return
        }
        onFeedback("Downloading ${pending.size} tracks for offline playback…")
        downloadManager.downloadTracks(
            tracks = pending,
            onAllComplete = { successCount, failedCount ->
                val summary = if (failedCount == 0) {
                    "Downloaded $successCount tracks"
                } else {
                    "Downloaded $successCount tracks ($failedCount failed)"
                }
                onFeedback(summary)
            },
        )
    }

    fun loadLyricsForTrack(videoId: String) {
        val current = _lyricsState.value
        if (current is LyricsUiState.Loaded && current.videoId == videoId) return
        if (current is LyricsUiState.Unavailable && current.videoId == videoId) return

        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            runCatching {
                repository.fetchLyrics(videoId)
            }.onSuccess { data ->
                _lyricsState.value = if (data != null && data.lines.isNotEmpty()) {
                    LyricsUiState.Loaded(videoId, data)
                } else {
                    LyricsUiState.Unavailable(videoId, "Lyrics aren't available for this track.")
                }
            }.onFailure {
                _lyricsState.value = LyricsUiState.Unavailable(videoId, "Couldn't load lyrics right now.")
            }
        }
    }

    fun loadStreamDetails(track: Track) {
        viewModelScope.launch {
            _streamDetailsState.value = TrackStreamDetailsUiState.Loading
            runCatching {
                val quality = RazorTuneApplication.instance.performanceStore.settingsFlow.first().audioQuality
                repository.fetchStreamDetails(track, quality)
            }.onSuccess { details ->
                _streamDetailsState.value = TrackStreamDetailsUiState.Loaded(details)
            }.onFailure { err ->
                _streamDetailsState.value = TrackStreamDetailsUiState.Error(
                    err.localizedMessage ?: "Stream metadata unavailable",
                )
            }
        }
    }

    fun resolveArtist(
        track: Track,
        onResolved: (artistId: String, artistName: String) -> Unit,
        onUnavailable: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val id = runCatching { repository.resolveArtistIdForTrack(track) }.getOrNull()
            if (!id.isNullOrBlank()) {
                onResolved(id, track.artist)
            } else {
                onUnavailable("Artist page unavailable for ${track.artist}")
            }
        }
    }

    fun resolveAlbum(
        track: Track,
        onResolved: (albumId: String, albumTitle: String) -> Unit,
        onUnavailable: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val id = runCatching { repository.resolveAlbumBrowseIdForTrack(track) }.getOrNull()
            if (!id.isNullOrBlank()) {
                onResolved(id, track.album ?: track.title)
            } else {
                onUnavailable("Album page unavailable for ${track.title}")
            }
        }
    }
}

sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    data class Success(val data: ArtistDetailData) : ArtistDetailUiState
    data class Error(val message: String) : ArtistDetailUiState
}

class ArtistDetailViewModel : ViewModel() {
    private val repository = RazorTuneApplication.instance.repository

    private val _uiState = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private var loadedArtistId: String? = null

    fun load(artistId: String) {
        if (loadedArtistId == artistId && _uiState.value is ArtistDetailUiState.Success) return
        loadedArtistId = artistId
        viewModelScope.launch {
            _uiState.value = ArtistDetailUiState.Loading
            runCatching {
                repository.fetchArtistDetails(artistId)
            }.onSuccess { data ->
                _uiState.value = ArtistDetailUiState.Success(data)
            }.onFailure { err ->
                _uiState.value = ArtistDetailUiState.Error(
                    err.localizedMessage ?: "Failed to load artist details.",
                )
            }
        }
    }

    fun reload(artistId: String) {
        loadedArtistId = null
        load(artistId)
    }
}

class StorageViewModel : ViewModel() {
    private val app = RazorTuneApplication.instance
    private val downloadManager = app.downloadManager
    private val storageStore = app.storageStore

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = downloadManager.downloadedTracks
    val totalDownloadedBytes: StateFlow<Long> = downloadManager.totalDownloadedBytes
    val downloadOverWifiOnly: StateFlow<Boolean> = storageStore.downloadOverWifiOnlyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val cacheBytes = withContext(Dispatchers.IO) {
                downloadManager.refreshExistingDownloads()
                downloadManager.calculateCacheSizeBytes()
            }
            _cacheSizeBytes.value = cacheBytes
        }
    }

    fun setDownloadOverWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            storageStore.setDownloadOverWifiOnly(enabled)
        }
    }

    fun clearTemporaryCache(onCleared: (Long) -> Unit) {
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) {
                downloadManager.clearTemporaryCache()
            }
            val updatedCache = withContext(Dispatchers.IO) {
                downloadManager.calculateCacheSizeBytes()
            }
            _cacheSizeBytes.value = updatedCache
            onCleared(freed)
        }
    }

    fun removeDownloadedTrack(videoId: String) {
        downloadManager.removeDownload(videoId)
    }

    fun deleteAllDownloads(onComplete: () -> Unit) {
        downloadManager.deleteAllDownloads()
        onComplete()
    }
}

private fun hasSapCookie(cookie: String?): Boolean {
    if (cookie.isNullOrBlank()) return false
    return cookie.contains("SAPISID=") || cookie.contains("__Secure-3PAPISID=")
}
