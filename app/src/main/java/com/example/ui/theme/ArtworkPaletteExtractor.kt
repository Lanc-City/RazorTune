package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.model.ArtworkPaletteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkPaletteExtractor {

    private val cache = LruCache<String, ArtworkPaletteColors>(24)

    suspend fun extract(
        context: Context,
        imageUrl: String?,
    ): ArtworkPaletteColors? = withContext(Dispatchers.IO) {
        val url = imageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext null
        cache.get(url)?.let { return@withContext it }

        runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(256)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
            val bitmap = result.drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()

            val primary = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: return@runCatching null

            val secondary = palette.mutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: primary

            val tertiary = palette.lightVibrantSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: secondary

            val darkAccent = palette.darkVibrantSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: primary

            ArtworkPaletteColors(
                primaryArgb = primary,
                secondaryArgb = secondary,
                tertiaryArgb = tertiary,
                darkAccentArgb = darkAccent,
            ).also { colors ->
                cache.put(url, colors)
            }
        }.getOrNull()
    }
}
