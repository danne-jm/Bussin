package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.FinalScheduleResponseDto
import com.danieljm.bussin.domain.model.FinalSchedule

object FinalScheduleMapper {
    fun fromDto(dto: FinalScheduleResponseDto): FinalSchedule {
        // Pass the lines metadata into the arrivals mapper so each Arrival gets enriched
        val halteDoorkomsten = dto.halteDoorkomsten?.map { ArrivalsMapper.fromHalteDoorkomsten(it, dto.lines) } ?: emptyList()
        return FinalSchedule(
            halteDoorkomsten = halteDoorkomsten,
            doorkomstNotas = dto.doorkomstNotas ?: emptyList(),
            ritNotas = dto.ritNotas ?: emptyList(),
            omleidingen = dto.omleidingen ?: emptyList()
        )
    }
}
