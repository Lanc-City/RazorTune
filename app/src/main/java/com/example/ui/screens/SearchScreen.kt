package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.model.SearchBarStyle

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
    onOpenAccount: () -> Unit = onOpenSettings,
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
    var showProfileDropdown by remember { mutableStateOf(false) }

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

                        // Modern Profile Avatar Button with Custom Dropdown Menu
                        Box {
                            IconButton(
                                onClick = { showProfileDropdown = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("profile_avatar_button"),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountCircle,
                                            contentDescription = "Profile and Account",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showProfileDropdown,
                                onDismissRequest = { showProfileDropdown = false },
                                modifier = Modifier
                                    .width(280.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Sign in to YouTube",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Connect account & sync library",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        showProfileDropdown = false
                                        onOpenAccount()
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Login,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                                DropdownMenuItem(
                                    text = { Text("Statistics", style = MaterialTheme.typography.bodyLarge) },
                                    onClick = {
                                        showProfileDropdown = false
                                        onOpenStats()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.BarChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", style = MaterialTheme.typography.bodyLarge) },
                                    onClick = {
                                        showProfileDropdown = false
                                        onOpenSettings()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = "RazorTune (1.0.0)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "GPL-3.0 Open Source",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = { },
                                    enabled = false,
                                )
                            }
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
                        downloadStates = downloadStates,
                        onRefreshHome = onRefreshHome,
                        onPlayTrack = onPlayTrack,
                        onOpenTrackMenu = onOpenTrackMenu,
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
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onRefreshHome: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
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
                                    contentLayoutStyle = contentLayoutStyle,
                                    downloadStates = downloadStates,
                                    onPlayTrack = onPlayTrack,
                                    onOpenTrackMenu = onOpenTrackMenu,
                                    onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                )
                            }
                        }
                    }

                    ContentLayoutStyle.COMPACT -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_carousels_list"),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            homeState.shelves.forEachIndexed { shelfIndex, shelf ->
                                val shelfTracks = shelf.items
                                    .filterIsInstance<HomeCardItem.TrackCard>()
                                    .map { it.track }

                                if (shelfIndex == 0) {
                                    item(key = "speed_dial_${shelf.id}") {
                                        SpeedDialGridSection(
                                            title = "Speed dial",
                                            items = shelf.items,
                                            currentTrackId = currentTrackId,
                                            thumbnailCornerRadius = thumbnailCornerRadius,
                                            onPlayTrack = { track ->
                                                val queue = shelfTracks.ifEmpty { listOf(track) }
                                                onPlayTrack(track, queue)
                                            },
                                            onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                            onShuffleAll = {
                                                if (shelfTracks.isNotEmpty()) {
                                                    val shuffled = shelfTracks.shuffled()
                                                    onPlayTrack(shuffled.first(), shuffled)
                                                }
                                            },
                                        )
                                    }
                                } else if (shelfTracks.size >= 4) {
                                    item(key = "paged_tracks_${shelf.id}") {
                                        HorizontalStackedTracksSection(
                                            shelf = shelf,
                                            tracks = shelfTracks,
                                            currentTrackId = currentTrackId,
                                            thumbnailCornerRadius = thumbnailCornerRadius,
                                            downloadStates = downloadStates,
                                            onPlayTrack = onPlayTrack,
                                            onOpenTrackMenu = onOpenTrackMenu,
                                        )
                                    }
                                } else {
                                    item(key = "compact_carousel_${shelf.id}") {
                                        HomeShelfSection(
                                            shelf = shelf,
                                            currentTrackId = currentTrackId,
                                            thumbnailCornerRadius = thumbnailCornerRadius,
                                            contentLayoutStyle = contentLayoutStyle,
                                            downloadStates = downloadStates,
                                            onPlayTrack = onPlayTrack,
                                            onOpenTrackMenu = onOpenTrackMenu,
                                            onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ContentLayoutStyle.SHOWCASE -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_carousels_list"),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            homeState.shelves.forEachIndexed { shelfIndex, shelf ->
                                val shelfTracks = shelf.items
                                    .filterIsInstance<HomeCardItem.TrackCard>()
                                    .map { it.track }

                                if (shelfIndex == 0) {
                                    item(key = "hero_spotlight_${shelf.id}") {
                                        HeroSpotlightRow(
                                            items = shelf.items,
                                            currentTrackId = currentTrackId,
                                            thumbnailCornerRadius = (thumbnailCornerRadius + 8).coerceIn(16, 28),
                                            onPlayTrack = { track ->
                                                val queue = shelfTracks.ifEmpty { listOf(track) }
                                                onPlayTrack(track, queue)
                                            },
                                            onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                        )
                                    }
                                } else if (shelfIndex == 1) {
                                    item(key = "circular_shelf_${shelf.id}") {
                                        CircularSpotlightSection(
                                            shelf = shelf,
                                            currentTrackId = currentTrackId,
                                            onPlayTrack = { track ->
                                                val queue = shelfTracks.ifEmpty { listOf(track) }
                                                onPlayTrack(track, queue)
                                            },
                                            onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
                                        )
                                    }
                                } else {
                                    item(key = "showcase_carousel_${shelf.id}") {
                                        HomeShelfSection(
                                            shelf = shelf,
                                            currentTrackId = currentTrackId,
                                            thumbnailCornerRadius = thumbnailCornerRadius,
                                            contentLayoutStyle = contentLayoutStyle,
                                            downloadStates = downloadStates,
                                            onPlayTrack = onPlayTrack,
                                            onOpenTrackMenu = onOpenTrackMenu,
                                            onOpenPlaylistOrAlbum = onOpenPlaylistOrAlbum,
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

@Composable
private fun SpeedDialGridSection(
    title: String,
    items: List<HomeCardItem>,
    currentTrackId: String?,
    thumbnailCornerRadius: Int,
    onPlayTrack: (Track) -> Unit,
    onOpenPlaylistOrAlbum: (String, String) -> Unit,
    onShuffleAll: () -> Unit,
) {
    val gridItems = items.take(8)
    val rows = gridItems.chunked(3)
    val tileShape = RoundedCornerShape(thumbnailCornerRadius.coerceIn(8, 20).dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { item ->
                        val isCurrent = (item as? HomeCardItem.TrackCard)?.track?.videoId == currentTrackId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(tileShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (isCurrent) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tileShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    when (item) {
                                        is HomeCardItem.TrackCard -> onPlayTrack(item.track)
                                        is HomeCardItem.CollectionCard -> onOpenPlaylistOrAlbum(item.id, item.title)
                                    }
                                },
                        ) {
                            if (!item.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            0.45f to androidx.compose.ui.graphics.Color.Transparent,
                                            1.0f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f),
                                        )
                                    ),
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = androidx.compose.ui.graphics.Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }

                    val remaining = 3 - rowItems.size
                    repeat(remaining) { slotIdx ->
                        val isLastSlot = (rowIndex == rows.lastIndex && slotIdx == remaining - 1)
                        if (isLastSlot && gridItems.size >= 6) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(tileShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable(onClick = onShuffleAll),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    modifier = Modifier.size(44.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        DiceDot()
                                        DiceDot()
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        DiceDot()
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        DiceDot()
                                        DiceDot()
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceDot() {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSecondaryContainer),
    )
}

@Composable
private fun HorizontalStackedTracksSection(
    shelf: HomeShelf,
    tracks: List<Track>,
    currentTrackId: String?,
    thumbnailCornerRadius: Int,
    downloadStates: Map<String, DownloadStatus>,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
) {
    val trackColumns = remember(tracks) { tracks.chunked(4) }

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeShelfHeader(shelf = shelf)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp),
        ) {
            itemsIndexed(trackColumns) { _, columnTracks ->
                Column(
                    modifier = Modifier.width(310.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    columnTracks.forEach { track ->
                        TrackRow(
                            track = track,
                            isCurrentTrack = track.videoId == currentTrackId,
                            onClick = { onPlayTrack(track, tracks) },
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            contentLayoutStyle = ContentLayoutStyle.COMPACT,
                            downloadStatus = downloadStates[track.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
                            trailingContent = {
                                IconButton(
                                    onClick = { onOpenTrackMenu(track) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
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
                }
            }
        }
    }
}

@Composable
private fun HeroSpotlightRow(
    items: List<HomeCardItem>,
    currentTrackId: String?,
    thumbnailCornerRadius: Int,
    onPlayTrack: (Track) -> Unit,
    onOpenPlaylistOrAlbum: (String, String) -> Unit,
) {
    val cardShape = RoundedCornerShape(thumbnailCornerRadius.dp)
    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        val item = items[page]
        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absoluteOffset = offset.absoluteValue

        // Dynamically scale width smoothly from 256.dp (active/centered) down to 80.dp (side peek)
        val cardWidth = androidx.compose.ui.unit.lerp(256.dp, 80.dp, absoluteOffset.coerceIn(0f, 1f))
        val isCurrent = (item as? HomeCardItem.TrackCard)?.track?.videoId == currentTrackId

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(256.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = cardShape,
                )
                .clickable {
                    when (item) {
                        is HomeCardItem.TrackCard -> onPlayTrack(item.track)
                        is HomeCardItem.CollectionCard -> onOpenPlaylistOrAlbum(item.id, item.title)
                    }
                },
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.4f to androidx.compose.ui.graphics.Color.Transparent,
                            1.0f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
                        )
                    ),
            )

            // Fade title out slightly if it's compressed on the side
            val contentAlpha = (1f - (absoluteOffset * 0.7f)).coerceIn(0.2f, 1f)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = contentAlpha * 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CircularSpotlightSection(
    shelf: HomeShelf,
    currentTrackId: String?,
    onPlayTrack: (Track) -> Unit,
    onOpenPlaylistOrAlbum: (String, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeShelfHeader(shelf = shelf)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 12.dp),
        ) {
            items(shelf.items, key = { "circle_${it.key}" }) { item ->
                val isCurrent = (item as? HomeCardItem.TrackCard)?.track?.videoId == currentTrackId
                Column(
                    modifier = Modifier
                        .width(124.dp)
                        .clickable {
                            when (item) {
                                is HomeCardItem.TrackCard -> onPlayTrack(item.track)
                                is HomeCardItem.CollectionCard -> onOpenPlaylistOrAlbum(item.id, item.title)
                            }
                        },
                    horizontalAlignment = Alignment.Start,
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!item.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenPlaylistOrAlbum: (id: String, title: String) -> Unit,
) {
    val shelfTracks = remember(shelf.items) {
        shelf.items.filterIsInstance<HomeCardItem.TrackCard>().map { it.track }
    }

    val isVerticalListShelf = shelf.title.contains("quick picks", ignoreCase = true) ||
            shelf.title.contains("keep listening", ignoreCase = true) ||
            shelf.title.contains("listen again", ignoreCase = true)

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeShelfHeader(shelf = shelf)

        Spacer(modifier = Modifier.height(10.dp))

        if (isVerticalListShelf && shelfTracks.isNotEmpty() && contentLayoutStyle == ContentLayoutStyle.STANDARD) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                shelf.items.filterIsInstance<HomeCardItem.TrackCard>().forEach { cardItem ->
                    TrackRow(
                        track = cardItem.track,
                        isCurrentTrack = cardItem.track.videoId == currentTrackId,
                        onClick = { onPlayTrack(cardItem.track, shelfTracks) },
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        contentLayoutStyle = contentLayoutStyle,
                        downloadStatus = downloadStates[cardItem.track.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
                        trailingContent = {
                            IconButton(
                                onClick = { onOpenTrackMenu(cardItem.track) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("track_more_${cardItem.track.videoId}"),
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
            }
        } else {
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