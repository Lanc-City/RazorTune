package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import com.example.data.AudioEffectsPreferencesStore
import com.example.data.AudioEffectsSettings
import com.example.data.EqPreset
import java.util.Locale
import kotlin.math.roundToInt

private val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@Composable
fun AudioEffectsScreen(
    settings: AudioEffectsSettings,
    audioSessionId: Int,
    onBack: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSetBandLevel: (bandIndex: Int, levelMb: Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val systemEqIntent = remember(context, audioSessionId) {
        buildSystemEffectsIntent(context, audioSessionId)
    }
    val hasSystemEqPanel = remember(context, systemEqIntent) {
        systemEqIntent.resolveActivity(context.packageManager) != null
    }

    val controlsAlpha = if (settings.enabled) 1f else 0.72f
    val cardShape = RoundedCornerShape(20.dp)

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
                        .testTag("audio_effects_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Settings",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Audio Effects & EQ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onResetAll,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("audio_effects_reset_button"),
                ) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Master Enable Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                        .clickable { onSetEnabled(!settings.enabled) },
                    color = if (settings.enabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = cardShape,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (settings.enabled) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = if (settings.enabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Enable Audio Effects",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (settings.enabled) {
                                    "${settings.preset.label} preset active on ExoPlayer DSP"
                                } else {
                                    "Turn on equalizer, loudness gain, bass boost & spatializer"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onSetEnabled,
                            modifier = Modifier.testTag("switch_audio_effects_enabled"),
                        )
                    }
                }

                // 2. Loudness & Gain Booster Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(controlsAlpha)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = cardShape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EffectSectionHeader(
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                title = "Loudness & Gain Booster",
                                subtitle = "Amplify perceived volume beyond standard output",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ReadoutBadge(text = formatGainDb(settings.loudnessGainMb))
                        }

                        Slider(
                            value = settings.loudnessGainMb.toFloat(),
                            onValueChange = { value ->
                                val stepped = ((value / 50f).roundToInt() * 50).coerceIn(0, 1000)
                                onSetLoudnessGain(stepped)
                            },
                            valueRange = 0f..1000f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_loudness_gain"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "0 dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "+5 dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "+10 dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (settings.loudnessGainMb > 600) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "High gain boost (> +6 dB) may introduce clipping or speaker distortion on mastered tracks.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Bass Boost & Spatial Width Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(controlsAlpha)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = cardShape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EffectSectionHeader(
                            icon = Icons.Rounded.SpatialAudio,
                            title = "Bass Boost & Spatial Width",
                            subtitle = "Low-frequency punch and stereo soundstage expansion",
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Bass Boost",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                ReadoutBadge(text = "${(settings.bassBoostStrength / 10f).roundToInt()}%")
                            }
                            Slider(
                                value = settings.bassBoostStrength.toFloat(),
                                onValueChange = { value ->
                                    val stepped = ((value / 20f).roundToInt() * 20).coerceIn(0, 1000)
                                    onSetBassBoost(stepped)
                                },
                                valueRange = 0f..1000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slider_bass_boost"),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Spatial Virtualizer",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                ReadoutBadge(text = "${(settings.virtualizerStrength / 10f).roundToInt()}%")
                            }
                            Slider(
                                value = settings.virtualizerStrength.toFloat(),
                                onValueChange = { value ->
                                    val stepped = ((value / 20f).roundToInt() * 20).coerceIn(0, 1000)
                                    onSetVirtualizer(stepped)
                                },
                                valueRange = 0f..1000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slider_virtualizer"),
                            )
                        }
                    }
                }

                // 4. 5-Band Graphic Equalizer Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(controlsAlpha)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = cardShape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        EffectSectionHeader(
                            icon = Icons.Rounded.GraphicEq,
                            title = "5-Band Graphic Equalizer",
                            subtitle = "Choose a sound profile or tune each frequency band (-12 dB to +12 dB)",
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EqPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = settings.preset == preset,
                                    onClick = { onSelectPreset(preset) },
                                    label = {
                                        Text(
                                            text = preset.label,
                                            maxLines = 1,
                                        )
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("eq_preset_${preset.name.lowercase()}"),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BAND_LABELS.forEachIndexed { index, bandLabel ->
                                val levelMb = settings.bandLevelsMb.getOrElse(index) { 0 }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = formatEqBandDb(levelMb),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = if (levelMb == 0) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                    )

                                    VerticalEqSlider(
                                        valueMb = levelMb,
                                        bandLabel = bandLabel,
                                        onValueChange = { updatedMb ->
                                            onSetBandLevel(index, updatedMb)
                                        },
                                        modifier = Modifier.testTag("eq_band_$index"),
                                    )

                                    Text(
                                        text = bandLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Optional System Audio Effects Panel button
                if (hasSystemEqPanel) {
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(systemEqIntent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("audio_effects_system_panel_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Audio Settings")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun EffectSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
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
    }
}

@Composable
private fun ReadoutBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun VerticalEqSlider(
    valueMb: Int,
    bandLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val minMb = AudioEffectsPreferencesStore.MIN_BAND_MB
    val maxMb = AudioEffectsPreferencesStore.MAX_BAND_MB
    val span = (maxMb - minMb).toFloat()

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val zeroTickColor = MaterialTheme.colorScheme.outline
    val thumbHaloColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    fun mapYToMb(y: Float, height: Float): Int {
        if (height <= 0f) return 0
        val fraction = (1f - (y / height)).coerceIn(0f, 1f)
        val raw = minMb + fraction * span
        return ((raw / 50f).roundToInt() * 50).coerceIn(minMb, maxMb)
    }

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .height(176.dp)
            .semantics {
                contentDescription = "$bandLabel equalizer band ${formatEqBandDb(valueMb)} decibels"
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    latestOnValueChange(mapYToMb(offset.y, size.height.toFloat()))
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    latestOnValueChange(mapYToMb(change.position.y, size.height.toFloat()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .width(28.dp)
                .height(160.dp),
        ) {
            val trackWidth = 6.dp.toPx()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val thumbRadius = 9.dp.toPx()
            val verticalPadding = thumbRadius
            val usableHeight = (size.height - verticalPadding * 2f).coerceAtLeast(1f)

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(centerX - trackWidth / 2f, verticalPadding),
                size = Size(trackWidth, usableHeight),
                cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f),
            )

            // Subtle 0 dB center notch
            drawLine(
                color = zeroTickColor,
                start = Offset(centerX - trackWidth * 1.4f, centerY),
                end = Offset(centerX + trackWidth * 1.4f, centerY),
                strokeWidth = 1.5.dp.toPx(),
            )

            val fraction = ((valueMb - minMb) / span).coerceIn(0f, 1f)
            val thumbY = verticalPadding + usableHeight * (1f - fraction)
            val fillTop = minOf(centerY, thumbY)
            val fillBottom = maxOf(centerY, thumbY)
            if (fillBottom > fillTop) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(centerX - trackWidth / 2f, fillTop),
                    size = Size(trackWidth, fillBottom - fillTop),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f),
                )
            }

            drawCircle(
                color = thumbHaloColor,
                radius = thumbRadius * 1.45f,
                center = Offset(centerX, thumbY),
            )
            drawCircle(
                color = primaryColor,
                radius = thumbRadius,
                center = Offset(centerX, thumbY),
            )
        }
    }
}

private fun formatGainDb(gainMb: Int): String {
    val db = gainMb / 100f
    return String.format(Locale.US, "+%.1f dB", db)
}

private fun formatEqBandDb(levelMb: Int): String {
    val db = levelMb / 100f
    return when {
        levelMb > 0 -> String.format(Locale.US, "+%.1f", db)
        levelMb < 0 -> String.format(Locale.US, "%.1f", db)
        else -> "0.0"
    }
}

private fun buildSystemEffectsIntent(context: Context, audioSessionId: Int): Intent =
    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        }
    }
