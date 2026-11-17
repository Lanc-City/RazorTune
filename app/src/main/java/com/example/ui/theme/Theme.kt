package com.example.ui.theme

import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.model.ArtworkPaletteColors
import com.example.model.DarkThemeMode
import com.example.model.ThemeSettings

private val RazorTuneDarkColorScheme = darkColorScheme(
    primary = RazorCoral,
    onPrimary = Color.White,
    primaryContainer = RazorCoralContainer,
    onPrimaryContainer = OnRazorCoralContainer,
    secondary = SteelAccent,
    onSecondary = Color(0xFF042029),
    secondaryContainer = SteelContainer,
    onSecondaryContainer = OnSteelContainer,
    tertiary = WarmAmber,
    onTertiary = Color(0xFF261700),
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = ObsidianSurface,
    onSurface = InkPrimary,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = HairlineOutline,
    outlineVariant = HairlineVariant,
)

private val RazorTuneLightColorScheme = lightColorScheme(
    primary = Color(0xFFD92647),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3F0012),
    secondary = Color(0xFF00677D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5ECFF),
    onSecondaryContainer = Color(0xFF001F27),
    tertiary = Color(0xFF8C5000),
    onTertiary = Color.White,
    background = Color(0xFFFAF9FC),
    onBackground = Color(0xFF191B20),
    surface = Color(0xFFFAF9FC),
    onSurface = Color(0xFF191B20),
    surfaceVariant = Color(0xFFE6E8EF),
    onSurfaceVariant = Color(0xFF454854),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F4F8),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE5E8F0),
    surfaceContainerHighest = Color(0xFFDFE2EC),
    outline = Color(0xFF747886),
    outlineVariant = Color(0xFFC5C8D4),
)

private val RazorTuneShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RazorTuneTheme(
    settings: ThemeSettings = ThemeSettings(),
    artworkColors: ArtworkPaletteColors? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = when (settings.darkThemeMode) {
        DarkThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkThemeMode.ON -> true
        DarkThemeMode.OFF -> false
    }

    val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) RazorTuneDarkColorScheme else RazorTuneLightColorScheme
    }

    val artworkTintedScheme = if (settings.dynamicArtworkTheme && artworkColors != null) {
        applyArtworkPalette(baseScheme, artworkColors, isDark)
    } else {
        baseScheme
    }

    val finalScheme = if (isDark && settings.pureBlack) {
        artworkTintedScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF070709),
            surfaceContainer = Color(0xFF0D0E12),
            surfaceContainerHigh = Color(0xFF14161C),
            surfaceContainerHighest = Color(0xFF1B1E26),
        )
    } else {
        artworkTintedScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        shapes = RazorTuneShapes,
        content = content,
    )
}

private fun applyArtworkPalette(
    base: ColorScheme,
    palette: ArtworkPaletteColors,
    isDark: Boolean,
): ColorScheme {
    val primary = tunedAccentColor(palette.primaryArgb, isDark)
    val onPrimary = if (ColorUtils.calculateLuminance(colorInt(primary)) > 0.42) {
        Color(0xFF121318)
    } else {
        Color.White
    }
    val primaryContainer = tunedContainerColor(palette.primaryArgb, isDark)
    val onPrimaryContainer = if (isDark) {
        tunedLightTint(palette.primaryArgb, 0.90f)
    } else {
        tunedDarkTint(palette.primaryArgb, 0.16f)
    }

    val secondary = tunedAccentColor(palette.secondaryArgb, isDark)
    val onSecondary = if (ColorUtils.calculateLuminance(colorInt(secondary)) > 0.42) {
        Color(0xFF121318)
    } else {
        Color.White
    }
    val secondaryContainer = tunedContainerColor(palette.secondaryArgb, isDark)
    val onSecondaryContainer = if (isDark) {
        tunedLightTint(palette.secondaryArgb, 0.88f)
    } else {
        tunedDarkTint(palette.secondaryArgb, 0.18f)
    }

    val tertiary = tunedAccentColor(palette.tertiaryArgb, isDark)
    val surfaceTintColor = if (isDark) {
        Color(ColorUtils.blendARGB(0xFF101218.toInt(), palette.darkAccentArgb, 0.14f))
    } else {
        Color(ColorUtils.blendARGB(0xFFFAF9FC.toInt(), palette.primaryArgb, 0.05f))
    }
    val surfaceLow = if (isDark) {
        Color(ColorUtils.blendARGB(0xFF141720.toInt(), palette.darkAccentArgb, 0.16f))
    } else {
        Color(ColorUtils.blendARGB(0xFFF3F4F8.toInt(), palette.primaryArgb, 0.06f))
    }
    val surfaceContainer = if (isDark) {
        Color(ColorUtils.blendARGB(0xFF1A1E29.toInt(), palette.darkAccentArgb, 0.18f))
    } else {
        Color(ColorUtils.blendARGB(0xFFECEEF4.toInt(), palette.primaryArgb, 0.08f))
    }
    val surfaceHigh = if (isDark) {
        Color(ColorUtils.blendARGB(0xFF212634.toInt(), palette.darkAccentArgb, 0.22f))
    } else {
        Color(ColorUtils.blendARGB(0xFFE5E8F0.toInt(), palette.primaryArgb, 0.10f))
    }
    val surfaceHighest = if (isDark) {
        Color(ColorUtils.blendARGB(0xFF292F40.toInt(), palette.darkAccentArgb, 0.25f))
    } else {
        Color(ColorUtils.blendARGB(0xFFDFE2EC.toInt(), palette.primaryArgb, 0.12f))
    }

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        background = surfaceTintColor,
        surface = surfaceTintColor,
        surfaceContainerLowest = surfaceTintColor,
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
    )
}

private fun tunedAccentColor(@ColorInt argb: Int, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceIn(0.45f, 0.88f)
    hsl[2] = if (isDark) hsl[2].coerceIn(0.62f, 0.78f) else hsl[2].coerceIn(0.32f, 0.48f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun tunedContainerColor(@ColorInt argb: Int, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceIn(0.32f, 0.72f)
    hsl[2] = if (isDark) 0.24f else 0.88f
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun tunedLightTint(@ColorInt argb: Int, lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceAtMost(0.75f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun tunedDarkTint(@ColorInt argb: Int, lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceAtMost(0.75f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun colorInt(color: Color): Int = android.graphics.Color.argb(
    (color.alpha * 255).toInt(),
    (color.red * 255).toInt(),
    (color.green * 255).toInt(),
    (color.blue * 255).toInt(),
)
