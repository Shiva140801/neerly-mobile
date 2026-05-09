package com.neerly.mobile.feature.complaint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.repo.TrustRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the ComplaintFileScreen submit button. Posts to
 * `POST /api/v1/customer/complaints` and returns the new complaintId so
 * the navigation layer can jump straight into the thread view.
 */
@HiltViewModel
class ComplaintFileViewModel @Inject constructor(
    private val repo: TrustRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ComplaintFileUiState())
    val state: StateFlow<ComplaintFileUiState> = _state.asStateFlow()

    fun submit(
        orderId: String?,
        category: String,
        subject: String,
        description: String,
        evidencePhotos: List<String> = emptyList(),
        onCreated: (String) -> Unit
    ) {
        if (_state.value.submitting) return
        if (subject.isBlank() || description.isBlank()) {
            _state.value = _state.value.copy(error = "Subject and description are required")
            return
        }
        _state.value = ComplaintFileUiState(submitting = true)
        viewModelScope.launch {
            runCatching {
                repo.fileComplaint(
                    orderId = orderId,
                    category = category,
                    subject = subject.take(120),
                    description = description.take(2000),
                    evidencePhotos = evidencePhotos
                )
            }
                .onSuccess {
                    _state.value = ComplaintFileUiState(submitting = false)
                    onCreated(it.id)
                }
                .onFailure {
                    _state.value = ComplaintFileUiState(
                        submitting = false,
                        error = it.message ?: "Could not file complaint"
                    )
                }
        }
    }
}

data class ComplaintFileUiState(
    val submitting: Boolean = false,
    val error: String? = null
)
