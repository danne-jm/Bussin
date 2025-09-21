package com.danieljm.bussin.domain.model

/**
 * Domain model for an arrival/doorkomst at a stop
 */
data class Arrival(
    val doorkomstId: String?,
    val entiteitnummer: String?,
    val lijnnummer: String?,
    val richting: String?,
    val ritnummer: String?,
    val bestemming: String?,
    val plaatsBestemming: String?,
    val vias: List<String> = emptyList(),
    // Original raw timestamp strings from the API
    val dienstregelingTijdstip: String?,
    val realTimeTijdstip: String?,
    val vrtnum: String?,
    val predictionStatussen: List<String> = emptyList(),

    // New: enriched line metadata (may be null if not available)
    val lijnNummerPubliek: String? = null,
    val lijnOmschrijving: String? = null,
    val lijnKleurVoorGrond: String? = null,
    val lijnKleurAchterGrond: String? = null,
    val lijnKleurAchterGrondRand: String? = null,
    val lijnKleurVoorGrondRand: String? = null,

    // Computed fields for UI convenience
    // scheduledTimeFormatted holds a short "HH:mm" representation if dienstregelingTijdstip was present
    val scheduledTimeFormatted: String? = null,
    // expectedArrivalMillis = dienstregelingTijdstip parsed to epoch millis (UTC)
    val expectedArrivalMillis: Long = 0L,
    // realArrivalMillis = realtime timestamp if available (from nested realtime array), 0L if not provided
    val realArrivalMillis: Long = 0L
)
