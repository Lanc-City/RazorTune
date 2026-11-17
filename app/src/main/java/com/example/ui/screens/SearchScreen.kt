package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.model.SearchBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ContentLayoutStyle
import com.example.model.DownloadStatus
import com.example.model.HomeCardItem
import com.example.model.HomeMoodChip
import com.example.model.HomeShelf
import com.example.model.SearchFilterOption
import com.example.model.SearchResultItem
import com.example.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.SearchUiState
import kotlin.math.roundToInt

@Composable
fun SearchScreen(
    query: String,
    selectedFilter: SearchFilterOption,
    uiState: SearchUiState,
    homeState: HomeUiState,
    currentTrackId: String?,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    searchBarStyle: SearchBarStyle = SearchBarStyle.CLASSIC_BOX,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onQueryChange: (String) -> Unit,
    onSelectFilter: (SearchFilterOption) -> Unit,
    onSubmitSearch: () -> Unit,
    onRefreshHome: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenPlaylistOrAlbum: (id: String, title: String) -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onLoadMore: () -> Unit = {},
    moodChips: List<HomeMoodChip> = emptyList(),
    selectedMood: HomeMoodChip? = null,
    isLoadingMood: Boolean = false,
    onSelectMoodChip: (HomeMoodChip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            isSearchExpanded = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(if (searchBarStyle == SearchBarStyle.FLOATING_BOTTOM) Modifier.padding(bottom = 76.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchBarStyle == SearchBarStyle.HEADER_EXPAND && !isSearchExpanded && query.isBlank()) {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("header_search_expand_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenStats,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("stats_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Listening Stats",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                val showTopSearchBar = searchBarStyle != SearchBarStyle.FLOATING_BOTTOM &&
                    (searchBarStyle != SearchBarStyle.HEADER_EXPAND || isSearchExpanded || query.isNotBlank())

                AnimatedVisibility(visible = showTopSearchBar) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                if (searchBarStyle == SearchBarStyle.HEADER_EXPAND) {
                                    IconButton(
                                        onClick = {
                                            isSearchExpanded = false
                                            onQueryChange("")
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close search",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("search_clear_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                        )
                                    }
                                } else if (searchBarStyle == SearchBarStyle.HEADER_EXPAND) {
                                    IconButton(
                                        onClick = {
                                            isSearchExpanded = false
                                            onQueryChange("")
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Collapse search",
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = when (searchBarStyle) {
                                SearchBarStyle.CLASSIC_BOX -> RoundedCornerShape(14.dp)
                                else -> RoundedCornerShape(50.dp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (searchBarStyle == SearchBarStyle.CAPSULE_PILL) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSubmitSearch()
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input"),
                        )
                    }
                }

                val showFilterChips = query.isNotBlank() || isSearchExpanded
                AnimatedVisibility(visible = showFilterChips) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(SearchFilterOption.entries, key = { it.name }) { option ->
                                val selected = option == selectedFilter
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectFilter(option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("filter_chip_${option.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

            val showHomeFeed = query.isBlank() || uiState is SearchUiState.Idle
            if (showHomeFeed) {
                if (moodChips.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("home_mood_chips_row"),
                    ) {
                        items(moodChips, key = { it.title }) { chip ->
                            val selected = selectedMood?.title == chip.title
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMoodChip(chip) },
                                label = {
                                    Text(
                                        text = chip.title,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                shape = RoundedCornerShape(50.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("mood_chip_${chip.title.lowercase().replace(" ", "_")}"),
                            )
                        }
                    }
                    if (isLoadingMood) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .testTag("home_mood_loading_indicator"),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                HomeFeedContent(
                    homeState = homeState,
                    currentTrackId = currentTrackId,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    contentLayoutStyle = contentLayoutStyle,
                    onRefreshHome = onRefreshHome,
                    onPlayTrack = onPlayTrack,
                    onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                )
            } else {
                when (uiState) {
                    is SearchUiState.Idle -> Unit

                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Searching ${selectedFilter.label.lowercase()}…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is SearchUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Button(
                                    onClick = onSubmitSearch,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("search_retry_button"),
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    is SearchUiState.Success -> {
                        if (uiState.results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No ${selectedFilter.label.lowercase()} matched \"$query\".",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            val allSongs = remember(uiState.results) {
                                uiState.results
                                    .filterIsInstance<SearchResultItem.SongResult>()
                                    .map { it.track }
                            }
                            val resultSpacing = when (contentLayoutStyle) {
                                ContentLayoutStyle.COMPACT -> 2.dp
                                ContentLayoutStyle.STANDARD -> 4.dp
                                ContentLayoutStyle.SHOWCASE -> 8.dp
                            }

                            val listState = rememberLazyListState()
                            val currentOnLoadMore by rememberUpdatedState(onLoadMore)
                            var lastRequestedToken by remember(query, selectedFilter) {
                                mutableStateOf<String?>(null)
                            }
                            val shouldLoadMore by remember(
                                uiState.results.size,
                                uiState.continuationToken,
                                uiState.isLoadingMore,
                            ) {
                                derivedStateOf {
                                    if (
                                        uiState.results.isEmpty() ||
                                        uiState.continuationToken.isNullOrBlank() ||
                                        uiState.isLoadingMore
                                    ) {
                                        false
                                    } else {
                                        val lastVisibleIndex =
                                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                        lastVisibleIndex >= (uiState.results.lastIndex - 3).coerceAtLeast(0)
                                    }
                                }
                            }

                            LaunchedEffect(shouldLoadMore, uiState.continuationToken, uiState.isLoadingMore) {
                                val token = uiState.continuationToken
                                if (shouldLoadMore && !token.isNullOrBlank()) {
                                    if (token != lastRequestedToken) {
                                        lastRequestedToken = token
                                        currentOnLoadMore()
                                    }
                                } else if (!uiState.isLoadingMore) {
                                    lastRequestedToken = null
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("search_results_list"),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(resultSpacing),
                            ) {
                                items(uiState.results, key = { it.key }) { item ->
                                    when (item) {
                                        is SearchResultItem.SongResult -> {
                                            TrackRow(
                                                track = item.track,
                                                isCurrentTrack = item.track.videoId == currentTrackId,
                                                onClick = { onPlayTrack(item.track, allSongs) },
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                contentLayoutStyle = contentLayoutStyle,
                                                downloadStatus = downloadStates[item.track.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = { onOpenTrackMenu(item.track) },
                                                        modifier = Modifier
                                                            .minimumInteractiveComponentSize()
                                                            .testTag("track_more_${item.track.videoId}"),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.MoreVert,
                                                            contentDescription = "Track options",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                },
                                            )
                                        }

                                        is SearchResultItem.AlbumResult -> {
                                            CollectionResultRow(
                                                title = item.album.title,
                                                subtitle = buildString {
                                                    append(item.album.artist)
                                                    item.album.year?.let { append(" • $it") }
                                                },
                                                thumbnailUrl = item.album.thumbnailUrl,
                                                fallbackIcon = Icons.Rounded.Album,
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                contentLayoutStyle = contentLayoutStyle,
                                                onClick = {
                                                    onOpenPlaylistOrAlbum(
                                                        item.album.browseId,
                                                        item.album.title,
                                                    )
                                                },
                                            )
                                        }

                                        is SearchResultItem.PlaylistResult -> {
                                            CollectionResultRow(
                                                title = item.playlist.title,
                                                subtitle = item.playlist.subtitle,
                                                thumbnailUrl = item.playlist.thumbnailUrl,
                                                fallbackIcon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                contentLayoutStyle = contentLayoutStyle,
                                                onClick = {
                                                    onOpenPlaylistOrAlbum(
                                                        item.playlist.id,
                                                        item.playlist.title,
                                                    )
                                                },
                                            )
                                        }

                                        is SearchResultItem.ArtistResult -> {
                                            val rowMinHeight = when (contentLayoutStyle) {
                                                ContentLayoutStyle.COMPACT -> 48.dp
                                                ContentLayoutStyle.STANDARD -> 64.dp
                                                ContentLayoutStyle.SHOWCASE -> 76.dp
                                            }
                                            val avatarSize = when (contentLayoutStyle) {
                                                ContentLayoutStyle.COMPACT -> 40.dp
                                                ContentLayoutStyle.STANDARD -> 52.dp
                                                ContentLayoutStyle.SHOWCASE -> 60.dp
                                            }
                                            val vertPad = when (contentLayoutStyle) {
                                                ContentLayoutStyle.COMPACT -> 4.dp
                                                ContentLayoutStyle.STANDARD -> 8.dp
                                                ContentLayoutStyle.SHOWCASE -> 10.dp
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = rowMinHeight)
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .clickable { onOpenArtist(item.artist.id, item.artist.name) }
                                                    .padding(horizontal = 12.dp, vertical = vertPad)
                                                    .testTag("artist_row_${item.artist.id}"),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(avatarSize)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 20.dp else 24.dp),
                                                    )
                                                    if (!item.artist.thumbnailUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = item.artist.thumbnailUrl,
                                                            contentDescription = "${item.artist.name} photo",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.matchParentSize(),
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.artist.name,
                                                        style = if (contentLayoutStyle == ContentLayoutStyle.SHOWCASE) {
                                                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                        } else if (contentLayoutStyle == ContentLayoutStyle.COMPACT) {
                                                            MaterialTheme.typography.titleSmall
                                                        } else {
                                                            MaterialTheme.typography.titleMedium
                                                        },
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = item.artist.subtitle ?: "Artist",
                                                        style = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) {
                                                            MaterialTheme.typography.bodySmall
                                                        } else {
                                                            MaterialTheme.typography.bodyMedium
                                                        },
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (uiState.isLoadingMore) {
                                    item(key = "search_loading_more_footer") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp)
                                                .testTag("search_loading_more_footer"),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 2.5.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        if (searchBarStyle == SearchBarStyle.FLOATING_BOTTOM) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 720.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        if (it.isNotBlank()) {
                            isSearchExpanded = true
                        }
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("search_clear_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSubmitSearch()
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_floating"),
                )
            }
        }
    }
}

@Composable
private fun HomeFeedContent(
    homeState: HomeUiState,
    currentTrackId: String?,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    onRefreshHome: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenPlaylistOrAlbum: (id: String, title: String) -> Unit,
) {
    when (homeState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_loading_state"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Loading YouTube Music picks…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is HomeUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_error_state"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = homeState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onRefreshHome,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_retry_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }

        is HomeUiState.Success -> {
            if (homeState.shelves.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "No recommendations available right now.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onRefreshHome,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("home_refresh_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
            } else {
                when (contentLayoutStyle) {
                    ContentLayoutStyle.STANDARD -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_carousels_list"),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            items(homeState.shelves, key = { it.id }) { shelf ->
                                HomeShelfSection(
                                    shelf = shelf,
                                    currentTrackId = currentTrackId,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    onPlayTrack = onPlayTrack,
                                    onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                )
                            }
                        }
                    }

                    ContentLayoutStyle.COMPACT,
                    ContentLayoutStyle.SHOWCASE -> {
                        val columns = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 3 else 2
                        val horizontalGap = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 8.dp else 12.dp
                        val rowSpacing = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 10.dp else 14.dp

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_carousels_list"),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(rowSpacing),
                        ) {
                            homeState.shelves.forEach { shelf ->
                                val shelfTracks = shelf.items
                                    .filterIsInstance<HomeCardItem.TrackCard>()
                                    .map { it.track }
                                val chunkedRows = shelf.items.chunked(columns)

                                item(key = "shelf_header_${shelf.id}") {
                                    HomeShelfHeader(
                                        shelf = shelf,
                                        compact = contentLayoutStyle == ContentLayoutStyle.COMPACT,
                                    )
                                }

                                itemsIndexed(
                                    items = chunkedRows,
                                    key = { rowIndex, _ -> "shelf_${shelf.id}_row_$rowIndex" },
                                ) { _, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                                    ) {
                                        rowItems.forEach { item ->
                                            val isCurrent = (item as? HomeCardItem.TrackCard)?.track?.videoId == currentTrackId
                                            HomeGridCard(
                                                item = item,
                                                isCurrentTrack = isCurrent,
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                showcase = contentLayoutStyle == ContentLayoutStyle.SHOWCASE,
                                                onClick = {
                                                    when (item) {
                                                        is HomeCardItem.TrackCard -> {
                                                            val queue = shelfTracks.ifEmpty { listOf(item.track) }
                                                            onPlayTrack(item.track, queue)
                                                        }
                                                        is HomeCardItem.CollectionCard -> {
                                                            onOpenPlaylistOrAlbum(item.id, item.title)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        val remainder = columns - rowItems.size
                                        repeat(remainder) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShelfHeader(
    shelf: HomeShelf,
    compact: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = if (compact) 4.dp else 8.dp)) {
        if (!shelf.strapline.isNullOrBlank()) {
            Text(
                text = shelf.strapline.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = shelf.title,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeShelfSection(
    shelf: HomeShelf,
    currentTrackId: String?,
    thumbnailCornerRadius: Int = 16,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenPlaylistOrAlbum: (id: String, title: String) -> Unit,
) {
    val shelfTracks = remember(shelf.items) {
        shelf.items.filterIsInstance<HomeCardItem.TrackCard>().map { it.track }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeShelfHeader(shelf = shelf)

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(shelf.items, key = { it.key }) { item ->
                HomeCarouselCard(
                    item = item,
                    isCurrentTrack = (item as? HomeCardItem.TrackCard)?.track?.videoId == currentTrackId,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    onClick = {
                        when (item) {
                            is HomeCardItem.TrackCard -> {
                                val queue = shelfTracks.ifEmpty { listOf(item.track) }
                                onPlayTrack(item.track, queue)
                            }
                            is HomeCardItem.CollectionCard -> {
                                onOpenPlaylistOrAlbum(item.id, item.title)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeGridCard(
    item: HomeCardItem,
    isCurrentTrack: Boolean,
    thumbnailCornerRadius: Int,
    showcase: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(thumbnailCornerRadius.dp)
    val fallbackIcon = when (item) {
        is HomeCardItem.TrackCard -> Icons.Rounded.MusicNote
        is HomeCardItem.CollectionCard ->
            if (item.isAlbum) Icons.Rounded.Album else Icons.AutoMirrored.Rounded.PlaylistPlay
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showcase) Modifier.padding(8.dp) else Modifier.padding(bottom = 2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = if (isCurrentTrack) 2.dp else 1.dp,
                        color = if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = cardShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (showcase) 40.dp else 28.dp),
                )
                if (!item.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = "${item.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                when (item) {
                    is HomeCardItem.TrackCard -> {
                        Surface(
                            shape = CircleShape,
                            color = if (isCurrentTrack) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(if (showcase) 8.dp else 6.dp)
                                .size(if (showcase) 32.dp else 24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCurrentTrack) {
                                        Icons.Rounded.GraphicEq
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = if (isCurrentTrack) "Now playing" else "Play track",
                                    tint = if (isCurrentTrack) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(if (showcase) 18.dp else 14.dp),
                                )
                            }
                        }
                    }

                    is HomeCardItem.CollectionCard -> {
                        if (showcase) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = if (item.isAlbum) "Album" else "Playlist",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (showcase) 8.dp else 5.dp))

            Text(
                text = item.title,
                style = if (showcase) {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = if (isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showcase) {
        val containerShape = RoundedCornerShape((thumbnailCornerRadius + 4).coerceIn(12, 24).dp)
        Surface(
            modifier = modifier
                .clip(containerShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = containerShape,
                )
                .clickable(onClick = onClick)
                .testTag("home_card_${item.key}"),
            shape = containerShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .clip(cardShape)
                .clickable(onClick = onClick)
                .testTag("home_card_${item.key}"),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeCarouselCard(
    item: HomeCardItem,
    isCurrentTrack: Boolean,
    thumbnailCornerRadius: Int = 16,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(thumbnailCornerRadius.dp)
    val fallbackIcon = when (item) {
        is HomeCardItem.TrackCard -> Icons.Rounded.MusicNote
        is HomeCardItem.CollectionCard ->
            if (item.isAlbum) Icons.Rounded.Album else Icons.AutoMirrored.Rounded.PlaylistPlay
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
            .testTag("home_card_${item.key}"),
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = if (isCurrentTrack) 2.dp else 1.dp,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = cardShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = "${item.title} artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }

            when (item) {
                is HomeCardItem.TrackCard -> {
                    Surface(
                        shape = CircleShape,
                        color = if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(30.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCurrentTrack) {
                                    Icons.Rounded.GraphicEq
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                contentDescription = if (isCurrentTrack) "Now playing" else "Play track",
                                tint = if (isCurrentTrack) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                is HomeCardItem.CollectionCard -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                    ) {
                        Text(
                            text = if (item.isAlbum) "Album" else "Playlist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CollectionResultRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    onClick: () -> Unit,
) {
    val cornerDp = (thumbnailCornerRadius * 10f / 16f).roundToInt().coerceIn(0, 26).dp
    val thumbShape = RoundedCornerShape(cornerDp)
    val minRowHeight = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 48.dp
        ContentLayoutStyle.STANDARD -> 64.dp
        ContentLayoutStyle.SHOWCASE -> 76.dp
    }
    val thumbSize = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 40.dp
        ContentLayoutStyle.STANDARD -> 52.dp
        ContentLayoutStyle.SHOWCASE -> 60.dp
    }
    val vertPad = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 4.dp
        ContentLayoutStyle.STANDARD -> 8.dp
        ContentLayoutStyle.SHOWCASE -> 10.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minRowHeight)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = vertPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(thumbShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = thumbShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 20.dp else 24.dp),
            )
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "$title artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 1.dp else 3.dp),
        ) {
            Text(
                text = title,
                style = when (contentLayoutStyle) {
                    ContentLayoutStyle.COMPACT -> MaterialTheme.typography.titleSmall
                    ContentLayoutStyle.STANDARD -> MaterialTheme.typography.titleMedium
                    ContentLayoutStyle.SHOWCASE -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
