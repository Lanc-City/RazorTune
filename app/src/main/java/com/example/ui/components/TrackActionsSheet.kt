package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.DownloadStatus
import com.example.model.PlaylistSummary
import com.example.model.Track
import com.example.ui.viewmodel.PlaylistsUiState
import com.example.ui.viewmodel.TrackStreamDetailsUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: Track,
    downloadStatus: DownloadStatus,
    isLiked: Boolean,
    thumbnailCornerRadius: Int = 16,
    onDismiss: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onAddToLibrary: () -> Unit,
    onViewArtist: () -> Unit,
    onViewAlbum: () -> Unit,
    onShare: () -> Unit,
    onToggleLyrics: () -> Unit,
    onDetails: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val thumbRadius = (thumbnailCornerRadius * 10f / 16f).roundToInt().coerceIn(0, 26).dp
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(thumbRadius))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    SubcomposeAsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "${track.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                        error = {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            ActionMenuItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to queue",
                testTag = "action_add_to_queue",
                onClick = {
                    onDismiss()
                    onAddToQueue()
                },
            )
            ActionMenuItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = "Add to playlist",
                testTag = "action_add_to_playlist",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                },
            )

            val downloadLabel = when (downloadStatus) {
                DownloadStatus.NOT_DOWNLOADED -> "Download"
                DownloadStatus.DOWNLOADING -> "Downloading…"
                DownloadStatus.DOWNLOADED -> "Downloaded (Tap to remove)"
                DownloadStatus.FAILED -> "Download failed (Tap to retry)"
            }
            val downloadIcon = when (downloadStatus) {
                DownloadStatus.DOWNLOADED -> Icons.Rounded.DownloadDone
                else -> Icons.Rounded.Download
            }
            ActionMenuItem(
                icon = downloadIcon,
                label = downloadLabel,
                testTag = "action_download",
                trailingProgress = downloadStatus == DownloadStatus.DOWNLOADING,
                onClick = {
                    onDismiss()
                    onDownload()
                },
            )

            ActionMenuItem(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.LibraryAdd,
                label = if (isLiked) "Added to library" else "Add to library",
                testTag = "action_add_to_library",
                onClick = {
                    onDismiss()
                    onAddToLibrary()
                },
            )
            ActionMenuItem(
                icon = Icons.Rounded.Person,
                label = "View artist",
                testTag = "action_view_artist",
                onClick = {
                    onDismiss()
                    onViewArtist()
                },
            )
            ActionMenuItem(
                icon = Icons.Rounded.Album,
                label = "View album",
                testTag = "action_view_album",
                onClick = {
                    onDismiss()
                    onViewAlbum()
                },
            )
            ActionMenuItem(
                icon = Icons.Rounded.Share,
                label = "Share",
                testTag = "action_share",
                onClick = {
                    onDismiss()
                    onShare()
                },
            )
            ActionMenuItem(
                icon = Icons.Rounded.Lyrics,
                label = "Toggle lyrics",
                testTag = "action_toggle_lyrics",
                onClick = {
                    onDismiss()
                    onToggleLyrics()
                },
            )
            ActionMenuItem(
                icon = Icons.Rounded.Info,
                label = "Details",
                testTag = "action_details",
                onClick = {
                    onDismiss()
                    onDetails()
                },
            )
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    label: String,
    testTag: String,
    trailingProgress: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailingProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlistsState: PlaylistsUiState,
    onSelectPlaylist: (PlaylistSummary) -> Unit,
    onOpenAccountSync: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            when (playlistsState) {
                is PlaylistsUiState.SignedOut -> {
                    Text(
                        text = "Sign in via Settings > Account & Sync to add \"${track.title}\" to your YouTube Music playlists.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is PlaylistsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PlaylistsUiState.Error -> {
                    Text(
                        text = playlistsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is PlaylistsUiState.Success -> {
                    val editable = playlistsState.playlists.filterNot { it.isLikedSongs || it.id == "VLLM" || it.id == "LM" }
                    if (editable.isEmpty()) {
                        Text(
                            text = "No editable playlists found in your library.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                        ) {
                            items(editable, key = { it.id }) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectPlaylist(playlist)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = playlist.subtitle,
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
                }
            }
        },
        confirmButton = {
            if (playlistsState is PlaylistsUiState.SignedOut) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenAccountSync()
                    },
                ) {
                    Text("Account & Sync")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
fun TrackDetailsDialog(
    track: Track,
    downloadStatus: DownloadStatus,
    streamDetailsState: TrackStreamDetailsUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Track Details",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailRow("Title", track.title)
                DetailRow("Artist", track.artist)
                DetailRow("Album", track.album?.takeIf { it.isNotBlank() } ?: "Single / Unknown")
                DetailRow("Video ID", track.videoId)
                DetailRow(
                    "Duration",
                    track.formattedDuration.ifEmpty { "Unknown" },
                )
                DetailRow(
                    "Offline cache",
                    when (downloadStatus) {
                        DownloadStatus.DOWNLOADED -> "Downloaded locally"
                        DownloadStatus.DOWNLOADING -> "Downloading…"
                        DownloadStatus.NOT_DOWNLOADED -> "Streaming only"
                        DownloadStatus.FAILED -> "Download failed"
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                when (streamDetailsState) {
                    is TrackStreamDetailsUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Inspecting audio stream…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is TrackStreamDetailsUiState.Error -> {
                        DetailRow("Audio stream", streamDetailsState.message)
                    }
                    is TrackStreamDetailsUiState.Loaded -> {
                        val details = streamDetailsState.details
                        DetailRow("Codec", details.codec)
                        DetailRow(
                            "Bitrate",
                            details.bitrateKbps?.let { "$it kbps" } ?: "Variable",
                        )
                        details.mimeType?.let { DetailRow("MIME type", it) }
                        details.contentLengthBytes?.let { bytes ->
                            val mb = bytes.toDouble() / (1024.0 * 1024.0)
                            DetailRow("Stream size", "%.2f MB".format(mb))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
