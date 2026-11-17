package com.example.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AudioQualityMode
import com.example.data.PerformanceSettings

@Composable
fun BatteryPerformanceScreen(
    settings: PerformanceSettings,
    onBack: () -> Unit,
    onSetAudioQuality: (AudioQualityMode) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onSetPreloadNextTrack: (Boolean) -> Unit,
    onSetReduceVisualEffects: (Boolean) -> Unit,
    onSetAutoReduceOnLowBattery: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    var isUnrestrictedBattery by remember {
        mutableStateOf(
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false,
        )
    }
    var isSystemPowerSaveMode by remember {
        mutableStateOf(powerManager?.isPowerSaveMode ?: false)
    }

    DisposableEffect(lifecycleOwner, context, powerManager) {
        fun refreshPowerState() {
            isUnrestrictedBattery =
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            isSystemPowerSaveMode = powerManager?.isPowerSaveMode ?: false
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPowerState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        val powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshPowerState()
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        ContextCompat.registerReceiver(
            context,
            powerSaveReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching { context.unregisterReceiver(powerSaveReceiver) }
        }
    }

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
                        .testTag("battery_performance_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Settings",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Battery & Performance",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val cardShape = RoundedCornerShape(20.dp)

                // 1. Background Playback Health Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isUnrestrictedBattery) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isUnrestrictedBattery) {
                                        Icons.Rounded.BatteryChargingFull
                                    } else {
                                        Icons.Rounded.BatteryAlert
                                    },
                                    contentDescription = null,
                                    tint = if (isUnrestrictedBattery) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Playback Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isUnrestrictedBattery) {
                                        "Unrestricted (Optimal for background playback)"
                                    } else {
                                        "Battery optimization active (may pause playback in background)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUnrestrictedBattery) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                        }

                        if (isSystemPowerSaveMode) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BatterySaver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = if (settings.autoReduceOnLowBattery) {
                                            "System Battery Saver is active — blur & continuous animations are paused."
                                        } else {
                                            "System Battery Saver is active."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }

                        if (!isUnrestrictedBattery) {
                            Button(
                                onClick = { openBatteryOptimizationSettings(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("battery_allow_unrestricted_button"),
                            ) {
                                Text("Allow Unrestricted Battery")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { openBatteryOptimizationSettings(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("battery_manage_settings_button"),
                            ) {
                                Text("Manage System Battery Settings")
                            }
                        }
                    }
                }

                // 2. Streaming & Radio Efficiency Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        SectionHeaderRow(
                            icon = Icons.Rounded.Tune,
                            title = "Streaming & Radio Efficiency",
                            subtitle = "Adjust audio bitrate and preloading to balance sound quality with battery and data usage.",
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AudioQualityMode.entries.forEach { mode ->
                                val selected = settings.audioQuality == mode
                                val rowShape = RoundedCornerShape(14.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clip(rowShape)
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            },
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = rowShape,
                                        )
                                        .clickable { onSetAudioQuality(mode) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("audio_quality_${mode.name.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { onSetAudioQuality(mode) },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        PerformanceSwitchRow(
                            title = "Preload Next Track",
                            subtitle = "Pre-resolves the upcoming song stream URL so transitions feel instant. Disable to reduce background network wakeups.",
                            checked = settings.preloadNextTrack,
                            onCheckedChange = onSetPreloadNextTrack,
                            testTag = "switch_preload_next_track",
                        )
                    }
                }

                // 3. Playback Engine Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        SectionHeaderRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Playback Engine",
                            subtitle = "Configure ExoPlayer real-time audio processing.",
                        )

                        PerformanceSwitchRow(
                            title = "Skip Silence",
                            subtitle = "Automatically compresses quiet gaps and long intros or outros during playback.",
                            checked = settings.skipSilence,
                            onCheckedChange = onSetSkipSilence,
                            testTag = "switch_skip_silence",
                        )
                    }
                }

                // 4. GPU & Display Power Saver Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        SectionHeaderRow(
                            icon = Icons.Rounded.Speed,
                            title = "GPU & Display Power Saver",
                            subtitle = "Lower GPU compositing work and continuous frame redraws on the Now Playing screen.",
                        )

                        PerformanceSwitchRow(
                            title = "Reduce Blur & Continuous Animations",
                            subtitle = "Replaces live artwork blur with a lightweight gradient and pauses vinyl rotation and squiggly wave motion.",
                            checked = settings.reduceVisualEffects,
                            onCheckedChange = onSetReduceVisualEffects,
                            testTag = "switch_reduce_visual_effects",
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        PerformanceSwitchRow(
                            title = "Auto-Reduce in System Battery Saver",
                            subtitle = "Automatically pauses blur and continuous animations whenever Android Battery Saver is active.",
                            checked = settings.autoReduceOnLowBattery,
                            onCheckedChange = onSetAutoReduceOnLowBattery,
                            testTag = "switch_auto_reduce_low_battery",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PerformanceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val optimizationIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val launchedOptimization = runCatching {
        context.startActivity(optimizationIntent)
    }.isSuccess

    if (!launchedOptimization) {
        val appDetailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(appDetailsIntent) }
    }
}
