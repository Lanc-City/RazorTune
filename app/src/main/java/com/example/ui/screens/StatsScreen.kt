package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.StatsTimeRange
import com.example.model.Track
import com.example.ui.components.StudioEmptyCard
import com.example.ui.viewmodel.ArtistShareStat
import com.example.ui.viewmodel.ArtistStat
import com.example.ui.viewmodel.DayStat
import com.example.ui.viewmodel.SongStat
import com.example.ui.viewmodel.StatsUiState
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    selectedRange: StatsTimeRange,
    uiState: StatsUiState,
    thumbnailCornerRadius: Int = 16,
    onBack: () -> Unit,
    onSelectRange: (StatsTimeRange) -> Unit,
    onClearStats: () -> Unit,
    onOpenArtist: (Track) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onShuffleTopTracks: (List<Track>) -> Unit,
    onExploreMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stats_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stats_actions_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Stats options",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear listening history") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            enabled = uiState is StatsUiState.Success,
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(StatsTimeRange.entries, key = { it.name }) { range ->
                    val selected = range == selectedRange
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRange(range) },
                        label = {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("stats_range_${range.name.lowercase()}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (uiState) {
                is StatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is StatsUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        StudioEmptyCard(
                            title = "No listening stats yet",
                            subtitle = "Play any track for at least 15 seconds to track your listening time, favourite artists, and daily trends.",
                            actionLabel = "Explore Music",
                            actionTestTag = "stats_empty_explore_button",
                            onActionClick = onExploreMusic,
                        )
                    }
                }

                is StatsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("stats_dashboard_list"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item(key = "hero_total_time") {
                            TotalTimeHeroCard(
                                totalTimeMs = uiState.totalTimeListenedMs,
                                rangeLabel = selectedRange.label,
                            )
                        }

                        item(key = "summary_metrics_row") {
                            SummaryMetricsRow(
                                totalPlays = uiState.totalPlays,
                                uniqueSongs = uiState.uniqueSongs,
                                uniqueArtists = uiState.uniqueArtists,
                            )
                        }

                        uiState.favouriteArtist?.let { artist ->
                            item(key = "favourite_artist") {
                                FavouriteArtistCard(
                                    artist = artist,
                                    onClick = {
                                        artist.topTrack?.let(onOpenArtist)
                                    },
                                )
                            }
                        }

                        uiState.favouriteSong?.let { song ->
                            item(key = "favourite_song") {
                                FavouriteSongCard(
                                    song = song,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    onClick = {
                                        val queue = uiState.topTracks.ifEmpty { listOf(song.track) }
                                        onPlayTrack(song.track, queue)
                                    },
                                )
                            }
                        }

                        if (uiState.topArtistsShare.isNotEmpty()) {
                            item(key = "artist_share_donut") {
                                ArtistShareDonutCard(shares = uiState.topArtistsShare)
                            }
                        }

                        item(key = "listening_by_day") {
                            ListeningByDayCard(days = uiState.listeningByDay)
                        }
                    }
                }
            }
        }

        if (uiState is StatsUiState.Success && uiState.topTracks.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { onShuffleTopTracks(uiState.topTracks) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                    )
                },
                text = { Text("Shuffle") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 20.dp)
                    .testTag("stats_shuffle_fab"),
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear listening history?") },
            text = {
                Text("This permanently resets your local play counts, favourite artists, and listening time charts.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearStats()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_clear_stats_button"),
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TotalTimeHeroCard(
    totalTimeMs: Long,
    rangeLabel: String,
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .testTag("stats_hero_card"),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total Time Listened",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = rangeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Text(
                text = formatClockDuration(totalTimeMs),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SummaryMetricsRow(
    totalPlays: Int,
    uniqueSongs: Int,
    uniqueArtists: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryMetricTile(
            label = "Total Plays",
            value = totalPlays.toString(),
            modifier = Modifier
                .weight(1f)
                .testTag("stats_total_plays_card"),
        )
        SummaryMetricTile(
            label = "Unique Songs",
            value = uniqueSongs.toString(),
            modifier = Modifier
                .weight(1f)
                .testTag("stats_unique_songs_card"),
        )
        SummaryMetricTile(
            label = "Unique Artists",
            value = uniqueArtists.toString(),
            modifier = Modifier
                .weight(1f)
                .testTag("stats_unique_artists_card"),
        )
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = shape,
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FavouriteArtistCard(
    artist: ArtistStat,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val songWord = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(enabled = artist.topTrack != null, onClick = onClick)
            .testTag("stats_favourite_artist_card"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Your Favourite Artist",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp),
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$songWord • ${formatReadableDuration(artist.totalTimeMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Open ${artist.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FavouriteSongCard(
    song: SongStat,
    thumbnailCornerRadius: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val thumbCorner = (thumbnailCornerRadius * 12f / 16f).roundToInt().coerceIn(0, 28).dp
    val playWord = if (song.playCount == 1) "1 time" else "${song.playCount} times"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .testTag("stats_favourite_song_card"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Your Favourite Song",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(thumbCorner))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    SubcomposeAsyncImage(
                        model = song.track.thumbnailUrl,
                        contentDescription = "${song.track.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                        error = {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = song.track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$playWord • ${formatReadableDuration(song.totalTimeMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play ${song.track.title}",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistShareDonutCard(
    shares: List<ArtistShareStat>,
) {
    val shape = RoundedCornerShape(20.dp)
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, 0.5f),
        MaterialTheme.colorScheme.outline,
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .testTag("stats_artist_share_card"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Artist Share",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val diameter = size.minDimension
                        val strokePx = diameter * 0.19f
                        val inset = strokePx / 2f
                        val arcSize = Size(diameter - strokePx, diameter - strokePx)
                        val topLeft = Offset(
                            (size.width - arcSize.width) / 2f,
                            (size.height - arcSize.height) / 2f,
                        )
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                        )

                        var startAngle = -90f
                        val gapAngle = if (shares.size > 1) 2.5f else 0f
                        shares.forEach { share ->
                            val rawSweep = (share.percentage / 100f) * 360f
                            val sweep = (rawSweep - gapAngle).coerceAtLeast(1.5f)
                            val color = palette[share.colorIndex.coerceIn(0, palette.lastIndex)]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                            )
                            startAngle += rawSweep
                        }
                    }

                    shares.firstOrNull()?.let { top ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${top.percentage.roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Top",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shares.forEach { share ->
                        val swatchColor = palette[share.colorIndex.coerceIn(0, palette.lastIndex)]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor),
                            )
                            Text(
                                text = share.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${share.percentage.roundToInt()}% • ${formatReadableDuration(share.totalTimeMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningByDayCard(
    days: List<DayStat>,
) {
    val shape = RoundedCornerShape(20.dp)
    val maxDayMs = remember(days) { days.maxOfOrNull { it.totalTimeMs }?.coerceAtLeast(1L) ?: 1L }
    val barColor = MaterialTheme.colorScheme.primary
    val inactiveBarColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val peakDayIndex = remember(days) {
        days.indices.maxByOrNull { days[it].totalTimeMs } ?: -1
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .testTag("stats_weekly_bar_card"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Listening by Day",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val peak = days.getOrNull(peakDayIndex)
                if (peak != null && peak.totalTimeMs > 0L) {
                    Text(
                        text = "Peak: ${peak.dayLabel} (${formatReadableDuration(peak.totalTimeMs)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            ) {
                if (days.isEmpty()) return@Canvas
                val slotWidth = size.width / days.size.toFloat()
                val barWidth = (slotWidth * 0.46f).coerceAtMost(32.dp.toPx())
                val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
                val maxHeight = size.height

                days.forEachIndexed { index, day ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    // Subtle background track pill
                    drawRoundRect(
                        color = inactiveBarColor.copy(alpha = 0.55f),
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, maxHeight),
                        cornerRadius = corner,
                    )

                    if (day.totalTimeMs > 0L) {
                        val fraction = (day.totalTimeMs.toFloat() / maxDayMs.toFloat()).coerceIn(0.08f, 1f)
                        val barHeight = maxHeight * fraction
                        drawRoundRect(
                            color = if (index == peakDayIndex) barColor else barColor.copy(alpha = 0.72f),
                            topLeft = Offset(left, maxHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                days.forEachIndexed { index, day ->
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == peakDayIndex && day.totalTimeMs > 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun formatClockDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun formatReadableDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
