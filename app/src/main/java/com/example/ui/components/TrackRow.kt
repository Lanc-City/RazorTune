package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.ContentLayoutStyle
import com.example.model.DownloadStatus
import com.example.model.Track
import kotlin.math.roundToInt

@Composable
fun TrackRow(
    track: Track,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isCurrentTrack) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        label = "track_row_bg",
    )

    val thumbRadius = (thumbnailCornerRadius * 10f / 16f).roundToInt().coerceIn(0, 26).dp
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
    val horizontalPad = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 10.dp
        ContentLayoutStyle.STANDARD -> 12.dp
        ContentLayoutStyle.SHOWCASE -> 14.dp
    }
    val verticalPad = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 4.dp
        ContentLayoutStyle.STANDARD -> 8.dp
        ContentLayoutStyle.SHOWCASE -> 10.dp
    }
    val spacerWidth = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> 10.dp
        ContentLayoutStyle.STANDARD -> 14.dp
        ContentLayoutStyle.SHOWCASE -> 16.dp
    }
    val titleStyle = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> MaterialTheme.typography.titleSmall
        ContentLayoutStyle.STANDARD -> MaterialTheme.typography.titleMedium
        ContentLayoutStyle.SHOWCASE -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    }
    val subtitleStyle = when (contentLayoutStyle) {
        ContentLayoutStyle.COMPACT -> MaterialTheme.typography.bodySmall
        ContentLayoutStyle.STANDARD -> MaterialTheme.typography.bodyMedium
        ContentLayoutStyle.SHOWCASE -> MaterialTheme.typography.bodyMedium
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minRowHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPad, vertical = verticalPad)
            .testTag("track_row_${track.videoId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(thumbRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 20.dp else 24.dp),
            )

            if (!track.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = "${track.title} artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Now playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(spacerWidth))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (contentLayoutStyle == ContentLayoutStyle.COMPACT) 1.dp else 3.dp),
        ) {
            Text(
                text = track.title,
                style = titleStyle,
                color = if (isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                append(track.artist)
                if (!track.album.isNullOrBlank()) {
                    append(" • ")
                    append(track.album)
                }
            }
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when (downloadStatus) {
            DownloadStatus.DOWNLOADED -> {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = "Downloaded for offline playback",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("track_downloaded_badge_${track.videoId}"),
                )
            }
            DownloadStatus.DOWNLOADING -> {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag("track_downloading_badge_${track.videoId}"),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            else -> Unit
        }

        if (track.formattedDuration.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = track.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailingContent()
        }
    }
}
