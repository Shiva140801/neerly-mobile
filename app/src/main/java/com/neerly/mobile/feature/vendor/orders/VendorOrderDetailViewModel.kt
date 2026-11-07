package com.neerly.mobile.feature.vendor.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

@HiltViewModel
class VendorOrderDetailViewModel @Inject constructor(
    private val repo: VendorRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedState["orderId"] ?: error("orderId required")

    private val _state = MutableStateFlow(VendorOrderDetailUiState())
    val state: StateFlow<VendorOrderDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.order(orderId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, order = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun markReady() = transition { repo.ready(orderId) }

    /**
     * The designed hand-off (S-DRIV-ASSIGN): the run is offered to an on-shift team
     * driver who accepts it on their phone. Typing a driver's UUID was the only way before.
     */
    fun offerDriver() {
        if (_state.value.offering) return
        _state.value = _state.value.copy(offering = true, error = null, offerNote = null)
        viewModelScope.launch {
            runCatching { repo.offerDriver(orderId) }
                .onSuccess { r ->
                    _state.value = _state.value.copy(
                        offering = false,
                        offerNote = if (r.offered) "Offered to a driver — waiting for them to accept."
                            else "No driver is on shift right now. Ask one to start their shift, then offer again."
                    )
                    load()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        offering = false,
                        error = it.userMessage(context = "offer driver", fallback = "Couldn't offer this run. Try again.")
                    )
                }
        }
    }

    private fun transition(call: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { load() }
                .onFailure {
                    _state.value = _state.value.copy(
                        error = it.userMessage(context = "vendor order action", fallback = "That didn't go through. Try again.")
                    )
                }
        }
    }
}

data class VendorOrderDetailUiState(
    val loading: Boolean = true,
    val order: VendorOrderResponse? = null,
    val error: String? = null,
    val offering: Boolean = false,
    val offerNote: String? = null
)
