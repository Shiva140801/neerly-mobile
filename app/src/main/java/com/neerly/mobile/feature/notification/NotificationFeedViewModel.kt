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
import com.neerly.mobile.core.util.userMessage
import javax.inject.Inject

/**
 * Pulls /api/v1/notifications and exposes the rows the screen renders.
 * Tapping a row marks it read via /notifications/{id}/read.
 *
 * The screen takes [FeedItem] (its own UI shape); we map the DTO to that.
 */
@HiltViewModel
class NotificationFeedViewModel @Inject constructor(
    private val repo: TrustRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationFeedUiState())
    val state: StateFlow<NotificationFeedUiState> = _state.asStateFlow()

    /**
     * Rows the customer marked read on this screen. A refresh that was already in flight
     * (live refresh on return / on a push) can land after "Mark all read" with the old
     * unread rows — without this they came back as unread (user report).
     */
    private val readHere = mutableSetOf<String>()
    private var markAllPending = false

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.notifications() }
                .onSuccess { rows ->
                    _state.value = NotificationFeedUiState(
                        loading = false,
                        items = rows.map { it.toFeedItem() }.map {
                            if (it.id in readHere || (markAllPending && !it.isRead)) it.copy(isRead = true) else it
                        }
                    )
                }
                .onFailure {
                    _state.value = NotificationFeedUiState(
                        loading = false,
                        error = it.userMessage("notifications", com.neerly.mobile.R.string.acct_ntf_load_failed, strings)
                    )
                }
        }
    }

    fun markRead(id: String) {
        readHere += id
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

    /** One call to `/notifications/mark-all-read`; on failure the rows go back to unread. */
    fun markAllRead() {
        val before = _state.value.items
        if (before.none { !it.isRead }) return
        readHere += before.map { it.id }
        markAllPending = true
        _state.value = _state.value.copy(items = before.map { it.copy(isRead = true) }, markError = null)
        viewModelScope.launch {
            runCatching { repo.markAllNotificationsRead() }
                .onSuccess { markAllPending = false; refresh() }
                .onFailure {
                    // Say so instead of silently flipping the dots back.
                    markAllPending = false
                    readHere -= before.filter { !it.isRead }.map { it.id }.toSet()
                    _state.value = _state.value.copy(items = before,
                        markError = it.userMessage("notifications", com.neerly.mobile.R.string.acct_ntf_mark_failed, strings))
                }
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
    val error: String? = null,
    /** "Mark all read" failed — shown above the list, which stays visible. */
    val markError: String? = null
)
