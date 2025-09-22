package com.danieljm.bussin.data.mapper

import android.util.Log
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

    // Normalize a lijnnummer value coming from DTO (Any? or String) into a canonical string key
    private fun normalizeLijnnummer(value: Any?): String? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toInt().toString()
            is String -> {
                val trimmed = value.trim()
                // If it is a numeric string containing decimal .0, normalize
                if (trimmed.matches(Regex("^\\d+\\.0$"))) {
                    trimmed.substringBefore('.')
                } else trimmed
            }
            else -> value.toString().trim().let { s ->
                if (s.matches(Regex("^\\d+\\.0$"))) s.substringBefore('.') else s
            }
        }
    }

    // Build helper maps for robust matching: byLijnnummer, byEntiteitnummer(list), byPubliek
    private fun buildLineIndexes(lines: List<LineDto>?): Triple<Map<String, LineDto>, Map<String, List<LineDto>>, Map<String, LineDto>> {
        val byLijn = mutableMapOf<String, LineDto>()
        val byEntiteit = mutableMapOf<String, MutableList<LineDto>>()
        val byPubliek = mutableMapOf<String, LineDto>()
        if (lines == null) return Triple(byLijn, byEntiteit, byPubliek)
        for (l in lines) {
            val norm = normalizeLijnnummer(l.lijnnummer)
            if (!norm.isNullOrBlank()) byLijn[norm] = l
            l.entiteitnummer?.let { id ->
                val list = byEntiteit.getOrPut(id) { mutableListOf() }
                list.add(l)
            }
            l.lijnNummerPubliek?.let { pub ->
                val trimmed = pub.trim()
                // store exact label and lowercase variant for case-insensitive lookup
                byPubliek[trimmed] = l
                byPubliek[trimmed.lowercase(Locale.getDefault())] = l
                // also store the numeric portion if present (e.g., 'R36' -> '36') to help numeric lookups
                val digits = Regex("\\d+").find(trimmed)?.value
                if (!digits.isNullOrBlank()) {
                    byPubliek[digits] = l
                }
            }
        }
        try {
            // Debug: report what keys we built so it's clear how matching will behave.
            Log.d("ArrivalsMapper", "buildLineIndexes: byLijnKeys=${byLijn.keys}, byPubliekKeys=${byPubliek.keys}, byEntiteitCounts=${byEntiteit.mapValues { it.value.size }}")
        } catch (_: Exception) {}
        return Triple(byLijn, byEntiteit, byPubliek)
    }

    fun fromDoorkomst(dto: DoorkomstDto, lines: List<LineDto>? = null): Arrival {
        val nestedRealtime = dto.realtime?.firstOrNull()
        val realTimeStr = nestedRealtime?.realTimeTijdstip ?: dto.realTimeTijdstip
        val vrt = nestedRealtime?.vrtnum ?: dto.vrtnum
        val preds = nestedRealtime?.predictionStatussen ?: dto.predictionStatussen ?: emptyList()

        val (byLijn, byEntiteit, byPubliek) = buildLineIndexes(lines)

        // Prefer matching by normalized lijnnummer if present (handles numeric vs string mismatch)
        var lineMeta: LineDto? = null
        val dtoLijnStr = normalizeLijnnummer(dto.lijnnummer)
        val dtoEntiteitStr = dto.entiteitnummer
        val dtoRichtingStr = dto.richting

        if (!dtoLijnStr.isNullOrBlank() && !dtoEntiteitStr.isNullOrBlank()) {
            // First, try to find a line that matches on lijnnummer, entiteitnummer, and richting
            lineMeta = lines?.firstOrNull { line ->
                normalizeLijnnummer(line.lijnnummer) == dtoLijnStr &&
                        line.entiteitnummer == dtoEntiteitStr &&
                        (dtoRichtingStr.isNullOrBlank() || line.richting.isNullOrBlank() || line.richting.equals(dtoRichtingStr, ignoreCase = true))
            }

            // If no match with richting, try without it, but only if there's a unique result
            if (lineMeta == null) {
                val candidates = lines?.filter { line ->
                    normalizeLijnnummer(line.lijnnummer) == dtoLijnStr &&
                            line.entiteitnummer == dtoEntiteitStr
                }
                if (candidates?.size == 1) {
                    lineMeta = candidates.first()
                }
            }
        }

        // If not found, try to match by entiteitnummer only when unique
        if (lineMeta == null && !dto.entiteitnummer.isNullOrBlank()) {
            val list = byEntiteit[dto.entiteitnummer]
            if (list != null && list.size == 1) {
                lineMeta = list[0]
            }
        }

        // If still not found, try matching by normalized lijnnummer alone using the index map.
        // This handles cases where the `lines` entry doesn't include entiteitnummer but
        // the doorkomst `lijnnummer` (often a number) matches the line's `lijnnummer`.
        if (lineMeta == null && !dtoLijnStr.isNullOrBlank()) {
            val byLijnMatch = byLijn[dtoLijnStr]
            if (byLijnMatch != null) {
                lineMeta = byLijnMatch
            }
        }

        // As a last resort, try matching by public label using dtoLijnStr
        if (lineMeta == null && !dtoLijnStr.isNullOrBlank()) {
            // Try direct lookup (case-sensitive), then lowercase key, then try numeric suffixes
            lineMeta = byPubliek[dtoLijnStr] ?: byPubliek[dtoLijnStr.lowercase(Locale.getDefault())]
            if (lineMeta == null) {
                // Try suffixes (e.g. dtoLijnStr='136' -> try '36' to match public label 'R36')
                val maxSuffix = minOf(3, dtoLijnStr.length)
                for (len in 1..maxSuffix) {
                    val suffix = dtoLijnStr.takeLast(len)
                    val candidate = byPubliek[suffix]
                    if (candidate != null) {
                        lineMeta = candidate
                        break
                    }
                }
            }
        }

        // If still not found, but entiteit maps to multiple candidates, prefer one matching richting
        if (lineMeta == null && !dto.entiteitnummer.isNullOrBlank()) {
            val candidates = byEntiteit[dto.entiteitnummer]
            if (!candidates.isNullOrEmpty()) {
                val matchByRichting = candidates.firstOrNull { c ->
                    // Match by richting if available
                    !dto.richting.isNullOrBlank() && !c.richting.isNullOrBlank() && c.richting.equals(dto.richting, ignoreCase = true)
                }
                if (matchByRichting != null) {
                    lineMeta = matchByRichting
                } else {
                    // Fall back to matching by normalized lijnnummer or public label among candidates
                    val match = candidates.firstOrNull { c ->
                        val cLijnNorm = normalizeLijnnummer(c.lijnnummer)
                        (dtoLijnStr != null && cLijnNorm == dtoLijnStr) || (c.lijnNummerPubliek == dtoLijnStr)
                    }
                    if (match != null) lineMeta = match
                }
            }
        }

        // Final fallback: try to match public label that contains the numeric lijn (e.g. 'R92' contains '92')
        if (lineMeta == null && !dtoLijnStr.isNullOrBlank()) {
            val candidate = byPubliek.values.firstOrNull { pub ->
                try {
                    val label = pub.lijnNummerPubliek ?: return@firstOrNull false
                    // match case-insensitive, allow labels like 'R92' or '92-R'
                    label.endsWith(dtoLijnStr, ignoreCase = true) || label.contains(dtoLijnStr)
                } catch (_: Exception) { false }
            }
            if (candidate != null) {
                lineMeta = candidate
                try { Log.d("ArrivalsMapper", "Fallback matched public label ${'$'}{candidate.lijnNummerPubliek} for doorkomst ${'$'}{dto.doorkomstId}") } catch (_: Exception) {}
            }
        }

        // Debug logging to help trace matching issues
        try {
            Log.d("ArrivalsMapper", "doorkomstId=${dto.doorkomstId} lijnnummer=${dto.lijnnummer} entiteit=${dto.entiteitnummer} normalizedLijn=$dtoLijnStr matched=${lineMeta?.lijnNummerPubliek}")
        } catch (_: Exception) { }

        val scheduledMillis = parseIsoToMillis(dto.dienstregelingTijdstip)
        val realMillis = parseIsoToMillis(realTimeStr)
        val scheduledFormatted = formatToHm(dto.dienstregelingTijdstip)

        return Arrival(
            doorkomstId = dto.doorkomstId,
            entiteitnummer = dto.entiteitnummer,
            // Prefer the normalized lijn string (e.g. '136' instead of '136.0') so UI and matching
            // consistently see the cleaned value.
            lijnnummer = dtoLijnStr ?: dto.lijnnummer?.toString(),
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