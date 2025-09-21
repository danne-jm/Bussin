package com.danieljm.bussin.ui.components.stopdetails

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.danieljm.bussin.domain.model.Arrival
import kotlin.math.abs

@Composable
fun BusCard(arrival: Arrival, modifier: Modifier = Modifier) {
    // Logging for debugging
    Log.i(
        "BusCard",
        "Rendering BusCard: lineEnt=${arrival.entiteitnummer}, linePub=${arrival.lijnNummerPubliek}, lineBg=${arrival.lijnKleurAchterGrond}"
    )

    // Determine badge background color from hex if available, else fallback to green
    val lineBgColor: Color = try {
        arrival.lijnKleurAchterGrond?.let { hex ->
            Color(hex.toColorInt())
        } ?: Color(0xFF4CAF50)
    } catch (_: Exception) {
        Log.w("BusCard", "Failed to parse badge color: ${arrival.lijnKleurAchterGrond}")
        Color(0xFF4CAF50)
    }

    val lineBadgeText = arrival.lijnNummerPubliek ?: arrival.lijnnummer ?: "-"

    val lineBadgeTextColor: Color = try {
        arrival.lijnKleurVoorGrond?.let { hex ->
            Color(hex.toColorInt())
        } ?: Color.Black
    } catch (_: Exception) {
        Color.Black
    }

    val borderModifier = arrival.lijnKleurAchterGrondRand?.let { hex ->
        try {
            val parsed = Color(hex.toColorInt())
            Modifier.border(width = 2.dp, color = parsed, shape = RoundedCornerShape(8.dp))
        } catch (_: Exception) {
            Modifier
        }
    } ?: Modifier

    val realtimeAvailable = arrival.realArrivalMillis > 0L

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D32)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Line badge with optional border
                    Box(modifier = borderModifier) {
                        Box(
                            modifier = Modifier
                                .background(lineBgColor, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lineBadgeText,
                                color = lineBadgeTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    // Vehicle id or schedule icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF424242), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (realtimeAvailable) Lucide.Bus else Lucide.CloudOff,
                            contentDescription = if (realtimeAvailable) "Bus" else "No GPS",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (realtimeAvailable) (arrival.vrtnum ?: "-") else "sched",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = arrival.scheduledTimeFormatted ?: "--:--",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (realtimeAvailable) {
                        val delayMinutes = ((arrival.realArrivalMillis - arrival.expectedArrivalMillis) / 60_000).toInt()
                        val delayText = when {
                            delayMinutes == 0 -> "on time"
                            delayMinutes > 0 -> "+ $delayMinutes"
                            else -> "- ${abs(delayMinutes)}"
                        }
                        val delayColor = when {
                            delayMinutes == 0 -> Color(0xFF74C4AB)
                            delayMinutes > 0 -> Color(0xFFD6978E)
                            else -> Color(0xFF5C86EC)
                        }
                        Text(
                            text = delayText,
                            color = delayColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "schedule",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Destination + description + countdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = arrival.bestemming ?: "-",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = arrival.lijnOmschrijving ?: (arrival.plaatsBestemming ?: ""),
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val now = System.currentTimeMillis()
                    val remainingMillis = when {
                        arrival.realArrivalMillis > 0L -> arrival.realArrivalMillis - now
                        arrival.expectedArrivalMillis > 0L -> arrival.expectedArrivalMillis - now
                        else -> Long.MIN_VALUE
                    }

                    Text(
                        text = formatCountdownMillis(remainingMillis),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatCountdownMillis(remainingMillis: Long): String {
    if (remainingMillis == Long.MIN_VALUE) return ""
    return when {
        remainingMillis < 0L -> "departed"
        remainingMillis < 20_000L -> "at stop"
        remainingMillis < 60_000L -> "arriving"
        else -> {
            val minutes = remainingMillis / 60_000L
            if (minutes < 60L) {
                "in $minutes min"
            } else {
                val hours = minutes / 60L
                val mins = minutes % 60L
                if (mins == 0L) {
                    "in $hours h"
                } else {
                    "in $hours h $mins min"
                }
            }
        }
    }
}
