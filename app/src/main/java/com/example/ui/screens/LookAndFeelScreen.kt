package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ContentLayoutStyle
import com.example.model.DarkThemeMode
import com.example.model.MiniPlayerStyle
import com.example.model.NavBarStyle
import com.example.model.NowPlayingStyle
import com.example.model.PlayerBackgroundStyle
import com.example.model.PlayerButtonsLayout
import com.example.model.PlayerDesignStyle
import com.example.model.PlayerSliderStyle
import com.example.model.SearchBarStyle
import com.example.model.ThemeSettings
import com.example.model.VisualizerStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LookAndFeelScreen(
    settings: ThemeSettings,
    onBack: () -> Unit,
    onSetDynamicArtworkTheme: (Boolean) -> Unit,
    onSetDarkThemeMode: (DarkThemeMode) -> Unit,
    onSetPureBlack: (Boolean) -> Unit,
    onSetPlayerBackgroundStyle: (PlayerBackgroundStyle) -> Unit,
    onSetLiquidGlass: (Boolean) -> Unit,
    onSetGlassIntensity: (Float) -> Unit,
    onSetPlayerButtonsLayout: (PlayerButtonsLayout) -> Unit,
    onSetNowPlayingStyle: (NowPlayingStyle) -> Unit,
    onSetPlayerDesignStyle: (PlayerDesignStyle) -> Unit,
    onSetPlayerSliderStyle: (PlayerSliderStyle) -> Unit,
    onSetMiniPlayerStyle: (MiniPlayerStyle) -> Unit,
    onSetNavBarStyle: (NavBarStyle) -> Unit,
    onSetContentLayoutStyle: (ContentLayoutStyle) -> Unit,
    onSetMiniPlayerCornerRadiusDp: (Int) -> Unit,
    onSetMiniPlayerShowLike: (Boolean) -> Unit,
    onSetMiniPlayerShowPrevious: (Boolean) -> Unit,
    onSetMiniPlayerSwipeToSkip: (Boolean) -> Unit,
    onSetThumbnailCornerRadius: (Int) -> Unit,
    onSetShowSpectralVisualizer: (Boolean) -> Unit,
    onSetVisualizerStyle: (VisualizerStyle) -> Unit,
    onSetSearchBarStyle: (SearchBarStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("look_and_feel_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Settings",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Look & Feel",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSettingCard(
                    title = "Enable dynamic theme",
                    subtitle = "Take the app's colours from the cover of the song that is playing. Off, or with nothing playing, it uses your phone's own colours (Android 12 and newer).",
                    checked = settings.dynamicArtworkTheme,
                    onCheckedChange = onSetDynamicArtworkTheme,
                    testTag = "switch_dynamic_theme",
                )

                SelectorSettingCard(
                    title = "Dark theme",
                    subtitle = "Choose when RazorTune uses dark surfaces.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DarkThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.darkThemeMode == mode,
                                onClick = { onSetDarkThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = DarkThemeMode.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dark_theme_${mode.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = mode.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                ToggleSettingCard(
                    title = "Pure black",
                    subtitle = "Use true black instead of dark grey. On an OLED screen this also saves a little battery.",
                    checked = settings.pureBlack,
                    onCheckedChange = onSetPureBlack,
                    testTag = "switch_pure_black",
                )

                SelectorSettingCard(
                    title = "Now Playing style",
                    subtitle = "Controls the artwork layout and upper visual presentation.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        NowPlayingStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.nowPlayingStyle == style,
                                onClick = { onSetNowPlayingStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = NowPlayingStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("now_playing_style_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                ToggleSettingCard(
                    title = "Spectral Bar Visualizer",
                    subtitle = "Display a live audio frequency visualizer under the cover art in Now Playing. Automatically pauses when battery visual reduction is enabled.",
                    checked = settings.showSpectralVisualizer,
                    onCheckedChange = onSetShowSpectralVisualizer,
                    testTag = "switch_spectral_visualizer",
                )

                AnimatedVisibility(visible = settings.showSpectralVisualizer) {
                    SelectorSettingCard(
                        title = "Visualizer style",
                        subtitle = "Choose how frequencies are rendered beneath the album artwork.",
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            VisualizerStyle.entries.forEachIndexed { index, style ->
                                SegmentedButton(
                                    selected = settings.visualizerStyle == style,
                                    onClick = { onSetVisualizerStyle(style) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = VisualizerStyle.entries.size,
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("visualizer_style_${style.name.lowercase()}"),
                                    label = {
                                        Text(
                                            text = style.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Search Bar Style",
                    subtitle = "Customize the appearance and behavior of the search bar.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SearchBarStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.searchBarStyle == style,
                                onClick = { onSetSearchBarStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SearchBarStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("search_bar_style_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Player design style",
                    subtitle = "Controls the transport controls, seek bar, and Mini-Player styling.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PlayerDesignStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.playerDesignStyle == style,
                                onClick = { onSetPlayerDesignStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PlayerDesignStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("player_design_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Player slider style",
                    subtitle = "Customize the progress bar track style on the Now Playing screen.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PlayerSliderStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.playerSliderStyle == style,
                                onClick = { onSetPlayerSliderStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PlayerSliderStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("player_slider_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Navigation Bar Style",
                    subtitle = "Choose the shape and layout of the bottom navigation bar.",
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NavBarStyle.entries.forEach { style ->
                            val selected = settings.navBarStyle == style
                            FilterChip(
                                selected = selected,
                                onClick = { onSetNavBarStyle(style) },
                                label = {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("nav_bar_style_${style.name.lowercase()}"),
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Content Layout Style",
                    subtitle = "Choose density and card sizing across Home shelves, Search results, and Playlists.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ContentLayoutStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.contentLayoutStyle == style,
                                onClick = { onSetContentLayoutStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ContentLayoutStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("content_layout_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Mini-Player style",
                    subtitle = "Choose how the persistent bottom Mini-Player is docked or styled.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        MiniPlayerStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.miniPlayerStyle == style,
                                onClick = { onSetMiniPlayerStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = MiniPlayerStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("mini_player_style_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                IntSliderSettingCard(
                    title = "Mini-Player corner radius",
                    subtitle = "Adjust the curvature of the mini-player container.",
                    value = settings.miniPlayerCornerRadiusDp,
                    valueRange = 0..32,
                    unitLabel = "dp",
                    onValueChange = onSetMiniPlayerCornerRadiusDp,
                    testTag = "slider_mini_player_radius",
                )

                ThumbnailRadiusSettingCard(
                    title = "Customize thumbnail corner radius",
                    subtitle = "Adjust the corner curvature for album covers and thumbnails throughout RazorTune.",
                    value = settings.thumbnailCornerRadius,
                    valueRange = 0..32,
                    onValueChange = onSetThumbnailCornerRadius,
                    testTag = "slider_thumbnail_corner_radius",
                )

                ToggleSettingCard(
                    title = "Show Like button on Mini-Player",
                    subtitle = "Add a quick Like/Unlike heart button directly on the Mini-Player.",
                    checked = settings.miniPlayerShowLike,
                    onCheckedChange = onSetMiniPlayerShowLike,
                    testTag = "switch_mini_player_like",
                )

                ToggleSettingCard(
                    title = "Show Previous button on Mini-Player",
                    subtitle = "Add a Previous track button beside Play/Pause and Next.",
                    checked = settings.miniPlayerShowPrevious,
                    onCheckedChange = onSetMiniPlayerShowPrevious,
                    testTag = "switch_mini_player_prev",
                )

                ToggleSettingCard(
                    title = "Swipe to skip on Mini-Player",
                    subtitle = "Swipe left/right on the Mini-Player to skip tracks.",
                    checked = settings.miniPlayerSwipeToSkip,
                    onCheckedChange = onSetMiniPlayerSwipeToSkip,
                    testTag = "switch_mini_player_swipe",
                )

                SelectorSettingCard(
                    title = "Player background style",
                    subtitle = "Customize the backdrop behind the Now Playing screen.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PlayerBackgroundStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = settings.playerBackgroundStyle == style,
                                onClick = { onSetPlayerBackgroundStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PlayerBackgroundStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("player_bg_${style.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = style.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                SelectorSettingCard(
                    title = "Player buttons",
                    subtitle = "Connected puts share and like together by the title, with lyrics, the sleep timer and the menu in a row under the controls.",
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PlayerButtonsLayout.entries.forEachIndexed { index, layout ->
                            SegmentedButton(
                                selected = settings.playerButtonsLayout == layout,
                                onClick = { onSetPlayerButtonsLayout(layout) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PlayerButtonsLayout.entries.size,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("player_buttons_${layout.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = layout.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                            )
                        }
                    }
                }

                ToggleSettingCard(
                    title = "Liquid glass",
                    subtitle = "Floating glass panels for the navigation bar, mini player and transport controls, and richer artwork colours behind the now playing screen",
                    checked = settings.liquidGlass,
                    onCheckedChange = onSetLiquidGlass,
                    testTag = "switch_liquid_glass",
                )

                SliderSettingCard(
                    title = "Glass intensity",
                    subtitle = "Higher is more see-through and more strongly refracted; lower is closer to plain solid surfaces",
                    value = settings.glassIntensity,
                    enabled = settings.liquidGlass,
                    onValueChange = onSetGlassIntensity,
                    testTag = "slider_glass_intensity",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SelectorSettingCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun SliderSettingCard(
    title: String,
    subtitle: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(20.dp)
    val percent = (value.coerceIn(0f, 1f) * 100f).roundToInt()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.52f)
            .border(
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value.coerceIn(0f, 1f),
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
            )
        }
    }
}

@Composable
private fun IntSliderSettingCard(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: IntRange,
    unitLabel: String,
    onValueChange: (Int) -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "$value $unitLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = valueRange.last - valueRange.first - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
            )
        }
    }
}

@Composable
private fun ThumbnailRadiusSettingCard(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Live preview thumbnail box showing the curve adjust in real time!
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(value.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(value.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Preview thumbnail corner radius",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "$value dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Slider(
                value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = valueRange.last - valueRange.first - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
            )
        }
    }
}

