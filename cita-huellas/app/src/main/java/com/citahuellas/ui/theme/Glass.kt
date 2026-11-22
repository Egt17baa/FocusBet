package com.citahuellas.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

val LocalHaze = staticCompositionLocalOf { HazeState() }

val GlassShape = RoundedCornerShape(24.dp)

/** Fondo con degradado y manchas pastel; todo lo que hay encima puede desenfocarlo con [glass]. */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = isSystemInDarkTheme()
    val state = remember { HazeState() }
    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .haze(state)
        ) {
            val base = if (dark) listOf(Night, Color(0xFF1B1F48)) else listOf(Color(0xFFF7F4FF), Color(0xFFFFF7F1))
            drawRect(Brush.verticalGradient(base))
            val a = if (dark) 0.45f else 0.9f
            fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = 0f)), Offset(size.width * x, size.height * y), size.minDimension * r),
                radius = size.minDimension * r,
                center = Offset(size.width * x, size.height * y),
            )
            blob(Lavender, 0.15f, 0.12f, 0.75f)
            blob(Mint, 0.95f, 0.45f, 0.7f)
            blob(Peach, 0.25f, 0.9f, 0.8f)
        }
        CompositionLocalProvider(LocalHaze provides state) { content() }
    }
}

/** Cristal: blanco translúcido (ahumado en oscuro), desenfoque del fondo en Android 12+ y borde fino. */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier {
    val dark = isSystemInDarkTheme()
    val tint = when {
        dark -> Color(0xFF1A2147).copy(alpha = if (strong) 0.70f else 0.35f)
        else -> Color.White.copy(alpha = if (strong) 0.80f else 0.55f)
    }
    val style = HazeStyle(
        backgroundColor = if (dark) Night else Color.White,
        tint = HazeTint(tint),
        blurRadius = 24.dp,
        noiseFactor = 0.08f,
        fallbackTint = HazeTint(tint.copy(alpha = (tint.alpha + 0.2f).coerceAtMost(0.92f))),
    )
    return this
        .hazeChild(LocalHaze.current, shape, style)
        .border(1.dp, Color.White.copy(alpha = if (dark) 0.18f else 0.6f), shape)
}

/** Píldora de color para estados. */
fun Modifier.pill(color: Color, radius: Dp = 50.dp): Modifier =
    this.background(color.copy(alpha = 0.22f), RoundedCornerShape(radius)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(radius))
