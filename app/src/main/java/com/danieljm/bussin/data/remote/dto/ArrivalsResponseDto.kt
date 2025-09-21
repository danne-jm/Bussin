package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RealtimeDto(
    @param:Json(name = "dienstregelingTijdstip")
    val dienstregelingTijdstip: String? = null,
    @param:Json(name = "real-timeTijdstip")
    val realTimeTijdstip: String? = null,
    @param:Json(name = "vrtnum")
    val vrtnum: String? = null,
    @param:Json(name = "predictionStatussen")
    val predictionStatussen: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class DoorkomstDto(
    @param:Json(name = "doorkomstId")
    val doorkomstId: String? = null,
    @param:Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    // Accept any type (string or number) for lijnnummer coming from the API
    @param:Json(name = "lijnnummer")
    val lijnnummer: Any? = null,
    @param:Json(name = "richting")
    val richting: String? = null,
    @param:Json(name = "ritnummer")
    val ritnummer: String? = null,
    @param:Json(name = "bestemming")
    val bestemming: String? = null,
    @param:Json(name = "plaatsBestemming")
    val plaatsBestemming: String? = null,
    @param:Json(name = "dienstregelingTijdstip")
    val dienstregelingTijdstip: String? = null,
    // Keep legacy top-level real-time field if present
    @param:Json(name = "real-timeTijdstip")
    val realTimeTijdstip: String? = null,
    // The API sometimes nests realtime info inside a `realtime` array — parse it too
    @param:Json(name = "realtime")
    val realtime: List<RealtimeDto>? = null,
    @param:Json(name = "vias")
    val vias: List<String>? = null,
    @param:Json(name = "vrtnum")
    val vrtnum: String? = null,
    @param:Json(name = "predictionStatussen")
    val predictionStatussen: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class HalteDoorkomstenDto(
    @param:Json(name = "haltenummer")
    val haltenummer: String,
    @param:Json(name = "doorkomsten")
    val doorkomsten: List<DoorkomstDto>? = null
)

// New DTO for line metadata returned alongside halteDoorkomsten
@JsonClass(generateAdapter = true)
data class LineDto(
    @param:Json(name = "lijnNummerPubliek")
    val lijnNummerPubliek: String? = null,
    @param:Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    @param:Json(name = "lijnnummer")
    val lijnnummer: Any? = null,
    @param:Json(name = "richting")
    val richting: String? = null,
    @param:Json(name = "omschrijving")
    val omschrijving: String? = null,
    @param:Json(name = "kleurVoorGrond")
    val kleurVoorGrond: String? = null,
    @param:Json(name = "kleurAchterGrond")
    val kleurAchterGrond: String? = null,
    @param:Json(name = "kleurAchterGrondRand")
    val kleurAchterGrondRand: String? = null,
    @param:Json(name = "kleurVoorGrondRand")
    val kleurVoorGrondRand: String? = null
)

@JsonClass(generateAdapter = true)
data class ArrivalsResponseDto(
    @param:Json(name = "lines")
    val lines: List<LineDto>? = null,
    @param:Json(name = "halteDoorkomsten")
    val halteDoorkomsten: List<HalteDoorkomstenDto>? = null
)
