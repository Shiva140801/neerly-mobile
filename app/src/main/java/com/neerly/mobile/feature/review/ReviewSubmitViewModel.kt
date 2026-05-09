package com.neerly.mobile.feature.review

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
 * Posts the 2-axis review (vendor experience + water quality) via TrustRepository.
 * The screen owns the form state; this VM handles the network call only.
 *
 * `rating` field on the request is the 1-5 average — the backend uses it for
 * single-axis aggregations (vendor leaderboards) while keeping the 2-axis
 * detail for the per-review record.
 */
@HiltViewModel
class ReviewSubmitViewModel @Inject constructor(
    private val repo: TrustRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewSubmitUiState())
    val state: StateFlow<ReviewSubmitUiState> = _state.asStateFlow()

    fun submit(
        orderId: String,
        vendorStars: Int,
        waterStars: Int,
        text: String?,
        onDone: () -> Unit
    ) {
        if (_state.value.submitting) return
        if (vendorStars !in 1..5 || waterStars !in 1..5) {
            _state.value = _state.value.copy(error = "Pick a star rating for both")
            return
        }
        _state.value = ReviewSubmitUiState(submitting = true)
        // Average rounds half-up to keep the leaderboard sane (e.g. 4 + 5 → 5).
        val overall = ((vendorStars + waterStars + 1) / 2).coerceIn(1, 5)
        viewModelScope.launch {
            runCatching {
                repo.submitReview(
                    orderId = orderId,
                    rating = overall,
                    text = text?.takeIf { it.isNotBlank() },
                    vendorRating = vendorStars,
                    waterQualityRating = waterStars
                )
            }
                .onSuccess {
                    _state.value = ReviewSubmitUiState(submitting = false)
                    onDone()
                }
                .onFailure {
                    _state.value = ReviewSubmitUiState(
                        submitting = false,
                        error = it.message ?: "Could not submit review"
                    )
                }
        }
    }
}

data class ReviewSubmitUiState(
    val submitting: Boolean = false,
    val error: String? = null
)
