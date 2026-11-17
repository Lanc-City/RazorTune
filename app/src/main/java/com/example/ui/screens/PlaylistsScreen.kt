package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.OfflineDownloadManager
import com.example.model.ContentLayoutStyle
import com.example.model.DownloadStatus
import com.example.model.DownloadedTrack
import com.example.model.PlaylistSummary
import com.example.model.Track
import com.example.ui.components.StudioEmptyCard
import com.example.ui.components.TrackRow
import com.example.ui.viewmodel.PlaylistsUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    uiState: PlaylistsUiState,
    downloadedTracks: List<DownloadedTrack> = emptyList(),
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    currentTrackId: String? = null,
    onRefresh: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit = { _, _ -> },
    onOpenTrackMenu: (Track) -> Unit = {},
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Library, 1 = Downloaded

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
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.nav_playlists),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Synced Library & Offline Downloads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (selectedTab == 0 && uiState !is PlaylistsUiState.SignedOut) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_playlists_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh playlists",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("playlists_tab_library"),
                    label = { Text("Library") },
                )
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("playlists_tab_downloaded"),
                    label = {
                        val suffix = if (downloadedTracks.isNotEmpty()) " (${downloadedTracks.size})" else ""
                        Text("Downloaded$suffix")
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 1) {
                val offlineTracks = remember(downloadedTracks) { downloadedTracks.map { it.toTrack() } }
                val totalOfflineBytes = remember(downloadedTracks) {
                    downloadedTracks.sumOf { it.fileSizeBytes }
                }

                if (offlineTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "No offline songs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap the 3-dot menu on any track or Download All inside a playlist to listen without internet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    val offlineSpacing = when (contentLayoutStyle) {
                        ContentLayoutStyle.COMPACT -> 2.dp
                        ContentLayoutStyle.STANDARD -> 4.dp
                        ContentLayoutStyle.SHOWCASE -> 8.dp
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("downloaded_tracks_list"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(offlineSpacing),
                    ) {
                        item(key = "offline_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${offlineTracks.size} tracks • ${OfflineDownloadManager.formatBytes(totalOfflineBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(
                                    onClick = { onPlayTracks(offlineTracks, 0) },
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("downloaded_play_all_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_play_all))
                                }
                            }
                        }

                        itemsIndexed(
                            items = offlineTracks,
                            key = { index, track -> "offline_${index}_${track.videoId}" },
                        ) { index, track ->
                            TrackRow(
                                track = track,
                                isCurrentTrack = track.videoId == currentTrackId,
                                onClick = { onPlayTracks(offlineTracks, index) },
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                contentLayoutStyle = contentLayoutStyle,
                                downloadStatus = downloadStates[track.videoId] ?: DownloadStatus.DOWNLOADED,
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
                }
            } else {
                when (uiState) {
                    is PlaylistsUiState.SignedOut -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            StudioEmptyCard(
                                title = stringResource(R.string.playlists_signed_out_title),
                                subtitle = stringResource(R.string.playlists_signed_out_subtitle),
                                actionLabel = stringResource(R.string.action_go_to_account),
                                actionTestTag = "go_to_account_button",
                                onActionClick = onNavigateToAccount,
                            )
                        }
                    }

                    is PlaylistsUiState.Loading -> {
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
                                    text = "Syncing your YouTube Music library…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is PlaylistsUiState.Error -> {
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onRefresh,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("playlists_retry_button"),
                                    ) {
                                        Text("Retry Sync")
                                    }
                                    if (downloadedTracks.isNotEmpty()) {
                                        Button(
                                            onClick = { selectedTab = 1 },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("playlists_open_downloaded_button"),
                                        ) {
                                            Text("Open Downloaded")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is PlaylistsUiState.Success -> {
                        if (uiState.playlists.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No playlists found in your YouTube Music library.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            when (contentLayoutStyle) {
                                ContentLayoutStyle.STANDARD -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("playlists_list"),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        items(uiState.playlists, key = { it.id }) { playlist ->
                                            PlaylistCardRow(
                                                playlist = playlist,
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                onClick = { onOpenPlaylist(playlist) },
                                            )
                                        }
                                    }
                                }

                                ContentLayoutStyle.COMPACT,
                                ContentLayoutStyle.SHOWCASE -> {
                                    val columns = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 3 else 2
                                    val gap = if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 8.dp else 12.dp
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("playlists_list"),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(gap),
                                        verticalArrangement = Arrangement.spacedBy(gap),
                                    ) {
                                        items(uiState.playlists, key = { it.id }) { playlist ->
                                            PlaylistGridCard(
                                                playlist = playlist,
                                                thumbnailCornerRadius = thumbnailCornerRadius,
                                                showcase = contentLayoutStyle == ContentLayoutStyle.SHOWCASE,
                                                onClick = { onOpenPlaylist(playlist) },
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
}

@Composable
private fun PlaylistGridCard(
    playlist: PlaylistSummary,
    thumbnailCornerRadius: Int,
    showcase: Boolean,
    onClick: () -> Unit,
) {
    val thumbRadius = (thumbnailCornerRadius * 12f / 16f).roundToInt().coerceIn(0, 28).dp
    val cardShape = RoundedCornerShape((thumbnailCornerRadius + 2).coerceIn(10, 22).dp)
    val thumbShape = RoundedCornerShape(thumbRadius)

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showcase) Modifier.padding(10.dp) else Modifier.padding(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(thumbShape)
                    .background(
                        if (playlist.isLikedSongs) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = thumbShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playlist.isLikedSongs) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.AutoMirrored.Rounded.PlaylistPlay
                    },
                    contentDescription = null,
                    tint = if (playlist.isLikedSongs) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(if (showcase) 38.dp else 28.dp),
                )
                if (!playlist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = "${playlist.title} cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (showcase) 8.dp else 5.dp))

            Text(
                text = playlist.title,
                style = if (showcase) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlist.songCountText ?: playlist.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showcase) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = cardShape,
                )
                .clickable(onClick = onClick)
                .testTag("playlist_card_${playlist.id}"),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = cardShape,
        ) {
            cardContent()
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(thumbShape)
                .clickable(onClick = onClick)
                .testTag("playlist_card_${playlist.id}"),
        ) {
            cardContent()
        }
    }
}

@Composable
private fun PlaylistCardRow(
    playlist: PlaylistSummary,
    thumbnailCornerRadius: Int = 16,
    onClick: () -> Unit,
) {
    val thumbRadius = (thumbnailCornerRadius * 12f / 16f).roundToInt().coerceIn(0, 28).dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(thumbRadius))
                    .background(
                        if (playlist.isLikedSongs) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playlist.isLikedSongs) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.AutoMirrored.Rounded.PlaylistPlay
                    },
                    contentDescription = null,
                    tint = if (playlist.isLikedSongs) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp),
                )
                if (!playlist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = "${playlist.title} cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
