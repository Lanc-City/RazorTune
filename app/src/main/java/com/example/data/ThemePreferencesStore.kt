package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "razortune_theme")

class ThemePreferencesStore(private val context: Context) {

    private object Keys {
        val DYNAMIC_ARTWORK_THEME = booleanPreferencesKey("dynamic_artwork_theme")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val PLAYER_BACKGROUND_STYLE = stringPreferencesKey("player_background_style")
        val LIQUID_GLASS = booleanPreferencesKey("liquid_glass")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val PLAYER_BUTTONS_LAYOUT = stringPreferencesKey("player_buttons_layout")
        val NOW_PLAYING_STYLE = stringPreferencesKey("now_playing_style")
        val PLAYER_DESIGN_STYLE = stringPreferencesKey("player_design_style")
        val PLAYER_SLIDER_STYLE = stringPreferencesKey("player_slider_style")
        val MINI_PLAYER_STYLE = stringPreferencesKey("mini_player_style")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val CONTENT_LAYOUT_STYLE = stringPreferencesKey("content_layout_style")
        val MINI_PLAYER_CORNER_RADIUS = intPreferencesKey("mini_player_corner_radius")
        val MINI_PLAYER_SHOW_LIKE = booleanPreferencesKey("mini_player_show_like")
        val MINI_PLAYER_SHOW_PREVIOUS = booleanPreferencesKey("mini_player_show_previous")
        val MINI_PLAYER_SWIPE_TO_SKIP = booleanPreferencesKey("mini_player_swipe_to_skip")
        val THUMBNAIL_CORNER_RADIUS = intPreferencesKey("thumbnail_corner_radius")
        val SHOW_SPECTRAL_VISUALIZER = booleanPreferencesKey("show_spectral_visualizer")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val SEARCH_BAR_STYLE = stringPreferencesKey("search_bar_style")
        val CUSTOM_DNS_ENABLED = booleanPreferencesKey("custom_dns_enabled")
    }

    val settingsFlow: Flow<ThemeSettings> = context.themeDataStore.data.map { prefs ->
        val darkMode = prefs[Keys.DARK_THEME_MODE]?.let { name ->
            runCatching { DarkThemeMode.valueOf(name) }.getOrNull()
        } ?: DarkThemeMode.FOLLOW_SYSTEM

        val bgStyle = prefs[Keys.PLAYER_BACKGROUND_STYLE]?.let { name ->
            runCatching { PlayerBackgroundStyle.valueOf(name) }.getOrNull()
        } ?: PlayerBackgroundStyle.BLUR

        val buttonsLayout = prefs[Keys.PLAYER_BUTTONS_LAYOUT]?.let { name ->
            runCatching { PlayerButtonsLayout.valueOf(name) }.getOrNull()
        } ?: PlayerButtonsLayout.CONNECTED

        val npStyle = prefs[Keys.NOW_PLAYING_STYLE]?.let { name ->
            runCatching { NowPlayingStyle.valueOf(name) }.getOrNull()
        } ?: NowPlayingStyle.CLASSIC

        val designStyle = prefs[Keys.PLAYER_DESIGN_STYLE]?.let { name ->
            runCatching { PlayerDesignStyle.valueOf(name) }.getOrNull()
        } ?: PlayerDesignStyle.MODERN_GLASS

        val sliderStyle = prefs[Keys.PLAYER_SLIDER_STYLE]?.let { name ->
            runCatching { PlayerSliderStyle.valueOf(name) }.getOrNull()
        } ?: PlayerSliderStyle.STANDARD

        val miniStyle = prefs[Keys.MINI_PLAYER_STYLE]?.let { name ->
            runCatching { MiniPlayerStyle.valueOf(name) }.getOrNull()
        } ?: MiniPlayerStyle.FLOATING_CARD

        val navStyle = prefs[Keys.NAV_BAR_STYLE]?.let { name ->
            runCatching { NavBarStyle.valueOf(name) }.getOrNull()
        } ?: NavBarStyle.FLOATING_GLASS

        val layoutStyle = prefs[Keys.CONTENT_LAYOUT_STYLE]?.let { name ->
            runCatching { ContentLayoutStyle.valueOf(name) }.getOrNull()
        } ?: ContentLayoutStyle.STANDARD

        val visStyle = prefs[Keys.VISUALIZER_STYLE]?.let { name ->
            runCatching { VisualizerStyle.valueOf(name) }.getOrNull()
        } ?: VisualizerStyle.BARS

        val searchStyle = prefs[Keys.SEARCH_BAR_STYLE]?.let { name ->
            runCatching { SearchBarStyle.valueOf(name) }.getOrNull()
        } ?: SearchBarStyle.CLASSIC_BOX

        ThemeSettings(
            dynamicArtworkTheme = prefs[Keys.DYNAMIC_ARTWORK_THEME] ?: false,
            darkThemeMode = darkMode,
            pureBlack = prefs[Keys.PURE_BLACK] ?: false,
            playerBackgroundStyle = bgStyle,
            liquidGlass = prefs[Keys.LIQUID_GLASS] ?: false,
            glassIntensity = (prefs[Keys.GLASS_INTENSITY] ?: 0.6f).coerceIn(0f, 1f),
            playerButtonsLayout = buttonsLayout,
            nowPlayingStyle = npStyle,
            playerDesignStyle = designStyle,
            playerSliderStyle = sliderStyle,
            miniPlayerStyle = miniStyle,
            navBarStyle = navStyle,
            contentLayoutStyle = layoutStyle,
            miniPlayerCornerRadiusDp = prefs[Keys.MINI_PLAYER_CORNER_RADIUS] ?: 20,
            miniPlayerShowLike = prefs[Keys.MINI_PLAYER_SHOW_LIKE] ?: false,
            miniPlayerShowPrevious = prefs[Keys.MINI_PLAYER_SHOW_PREVIOUS] ?: false,
            miniPlayerSwipeToSkip = prefs[Keys.MINI_PLAYER_SWIPE_TO_SKIP] ?: true,
            thumbnailCornerRadius = prefs[Keys.THUMBNAIL_CORNER_RADIUS] ?: 16,
            showSpectralVisualizer = prefs[Keys.SHOW_SPECTRAL_VISUALIZER] ?: true,
            visualizerStyle = visStyle,
            searchBarStyle = searchStyle,
            customDnsEnabled = prefs[Keys.CUSTOM_DNS_ENABLED] ?: false,
        )
    }

