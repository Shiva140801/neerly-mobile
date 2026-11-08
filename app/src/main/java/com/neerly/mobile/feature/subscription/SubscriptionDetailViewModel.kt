package com.neerly.mobile.feature.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.core.util.serverErrorCode
import com.neerly.mobile.data.dto.CancelSubscriptionRequest
import com.neerly.mobile.data.dto.ModifySubscriptionRequest
import com.neerly.mobile.data.dto.PauseSubscriptionRequest
import com.neerly.mobile.data.dto.SkipSubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionInstanceResponse
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val extras: SubscriptionExtrasApi,
    savedState: SavedStateHandle,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val subscriptionId: String = savedState["subscriptionId"]
        ?: error("subscriptionId required")

    private val _state = MutableStateFlow(SubscriptionDetailUiState())
    val state: StateFlow<SubscriptionDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.subscription(subscriptionId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, subscription = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(com.neerly.mobile.R.string.acct_sub_load_one_failed)) }
            loadInstances()
        }
    }

    /** The calendar is a nice-to-have: if it fails the screen still works from the subscription row. */
    private suspend fun loadInstances() {
        runCatching { extras.instances(subscriptionId) }
            .onSuccess { _state.value = _state.value.copy(instances = it) }
    }

    /** Dates are yyyy-MM-dd. */
    /** [pausedUntil] null = until the customer resumes. */
    fun pause(pausedFrom: String, pausedUntil: String?, reason: String?) = act(com.neerly.mobile.R.string.acct_sub_pause_failed) {
        repo.pauseSubscription(subscriptionId, PauseSubscriptionRequest(pausedFrom, pausedUntil, reason))
            .also { _state.value = _state.value.copy(done = Done.Paused(pausedFrom, pausedUntil)) }
    }

    fun resume() = act(com.neerly.mobile.R.string.acct_sub_resume_failed) { extras.resume(subscriptionId) }

    /** Skip answers with the skipped delivery, not the subscription; re-read the subscription. */
    fun skip(deliveryDate: String) = act(com.neerly.mobile.R.string.acct_sub_skip_failed) {
        repo.skipSubscription(subscriptionId, SkipSubscriptionRequest(deliveryDate))
        repo.subscription(subscriptionId)
    }

    /** Quantity and/or slot ("HH:mm"); null leaves that part as it is. */
    fun change(quantity: Int?, slotStart: String?, slotEnd: String?) = act(com.neerly.mobile.R.string.acct_sub_change_failed) {
        extras.modify(subscriptionId, ModifySubscriptionRequest(quantity, slotStart, slotEnd))
    }

    /** A plan stuck waiting for an autopay mandate starts as soon as it pays from the wallet. */
    fun payFromWallet() = act(com.neerly.mobile.R.string.acct_sub_switch_failed) {
        extras.modify(subscriptionId, com.neerly.mobile.data.dto.ModifySubscriptionRequest(paymentMethod = "WALLET"))
    }

    fun cancel(reason: String?) = act(com.neerly.mobile.R.string.acct_sub_end_failed) {
        repo.cancelSubscription(subscriptionId, CancelSubscriptionRequest(reason))
            .also { _state.value = _state.value.copy(done = Done.Ended(it.finalDeliveryDate)) }
    }

    fun dismissDone() { _state.value = _state.value.copy(done = null) }

    private fun act(@androidx.annotation.StringRes fallback: Int, block: suspend () -> SubscriptionResponse) {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _state.value = _state.value.copy(working = false, subscription = it); loadInstances() }
                .onFailure { _state.value = _state.value.copy(working = false, done = null, error = it.say(fallback)) }
        }
    }

    // Server messages (cut-off passed, shop closed…) are written for customers; transport noise isn't.
    private fun Throwable.say(@androidx.annotation.StringRes fallbackRes: Int): String {
        val fallback = strings.get(fallbackRes)
        if (this is retrofit2.HttpException && code() == 409) {
            // Read the body once: either our own wording for an ending plan, or the server's line.
            return when (serverErrorCode()) {
                "SUBSCRIPTION_ENDING" -> strings.get(com.neerly.mobile.R.string.acct_sub_ending_locked)
                else -> fallback
            }
        }
        return userMessage(context = "subscription detail", fallback = fallback)
    }
}

data class SubscriptionDetailUiState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val subscription: SubscriptionResponse? = null,
    val instances: List<SubscriptionInstanceResponse> = emptyList(),
    val done: Done? = null,
    val error: String? = null
) {
    /** From the cut-off date on, newest last: what the customer can still skip. */
    fun comingUp(first: LocalDate): List<SubscriptionInstanceResponse> =
        instances.filter { d(it)?.let { x -> !x.isBefore(first) } == true && it.status in UPCOMING }.sortedBy { it.deliveryDate }
    /** Delivered or otherwise finished, newest first. */
    val lately: List<SubscriptionInstanceResponse>
        get() = instances.filter { it.status in FINISHED }.sortedByDescending { it.deliveryDate }.take(5)

    companion object {
        val UPCOMING = setOf("SCHEDULED", "SKIPPED")
        val FINISHED = setOf("DELIVERED", "MISSED", "FAILED")
        fun d(i: SubscriptionInstanceResponse) = runCatching { LocalDate.parse(i.deliveryDate.take(10)) }.getOrNull()
    }
}

/** What just happened, for the confirmation boards (Paused / Ended). */
sealed interface Done {
    /** [until] null = paused until the customer resumes. */
    data class Paused(val from: String, val until: String?) : Done
    /** [finalDelivery] non-null = ended after the cut-off; that delivery still comes. */
    data class Ended(val finalDelivery: String?) : Done
}
