package com.neerly.mobile.feature.vendor.shop

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.serverErrorCode
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.ComplianceDocResponse
import com.neerly.mobile.data.dto.VendorHolidayRow
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------- Shop tab (V38)

data class ShopTabUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dashboard: ShopDashboard? = null,
    val rating: ShopRatingSummary? = null,
    val reviewsToReply: Int = 0,
    val openComplaints: Int = 0,
    val products: Int = 0,
    val nextHoliday: VendorHolidayRow? = null,
    val renewDoc: ComplianceDocResponse? = null,
    val renewInDays: Int? = null,
    val drivers: Int = 0
)

@HiltViewModel
class ShopTabViewModel @Inject constructor(
    private val api: VendorShopApi,
    private val repo: VendorRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(ShopTabUiState())
    val state: StateFlow<ShopTabUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.dashboard == null, error = null)
        viewModelScope.launch {
            runCatching {
                val dash = async { api.dashboard() }
                val rating = async { runCatching { api.reviewSummary() }.getOrNull() }
                val reviews = async { runCatching { api.reviews() }.getOrDefault(emptyList()) }
                val complaints = async { runCatching { api.complaints() }.getOrDefault(emptyList()) }
                val products = async { runCatching { api.products() }.getOrDefault(emptyList()) }
                val holidays = async { runCatching { repo.holidays() }.getOrDefault(emptyList()) }
                val docs = async { runCatching { repo.complianceDocs() }.getOrDefault(emptyList()) }
                val team = async { runCatching { repo.team() }.getOrDefault(emptyList()) }
                val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
                val renew = docs.await().mapNotNull { d -> daysUntil(d.expiresDate)?.let { d to it } }
                    .filter { (d, _) -> d.status in setOf("APPROVED", "EXPIRED") }
                    .minByOrNull { it.second }
                ShopTabUiState(
                    loading = false,
                    dashboard = dash.await(),
                    rating = rating.await(),
                    reviewsToReply = reviews.await().count { it.vendorResponse == null && it.status != "HIDDEN" },
                    openComplaints = complaints.await().count { it.open },
                    products = products.await().size,
                    nextHoliday = holidays.await().filter { it.date >= today }.minByOrNull { it.date },
                    renewDoc = renew?.first,
                    renewInDays = renew?.second,
                    drivers = team.await().size
                )
            }.onSuccess { _state.value = it }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage("vendor shop tab", strings.get(R.string.vend_err_load_shop))) }
        }
    }
}

/** Days from today (IST) to an ISO date; negative once it has passed. */
internal fun daysUntil(isoDate: String?): Int? = isoDate?.take(10)?.let {
    runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")), java.time.LocalDate.parse(it)).toInt()
    }.getOrNull()
}

// ---------------------------------------------------------------- Reviews (V23)

data class ReviewsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val reviews: List<ShopReview> = emptyList(),
    val summary: ShopRatingSummary? = null,
    val filter: ReviewFilter = ReviewFilter.ToReply,
    val sending: String? = null,
    val sendError: String? = null
) {
    val shown: List<ShopReview> get() = when (filter) {
        ReviewFilter.ToReply -> reviews.filter { it.vendorResponse == null }
        ReviewFilter.Low -> reviews.filter { it.rating <= 3 }
        ReviewFilter.All -> reviews
    }
}

enum class ReviewFilter(@androidx.annotation.StringRes val label: Int) { ToReply(R.string.vend_rf_to_reply), Low(R.string.vend_rf_low), All(R.string.vend_rf_all) }

@HiltViewModel
class VendorReviewsViewModel @Inject constructor(
    private val api: VendorShopApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.reviews.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching {
                val r = async { api.reviews() }
                val s = async { runCatching { api.reviewSummary() }.getOrNull() }
                r.await() to s.await()
            }.onSuccess { (r, s) -> _state.value = _state.value.copy(loading = false, reviews = r, summary = s) }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage("vendor reviews", strings.get(R.string.vend_err_load_reviews))) }
        }
    }

    fun setFilter(f: ReviewFilter) { _state.value = _state.value.copy(filter = f) }

    /** Post a public reply; the review is replaced by the server's copy (with the reply). */
    fun reply(id: String, text: String, onDone: () -> Unit = {}) {
        val t = text.trim()
        if (t.isEmpty() || _state.value.sending != null) return
        _state.value = _state.value.copy(sending = id, sendError = null)
        viewModelScope.launch {
            runCatching { api.reply(id, ReplyBody(t)) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(sending = null,
                        reviews = _state.value.reviews.map { if (it.id == id) updated else it })
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(sending = null,
                    sendError = it.userMessage("vendor review reply", strings.get(R.string.vend_err_post_reply))) }
        }
    }
}

// ---------------------------------------------------------------- Complaints (V24/V25)

data class ComplaintsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val complaints: List<ShopComplaint> = emptyList(),
    val open: ShopComplaint? = null,
    val threadLoading: Boolean = false,
    val sending: Boolean = false,
    val sendError: String? = null
)

