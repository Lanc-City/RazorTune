package com.example.model

import androidx.compose.runtime.Immutable

enum class DarkThemeMode(val label: String) {
    FOLLOW_SYSTEM("Follow system"),
    ON("On"),
    OFF("Off"),
}

enum class PlayerBackgroundStyle(val label: String) {
    BLUR("Blur"),
    GRADIENT("Gradient"),
    SOLID("Solid"),
}

enum class PlayerButtonsLayout(val label: String) {
    CONNECTED("Connected"),
    CLASSIC("Classic"),
}

enum class NowPlayingStyle(val label: String) {
    CLASSIC("Classic"),
    FULL_BLEED("Full-Bleed"),
    VINYL("Vinyl"),
}

enum class PlayerDesignStyle(val label: String) {
    MODERN_GLASS("Modern Glass"),
    MINIMALIST("Minimalist"),
    CAPSULE("Capsule"),
}

enum class PlayerSliderStyle(val label: String) {
    STANDARD("Standard"),
    SQUIGGLY("Squiggly"),
    THICK_BAR("Thick Bar"),
    GLOW("Glow"),
}

enum class MiniPlayerStyle(val label: String) {
    FLOATING_CARD("Floating Card"),
    DOCKED("Docked"),
    COMPACT_PILL("Compact Pill"),
}

enum class NavBarStyle(val label: String) {
    FLOATING_GLASS("Floating Glass"),
    ARCHIVE_PILL("Archive Pill"),
    COMPACT_PILL("Compact Pill"),
    DOCKED("Docked"),
    MINIMAL_ICONS("Minimal Icons"),
}

enum class ContentLayoutStyle(val label: String) {
    STANDARD("Standard"),
    COMPACT("Compact"),
    SHOWCASE("Showcase"),
}

enum class VisualizerStyle(val label: String) {
    BARS("Classic Bars"),
    MIRRORED("Mirrored Wave"),
    DOT_PEAKS("Glowing Peaks"),
}

enum class SearchBarStyle(val label: String) {
    CLASSIC_BOX("Classic Box"),
    CAPSULE_PILL("Capsule Pill"),
    HEADER_EXPAND("Header Icon"),
    FLOATING_BOTTOM("Bottom Thumb Bar"),
}

@Immutable
data class ThemeSettings(
    val dynamicArtworkTheme: Boolean = false,
    val darkThemeMode: DarkThemeMode = DarkThemeMode.FOLLOW_SYSTEM,
    val pureBlack: Boolean = false,
    val playerBackgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.BLUR,
    val liquidGlass: Boolean = false,
    val glassIntensity: Float = 0.6f,
    val playerButtonsLayout: PlayerButtonsLayout = PlayerButtonsLayout.CONNECTED,
    val nowPlayingStyle: NowPlayingStyle = NowPlayingStyle.CLASSIC,
    val playerDesignStyle: PlayerDesignStyle = PlayerDesignStyle.MODERN_GLASS,
    val playerSliderStyle: PlayerSliderStyle = PlayerSliderStyle.STANDARD,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING_CARD,
    val navBarStyle: NavBarStyle = NavBarStyle.FLOATING_GLASS,
    val contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    val miniPlayerCornerRadiusDp: Int = 20,
    val miniPlayerShowLike: Boolean = false,
    val miniPlayerShowPrevious: Boolean = false,
    val miniPlayerSwipeToSkip: Boolean = true,
    val thumbnailCornerRadius: Int = 16,
    val showSpectralVisualizer: Boolean = true,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val searchBarStyle: SearchBarStyle = SearchBarStyle.CLASSIC_BOX,
)

@Immutable
data class ArtworkPaletteColors(
    val primaryArgb: Int,
    val secondaryArgb: Int,
    val tertiaryArgb: Int,
    val darkAccentArgb: Int,
)
