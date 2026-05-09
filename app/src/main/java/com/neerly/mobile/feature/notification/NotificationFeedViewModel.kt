package com.neerly.mobile.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.NotificationResponse
import com.neerly.mobile.data.repo.TrustRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pulls /api/v1/notifications and exposes the rows the screen renders.
 * Tapping a row marks it read via /notifications/{id}/read.
 *
 * The screen takes [FeedItem] (its own UI shape); we map the DTO to that.
 */
@HiltViewModel
class NotificationFeedViewModel @Inject constructor(
    private val repo: TrustRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationFeedUiState())
    val state: StateFlow<NotificationFeedUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.notifications() }
                .onSuccess { rows ->
                    _state.value = NotificationFeedUiState(
                        loading = false,
                        items = rows.map { it.toFeedItem() }
                    )
                }
                .onFailure {
                    _state.value = NotificationFeedUiState(
                        loading = false, error = it.message
                    )
                }
        }
    }

    fun markRead(id: String) {
        // Optimistic flip.
        _state.value = _state.value.copy(
            items = _state.value.items.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
        )
        viewModelScope.launch {
            runCatching { repo.markNotificationRead(id) }
                .onFailure { /* swallow — next refresh will re-converge */ }
        }
    }

    private fun NotificationResponse.toFeedItem() = FeedItem(
        id = id,
        category = category,
        subject = subject,
        body = body,
        isRead = readAt != null,
        queuedAt = queuedAt
    )
}

data class NotificationFeedUiState(
    val loading: Boolean = true,
    val items: List<FeedItem> = emptyList(),
    val error: String? = null
)
