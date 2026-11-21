package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import com.example.model.MiniPlayerStyle
import com.example.model.PlaybackUiState
import com.example.model.PlayerDesignStyle
import kotlin.math.roundToInt

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    liquidGlass: Boolean = false,
    glassIntensity: Float = 0.6f,
    playerDesignStyle: PlayerDesignStyle = PlayerDesignStyle.MODERN_GLASS,
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING_CARD,
    cornerRadiusDp: Int = 20,
    thumbnailCornerRadius: Int = 16,
    showLike: Boolean = false,
    showPrevious: Boolean = false,
    swipeToSkip: Boolean = true,
    isLiked: Boolean = false,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playbackState.currentTrack

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (track != null) {
            val progress = if (playbackState.durationMs > 0L) {
                (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            val t = glassIntensity.coerceIn(0f, 1f)
            val isCapsuleDesign = playerDesignStyle == PlayerDesignStyle.CAPSULE
            val isCompactPill = miniPlayerStyle == MiniPlayerStyle.COMPACT_PILL
            val isDocked = miniPlayerStyle == MiniPlayerStyle.DOCKED

            val shape = when {
                isCompactPill -> RoundedCornerShape(50.dp)
                isDocked -> RoundedCornerShape(topStart = cornerRadiusDp.dp, topEnd = cornerRadiusDp.dp)
                isCapsuleDesign -> RoundedCornerShape(50.dp)
                else -> RoundedCornerShape(cornerRadiusDp.dp)
            }

            val horizontalPadding = when {
                isCompactPill -> 32.dp
                isDocked -> 0.dp
                else -> 14.dp
            }

            val containerColor = if (liquidGlass) {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = lerp(0.95f, 0.44f, t))
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }

            val liquidGlassModifier = if (liquidGlass) {
                Modifier.liquidGlass(
                    cornerRadius = cornerRadiusDp.dp,
                    glassIntensity = glassIntensity,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            } else if (isDocked) {
                Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
            } else {
                Modifier
            }

            val swipeModifier = if (swipeToSkip) {
                Modifier.pointerInput(track.videoId) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 50f) {
                            if (playbackState.hasPrevious || playbackState.positionMs > 0L) {
                                onSkipPrevious()
                            }
                        } else if (dragAmount < -50f) {
                            if (playbackState.hasNext) {
                                onSkipNext()
                            }
                        }
                    }
                }
            } else {
                Modifier
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = if (isDocked) 0.dp else 6.dp)
                    .clip(shape)
                    .then(liquidGlassModifier)
                    .then(swipeModifier)
                    .clickable(onClick = onExpand)
                    .testTag("mini_player_bar"),
                color = if (liquidGlass) Color.Transparent else containerColor,
                shape = shape,
                tonalElevation = 0.dp,
                shadowElevation = if (liquidGlass) (6f + 12f * t).dp else 8.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumbRadius = (thumbnailCornerRadius * 10f / 16f).roundToInt().coerceIn(0, 26).dp
                        val thumbShape = if (isCompactPill || isCapsuleDesign) RoundedCornerShape(50.dp) else RoundedCornerShape(thumbRadius)

                        Box(
                            modifier = Modifier
                                .size(if (isCompactPill) 38.dp else 44.dp)
                                .clip(thumbShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
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

                        if (showLike) {
                            IconButton(
                                onClick = onToggleLike,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mini_player_like"),
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        if (showPrevious) {
                            IconButton(
                                onClick = onSkipPrevious,
                                enabled = playbackState.hasPrevious || playbackState.positionMs > 0L,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mini_player_prev"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous track",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_player_play_pause"),
                        ) {
                            if (playbackState.isBuffering && !playbackState.isPlaying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        IconButton(
                            onClick = onSkipNext,
                            enabled = playbackState.hasNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_player_next"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = if (playbackState.hasNext) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                },
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCapsuleDesign || isDocked) 4.dp else 2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
    }
}