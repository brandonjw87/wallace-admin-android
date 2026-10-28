package com.brandonwallace.wallaceadmin.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object W {
    val Bg = Color(0xFF070B16); val Bg2 = Color(0xFF0D1530); val Card = Color(0x1AFFFFFF); val CardHi = Color(0x26FFFFFF); val Stroke = Color(0x22FFFFFF)
    val Text = Color(0xFFE8ECF7); val Sub = Color(0xFF94A0BD)
    val Green = Color(0xFF3DDC97); val Yellow = Color(0xFFFFC857); val Red = Color(0xFFFF5D73); val Blue = Color(0xFF5AA9FF); val Violet = Color(0xFF9B7BFF); val Cyan = Color(0xFF3FD5E6)
    val bgBrush = Brush.verticalGradient(listOf(Color(0xFF0B1433), Bg, Color(0xFF050811)))
    val accent = Brush.linearGradient(listOf(Blue, Violet))
    fun status(s: String?) = when (s?.lowercase()) { "green", "on", "up", "true" -> Green; "yellow" -> Yellow; "red", "off", "down", "false" -> Red; else -> Sub }
}

@Composable
fun WallaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = W.Blue, secondary = W.Violet, tertiary = W.Green, background = W.Bg, surface = W.Bg2, onSurface = W.Text, onBackground = W.Text, error = W.Red, surfaceVariant = Color(0xFF18213F), onSurfaceVariant = W.Sub),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = W.Text),
            headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = W.Text),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = W.Text),
            bodyMedium = TextStyle(fontSize = 14.sp, color = W.Text),
            bodySmall = TextStyle(fontSize = 12.sp, color = W.Sub),
            labelSmall = TextStyle(fontSize = 11.sp, color = W.Sub, fontWeight = FontWeight.Medium),
        ),
        content = content
    )
}
