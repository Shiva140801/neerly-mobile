package com.neerly.mobile.feature.complaint

import com.neerly.mobile.core.util.userMessage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.ComplaintMessageDto
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintThreadViewModel @Inject constructor(
    private val repo: CustomerRepository,
    savedState: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val complaintId: String = savedState["complaintId"]
        ?: error("complaintId required")

    private val _state = MutableStateFlow(ComplaintThreadUiState())
    val state: StateFlow<ComplaintThreadUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.complaint(complaintId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, complaint = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("complaint thread", com.neerly.mobile.R.string.acct_cmp_thread_failed, strings)) }
        }
    }

    /** Background re-read for replies and status (poll / return / push); keeps the thread on screen. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.complaint(complaintId) }.onSuccess { _state.value = _state.value.copy(loading = false, error = null, complaint = it) }
        }
    }

    fun appendMessage(message: String) {
        if (message.isBlank()) return
        _state.value = _state.value.copy(sending = true, error = null)
        viewModelScope.launch {
            runCatching { repo.appendComplaintMessage(complaintId, message) }
                .onSuccess { msg ->
                    val updated = _state.value.complaint?.let { c ->
                        c.copy(messages = c.messages + msg)
                    }
                    _state.value = _state.value.copy(sending = false, complaint = updated)
                }
                .onFailure { _state.value = _state.value.copy(sending = false, error = it.userMessage("complaint thread", com.neerly.mobile.R.string.acct_cmp_thread_failed, strings)) }
        }
    }

    /** "Yes, all sorted" — the customer closes it. */
    fun close() {
        viewModelScope.launch {
            runCatching { repo.closeComplaint(complaintId) }
                .onSuccess { _state.value = _state.value.copy(complaint = it.copy(messages = _state.value.complaint?.messages ?: emptyList()), closedNow = true) }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("complaint thread", com.neerly.mobile.R.string.acct_cmp_thread_failed, strings)) }
        }
    }

    fun withdraw() {
        viewModelScope.launch {
            runCatching { repo.withdrawComplaint(complaintId) }
                .onSuccess { _state.value = _state.value.copy(complaint = it) }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage("complaint thread", com.neerly.mobile.R.string.acct_cmp_thread_failed, strings)) }
        }
    }
}

data class ComplaintThreadUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val complaint: ComplaintResponse? = null,
    val closedNow: Boolean = false,
    val error: String? = null
)
