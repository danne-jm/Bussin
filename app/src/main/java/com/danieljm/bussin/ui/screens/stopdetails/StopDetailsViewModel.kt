package com.danieljm.bussin.ui.screens.stopdetails

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.usecase.GetFinalScheduleRawUseCase
import com.danieljm.bussin.domain.usecase.GetFinalScheduleUseCase
import com.danieljm.bussin.domain.usecase.GetStopDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StopDetailsViewModel @Inject constructor(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getFinalScheduleUseCase: GetFinalScheduleUseCase,
    private val getFinalScheduleRawUseCase: GetFinalScheduleRawUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopDetailsUiState())
    val uiState: StateFlow<StopDetailsUiState> = _uiState.asStateFlow()

    fun loadStopDetails(stopId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = getStopDetailsUseCase(stopId)
            if (res.isSuccess) {
                val stop = res.getOrNull()
                _uiState.value = _uiState.value.copy(isLoading = false, selectedStop = stop)
                // After loading stop details, fetch final schedule arrivals for today
                stop?.id?.let { fetchFinalScheduleForStop(it) }
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                Log.w("StopDetailsVM", "Failed to load stop details: $err")
                _uiState.value = _uiState.value.copy(isLoading = false, error = err)
            }
        }
    }

    private fun fetchFinalScheduleForStop(stopId: String) {
        viewModelScope.launch {
            Log.d("StopDetailsVM", "Fetching final schedule for stop=$stopId")
            // Build explicit new UI state to avoid named-parameter copy issues
            val cur = _uiState.value
            _uiState.value = StopDetailsUiState(
                isLoading = cur.isLoading,
                selectedStop = cur.selectedStop,
                error = cur.error,
                arrivalsLoading = true,
                arrivals = cur.arrivals,
                arrivalsError = null,
                arrivalsRawJson = null,
                noHalteDoorkomsten = false
            )
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val today = sdf.format(Date())

                // Also fetch the raw JSON for debugging
                val rawRes = getFinalScheduleRawUseCase(stopId, today, 200)
                if (rawRes.isSuccess) {
                    val cur2 = _uiState.value
                    _uiState.value = cur2.copy(arrivalsRawJson = rawRes.getOrNull())
                }

                val res = getFinalScheduleUseCase(stopId, today, 200)
                if (res.isSuccess) {
                    val final = res.getOrNull()
                    val arrivals: List<Arrival> = final?.halteDoorkomsten?.flatMap { it.doorkomsten } ?: emptyList()

                    // Determine whether the final schedule explicitly had no halteDoorkomsten
                    val noHalte = final?.halteDoorkomsten.isNullOrEmpty()

                    // Sort arrivals by effective epoch time (prefer realArrivalMillis, then expectedArrivalMillis)
                    val sorted = arrivals.sortedWith(compareBy { a ->
                        when {
                            a.realArrivalMillis > 0L -> a.realArrivalMillis
                            a.expectedArrivalMillis > 0L -> a.expectedArrivalMillis
                            else -> Long.MAX_VALUE
                        }
                    })

                    Log.d("StopDetailsVM", "Fetched final schedule: arrivalsCount=${'$'}{sorted.size}")
                    val cur3 = _uiState.value
                    _uiState.value = StopDetailsUiState(
                        isLoading = cur3.isLoading,
                        selectedStop = cur3.selectedStop,
                        error = cur3.error,
                        arrivalsLoading = false,
                        arrivals = sorted,
                        arrivalsError = cur3.arrivalsError,
                        arrivalsRawJson = cur3.arrivalsRawJson,
                        noHalteDoorkomsten = noHalte
                    )
                } else {
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Failed to load arrivals"
                    Log.w("StopDetailsVM", "Final schedule error: $err")
                    val cur4 = _uiState.value
                    _uiState.value = StopDetailsUiState(
                        isLoading = cur4.isLoading,
                        selectedStop = cur4.selectedStop,
                        error = cur4.error,
                        arrivalsLoading = false,
                        arrivals = emptyList(),
                        arrivalsError = err,
                        arrivalsRawJson = cur4.arrivalsRawJson,
                        noHalteDoorkomsten = false
                    )
                }
            } catch (e: Throwable) {
                Log.e("StopDetailsVM", "Exception fetching final schedule", e)
                val cur5 = _uiState.value
                _uiState.value = StopDetailsUiState(
                    isLoading = cur5.isLoading,
                    selectedStop = cur5.selectedStop,
                    error = cur5.error,
                    arrivalsLoading = false,
                    arrivals = emptyList(),
                    arrivalsError = e.localizedMessage ?: "Unknown error",
                    arrivalsRawJson = cur5.arrivalsRawJson,
                    noHalteDoorkomsten = false
                )
            }
        }
    }
}
