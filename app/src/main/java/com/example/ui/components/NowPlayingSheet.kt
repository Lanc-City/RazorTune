package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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

    // Full-Bleed takes visual precedence over Modern Glass so header & bottom bars never duplicate
    val isFullBleedBackdrop = nowPlayingStyle == NowPlayingStyle.FULL_BLEED
    val isModernGlass = !isFullBleedBackdrop && playerDesign == PlayerDesignStyle.MODERN_GLASS

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
                .background(if (isFullBleedBackdrop) Color.Black else baseSurface),
        ) {
            // 1. Background Layer
            if (isFullBleedBackdrop) {
                SubcomposeAsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.48f),
                                    Color.Black.copy(alpha = 0.36f),
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.90f),
                                ),
                            ),
                        ),
                )
            } else {
                when (themeSettings.playerBackgroundStyle) {
                    PlayerBackgroundStyle.BLUR -> {
                        if (reduceVisualEffects) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = if (liquidGlass || isModernGlass) lerp(0.32f, 0.54f, glassT) else 0.42f),
                                                darkAccent.copy(alpha = if (liquidGlass || isModernGlass) lerp(0.18f, 0.36f, glassT) else 0.24f),
                                                baseSurface,
                                            ),
                                        ),
                                    ),
                            )
                        } else {
                            val blurDp = if (isModernGlass) 56.dp else if (liquidGlass) (34f + 54f * glassT).dp else 64.dp
                            SubcomposeAsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(blurDp),
                            )
                            val topAlpha = if (isModernGlass) 0.44f else if (liquidGlass) lerp(0.68f, 0.30f, glassT) else 0.56f
                            val midAlpha = if (isModernGlass) 0.62f else if (liquidGlass) lerp(0.82f, 0.48f, glassT) else 0.74f
                            val bottomAlpha = if (isModernGlass) 0.80f else if (liquidGlass) lerp(0.94f, 0.68f, glassT) else 0.90f
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
                            if (liquidGlass || isModernGlass) {
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
                    }

                    PlayerBackgroundStyle.SOLID -> {
                        if (liquidGlass || isModernGlass) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = lerp(0.14f, 0.38f, glassT)),
                                                baseSurface,
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }

            // 2. Foreground Content Column
            val foregroundTextColor = if (isFullBleedBackdrop) Color.White else MaterialTheme.colorScheme.onSurface
            val foregroundSubtleColor = if (isFullBleedBackdrop) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant

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
                // Top Header Bar
                if (isModernGlass) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                onClick = onDismiss,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("now_playing_collapse_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Collapse player",
                                        tint = foregroundTextColor,
                                    )
                                }
                            }
                            Text(
                                text = if (showLyrics) "Lyrics" else "Now Playing",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = foregroundTextColor,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                onClick = onToggleLyrics,
                                shape = CircleShape,
                                color = if (showLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("now_playing_lyrics_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lyrics,
                                        contentDescription = "Lyrics",
                                        tint = if (showLyrics) MaterialTheme.colorScheme.onPrimary else foregroundTextColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Surface(
                                onClick = { showQueueSheet = true },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("now_playing_queue_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = foregroundTextColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                                tint = foregroundTextColor,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (showLyrics) "Lyrics" else "Now Playing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = foregroundTextColor,
                            )
                            val headerSub = track.album?.takeIf { it.isNotBlank() } ?: track.artist
                            if (headerSub.isNotBlank()) {
                                Text(
                                    text = headerSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = foregroundSubtleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (!isConnectedLayout && !isFullBleedBackdrop) {
                            IconButton(
                                onClick = onOpenMoreMenu,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_more_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More options",
                                    tint = foregroundTextColor,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
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
                    val artBorderColor = if (liquidGlass || isModernGlass || isFullBleedBackdrop) {
                        Color.White.copy(alpha = lerp(0.14f, 0.42f, glassT))
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    // Foreground Artwork Card
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
                        val cardSize = if (isFullBleedBackdrop) 276.dp else 256.dp
                        val cardShape = RoundedCornerShape(
                            if (isFullBleedBackdrop || isModernGlass) 20.dp else themeSettings.thumbnailCornerRadius.dp,
                        )
                        Box(
                            modifier = Modifier
                                .size(cardSize)
                                .sizeIn(maxWidth = 288.dp, maxHeight = 288.dp)
                                .aspectRatio(1f)
                                .shadow(18.dp, cardShape)
                                .clip(cardShape)
                                .background(
                                    if (showLyrics) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                )
                                .border(width = 1.dp, color = artBorderColor, shape = cardShape),
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

                    // Spectral Visualizer Bar
                    if (canShowVisualizer) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (hasAudioPermission) {
                            SpectralVisualizerBar(
                                magnitudes = visualizerMagnitudes,
                                style = themeSettings.visualizerStyle,
                                primaryColor = if (isFullBleedBackdrop) Color.White else primaryAccent,
                                secondaryColor = secondaryAccent,
                                isPlaying = playbackState.isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .testTag("now_playing_spectral_visualizer"),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 28.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onMarkAudioPermissionPrompted()
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("now_playing_visualizer_permission_prompt"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tap to allow audio session access for live visualizer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = foregroundSubtleColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val subtitleText = buildString {
                        append(track.artist)
                        if (!track.album.isNullOrBlank() && !isModernGlass && !isFullBleedBackdrop) {
                            append(" • ")
                            append(track.album)
                        }
                    }

                    // Track Metadata & Action Buttons
                    if (isFullBleedBackdrop) {
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
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    onClick = onShare,
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("now_playing_share_button"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share track",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Surface(
                                    onClick = onToggleLike,
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("now_playing_like_button"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = if (isLiked) "Unlike track" else "Like track",
                                            tint = if (isLiked) primaryAccent else Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Surface(
                                    onClick = onOpenMoreMenu,
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("now_playing_more_button"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreHoriz,
                                            contentDescription = "More options",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isConnectedLayout && !isModernGlass) {
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
                                    color = foregroundTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = foregroundSubtleColor,
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
                                        tint = foregroundSubtleColor,
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
                                            foregroundSubtleColor
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = foregroundTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = foregroundSubtleColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seek Slider
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
                            val primaryColor = if (isFullBleedBackdrop || isModernGlass) Color.White else MaterialTheme.colorScheme.primary
                            val trackColor = if (isFullBleedBackdrop || isModernGlass) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest

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
                                        start = Offset(0f, centerY),
                                        end = Offset(width, centerY),
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
                                        center = Offset(activeWidth, centerY),
                                    )
                                }
                            }
                        }

                        PlayerSliderStyle.THICK_BAR -> {
                            val barHeight = if (isDragging) 12.dp else 8.dp
                            val primaryColor = if (isFullBleedBackdrop || isModernGlass) Color.White else MaterialTheme.colorScheme.primary
                            val trackColor = if (isFullBleedBackdrop || isModernGlass) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest

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
                                        topLeft = Offset.Zero,
                                        size = Size(width, height),
                                        cornerRadius = CornerRadius(radius, radius),
                                    )
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset.Zero,
                                        size = Size(activeWidth, height),
                                        cornerRadius = CornerRadius(radius, radius),
                                    )
                                }
                            }
                        }

                        PlayerSliderStyle.GLOW -> {
                            val sliderAccent = if (isFullBleedBackdrop || isModernGlass) Color.White else MaterialTheme.colorScheme.primary
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
                                    thumbColor = sliderAccent,
                                    activeTrackColor = sliderAccent,
                                    inactiveTrackColor = if (isFullBleedBackdrop || isModernGlass) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(10.dp, RoundedCornerShape(50.dp), spotColor = sliderAccent)
                                    .testTag("now_playing_seek_slider"),
                            )
                        }

                        else -> {
                            val sliderAccent = if (isFullBleedBackdrop || isModernGlass) Color.White else MaterialTheme.colorScheme.primary
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
                                    thumbColor = sliderAccent,
                                    activeTrackColor = sliderAccent,
                                    inactiveTrackColor = if (isFullBleedBackdrop || isModernGlass) {
                                        Color.White.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                            alpha = if (liquidGlass) lerp(0.85f, 0.35f, glassT) else 1f,
                                        )
                                    },
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
                            color = foregroundSubtleColor,
                        )
                        Text(
                            text = formatTimeMs(playbackState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundSubtleColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Transport Controls
                    if (isFullBleedBackdrop) {
                        // Grouped translucent pill dock matching your screenshot
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val prevEnabled = playbackState.hasPrevious || playbackState.positionMs > 0L
                                Surface(
                                    onClick = onSkipPrevious,
                                    enabled = prevEnabled,
                                    shape = RoundedCornerShape(18.dp),
                                    color = primaryAccent.copy(alpha = 0.42f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .testTag("now_playing_prev"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.FastRewind,
                                            contentDescription = "Previous track",
                                            tint = if (prevEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(width = 88.dp, height = 74.dp)
                                        .testTag("now_playing_play_pause"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (playbackState.isBuffering && !playbackState.isPlaying) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(30.dp),
                                                strokeWidth = 3.dp,
                                                color = Color.Black,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                                tint = Color.Black,
                                                modifier = Modifier.size(38.dp),
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = onSkipNext,
                                    enabled = playbackState.hasNext,
                                    shape = RoundedCornerShape(18.dp),
                                    color = primaryAccent.copy(alpha = 0.42f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .testTag("now_playing_next"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.FastForward,
                                            contentDescription = "Next track",
                                            tint = if (playbackState.hasNext) Color.White else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isModernGlass) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val prevEnabled = playbackState.hasPrevious || playbackState.positionMs > 0L
                            Surface(
                                onClick = onSkipPrevious,
                                enabled = prevEnabled,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(62.dp)
                                    .testTag("now_playing_prev"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous track",
                                        tint = if (prevEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(12.dp, CircleShape)
                                    .testTag("now_playing_play_pause"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (playbackState.isBuffering && !playbackState.isPlaying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            strokeWidth = 3.dp,
                                            color = Color.Black,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onSkipNext,
                                enabled = playbackState.hasNext,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(62.dp)
                                    .testTag("now_playing_next"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next track",
                                        tint = if (playbackState.hasNext) Color.White else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                    } else {
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
                        } else {
                            transportControls()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val sleepTimerActive = sleepTimerState !is SleepTimerState.Off
                    val sleepLabel = when (sleepTimerState) {
                        is SleepTimerState.Off -> "Sleep"
                        is SleepTimerState.EndOfTrack -> "End of track"
                        is SleepTimerState.Countdown -> formatTimeMs(sleepTimerState.remainingSeconds * 1000L)
                    }
                    val queueLabel = if (playbackState.queue.isNotEmpty()) "Queue (${playbackState.queue.size})" else "Queue"

                    // Bottom Action Bar
                    if (isFullBleedBackdrop) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                onClick = { showQueueSheet = true },
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("now_playing_queue_button"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Queue",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                    )
                                }
                            }

                            Surface(
                                onClick = { showSleepTimerDialog = true },
                                shape = CircleShape,
                                color = if (sleepTimerActive) primaryAccent else Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("now_playing_sleep_timer_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bedtime,
                                        contentDescription = sleepLabel,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Surface(
                                onClick = onToggleLyrics,
                                shape = RoundedCornerShape(50.dp),
                                color = if (showLyrics) primaryAccent else Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("now_playing_lyrics_button"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lyrics,
                                        contentDescription = "Lyrics",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (showLyrics) "Artwork" else "Lyrics",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    } else if (isModernGlass) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                IconButton(
                                    onClick = onToggleLike,
                                    modifier = Modifier.testTag("now_playing_like_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) primaryAccent else Color.White,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(22.dp)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                )
                                IconButton(
                                    onClick = { showSleepTimerDialog = true },
                                    modifier = Modifier.testTag("now_playing_sleep_timer_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bedtime,
                                        contentDescription = sleepLabel,
                                        tint = if (sleepTimerActive) primaryAccent else Color.White,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(22.dp)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                )
                                IconButton(
                                    onClick = onOpenMoreMenu,
                                    modifier = Modifier.testTag("now_playing_more_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    } else if (isConnectedLayout) {
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
                    initialFirstVisibleItemIndex = playbackState.currentIndex.coerceIn(0, (playbackState.queue.size - 1).coerceAtLeast(0)),
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