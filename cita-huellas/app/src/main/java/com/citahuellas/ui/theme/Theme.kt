package com.citahuellas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Lavender = Color(0xFFC9B8FF)
val Mint = Color(0xFFB5F0DC)
val Peach = Color(0xFFFFD3B5)
val Night = Color(0xFF0E1630)

val StatusSearching = Color(0xFF7FA8FF)
val StatusFound = Color(0xFF5BD6A0)
val StatusStopped = Color(0xFFFFC56B)

private val Light = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    secondary = Color(0xFF2BB596),
    tertiary = Color(0xFFFF9F6B),
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = Color(0xFF1F1B3A),
    onSurface = Color(0xFF1F1B3A),
    onSurfaceVariant = Color(0xFF5A5478),
    error = Color(0xFFD64545),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB7AAFF),
    onPrimary = Color(0xFF1F1B3A),
    secondary = Color(0xFF7FE3C4),
    tertiary = Color(0xFFFFC09A),
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = Color(0xFFF1EEFF),
    onSurface = Color(0xFFF1EEFF),
    onSurfaceVariant = Color(0xFFC3BEDD),
    error = Color(0xFFFF8A8A),
)

private val Rounded = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val Friendly = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
        bodyLarge = bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    )
}

@Composable
fun CitaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, shapes = Rounded, typography = Friendly, content = content)
}