    suspend fun setDynamicArtworkTheme(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.DYNAMIC_ARTWORK_THEME] = enabled }
    }

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[Keys.DARK_THEME_MODE] = mode.name }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PURE_BLACK] = enabled }
    }

    suspend fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PLAYER_BACKGROUND_STYLE] = style.name }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.LIQUID_GLASS] = enabled }
    }

    suspend fun setGlassIntensity(intensity: Float) {
        context.themeDataStore.edit { prefs -> prefs[Keys.GLASS_INTENSITY] = intensity.coerceIn(0f, 1f) }
    }

    suspend fun setPlayerButtonsLayout(layout: PlayerButtonsLayout) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PLAYER_BUTTONS_LAYOUT] = layout.name }
    }

    suspend fun setNowPlayingStyle(style: NowPlayingStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.NOW_PLAYING_STYLE] = style.name }
    }

    suspend fun setPlayerDesignStyle(style: PlayerDesignStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PLAYER_DESIGN_STYLE] = style.name }
    }

    suspend fun setPlayerSliderStyle(style: PlayerSliderStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PLAYER_SLIDER_STYLE] = style.name }
    }

    suspend fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MINI_PLAYER_STYLE] = style.name }
    }

    suspend fun setNavBarStyle(style: NavBarStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.NAV_BAR_STYLE] = style.name }
    }

    suspend fun setContentLayoutStyle(style: ContentLayoutStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.CONTENT_LAYOUT_STYLE] = style.name }
    }

    suspend fun setMiniPlayerCornerRadiusDp(radius: Int) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MINI_PLAYER_CORNER_RADIUS] = radius.coerceIn(0, 32) }
    }

    suspend fun setMiniPlayerShowLike(show: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MINI_PLAYER_SHOW_LIKE] = show }
    }

    suspend fun setMiniPlayerShowPrevious(show: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MINI_PLAYER_SHOW_PREVIOUS] = show }
    }

    suspend fun setMiniPlayerSwipeToSkip(swipe: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MINI_PLAYER_SWIPE_TO_SKIP] = swipe }
    }

    suspend fun setThumbnailCornerRadius(radius: Int) {
        context.themeDataStore.edit { prefs -> prefs[Keys.THUMBNAIL_CORNER_RADIUS] = radius.coerceIn(0, 32) }
    }

    suspend fun setShowSpectralVisualizer(show: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.SHOW_SPECTRAL_VISUALIZER] = show }
    }

    suspend fun setVisualizerStyle(style: VisualizerStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.VISUALIZER_STYLE] = style.name }
    }

    suspend fun setSearchBarStyle(style: SearchBarStyle) {
        context.themeDataStore.edit { prefs -> prefs[Keys.SEARCH_BAR_STYLE] = style.name }
    }

    suspend fun setCustomDnsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[Keys.CUSTOM_DNS_ENABLED] = enabled }
    }
}
