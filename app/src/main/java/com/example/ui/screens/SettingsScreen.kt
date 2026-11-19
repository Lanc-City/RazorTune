package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ThemeSettings
import com.example.ui.viewmodel.AccountUiState

@Composable
fun SettingsScreen(
    accountState: AccountUiState,
    themeSettings: ThemeSettings,
    onBack: () -> Unit,
    onOpenAccountAndSync: () -> Unit,
    onOpenLookAndFeel: () -> Unit,
    onToggleCustomDns: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAbout: () -> Unit = {},
    storageSummary: String = "Manage offline downloads & cache",
    batteryPerformanceSummary: String = "High quality • Background health",
    audioEffectsSummary: String = "Off",
    onOpenStorage: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenBatteryPerformance: () -> Unit = {},
    onOpenAudioEffects: () -> Unit = {},
) {
    val accountSubtitle = if (accountState.isSignedIn) {
        val name = accountState.accountInfo?.name ?: "YouTube Music Connected"
        "$name • Manage login & sync playlists"
    } else {
        "Sign in to YouTube Music & sync playlists"
    }

    val lookAndFeelSubtitle = buildString {
        append(themeSettings.darkThemeMode.label)
        append(" • ")
        append(themeSettings.playerBackgroundStyle.label)
        append(" player")
        if (themeSettings.liquidGlass) {
            append(" • Liquid glass")
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
            Spacer(modifier = Modifier.height(12.dp))

            // Modern Top Bar Header with subtle back navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grouped modern settings sections
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsGroupSection(title = "Preferences") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsCategoryCard(
                            title = "Account & Sync",
                            subtitle = accountSubtitle,
                            icon = Icons.Rounded.AccountCircle,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenAccountAndSync,
                            testTag = "settings_account_card",
                        )

                        SettingsCategoryCard(
                            title = "Look & Feel",
                            subtitle = lookAndFeelSubtitle,
                            icon = Icons.Rounded.Palette,
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenLookAndFeel,
                            testTag = "settings_look_and_feel_card",
                        )
                    }
                }

                SettingsGroupSection(title = "Playback & Audio") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsCategoryCard(
                            title = "Listening Stats",
                            subtitle = "Play history, top artists & listening trends",
                            icon = Icons.Rounded.BarChart,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenStats,
                            testTag = "settings_stats_card",
                        )

                        SettingsCategoryCard(
                            title = "Audio Effects & EQ",
                            subtitle = audioEffectsSummary,
                            icon = Icons.Rounded.Equalizer,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onOpenAudioEffects,
                            testTag = "settings_audio_effects_card",
                        )
                    }
                }

                SettingsGroupSection(title = "System & Network") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsCategoryCard(
                            title = "Storage",
                            subtitle = storageSummary,
                            icon = Icons.Rounded.Storage,
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenStorage,
                            testTag = "settings_storage_card",
                        )

                        SettingsCategoryCard(
                            title = "Battery & Performance",
                            subtitle = batteryPerformanceSummary,
                            icon = Icons.Rounded.BatteryChargingFull,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenBatteryPerformance,
                            testTag = "settings_battery_performance_card",
                        )

                        SettingsSwitchCategoryCard(
                            title = "Cloudflare DoH (Bypass Wi-Fi Limits)",
                            subtitle = if (themeSettings.customDnsEnabled) {
                                "Active • Using 1.1.1.1 over HTTPS"
                            } else {
                                "Off • Using system default DNS"
                            },
                            icon = Icons.Rounded.Code,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            checked = themeSettings.customDnsEnabled,
                            onCheckedChange = onToggleCustomDns,
                            testTag = "settings_custom_dns_card",
                        )
                    }
                }

                SettingsGroupSection(title = "Info") {
                    SettingsCategoryCard(
                        title = "About RazorTune",
                        subtitle = "Version 1.0.0 • Open source licenses & info",
                        icon = Icons.Rounded.Code,
                        iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenAbout,
                        testTag = "settings_about_card",
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsGroupSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SettingsSwitchCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