@HiltViewModel
class VendorComplaintsViewModel @Inject constructor(
    private val api: VendorShopApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(ComplaintsUiState())
    val state: StateFlow<ComplaintsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.complaints.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching { api.complaints() }
                .onSuccess { _state.value = _state.value.copy(loading = false, complaints = it.sortedByDescending { c -> c.filedAt }) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor complaints", strings.get(R.string.vend_err_load_complaints))) }
        }
    }

    fun openThread(id: String) {
        _state.value = _state.value.copy(open = _state.value.complaints.firstOrNull { it.id == id }, threadLoading = true, sendError = null)
        viewModelScope.launch {
            runCatching { api.complaint(id) }
                .onSuccess { _state.value = _state.value.copy(open = it, threadLoading = false) }
                .onFailure { _state.value = _state.value.copy(threadLoading = false,
                    sendError = it.userMessage("vendor complaint", strings.get(R.string.vend_err_load_convo))) }
        }
    }

    fun closeThread() { _state.value = _state.value.copy(open = null, sendError = null); load() }

    fun send(text: String, onSent: () -> Unit = {}) {
        val c = _state.value.open ?: return
        val t = text.trim()
        if (t.isEmpty() || _state.value.sending) return
        _state.value = _state.value.copy(sending = true, sendError = null)
        viewModelScope.launch {
            runCatching { api.sendMessage(c.id, MessageBody(t)) }
                .onSuccess { m ->
                    _state.value = _state.value.copy(sending = false, open = c.copy(messages = c.messages + m))
                    onSent()
                }
                .onFailure { _state.value = _state.value.copy(sending = false,
                    sendError = it.userMessage("vendor complaint reply", strings.get(R.string.vend_err_send))) }
        }
    }
}

// ---------------------------------------------------------------- Event bookings (V22/V45)

data class EventsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val events: List<ShopEvent> = emptyList(),
    val open: ShopEvent? = null,
    val declining: Boolean = false,
    val working: Boolean = false,
    val actionError: String? = null,
    val done: String? = null
)

/** A decline reason: [serverText] is what's recorded; the shop picks it in its own language. */
data class EventDeclineReason(val serverText: String, @androidx.annotation.StringRes val label: Int, @androidx.annotation.StringRes val detail: Int)

/** Reasons the customer sees on a decline (V45) — they need to book elsewhere in time. */
val EVENT_DECLINE_REASONS = listOf(
    EventDeclineReason("Fully booked that day", R.string.vend_edr_booked, R.string.vend_edr_booked_sub),
    EventDeclineReason("Too far from my shop", R.string.vend_edr_far, R.string.vend_edr_far_sub),
    EventDeclineReason("Not enough jars or chilling", R.string.vend_edr_jars, R.string.vend_edr_jars_sub),
    EventDeclineReason("Something else", R.string.vend_edr_other, R.string.vend_edr_other_sub)
)

@HiltViewModel
class VendorEventsViewModel @Inject constructor(
    private val api: VendorShopApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(EventsUiState())
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.events.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching { api.events() }
                .onSuccess { _state.value = _state.value.copy(loading = false,
                    events = it.sortedWith(compareByDescending<ShopEvent> { e -> e.awaitingShop }.thenBy { e -> e.eventStart })) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor events", strings.get(R.string.vend_err_load_events))) }
        }
    }

    fun open(id: String) {
        _state.value = _state.value.copy(open = _state.value.events.firstOrNull { it.id == id }, declining = false, actionError = null, done = null)
        viewModelScope.launch { runCatching { api.event(id) }.onSuccess { _state.value = _state.value.copy(open = it) } }
    }

    fun close() { _state.value = _state.value.copy(open = null, declining = false, done = null); load() }
    fun startDecline() { _state.value = _state.value.copy(declining = true, actionError = null) }
    fun cancelDecline() { _state.value = _state.value.copy(declining = false, actionError = null) }

    fun confirm(note: String?) = act(strings.get(R.string.vend_booking_confirmed)) {
        api.confirmEvent(it, ConfirmEventBody(note?.trim()?.ifBlank { null }))
    }

    fun reject(reason: String) {
        if (reason.isBlank()) { _state.value = _state.value.copy(actionError = strings.get(R.string.vend_pick_reason_event)); return }
        act(strings.get(R.string.vend_event_declined)) { api.rejectEvent(it, RejectEventBody(reason.trim().take(500))) }
    }

    private fun act(doneCopy: String, call: suspend (String) -> ShopEvent) {
        val e = _state.value.open ?: return
        if (_state.value.working) return
        _state.value = _state.value.copy(working = true, actionError = null)
        viewModelScope.launch {
            runCatching { call(e.id) }
                .onSuccess { _state.value = _state.value.copy(working = false, open = it, declining = false, done = doneCopy) }
                .onFailure { err ->
                    val expired = err.serverErrorCode() == "EVENT_EXPIRED"
                    _state.value = _state.value.copy(working = false,
                        actionError = if (expired) strings.get(R.string.vend_event_expired)
                            else err.userMessage("vendor event action", strings.get(R.string.vend_err_save_try)))
                    if (expired) load()
                }
        }
    }
}
