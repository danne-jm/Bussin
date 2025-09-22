package com.danieljm.bussin.ui.theme

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import android.graphics.Color as AndroidColor

private const val TAG = "SystemBars"

/**
 * Configure transparent status bar (for map screens). The navigation bar remains opaque to
 * avoid layout/inset inconsistencies on some devices. This allows the map to render under
 * the top status bar without drawing behind the navigation bar.
 */
@Composable
fun TransparentSystemBars() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            try {
                Log.d(TAG, "Applying TransparentSystemBars: statusBar transparent, navBar opaque, decorFits=false")
                // Make only the status bar transparent so the map can draw under it
                systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                // Keep navigation bar opaque to avoid the black container / layout issues
                val darkColor = Color(0xFF1D2124)
                systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                // Also set the Activity window colors directly (stronger override on some devices)
                val window = (view.context as Activity).window
                try { window.statusBarColor = AndroidColor.TRANSPARENT } catch (_: Throwable) {}
                try { window.navigationBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                try { Log.d(TAG, "TransparentSystemBars - window.statusBarColor=${window.statusBarColor}, navigationBarColor=${window.navigationBarColor}") } catch (_: Throwable) {}
                 // Allow content to draw behind the status bar (top) while composed
                 WindowCompat.setDecorFitsSystemWindows(window, false)
             } catch (_: Throwable) {}

            onDispose {
                try {
                    Log.d(TAG, "Restoring system bars to dark default from TransparentSystemBars")
                    val darkColor = Color(0xFF1D2124)
                    systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                    systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                    val window = (view.context as Activity).window
                    try { window.statusBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                    try { window.navigationBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                } catch (_: Throwable) {}
            }
        }
    }
}


/**
 * Configure a darkBlack status bar for regular (non-stop) screens. The navigation bar remains opaque.
 * This uses DisposableEffect to set the status bar to a darkBlack color and to restore defaults when
 * the composable leaves composition.
 */
@Composable
fun DarkStatusBar() {
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            try {
                Log.d(TAG, "Applying darkBlackStatusBar: statusBar darkBlack, navBar opaque, decorFits=true")
                // Use a clear darkBlack; adjust to your brand color if needed
                val darkColor = Color(0xFF1D2124)
                systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                // Also set Activity window colors (stronger override on some devices)
                val window = (view.context as Activity).window
                try { window.statusBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                try { window.navigationBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                try { Log.d(TAG, "DarkStatusBar - window.statusBarColor=${window.statusBarColor}, navigationBarColor=${window.navigationBarColor}") } catch (_: Throwable) {}
                 // For normal screens we want content to NOT draw behind the status bar
                 WindowCompat.setDecorFitsSystemWindows(window, true)
            } catch (_: Throwable) {}

            onDispose {
                try {
                    Log.d(TAG, "Leaving darkBlackStatusBar, restoring dark default")
                    val darkColor = Color(0xFF1D2124)
                    systemUiController.setStatusBarColor(darkColor, darkIcons = false)
                    systemUiController.setNavigationBarColor(darkColor, darkIcons = false)
                    val window = (view.context as Activity).window
                    try { window.statusBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                    try { window.navigationBarColor = AndroidColor.parseColor("#1D2124") } catch (_: Throwable) {}
                    try { Log.d(TAG, "DarkStatusBar onDispose - window.statusBarColor=${window.statusBarColor}, navigationBarColor=${window.navigationBarColor}") } catch (_: Throwable) {}
                     WindowCompat.setDecorFitsSystemWindows(window, true)
                 } catch (_: Throwable) {}
             }
         }
     }
 }
