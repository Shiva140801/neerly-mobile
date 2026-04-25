package com.neerly.mobile.feature.complaint

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
    savedState: SavedStateHandle
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
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
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
                .onFailure { _state.value = _state.value.copy(sending = false, error = it.message) }
        }
    }

    fun withdraw() {
        viewModelScope.launch {
            runCatching { repo.withdrawComplaint(complaintId) }
                .onSuccess { _state.value = _state.value.copy(complaint = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

data class ComplaintThreadUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val complaint: ComplaintResponse? = null,
    val error: String? = null
)
