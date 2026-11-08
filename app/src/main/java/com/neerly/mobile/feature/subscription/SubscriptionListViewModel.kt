package com.neerly.mobile.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val extras: SubscriptionExtrasApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionListUiState())
    val state: StateFlow<SubscriptionListUiState> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    /** "Resume now" on a paused card (board 17). */
    fun resume(id: String) {
        _state.value = _state.value.copy(resuming = id, error = null)
        viewModelScope.launch {
            runCatching { extras.resume(id) }
                .onSuccess { r -> _state.value = _state.value.copy(resuming = null,
                    subscriptions = _state.value.subscriptions.map { if (it.id == r.id) r else it }) }
                .onFailure { _state.value = _state.value.copy(resuming = null,
                    error = it.userMessage("resume subscription", com.neerly.mobile.R.string.acct_sub_resume_failed, strings)) }
        }
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.subscriptions() }
                .onSuccess { _state.value = SubscriptionListUiState(subscriptions = it, loading = false) }
                .onFailure {
                    // `it.message` here was Retrofit/Moshi internals; never the customer's problem.
                    _state.value = SubscriptionListUiState(
                        loading = false,
                        error = it.userMessage("subscription list", com.neerly.mobile.R.string.acct_sub_load_failed, strings)
                    )
                }
        }
    }
}

data class SubscriptionListUiState(
    val loading: Boolean = true,
    val subscriptions: List<SubscriptionResponse> = emptyList(),
    val resuming: String? = null,
    val error: String? = null
) {
    val active   : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "ACTIVE" }
    val paused   : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "PAUSED" }
    val pending  : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "PENDING_MANDATE" }
    val ended    : List<SubscriptionResponse> get() = subscriptions.filter { it.status == "CANCELLED" }
}
