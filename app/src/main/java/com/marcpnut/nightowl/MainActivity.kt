package com.marcpnut.nightowl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.marcpnut.nightowl.data.SessionStore
import com.marcpnut.nightowl.ui.NightOwlApp

private val NightOwlColors = darkColorScheme(
    primary = Color(0xFF22D6F5),
    onPrimary = Color(0xFF061015),
    secondary = Color(0xFFAC50FF),
    background = Color(0xFF080A0F),
    surface = Color(0xFF171A21),
    surfaceVariant = Color(0xFF242832),
    onBackground = Color(0xFFF7F8FB),
    onSurface = Color(0xFFF7F8FB)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        val sessionStore = SessionStore(applicationContext)
        setContent {
            MaterialTheme(colorScheme = NightOwlColors) {
                NightOwlApp(sessionStore = sessionStore)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
