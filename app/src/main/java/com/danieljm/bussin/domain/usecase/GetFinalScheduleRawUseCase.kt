package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetFinalScheduleRawUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(stop: String, datum: String, maxAantalDoorkomsten: Int? = null): Result<String> {
        return repository.getFinalSchedule(stop, datum, maxAantalDoorkomsten)
    }
}

