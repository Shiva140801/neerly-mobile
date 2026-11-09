package com.neerly.mobile.feature.notification

import com.neerly.mobile.core.util.userMessage

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.R
import com.neerly.mobile.data.dto.NotificationPrefDto
import com.neerly.mobile.data.repo.CustomerRepository
import com.neerly.mobile.push.PushChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Board NotifPrefs — "What we tell you about". One switch per topic, stored as rows of the
 * existing `notification-prefs` endpoint (plan "Changes during implementation"); the server's
 * push policy reads the same rows. Optimistic: flips at once, rolls back if the save fails.
 */
@HiltViewModel
class NotificationPrefsViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    /** A switch on the board. [default] applies while the server has no row for it. */
    enum class Topic(
        val channel: String, val category: String,
        @StringRes val title: Int, @StringRes val sub: Int,
        val default: Boolean, val locked: Boolean = false,
        /** The Android channel this topic maps to, for "sound and vibration" deep links. */
        val androidChannel: PushChannel? = null
    ) {
        ORDER("PUSH", "ORDER_UPDATES", R.string.push_prefs_order, R.string.push_prefs_order_sub, true, locked = true, androidChannel = PushChannel.CUST_ORDERS),
        SUBS("PUSH", "SUBSCRIPTIONS", R.string.push_prefs_subs, R.string.push_prefs_subs_sub, true, androidChannel = PushChannel.CUST_SUBS),
        JARS("PUSH", "JARS", R.string.push_prefs_jars, R.string.push_prefs_jars_sub, true, androidChannel = PushChannel.CUST_JARS),
        WALLET("PUSH", "WALLET", R.string.push_prefs_wallet, R.string.push_prefs_wallet_sub, true, androidChannel = PushChannel.CUST_MONEY),
        OFFERS("PUSH", "OFFERS", R.string.push_prefs_offers, R.string.push_prefs_offers_sub, false, androidChannel = PushChannel.CUST_OFFERS),
        SMS("SMS", "ORDER_UPDATES", R.string.push_prefs_sms, R.string.push_prefs_sms_sub, true),
        QUIET("PUSH", "QUIET_HOURS", R.string.push_prefs_quiet, R.string.push_prefs_quiet_sub, true)
    }

    private val _state = MutableStateFlow(NotificationPrefsUiState())
    val state: StateFlow<NotificationPrefsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.notificationPrefs() }
                .onSuccess { rows -> _state.value = _state.value.copy(loading = false, rows = rows) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage("notification prefs", R.string.acct_ntf_prefs_load_failed, strings)) }
        }
    }

    fun isOn(t: Topic): Boolean {
        if (t.locked) return true
        return _state.value.rows.firstOrNull { it.channel == t.channel && it.category == t.category }?.enabled ?: t.default
    }

    fun toggle(t: Topic, on: Boolean) {
        if (t.locked) return
        val before = _state.value.rows
        val updated = before.filterNot { it.channel == t.channel && it.category == t.category } + NotificationPrefDto(t.channel, t.category, on)
        _state.value = _state.value.copy(rows = updated, error = null)
        viewModelScope.launch {
            runCatching { repo.setNotificationPref(t.channel, t.category, on) }
                .onFailure { _state.value = _state.value.copy(rows = before, error = strings.get(R.string.push_prefs_failed)) }
        }
    }
}

data class NotificationPrefsUiState(
    val loading: Boolean = true,
    val rows: List<NotificationPrefDto> = emptyList(),
    val error: String? = null
)
