package com.example.tickerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.example.tickerdemo.presentation.ticker.TickerScreen

private val TickerColorScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    background = Color(0xFF0B1220),
    surface = Color(0xFF0B1220),
    surfaceContainer = Color(0xFF151F32),
    onBackground = Color(0xFFE6EDF7),
    onSurface = Color(0xFFE6EDF7),
    onSurfaceVariant = Color(0xFF8A9BB5),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = TickerColorScheme) {
                Surface {
                    TickerScreen()
                }
            }
        }
    }
}
