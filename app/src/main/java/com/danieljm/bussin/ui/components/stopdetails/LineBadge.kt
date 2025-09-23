package com.danieljm.bussin.ui.components.stopdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.danieljm.bussin.domain.model.Arrival

/**
 * Reusable line badge matching the visuals used in `BusCard` and StopDetail header.
 * Accepts an [Arrival] and renders the badge background, optional border and text safely.
 */
@Composable
fun LineBadge(
    arrival: Arrival,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    cornerRadius: Dp = 8.dp,
    borderWidth: Dp = 2.dp,
    paddingHorizontal: Dp = 10.dp,
    paddingVertical: Dp = 6.dp
) {
    val lineBgColor: Color = try {
        arrival.lijnKleurAchterGrond?.let { hex -> Color(hex.toColorInt()) } ?: Color(0xFF4CAF50)
    } catch (_: Exception) {
        Color(0xFF4CAF50)
    }

    val lineBadgeText = arrival.lijnNummerPubliek ?: arrival.lijnnummer ?: "-"

    val lineBadgeTextColor: Color = try {
        arrival.lijnKleurVoorGrond?.let { hex -> Color(hex.toColorInt()) } ?: Color.Black
    } catch (_: Exception) {
        Color.Black
    }

    val borderModifier = arrival.lijnKleurAchterGrondRand?.let { hex ->
        try {
            val parsed = Color(hex.toColorInt())
            Modifier.border(width = borderWidth, color = parsed, shape = RoundedCornerShape(cornerRadius))
        } catch (_: Exception) {
            Modifier
        }
    } ?: Modifier

    Box(modifier = borderModifier.then(modifier)) {
        Box(
            modifier = Modifier
                .background(lineBgColor, shape = RoundedCornerShape(cornerRadius))
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
        ) {
            Text(
                text = lineBadgeText,
                color = lineBadgeTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}
