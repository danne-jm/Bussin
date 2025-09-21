package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.LineSearchResponseDto
import com.danieljm.bussin.domain.model.LineDirection

object LineDirectionMapper {
    fun fromDto(dto: LineSearchResponseDto): LineDirection {
        return LineDirection(
            lijnnummer = dto.lijnnummer ?: dto.lijnNummerPubliek ?: "",
            richting = dto.richting ?: "",
            omschrijving = dto.omschrijving,
            kleurVoorGrond = dto.kleurVoorGrond,
            kleurAchterGrond = dto.kleurAchterGrond,
            kleurAchterGrondRand = dto.kleurAchterGrondRand,
            kleurVoorGrondRand = dto.kleurVoorGrondRand
        )
    }

    fun toLineDto(dto: com.danieljm.bussin.data.remote.dto.LineDirectionDto): com.danieljm.bussin.data.remote.dto.LineDto {
        return com.danieljm.bussin.data.remote.dto.LineDto(
            lijnnummer = dto.lijnnummer,
            entiteitnummer = dto.entiteitnummer,
            richting = dto.richting,
            lijnNummerPubliek = dto.lijnNummerPubliek,
            omschrijving = dto.omschrijving,
            kleurVoorGrond = dto.kleurVoorGrond,
            kleurAchterGrond = dto.kleurAchterGrond,
            kleurAchterGrondRand = dto.kleurAchterGrondRand,
            kleurVoorGrondRand = dto.kleurVoorGrondRand
        )
    }
}
