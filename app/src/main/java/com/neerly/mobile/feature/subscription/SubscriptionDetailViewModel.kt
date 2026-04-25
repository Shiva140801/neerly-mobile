package com.neerly.mobile.feature.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.CancelSubscriptionRequest
import com.neerly.mobile.data.dto.PauseSubscriptionRequest
import com.neerly.mobile.data.dto.SkipSubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    private val repo: CustomerRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val subscriptionId: String = savedState["subscriptionId"]
        ?: error("subscriptionId required")

    private val _state = MutableStateFlow(SubscriptionDetailUiState())
    val state: StateFlow<SubscriptionDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.subscription(subscriptionId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, subscription = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun pause(pausedFromIso: String, pausedUntilIso: String, reason: String?) {
        viewModelScope.launch {
            runCatching {
                repo.pauseSubscription(subscriptionId, PauseSubscriptionRequest(pausedFromIso, pausedUntilIso, reason))
            }.onSuccess { _state.value = _state.value.copy(subscription = it) }
             .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun skip(deliveryDate: String) {
        viewModelScope.launch {
            runCatching {
                repo.skipSubscription(subscriptionId, SkipSubscriptionRequest(deliveryDate))
            }.onSuccess { _state.value = _state.value.copy(subscription = it) }
             .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun cancel(reason: String?) {
        viewModelScope.launch {
            runCatching {
                repo.cancelSubscription(subscriptionId, CancelSubscriptionRequest(reason))
            }.onSuccess { _state.value = _state.value.copy(subscription = it) }
             .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class SubscriptionDetailUiState(
    val loading: Boolean = true,
    val subscription: SubscriptionResponse? = null,
    val error: String? = null
)
