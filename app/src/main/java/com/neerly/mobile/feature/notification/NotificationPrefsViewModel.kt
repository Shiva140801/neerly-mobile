package com.neerly.mobile.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.dto.NotificationPrefDto
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notification preferences matrix: channels (PUSH / SMS / EMAIL / WHATSAPP)
 * crossed with categories (ORDER_UPDATES / DELIVERY_STATUS / PROMOTIONS / NEWS / BILLING).
 *
 * IN_APP is implicit-on (the in-app feed always shows everything) so we omit it
 * from the toggle grid.
 *
 * Backend stores rows on demand (default = enabled when missing). Optimistic UI:
 * we flip the local state immediately and roll back on API error.
 */
@HiltViewModel
class NotificationPrefsViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    val channels = listOf("PUSH", "SMS", "EMAIL", "WHATSAPP")
    val categories = listOf(
        "ORDER_UPDATES" to "Order updates",
        "DELIVERY_STATUS" to "Driver / delivery status",
        "BILLING" to "Payment + receipts",
        "PROMOTIONS" to "Promotions + offers",
        "NEWS" to "Product news"
    )

    private val _state = MutableStateFlow(NotificationPrefsUiState())
    val state: StateFlow<NotificationPrefsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.notificationPrefs() }
                .onSuccess { rows -> _state.value = _state.value.copy(loading = false, rows = rows) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    /**
     * Pref lookup — defaults to true for any (channel, category) without an
     * explicit row. Backend follows the same rule for dispatching.
     */
    fun isEnabled(channel: String, category: String): Boolean {
        val row = _state.value.rows.firstOrNull { it.channel == channel && it.category == category }
        return row?.enabled ?: true
    }

    fun toggle(channel: String, category: String, enabled: Boolean) {
        // Optimistic update.
        val newRow = NotificationPrefDto(channel, category, enabled)
        val updated = _state.value.rows.toMutableList()
        val idx = updated.indexOfFirst { it.channel == channel && it.category == category }
        if (idx >= 0) updated[idx] = newRow else updated.add(newRow)
        _state.value = _state.value.copy(rows = updated)

        viewModelScope.launch {
            runCatching { repo.setNotificationPref(channel, category, enabled) }
                .onFailure {
                    // Rollback on failure
                    _state.value = _state.value.copy(error = it.message ?: "Save failed")
                    refresh()
                }
        }
    }
}

data class NotificationPrefsUiState(
    val loading: Boolean = true,
    val rows: List<NotificationPrefDto> = emptyList(),
    val error: String? = null
)
