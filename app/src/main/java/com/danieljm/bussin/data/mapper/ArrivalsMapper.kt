package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.DoorkomstDto
import com.danieljm.bussin.data.remote.dto.HalteDoorkomstenDto
import com.danieljm.bussin.data.remote.dto.LineDto
import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.model.HalteDoorkomsten
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ArrivalsMapper {

    private fun parseIsoToMillis(ts: String?): Long {
        if (ts.isNullOrBlank()) return 0L

        // Try a few common ISO-like patterns. For patterns without offset, interpret as local time.
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX", // with timezone like +02:00 or Z
            "yyyy-MM-dd'T'HH:mm:ssX",   // with timezone like +02
            "yyyy-MM-dd'T'HH:mm:ss"     // no timezone -> treat as local
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (!pattern.contains('X')) {
                    // No offset in the pattern -> interpret in device's default timezone (like previous behavior)
                    sdf.timeZone = TimeZone.getDefault()
                }
                val parsed = sdf.parse(ts)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // ignore and try next pattern
            }
        }

        // Fallback: some timestamps come with offset without colon (+0200) or other small variants.
        try {
            val alt = ts.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
            val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            val parsed2 = sdf2.parse(alt)
            if (parsed2 != null) return parsed2.time
        } catch (_: Exception) {
            // final fallback below
        }

        return 0L
    }

    private fun formatToHm(ts: String?): String? {
        if (ts.isNullOrBlank()) return null
        return try {
            val timePart = ts.substringAfter('T', ts)
            if (timePart.length >= 5) timePart.substring(0, 5) else timePart
        } catch (_: Exception) {
            null
        }
    }

    // Map of key -> LineDto, key may be entiteitnummer or lijnnummer string
    private fun buildLinesMap(lines: List<LineDto>?): Map<String, LineDto> {
        if (lines == null) return emptyMap()
        val m = mutableMapOf<String, LineDto>()
        for (l in lines) {
            l.entiteitnummer?.let { m[it] = l }
            l.lijnnummer?.let { m[it.toString()] = l }
            l.lijnNummerPubliek?.let { m[it] = l }
        }
        return m
    }

    fun fromDoorkomst(dto: DoorkomstDto, lines: List<LineDto>? = null): Arrival {
        val nestedRealtime = dto.realtime?.firstOrNull()
        val realTimeStr = nestedRealtime?.realTimeTijdstip ?: dto.realTimeTijdstip
        val vrt = nestedRealtime?.vrtnum ?: dto.vrtnum
        val preds = nestedRealtime?.predictionStatussen ?: dto.predictionStatussen ?: emptyList()

        val linesMap = buildLinesMap(lines)
        // Find matching line metadata by entiteitnummer first, then by lijnnummer string
        val lineMeta = dto.entiteitnummer?.let { linesMap[it] } ?: dto.lijnnummer?.toString()?.let { linesMap[it] }

        val scheduledMillis = parseIsoToMillis(dto.dienstregelingTijdstip)
        val realMillis = parseIsoToMillis(realTimeStr)
        val scheduledFormatted = formatToHm(dto.dienstregelingTijdstip)

        return Arrival(
            doorkomstId = dto.doorkomstId,
            entiteitnummer = dto.entiteitnummer,
            lijnnummer = dto.lijnnummer?.toString(),
            richting = dto.richting,
            ritnummer = dto.ritnummer,
            bestemming = dto.bestemming,
            plaatsBestemming = dto.plaatsBestemming,
            vias = dto.vias ?: emptyList(),
            dienstregelingTijdstip = dto.dienstregelingTijdstip,
            realTimeTijdstip = realTimeStr,
            vrtnum = vrt,
            predictionStatussen = preds,

            // line metadata
            lijnNummerPubliek = lineMeta?.lijnNummerPubliek,
            lijnOmschrijving = lineMeta?.omschrijving,
            lijnKleurVoorGrond = lineMeta?.kleurVoorGrond,
            lijnKleurAchterGrond = lineMeta?.kleurAchterGrond,
            lijnKleurAchterGrondRand = lineMeta?.kleurAchterGrondRand,
            lijnKleurVoorGrondRand = lineMeta?.kleurVoorGrondRand,

            // computed fields
            scheduledTimeFormatted = scheduledFormatted,
            expectedArrivalMillis = scheduledMillis,
            realArrivalMillis = realMillis
        )
    }

    fun fromHalteDoorkomsten(dto: HalteDoorkomstenDto, lines: List<LineDto>? = null): HalteDoorkomsten {
        val arrivals = dto.doorkomsten?.map { fromDoorkomst(it, lines) } ?: emptyList()
        return HalteDoorkomsten(haltenummer = dto.haltenummer, doorkomsten = arrivals)
    }
}
