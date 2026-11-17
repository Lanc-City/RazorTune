package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.example.model.ArtworkPaletteColors
import com.example.model.DownloadStatus
import com.example.model.NowPlayingStyle
import com.example.model.PlaybackUiState
import com.example.model.PlayerBackgroundStyle
import com.example.model.PlayerButtonsLayout
import com.example.model.PlayerDesignStyle
import com.example.model.PlayerSliderStyle
import com.example.model.SleepTimerState
import com.example.model.ThemeSettings
import com.example.model.Track
import com.example.model.VisualizerStyle
import com.example.ui.viewmodel.LyricsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackUiState,
    themeSettings: ThemeSettings = ThemeSettings(),
    artworkColors: ArtworkPaletteColors? = null,
    isLiked: Boolean = false,
    showLyrics: Boolean = false,
    lyricsState: LyricsUiState = LyricsUiState.Idle,
    sleepTimerState: SleepTimerState = SleepTimerState.Off,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    reduceVisualEffects: Boolean = false,
    visualizerMagnitudes: List<Float> = emptyList(),
    hasPromptedAudioPermission: Boolean = false,
    onMarkAudioPermissionPrompted: () -> Unit = {},
    onVisualizerActiveChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onToggleLike: () -> Unit,
    onShare: () -> Unit,
    onToggleLyrics: () -> Unit,
    onStartSleepTimerMinutes: (Int) -> Unit,
    onStartSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onOpenQueueTrackMenu: (Track) -> Unit,
) {
    val track = playbackState.currentTrack ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val canShowVisualizer = themeSettings.showSpectralVisualizer && !reduceVisualEffects
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
    }

    LaunchedEffect(canShowVisualizer, hasAudioPermission, hasPromptedAudioPermission) {
        if (canShowVisualizer && !hasAudioPermission && !hasPromptedAudioPermission) {
            onMarkAudioPermissionPrompted()
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val visualizerShouldRun = canShowVisualizer && playbackState.isPlaying && hasAudioPermission
    DisposableEffect(visualizerShouldRun) {
        onVisualizerActiveChange(visualizerShouldRun)
        onDispose {
            onVisualizerActiveChange(false)
        }
    }

    var isDragging by remember(track.videoId) { mutableStateOf(false) }
    var dragFraction by remember(track.videoId) { mutableFloatStateOf(0f) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }

    val shouldAnimateVinyl = playbackState.isPlaying && !reduceVisualEffects
    var vinylRotationAngle by remember(track.videoId) { mutableFloatStateOf(0f) }
    val targetVinylRotation = if (shouldAnimateVinyl) vinylRotationAngle + 360f else vinylRotationAngle
    val currentVinylRotation by animateFloatAsState(
        targetValue = targetVinylRotation,
        animationSpec = if (shouldAnimateVinyl) {
            tween(durationMillis = 10000, easing = LinearEasing)
        } else {
            spring(stiffness = Spring.StiffnessLow)
        },
        finishedListener = {
            if (shouldAnimateVinyl) {
                vinylRotationAngle = (it % 360f)
            }
        },
        label = "vinyl_rotation",
    )

    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        (playbackState.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val displayedPositionMs = if (isDragging) {
        (dragFraction * durationMs).toLong()
    } else {
        playbackState.positionMs
    }

    val liquidGlass = themeSettings.liquidGlass
    val glassT = themeSettings.glassIntensity.coerceIn(0f, 1f)
    val isConnectedLayout = themeSettings.playerButtonsLayout == PlayerButtonsLayout.CONNECTED
    val playerDesign = themeSettings.playerDesignStyle
    val nowPlayingStyle = themeSettings.nowPlayingStyle
    val sliderStyle = themeSettings.playerSliderStyle

    val primaryAccent = artworkColors?.primaryArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val secondaryAccent = artworkColors?.secondaryArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary
    val darkAccent = artworkColors?.darkAccentArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val baseSurface = MaterialTheme.colorScheme.surface
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(sheetShape)
                .background(baseSurface),
        ) {
            when (themeSettings.playerBackgroundStyle) {
                PlayerBackgroundStyle.BLUR -> {
                    if (reduceVisualEffects) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            primaryAccent.copy(alpha = if (liquidGlass) lerp(0.32f, 0.54f, glassT) else 0.42f),
                                            darkAccent.copy(alpha = if (liquidGlass) lerp(0.18f, 0.36f, glassT) else 0.24f),
                                            baseSurface,
                                        ),
                                    ),
                                ),
                        )
                    } else {
                        val blurDp = if (liquidGlass) (34f + 54f * glassT).dp else 64.dp
                        SubcomposeAsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .blur(blurDp),
                        )
                        val topAlpha = if (liquidGlass) lerp(0.68f, 0.30f, glassT) else 0.56f
                        val midAlpha = if (liquidGlass) lerp(0.82f, 0.48f, glassT) else 0.74f
                        val bottomAlpha = if (liquidGlass) lerp(0.94f, 0.68f, glassT) else 0.90f
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            darkAccent.copy(alpha = topAlpha),
                                            baseSurface.copy(alpha = midAlpha),
                                            baseSurface.copy(alpha = bottomAlpha),
                                        ),
                                    ),
                                ),
                        )
                        if (liquidGlass) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = lerp(0.08f, 0.36f, glassT)),
                                                secondaryAccent.copy(alpha = lerp(0.04f, 0.20f, glassT)),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }
                }

                PlayerBackgroundStyle.GRADIENT -> {
                    val topAlpha = if (liquidGlass) lerp(0.46f, 0.82f, glassT) else 0.52f
                    val midAlpha = if (liquidGlass) lerp(0.20f, 0.48f, glassT) else 0.24f
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primaryAccent.copy(alpha = topAlpha),
                                        darkAccent.copy(alpha = midAlpha),
                                        baseSurface,
                                    ),
                                ),
                            ),
                    )
                    if (liquidGlass) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            secondaryAccent.copy(alpha = lerp(0.06f, 0.32f, glassT)),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }
                }

                PlayerBackgroundStyle.SOLID -> {
                    if (liquidGlass) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            primaryAccent.copy(alpha = lerp(0.06f, 0.34f, glassT)),
                                            baseSurface,
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }

            if (nowPlayingStyle == NowPlayingStyle.FULL_BLEED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    if (showLyrics) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            LyricsBoxContent(
                                lyricsState = lyricsState,
                                positionMs = playbackState.positionMs,
                                onSeekTo = onSeekTo,
                                onCloseLyrics = onToggleLyrics,
                            )
                        }
                    } else {
                        SubcomposeAsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = "${track.title} album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            baseSurface.copy(alpha = 0.75f),
                                            baseSurface,
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("now_playing_collapse_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse player",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (showLyrics) "LYRICS" else "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isConnectedLayout) {
                        IconButton(
                            onClick = onOpenMoreMenu,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_more_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (nowPlayingStyle != NowPlayingStyle.FULL_BLEED) {
                        val artBorderColor = if (liquidGlass) {
                            Color.White.copy(alpha = lerp(0.08f, 0.46f, glassT))
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }

                        if (nowPlayingStyle == NowPlayingStyle.VINYL) {
                            Box(
                                modifier = Modifier
                                    .size(252.dp)
                                    .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(4.dp, Color.DarkGray, CircleShape)
                                    .rotate(currentVinylRotation),
                                contentAlignment = Alignment.Center,
                            ) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.DarkGray.copy(alpha = 0.4f), CircleShape),
                            )
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.DarkGray.copy(alpha = 0.6f), CircleShape),
                            )
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showLyrics) {
                                    LyricsBoxContent(
                                        lyricsState = lyricsState,
                                        positionMs = playbackState.positionMs,
                                        onSeekTo = onSeekTo,
                                        onCloseLyrics = onToggleLyrics,
                                    )
                                } else {
                                    SubcomposeAsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = "${track.title} vinyl label",
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
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .border(2.dp, Color.LightGray, CircleShape),
                                )
                            }
                        }
                    } else {
                        // CLASSIC
                        val classicArtShape = RoundedCornerShape(themeSettings.thumbnailCornerRadius.dp)
                        Box(
                            modifier = Modifier
                                .size(252.dp)
                                .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                                .aspectRatio(1f)
                                .clip(classicArtShape)
                                .background(
                                    if (showLyrics) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                            alpha = if (liquidGlass) lerp(0.88f, 0.42f, glassT) else 0.88f,
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    color = artBorderColor,
                                    shape = classicArtShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showLyrics) {
                                LyricsBoxContent(
                                    lyricsState = lyricsState,
                                    positionMs = playbackState.positionMs,
                                    onSeekTo = onSeekTo,
                                    onCloseLyrics = onToggleLyrics,
                                )
                            } else {
                                SubcomposeAsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = "${track.title} album art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize(),
                                    error = {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(64.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                if (canShowVisualizer) {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (hasAudioPermission) {
                        SpectralVisualizerBar(
                            magnitudes = visualizerMagnitudes,
                            style = themeSettings.visualizerStyle,
                            primaryColor = primaryAccent,
                            secondaryColor = secondaryAccent,
                            isPlaying = playbackState.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("now_playing_spectral_visualizer"),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onMarkAudioPermissionPrompted()
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("now_playing_visualizer_permission_prompt"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Tap to allow audio session access for live visualizer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                val subtitleText = buildString {
                    append(track.artist)
                    if (!track.album.isNullOrBlank()) {
                        append(" • ")
                        append(track.album)
                    }
                }

                if (isConnectedLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            IconButton(
                                onClick = onShare,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_share_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share track",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = onToggleLike,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_like_button"),
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike track" else "Like track",
                                    tint = if (isLiked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider rendering based on sliderStyle
                when (sliderStyle) {
                    PlayerSliderStyle.SQUIGGLY -> {
                        val shouldAnimateWave = playbackState.isPlaying && !reduceVisualEffects
                        val phaseAnim = if (shouldAnimateWave) {
                            val infiniteTransition = rememberInfiniteTransition(label = "squiggly_wave")
                            val animatedPhase by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 2f * Math.PI.toFloat(),
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                ),
                                label = "phase",
                            )
                            animatedPhase
                        } else {
                            0f
                        }
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        dragFraction = fraction
                                        onSeekTo((fraction * durationMs).toLong())
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isDragging = true },
                                        onDragEnd = {
                                            isDragging = false
                                            onSeekTo((dragFraction * durationMs).toLong())
                                        },
                                        onDragCancel = { isDragging = false },
                                    ) { change, dragAmount ->
                                        isDragging = true
                                        val newX = (change.position.x + dragAmount).coerceIn(0f, size.width.toFloat())
                                        dragFraction = (newX / size.width).coerceIn(0f, 1f)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                val width = size.width
                                val height = size.height
                                val centerY = height / 2f
                                val activeWidth = width * currentFraction

                                drawLine(
                                    color = trackColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                    end = androidx.compose.ui.geometry.Offset(width, centerY),
                                    strokeWidth = 4.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                )

                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, centerY)
                                    val freq = 0.08f
                                    val amplitude = if (shouldAnimateWave) 5.dp.toPx() else 0f
                                    val currentPhase = if (shouldAnimateWave) phaseAnim else 0f
                                    var x = 0f
                                    while (x <= activeWidth) {
                                        val y = centerY + amplitude * kotlin.math.sin(x * freq + currentPhase)
                                        lineTo(x, y)
                                        x += 2f
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 5.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    ),
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 7.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(activeWidth, centerY),
                                )
                            }
                        }
                    }

                    PlayerSliderStyle.THICK_BAR -> {
                        val barHeight = if (isDragging) 12.dp else 8.dp
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        dragFraction = fraction
                                        onSeekTo((fraction * durationMs).toLong())
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isDragging = true },
                                        onDragEnd = {
                                            isDragging = false
                                            onSeekTo((dragFraction * durationMs).toLong())
                                        },
                                        onDragCancel = { isDragging = false },
                                    ) { change, dragAmount ->
                                        isDragging = true
                                        val newX = (change.position.x + dragAmount).coerceIn(0f, size.width.toFloat())
                                        dragFraction = (newX / size.width).coerceIn(0f, 1f)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(barHeight)) {
                                val width = size.width
                                val height = size.height
                                val activeWidth = width * currentFraction
                                val radius = height / 2f

                                drawRoundRect(
                                    color = trackColor,
                                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                                    size = androidx.compose.ui.geometry.Size(width, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                                )
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                                    size = androidx.compose.ui.geometry.Size(activeWidth, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                                )
                            }
                        }
                    }

                    PlayerSliderStyle.GLOW -> {
                        Slider(
                            value = currentFraction,
                            onValueChange = { fraction ->
                                isDragging = true
                                dragFraction = fraction.coerceIn(0f, 1f)
                            },
                            onValueChangeFinished = {
                                val targetMs = (dragFraction * durationMs).toLong()
                                isDragging = false
                                onSeekTo(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(50.dp), spotColor = MaterialTheme.colorScheme.primary)
                                .testTag("now_playing_seek_slider"),
                        )
                    }

                    else -> {
                        // STANDARD
                        Slider(
                            value = currentFraction,
                            onValueChange = { fraction ->
                                isDragging = true
                                dragFraction = fraction.coerceIn(0f, 1f)
                            },
                            onValueChangeFinished = {
                                val targetMs = (dragFraction * durationMs).toLong()
                                isDragging = false
                                onSeekTo(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                    alpha = if (liquidGlass) lerp(0.85f, 0.35f, glassT) else 1f,
                                ),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("now_playing_seek_slider"),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTimeMs(displayedPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTimeMs(playbackState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val transportControls: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (liquidGlass || playerDesign == PlayerDesignStyle.CAPSULE) 10.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val prevEnabled = playbackState.hasPrevious || playbackState.positionMs > 0L
                        IconButton(
                            onClick = onSkipPrevious,
                            enabled = prevEnabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                            ),
                            modifier = Modifier
                                .size(if (playerDesign == PlayerDesignStyle.MINIMALIST) 62.dp else 56.dp)
                                .testTag("now_playing_prev"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = if (prevEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                                },
                                modifier = Modifier.size(if (playerDesign == PlayerDesignStyle.MINIMALIST) 38.dp else 34.dp),
                            )
                        }

                        if (playerDesign == PlayerDesignStyle.MINIMALIST) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(onClick = onTogglePlayPause)
                                    .testTag("now_playing_play_pause"),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (playbackState.isBuffering && !playbackState.isPlaying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                            }
                        } else {
                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("now_playing_play_pause"),
                            ) {
                                if (playbackState.isBuffering && !playbackState.isPlaying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onSkipNext,
                            enabled = playbackState.hasNext,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                            ),
                            modifier = Modifier
                                .size(if (playerDesign == PlayerDesignStyle.MINIMALIST) 62.dp else 56.dp)
                                .testTag("now_playing_next"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = if (playbackState.hasNext) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                                },
                                modifier = Modifier.size(if (playerDesign == PlayerDesignStyle.MINIMALIST) 38.dp else 34.dp),
                            )
                        }
                    }
                }

                if (playerDesign == PlayerDesignStyle.CAPSULE) {
                    val capsuleShape = RoundedCornerShape(50.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(capsuleShape)
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = capsuleShape)
                            .testTag("now_playing_capsule_controls"),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = capsuleShape,
                        shadowElevation = 4.dp,
                    ) {
                        transportControls()
                    }
                } else if (liquidGlass && playerDesign == PlayerDesignStyle.MODERN_GLASS) {
                    val glassShape = RoundedCornerShape(32.dp)
                    val glassBorder = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = lerp(0.08f, 0.60f, glassT)),
                            Color.White.copy(alpha = lerp(0.02f, 0.20f, glassT)),
                        ),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(glassShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = lerp(0f, 0.16f, glassT)),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .border(width = 1.dp, brush = glassBorder, shape = glassShape)
                            .testTag("now_playing_glass_controls"),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = lerp(0.92f, 0.28f, glassT)),
                        shape = glassShape,
                    ) {
                        transportControls()
                    }
                } else {
                    transportControls()
                }

                Spacer(modifier = Modifier.height(14.dp))

                val sleepTimerActive = sleepTimerState !is SleepTimerState.Off
                val sleepLabel = when (sleepTimerState) {
                    is SleepTimerState.Off -> "Sleep"
                    is SleepTimerState.EndOfTrack -> "End of track"
                    is SleepTimerState.Countdown -> formatTimeMs(sleepTimerState.remainingSeconds * 1000L)
                }

                val queueLabel = if (playbackState.queue.isNotEmpty()) "Queue (${playbackState.queue.size})" else "Queue"

                if (isConnectedLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.Lyrics,
                            label = if (showLyrics) "Artwork" else "Lyrics",
                            active = showLyrics,
                            testTag = "now_playing_lyrics_button",
                            onClick = onToggleLyrics,
                        )
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.Bedtime,
                            label = sleepLabel,
                            active = sleepTimerActive,
                            testTag = "now_playing_sleep_timer_button",
                            onClick = { showSleepTimerDialog = true },
                        )
                        PlayerSecondaryButton(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = queueLabel,
                            active = showQueueSheet,
                            testTag = "now_playing_queue_button",
                            onClick = { showQueueSheet = true },
                        )
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.MoreVert,
                            label = "Menu",
                            active = false,
                            testTag = "now_playing_more_button",
                            onClick = onOpenMoreMenu,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerSecondaryButton(
                            icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            label = if (isLiked) "Liked" else "Like",
                            active = isLiked,
                            testTag = "now_playing_like_button",
                            onClick = onToggleLike,
                        )
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.Lyrics,
                            label = if (showLyrics) "Artwork" else "Lyrics",
                            active = showLyrics,
                            testTag = "now_playing_lyrics_button",
                            onClick = onToggleLyrics,
                        )
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.Bedtime,
                            label = sleepLabel,
                            active = sleepTimerActive,
                            testTag = "now_playing_sleep_timer_button",
                            onClick = { showSleepTimerDialog = true },
                        )
                        PlayerSecondaryButton(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = queueLabel,
                            active = showQueueSheet,
                            testTag = "now_playing_queue_button",
                            onClick = { showQueueSheet = true },
                        )
                        PlayerSecondaryButton(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            active = false,
                            testTag = "now_playing_share_button",
                            onClick = onShare,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerState = sleepTimerState,
            onSelectMinutes = { mins ->
                onStartSleepTimerMinutes(mins)
                showSleepTimerDialog = false
            },
            onSelectEndOfTrack = {
                onStartSleepTimerEndOfTrack()
                showSleepTimerDialog = false
            },
            onTurnOff = {
                onCancelSleepTimer()
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false },
        )
    }

    if (showQueueSheet) {
        val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = queueSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countText = if (playbackState.queue.size == 1) "1 track" else "${playbackState.queue.size} tracks"
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Up Next • $countText",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (playbackState.queue.isNotEmpty() && playbackState.currentIndex in playbackState.queue.indices) {
                            Text(
                                text = "Playing: ${playbackState.queue[playbackState.currentIndex].title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(
                        onClick = { showQueueSheet = false },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_queue_sheet_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close queue",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                val queueListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = playbackState.currentIndex.coerceIn(0, (playbackState.queue.size - 1).coerceAtLeast(0))
                )

                LazyColumn(
                    state = queueListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("now_playing_queue_list"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(
                        items = playbackState.queue,
                        key = { index, item -> "${index}_${item.videoId}" },
                    ) { index, queueTrack ->
                        TrackRow(
                            track = queueTrack,
                            isCurrentTrack = index == playbackState.currentIndex,
                            onClick = { onSelectQueueIndex(index) },
                            thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                            downloadStatus = downloadStates[queueTrack.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index == playbackState.currentIndex) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = "Now playing",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(end = 4.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onOpenQueueTrackMenu(queueTrack) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("queue_track_more_${queueTrack.videoId}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "Track options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
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
private fun SpectralVisualizerBar(
    magnitudes: List<Float>,
    style: VisualizerStyle,
    primaryColor: Color,
    secondaryColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val barCount = 22
    val peakHolds = remember(barCount) { FloatArray(barCount) }

    LaunchedEffect(magnitudes, isPlaying) {
        for (i in 0 until barCount) {
            val current = if (isPlaying) magnitudes.getOrElse(i) { 0f }.coerceIn(0f, 1f) else 0f
            peakHolds[i] = maxOf(current, (peakHolds[i] - 0.045f).coerceAtLeast(0f))
        }
    }

    val animatedValues = List(barCount) { index ->
        val rawTarget = if (isPlaying) magnitudes.getOrElse(index) { 0f }.coerceIn(0f, 1f) else 0f
        animateFloatAsState(
            targetValue = rawTarget,
            animationSpec = tween(durationMillis = 85, easing = LinearEasing),
            label = "visualizer_bar_$index",
        ).value
    }

    val animatedPeaks = List(barCount) { index ->
        animateFloatAsState(
            targetValue = if (isPlaying) peakHolds[index] else 0f,
            animationSpec = tween(durationMillis = 120, easing = LinearEasing),
            label = "visualizer_peak_$index",
        ).value
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val gap = 4.dp.toPx()
        val barWidth = ((width - gap * (barCount - 1)) / barCount).coerceAtLeast(2.dp.toPx())
        val barBrush = Brush.verticalGradient(
            colors = listOf(
                primaryColor,
                secondaryColor.copy(alpha = 0.82f),
            ),
        )

        when (style) {
            VisualizerStyle.BARS -> {
                val minBarHeight = if (isPlaying) 3.dp.toPx() else 2.dp.toPx()
                for (i in 0 until barCount) {
                    val value = animatedValues[i]
                    val barHeight = (minBarHeight + value * (height - minBarHeight)).coerceIn(minBarHeight, height)
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        alpha = if (isPlaying) (0.45f + 0.55f * value) else 0.25f,
                    )
                }
            }

            VisualizerStyle.MIRRORED -> {
                val centerY = height / 2f
                val minHalfHeight = if (isPlaying) 2.dp.toPx() else 1.5.dp.toPx()
                for (i in 0 until barCount) {
                    val value = animatedValues[i]
                    val halfHeight = (minHalfHeight + value * (centerY - minHalfHeight)).coerceIn(minHalfHeight, centerY)
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, centerY - halfHeight),
                        size = Size(barWidth, halfHeight * 2f),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        alpha = if (isPlaying) (0.45f + 0.55f * value) else 0.25f,
                    )
                }
            }

            VisualizerStyle.DOT_PEAKS -> {
                val thinWidth = (barWidth * 0.56f).coerceAtLeast(2.dp.toPx())
                val dotRadius = (barWidth * 0.42f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                val usableHeight = (height - dotRadius * 2.2f).coerceAtLeast(4.dp.toPx())
                val minBarHeight = if (isPlaying) 2.5.dp.toPx() else 1.5.dp.toPx()

                for (i in 0 until barCount) {
                    val value = animatedValues[i]
                    val peak = maxOf(value, animatedPeaks[i])
                    val slotX = i * (barWidth + gap)
                    val barX = slotX + (barWidth - thinWidth) / 2f
                    val centerX = slotX + barWidth / 2f
                    val barHeight = (minBarHeight + value * usableHeight).coerceIn(minBarHeight, usableHeight)

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(barX, height - barHeight),
                        size = Size(thinWidth, barHeight),
                        cornerRadius = CornerRadius(thinWidth / 2f, thinWidth / 2f),
                        alpha = if (isPlaying) (0.35f + 0.45f * value) else 0.20f,
                    )

                    val peakY = (height - dotRadius - peak * usableHeight).coerceIn(dotRadius, height - dotRadius)
                    drawCircle(
                        color = primaryColor.copy(alpha = if (isPlaying) 0.28f else 0.12f),
                        radius = dotRadius * 1.65f,
                        center = Offset(centerX, peakY),
                    )
                    drawCircle(
                        color = if (isPlaying) primaryColor else primaryColor.copy(alpha = 0.45f),
                        radius = dotRadius,
                        center = Offset(centerX, peakY),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSecondaryButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val contentColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LyricsBoxContent(
    lyricsState: LyricsUiState,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    onCloseLyrics: () -> Unit,
) {
    when (lyricsState) {
        is LyricsUiState.Idle, is LyricsUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Fetching lyrics…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is LyricsUiState.Unavailable -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lyricsState.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCloseLyrics) {
                    Text("Show artwork")
                }
            }
        }

        is LyricsUiState.Loaded -> {
            val data = lyricsState.lyrics
            val listState = rememberLazyListState()
            val activeIndex by remember(data, positionMs) {
                derivedStateOf {
                    if (!data.isSynced) {
                        -1
                    } else {
                        data.lines.indexOfLast { line ->
                            val start = line.startTimeMs
                            start != null && start <= positionMs
                        }
                    }
                }
            }

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0 && activeIndex < data.lines.size) {
                    listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("now_playing_lyrics_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(data.lines) { idx, line ->
                    val isCurrent = data.isSynced && idx == activeIndex
                    val clickableModifier = if (line.startTimeMs != null) {
                        Modifier.clickable { onSeekTo(line.startTimeMs) }
                    } else {
                        Modifier
                    }
                    Text(
                        text = line.text,
                        style = if (isCurrent) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            data.isSynced -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(clickableModifier)
                            .padding(vertical = 2.dp),
                    )
                }

                data.source?.let { source ->
                    item(key = "lyrics_source") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presetMinutes = listOf(15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (sleepTimerState is SleepTimerState.Countdown) {
                    Text(
                        text = "Pauses in ${formatTimeMs(sleepTimerState.remainingSeconds * 1000L)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                presetMinutes.forEach { mins ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectMinutes(mins) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$mins minutes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val isEndOfTrack = sleepTimerState is SleepTimerState.EndOfTrack
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSelectEndOfTrack)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "End of track",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isEndOfTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (isEndOfTrack) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (sleepTimerState !is SleepTimerState.Off) {
                TextButton(onClick = onTurnOff) {
                    Text("Turn off")
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

private fun formatTimeMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
