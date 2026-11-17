package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ContentLayoutStyle
import com.example.model.DownloadStatus
import com.example.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.viewmodel.PlaylistDetailUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistDetailScreen(
    fallbackTitle: String,
    uiState: PlaylistDetailUiState,
    currentTrackId: String?,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onDownloadAll: (List<Track>) -> Unit = {},
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

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
                        .testTag("playlist_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = (uiState as? PlaylistDetailUiState.Success)?.title ?: fallbackTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when (uiState) {
                is PlaylistDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is PlaylistDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
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

                is PlaylistDetailUiState.Success -> {
                    val itemSpacing = when (contentLayoutStyle) {
                        ContentLayoutStyle.COMPACT -> 2.dp
                        ContentLayoutStyle.STANDARD -> 4.dp
                        ContentLayoutStyle.SHOWCASE -> 8.dp
                    }
                    val headerArtSize = when (contentLayoutStyle) {
                        ContentLayoutStyle.COMPACT -> 88.dp
                        ContentLayoutStyle.STANDARD -> 104.dp
                        ContentLayoutStyle.SHOWCASE -> 120.dp
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("playlist_tracks_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    ) {
                        item(key = "header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val headerArtShape = RoundedCornerShape(thumbnailCornerRadius.dp)
                                Box(
                                    modifier = Modifier
                                        .size(headerArtSize)
                                        .clip(headerArtShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = headerArtShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    if (!uiState.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = uiState.thumbnailUrl,
                                            contentDescription = "${uiState.title} artwork",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize(),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = uiState.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = uiState.subtitle
                                            ?: "${uiState.tracks.size} tracks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (uiState.tracks.isNotEmpty()) {
                                        val downloadedCount = uiState.tracks.count {
                                            downloadStates[it.videoId] == DownloadStatus.DOWNLOADED
                                        }
                                        val downloadingCount = uiState.tracks.count {
                                            downloadStates[it.videoId] == DownloadStatus.DOWNLOADING
                                        }
                                        val allDownloaded = downloadedCount == uiState.tracks.size

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Button(
                                                onClick = { onPlayTracks(uiState.tracks, 0) },
                                                modifier = Modifier
                                                    .heightIn(min = 44.dp)
                                                    .testTag("playlist_play_all_button"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = null,
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(stringResource(R.string.action_play_all))
                                            }

                                            FilledTonalButton(
                                                onClick = { onDownloadAll(uiState.tracks) },
                                                enabled = !allDownloaded && downloadingCount == 0,
                                                modifier = Modifier
                                                    .heightIn(min = 44.dp)
                                                    .testTag("playlist_download_all_button"),
                                            ) {
                                                if (downloadingCount > 0) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Downloading ($downloadedCount/${uiState.tracks.size})")
                                                } else if (allDownloaded) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DownloadDone,
                                                        contentDescription = null,
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Downloaded")
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Download,
                                                        contentDescription = null,
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Download All")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.tracks.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = "No playable tracks found in this playlist.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.tracks,
                                key = { index, track -> "${index}_${track.videoId}" },
                            ) { index, track ->
                                TrackRow(
                                    track = track,
                                    isCurrentTrack = track.videoId == currentTrackId,
                                    onClick = { onPlayTracks(uiState.tracks, index) },
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
                    }
                }
            }
        }
    }
}
