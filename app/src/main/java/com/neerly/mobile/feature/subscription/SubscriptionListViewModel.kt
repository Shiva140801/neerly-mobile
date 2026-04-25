package com.neerly.mobile.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionListUiState())
    val state: StateFlow<SubscriptionListUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.subscriptions() }
                .onSuccess { _state.value = SubscriptionListUiState(subscriptions = it, loading = false) }
                .onFailure { _state.value = SubscriptionListUiState(loading = false, error = it.message) }
        }
    }
}

data class SubscriptionListUiState(
    val loading: Boolean = true,
    val subscriptions: List<SubscriptionResponse> = emptyList(),
    val error: String? = null
) {
    val active   : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "ACTIVE" }
    val paused   : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "PAUSED" }
    val pending  : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "PENDING_MANDATE" }
    val ended    : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "CANCELLED" }
}
