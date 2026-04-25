package com.neerly.mobile.feature.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.SubmitVendorOnboardingRequest
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Submits the assembled [OnboardingState] payload to POST /api/v1/vendor/submit.
 * The wizard composable owns the per-screen state; this VM only handles the
 * final network call so the wizard can stay declarative.
 */
@HiltViewModel
class VendorOnboardingViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubmitState())
    val state: StateFlow<SubmitState> = _state.asStateFlow()

    fun submit(payload: OnboardingState, onDone: () -> Unit) {
        if (_state.value.submitting) return
        _state.value = SubmitState(submitting = true)
        val req = SubmitVendorOnboardingRequest(
            businessName = payload.businessName,
            proprietorName = payload.proprietor,
            businessType = payload.businessType,
            businessPincode = payload.pincode,
            fssaiNumber = payload.fssaiNumber.takeIf { it.isNotBlank() },
            fssaiExpiresAt = payload.fssaiExpiry.takeIf { it.isNotBlank() },
            serviceablePincodes = payload.pincodes,
            isTier2 = payload.isTier2
        )
        viewModelScope.launch {
            runCatching { repo.submitOnboarding(req) }
                .onSuccess {
                    _state.value = SubmitState(submitting = false)
                    onDone()
                }
                .onFailure {
                    _state.value = SubmitState(
                        submitting = false,
                        error = it.message ?: "Submission failed"
                    )
                }
        }
    }

    data class SubmitState(
        val submitting: Boolean = false,
        val error: String? = null
    )
}
