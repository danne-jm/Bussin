package com.danieljm.bussin.ui.theme

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private const val TAG = "SystemBars"

/**
 * Configure transparent system bars (for map screens). Uses Accompanist SystemUiController
 * to set status/navigation bar colors and also sets decorFitsSystemWindows=false so the
 * content can draw under the system bars. Overlay UI must consume insets (we do that in screens).
 *
 * This version uses DisposableEffect so that when the composable leaves composition we
 * restore a sane default (opaque system bars and decorFitsSystemWindows=true). This avoids
 * a lingering transparent/decorFitsSystemWindows=false state that can cause layout issues
 * (for example pushing up the nav bar or leaving a black area under it) after navigating away.
 */
@Composable
fun TransparentSystemBars() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            // Apply transparent bars and allow drawing behind them
            try {
                Log.d(TAG, "Applying TransparentSystemBars: decorFits=false")
                systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = false)
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } catch (_: Throwable) {}

            onDispose {
                // Restore to an opaque default when leaving the composable to avoid layout problems.
                try {
                    Log.d(TAG, "Restoring system bars to dark default from TransparentSystemBars")
                    val darkColor = Color(0xFF1D2124)
                    systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                    systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                    val window = (view.context as Activity).window
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                } catch (_: Throwable) {}
            }
        }
    }
}


/**
 * Configure dark system bars for regular screens. This sets an opaque status/navigation bar
 * color and ensures the window does not draw behind system bars (decorFitsSystemWindows=true).
 * Uses DisposableEffect to log and to restore a safe default on dispose as well.
 */
@Composable
fun DarkSystemBars() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            try {
                Log.d(TAG, "Applying DarkSystemBars: decorFits=true")
                val darkColor = Color(0xFF1D2124)
                systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } catch (_: Throwable) {}

            onDispose {
                try {
                    Log.d(TAG, "Leaving DarkSystemBars, restoring dark default (no-op)")
                    val darkColor = Color(0xFF1D2124)
                    systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                    systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                    val window = (view.context as Activity).window
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                } catch (_: Throwable) {}
            }
        }
    }
}
