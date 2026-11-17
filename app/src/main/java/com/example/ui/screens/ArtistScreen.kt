package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.AlbumSummary
import com.example.model.ContentLayoutStyle
import com.example.model.DownloadStatus
import com.example.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.viewmodel.ArtistDetailUiState

@Composable
fun ArtistScreen(
    fallbackName: String,
    uiState: ArtistDetailUiState,
    currentTrackId: String?,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenAlbum: (browseId: String, title: String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var showAllAlbumsGrid by rememberSaveable { mutableStateOf(false) }
    var showAllSinglesGrid by rememberSaveable { mutableStateOf(false) }
    var showAllFeaturedGrid by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("artist_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = (uiState as? ArtistDetailUiState.Success)?.data?.name ?: fallbackName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when (uiState) {
                is ArtistDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is ArtistDetailUiState.Error -> {
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
                                onClick = onRetry,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is ArtistDetailUiState.Success -> {
                    val artist = uiState.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("artist_detail_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item(key = "artist_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SubcomposeAsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = "${artist.name} photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize(),
                                        error = {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp),
                                            )
                                        },
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    artist.subscriberCount?.let { subs ->
                                        Text(
                                            text = subs,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (artist.topSongs.isNotEmpty()) {
                                        Button(
                                            onClick = { onPlayTracks(artist.topSongs, 0) },
                                            modifier = Modifier.heightIn(min = 44.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Play Top Songs")
                                        }
                                    }
                                }
                            }
                            if (!artist.description.isNullOrBlank()) {
                                Text(
                                    text = artist.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }

                        if (artist.topSongs.isNotEmpty()) {
                            item(key = "songs_heading") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("artist_songs_heading"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Songs",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text(
                                            text = "${artist.topSongs.size} songs",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                            itemsIndexed(
                                items = artist.topSongs,
                                key = { idx, track -> "artist_song_${idx}_${track.videoId}" },
                            ) { idx, track ->
                                TrackRow(
                                    track = track,
                                    isCurrentTrack = track.videoId == currentTrackId,
                                    onClick = { onPlayTracks(artist.topSongs, idx) },
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    contentLayoutStyle = contentLayoutStyle,
                                    downloadStatus = downloadStates[track.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
                                    trailingContent = {
                                        IconButton(
                                            onClick = { onOpenTrackMenu(track) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("track_more_${track.videoId}"),
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

                        if (artist.albums.isNotEmpty()) {
                            item(key = "albums_section") {
                                ArtistDiscographySection(
                                    title = "Albums",
                                    items = artist.albums,
                                    expanded = showAllAlbumsGrid,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    sectionTestTag = "artist_albums_section",
                                    toggleTestTag = "artist_albums_see_all_button",
                                    onToggleExpanded = { showAllAlbumsGrid = !showAllAlbumsGrid },
                                    onOpenAlbum = onOpenAlbum,
                                )
                            }
                        }

                        if (artist.singles.isNotEmpty()) {
                            item(key = "singles_section") {
                                ArtistDiscographySection(
                                    title = "Singles & EPs",
                                    items = artist.singles,
                                    expanded = showAllSinglesGrid,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    sectionTestTag = "artist_singles_section",
                                    toggleTestTag = "artist_singles_see_all_button",
                                    onToggleExpanded = { showAllSinglesGrid = !showAllSinglesGrid },
                                    onOpenAlbum = onOpenAlbum,
                                )
                            }
                        }

                        if (artist.featuredOn.isNotEmpty()) {
                            item(key = "featured_section") {
                                ArtistDiscographySection(
                                    title = "Featured on",
                                    items = artist.featuredOn,
                                    expanded = showAllFeaturedGrid,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    sectionTestTag = "artist_featured_section",
                                    toggleTestTag = "artist_featured_see_all_button",
                                    onToggleExpanded = { showAllFeaturedGrid = !showAllFeaturedGrid },
                                    onOpenAlbum = onOpenAlbum,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistDiscographySection(
    title: String,
    items: List<AlbumSummary>,
    expanded: Boolean,
    thumbnailCornerRadius: Int,
    sectionTestTag: String,
    toggleTestTag: String,
    onToggleExpanded: () -> Unit,
    onOpenAlbum: (browseId: String, title: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .testTag(sectionTestTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = items.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            if (items.size > 2) {
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(toggleTestTag),
                ) {
                    Text(if (expanded) "Row view" else "See all")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse $title" else "See all $title",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { album ->
                            ArtistAlbumCard(
                                album = album,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                onClick = { onOpenAlbum(album.browseId, album.title) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.browseId }) { album ->
                    ArtistAlbumCard(
                        album = album,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        onClick = { onOpenAlbum(album.browseId, album.title) },
                        modifier = Modifier.width(142.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: AlbumSummary,
    thumbnailCornerRadius: Int = 16,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(thumbnailCornerRadius.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = album.thumbnailUrl,
                contentDescription = "${album.title} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                error = {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp),
                    )
                },
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
