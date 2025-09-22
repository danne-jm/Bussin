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
        // Do not force decorFitsSystemWindows here; per-screen composables should
        // control whether content draws behind system bars to avoid persistent layout
        // changes when navigating between screens.
        // Ensure the window background is transparent so no white background shows through system bars
        try { window.setBackgroundDrawableResource(android.R.color.transparent) } catch (_: Throwable) {}
        setContent {
            BussinTheme {
                BussinNavHost()
            }
        }
    }
}
