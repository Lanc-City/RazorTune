package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

fun Modifier.liquidGlass(
    cornerRadius: Dp = 28.dp,
    glassIntensity: Float = 0.6f,
    containerColor: Color = Color(0xFF0F1117), // Deeper, richer dark tint
    shape: Shape = RoundedCornerShape(cornerRadius),
): Modifier = this then Modifier.composed {
    val t = glassIntensity.coerceIn(0f, 1f)

    this.drawBehind {
        val radius = cornerRadius.toPx()

        // 1. Much more translucent base fill so the background subtly shows through
        drawRoundRect(
            color = containerColor.copy(alpha = lerp(0.55f, 0.30f, t)),
            cornerRadius = CornerRadius(radius)
        )

        // 2. Stronger top specular light reflection to create the glass curve illusion
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = lerp(0.28f, 0.45f, t)),
                    Color.White.copy(alpha = lerp(0.04f, 0.10f, t)),
                    Color.Transparent
                )
            ),
            cornerRadius = CornerRadius(radius)
        )
    }.drawBehind {
        val radius = cornerRadius.toPx()
        // 3. Crisp, bright refractive edge border
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = lerp(0.25f, 0.55f, t)),
                    Color.White.copy(alpha = lerp(0.03f, 0.12f, t))
                )
            ),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}