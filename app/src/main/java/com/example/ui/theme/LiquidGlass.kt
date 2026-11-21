package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Applies a true iOS-style frosted glass background effect.
 * The glass styling is drawn entirely behind the component,
 * ensuring all text and icons remain 100% sharp and legible on top.
 */
fun Modifier.liquidGlass(
    cornerRadius: Dp = 28.dp,
    glassIntensity: Float = 0.8f,
    containerColor: Color = Color(0xFF141720),
    shape: Shape? = null,
): Modifier = this.drawBehind {
    val radius = cornerRadius.toPx()

    // 1. Deep translucent dark glass background fill
    drawRoundRect(
        color = containerColor.copy(alpha = lerp(0.70f, 0.88f, glassIntensity)),
        cornerRadius = CornerRadius(radius)
    )

    // 2. Glossy top specular highlight (gives the glass its curved depth)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = lerp(0.30f, 0.50f, glassIntensity)),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            endY = size.height * 0.4f
        ),
        cornerRadius = CornerRadius(radius)
    )
}.drawBehind {
    val radius = cornerRadius.toPx()
    // 3. Crisp, bright refractive outer border edge
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = lerp(0.45f, 0.75f, glassIntensity)),
                Color.White.copy(alpha = 0.10f)
            )
        ),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = 1.3.dp.toPx())
    )
}