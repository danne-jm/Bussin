package com.danieljm.bussin.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomBottomNavBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
    val itemCount = bottomNavItems.size

    // Animate the slider position
    val animatedSliderPosition by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) selectedIndex.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 0
        ),
        label = "slider_position"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D2124))
            .drawBehind {
                // Draw main top border
                drawLine(
                    color = Color(0xFF3A3F42),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw sliding indicator
                val sliderWidth = size.width / itemCount
                val sliderStart = animatedSliderPosition * sliderWidth
                val sliderEnd = sliderStart + sliderWidth

                // Indicator line at top
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = androidx.compose.ui.geometry.Offset(sliderStart, 0f),
                    end = androidx.compose.ui.geometry.Offset(sliderEnd, 0f),
                    strokeWidth = 4.dp.toPx()
                )
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            val isSelected = currentRoute == item.route

            Column(
                modifier = Modifier
                    .clickable { onItemClick(item.route) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                )
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                )
            }
        }
    }
}
