package com.danieljm.bussin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danieljm.bussin.ui.navigation.BussinNavHost
import com.danieljm.bussin.ui.theme.BussinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure the window background is transparent so no white background shows through system bars
        try { window.setBackgroundDrawableResource(android.R.color.transparent) } catch (_: Throwable) {}

        // Set a sane default status/navigation bar color for non-stop screens so they start with
        // the requested dark color (0xFF1D2124). Stop screens will override this to transparent.
        try {
            val darkColor = android.graphics.Color.parseColor("#1D2124")
            window.statusBarColor = darkColor
            window.navigationBarColor = darkColor
        } catch (_: Throwable) {}

        setContent {
            BussinTheme {
                BussinNavHost()
            }
        }
    }
}
