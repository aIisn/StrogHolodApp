package com.example.strogholodapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// 🎨 Цветовая схема на основе Color.kt
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = CardBackground,
    primaryContainer = Secondary,
    secondary = Dark,
    background = Light,
    surface = CardBackground,
    onSurface = TextPrimary
)

// 🧩 Формы
val Shapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)

// 🖼️ Применение темы
@Composable
fun StrogHolodAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
