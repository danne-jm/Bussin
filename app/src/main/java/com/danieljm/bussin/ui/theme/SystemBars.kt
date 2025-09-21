package com.danieljm.bussin.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Configure transparent system bars (for map screens). Uses Accompanist SystemUiController
 * to set status/navigation bar colors and also sets decorFitsSystemWindows=false so the
 * content can draw under the system bars. Overlay UI must consume insets (we do that in screens).
 */
@Composable
fun TransparentSystemBars() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        SideEffect {
            try {
                // Make both bars transparent and allow content to draw behind them
                systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = false)
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } catch (_: Throwable) {}
        }
    }
}


/**
 * Configure dark system bars for regular screens. This sets an opaque status/navigation bar
 * color and ensures the window does not draw behind system bars (decorFitsSystemWindows=true).
 */
@Composable
fun DarkSystemBars() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val darkColor = Color(0xFF1D2124)
                // Use dark icons if background is light; here we use light icons (darkIcons = false)
                systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                // Keep navigation bar opaque to avoid layout/inset inconsistencies on some devices
                systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } catch (_: Throwable) {}
        }
    }
}
