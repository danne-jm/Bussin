package com.danieljm.bussin.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.danieljm.bussin.ui.theme.DarkSystemBars

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigate: (route: String) -> Unit = {}
) {
    // Configure dark system bars for regular screens
    DarkSystemBars()

    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWelcome()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = ui.message ?: "Home")
        Button(onClick = { onNavigate("plan") }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Go to Plan")
        }
        Button(onClick = { onNavigate("stops") }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Go to Stops")
        }
    }
}
