package com.neerly.mobile.feature.driver

import com.neerly.mobile.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverOffer
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.dto.DriverTaskCard
import com.neerly.mobile.data.repo.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

/** Where the active drop is, from the driver's side (boards D04 → D05 → D06/D07). */
enum class DropStep { LOAD, RIDE, HAND_OVER }

/** Door photo for the hand-over (D06). */
sealed class DoorPhoto {
    data object None : DoorPhoto()
    data object Uploading : DoorPhoto()
    data class Ready(val s3Key: String) : DoorPhoto()
    data object StorageDown : DoorPhoto()
    data class Failed(val message: String) : DoorPhoto()
}

enum class GpsProblem { NO_PERMISSION, OFF }

/** D22 — at the door, nobody answering. [arrivedAtMs] is when "I'm at the door" was tapped here. */
data class NotHome(val orderId: String, val calls: Int, val arrivedAtMs: Long,
                   val sending: Boolean = false, val problem: String? = null)

/** D27 — the drop was marked failed; take the jars back to the shop. */
data class FailedDrop(val orderNumber: String, val customer: String, val result: FailDropResult,
                      val shopName: String?, val shopLat: Double?, val shopLng: Double?)

/** The ₹ task that timed out (D20). */
data class MissedTask(val fee: BigDecimal, val where: String, val shop: String)

/** The delivered screen (D08). [earning] is null until the server has recorded the drop. */
data class DropDone(
    val orderNumber: String,
    val customer: String,
    val earning: DriverDropEarning?,
    val offeredFee: BigDecimal?,
    val emptiesInAuto: Int,
    val cashTaken: BigDecimal?
)

/**
 * Driver home — the whole delivery loop runs off this one state:
 *   off duty (D01) → choose shop if several (D14) → on duty (D02) → offer (D03 / missed D20)
 *   → load at shop (D04) → ride (D05) → at the door: code + empties + photo (D06), cash (D07)
 *   → delivered (D08) → next drop or back to the shop (D32 / D33).
 * Large targets and one decision per screen: it's used one-handed on a bike.
 */
@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    private val repo: DriverRepository,
    private val ticker: DriverLocationTicker,
    private val ops: DriverOpsApi,
    private val photos: PhotoUploader,
    private val jobs: DriverJobStore,
    private val platform: DriverPlatformApi,
    private val strings: com.neerly.mobile.core.util.Strings,
    /** Feeds the on-duty notification card and gates task rings (push). */
    private val duty: com.neerly.mobile.push.DriverDutyState = com.neerly.mobile.push.DriverDutyState()
) : ViewModel() {

    private val _state = MutableStateFlow(DriverHomeUiState())
    val state: StateFlow<DriverHomeUiState> = _state.asStateFlow()

    private var sendJob: Job? = null
    private var offerJob: Job? = null
    private var visible = false
    private var locationPermitted = false
    /** The offer we last showed, to tell "it ran out" from "I accepted/passed it". */
    private var shownOffer: DriverOffer? = null
    private var answeredOffer: String? = null

    init { refresh() }

    /** Polling runs only while the screen is on screen — not for a backgrounded app. */
    fun setVisible(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) refresh() else syncOfferLoop()
    }

    /** The screen reports the runtime-permission result; nothing is sent without it. */
    fun onLocationPermission(granted: Boolean) {
        locationPermitted = granted
        syncLocationLoop()
    }

    /**
     * Broadcasts the driver's position while on shift: 10 s cadence on the move, 30 s parked.
     * Only a fix younger than [DriverLocationTicker.FIX_MAX_AGE_MS] is sent; failures are
     * dropped — the next tick is seconds away and the customer map shows "updating…".
     */
    private fun syncLocationLoop() {
        val shouldRun = locationPermitted && _state.value.isOnDuty
        if (!shouldRun) {
            sendJob?.cancel(); sendJob = null
            ticker.stop()
            return
        }
        if (sendJob?.isActive == true) return
        ticker.start()
        sendJob = viewModelScope.launch {
            while (isActive) {
                ticker.freshFix()?.let { fix ->
                    _state.value = _state.value.copy(here = fix.latitude to fix.longitude)
                    runCatching {
                        repo.ping(
                            latitude = fix.latitude, longitude = fix.longitude,
                            headingDeg = if (fix.hasBearing()) fix.bearing.toDouble() else null,
                            speedMps = if (fix.hasSpeed()) fix.speed.toDouble() else null,
                            accuracyM = if (fix.hasAccuracy()) fix.accuracy.toDouble() else null,
                            batteryPct = ticker.batteryPct(),
                            orderId = _state.value.activeAssignment?.orderId
                        )
                    }
                }
                delay(if (ticker.isParked()) DriverLocationTicker.PARKED_SEND_MS else DriverLocationTicker.ACTIVE_SEND_MS)
            }
        }
    }

    override fun onCleared() {
        ticker.stop()
        super.onCleared()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = _state.value.shift == null && _state.value.error == null, error = null)
        viewModelScope.launch {
            runCatching {
                val shift = repo.currentShift()
                val onDuty = shift != null && shift.endedAt == null
                val list = if (onDuty) repo.assignments() else emptyList()
                val today = runCatching { ops.today() }.getOrNull()
                val shops = if (onDuty && _state.value.shops.isNotEmpty()) _state.value.shops else loadShops()
                val shiftState = if (onDuty) runCatching { platform.shiftState() }.getOrNull() else null
                // Off duty: did the last shift close by itself? (DOff "Shift ended automatically")
                val lastShift = if (!onDuty) runCatching { platform.lastShift().body() }.getOrNull() else null
                Snapshot(shift, list, today, shops, shiftState, lastShift)
            }.onSuccess { s ->
                jobs.keepOnly(s.jobs.map { it.orderId }.toSet())
                _state.value = _state.value.copy(
                    loading = false, shift = s.shift, assignments = s.jobs, today = s.today ?: _state.value.today,
                    shops = s.shops, loadedOrders = s.jobs.map { it.orderId }.filter(jobs::isLoaded).toSet(), error = null,
                    shiftState = s.shiftState, lastShift = s.lastShift
                )
                publishDutyCard()
                syncLocationLoop()
                syncOfferLoop()
            }.onFailure { _state.value = _state.value.copy(loading = false, error = it.say("driver home")) }
        }
    }

    /** NPDLock card: drops done / total, time on duty, cash in hand, next customer. */
    private fun publishDutyCard() {
        val s = _state.value
        if (!s.isOnDuty) { duty.update(false, null); return }
        val open = s.assignments.count { it.status in setOf("PREPARING", "DISPATCHED", "ARRIVING") }
        val done = s.today?.drops ?: 0
        val next = s.activeAssignment ?: s.upNext.firstOrNull()
        duty.update(true, com.neerly.mobile.push.DutyCard(
            shopName = s.shops.firstOrNull { it.shopId == s.shift?.vendorId }?.shopName,
            startedAtMs = s.shift?.startedAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() },
            dropsDone = done,
            dropsTotal = done + open,
            cashInHand = s.today?.cashInHand ?: java.math.BigDecimal.ZERO,
            nextCustomer = next?.customerFirstName,
            nextLat = next?.deliveryLat,
            nextLng = next?.deliveryLng,
            onBreak = s.onBreak
        ))
    }

    private data class Snapshot(val shift: DriverShiftResponse?, val jobs: List<DriverAssignment>,
                                val today: DriverToday?, val shops: List<DriverShop>, val shiftState: DriverShiftState?,
                                val lastShift: LastShift? = null)

    /** D14 — the shops this driver drives for (`GET /driver/vendors`). */
    private suspend fun loadShops(): List<DriverShop> = runCatching { ops.vendors() }.getOrDefault(emptyList())

    /**
     * Start duty. With one shop (or no shop list yet) the server picks it; with several the
     * driver chooses ([shopId]) — the plain call answers 422 DRIVER_VENDOR_AMBIGUOUS for them.
     */
    fun startShift(shopId: String? = null) {
        val shops = _state.value.shops
        if (shopId == null && shops.size > 1) { _state.value = _state.value.copy(choosingShop = true); return }
        val target = shopId ?: shops.singleOrNull()?.shopId
        _state.value = _state.value.copy(starting = true, choosingShop = false, error = null)
        viewModelScope.launch {
            runCatching { if (target != null) ops.startShiftFor(target) else repo.startShift() }
                .onSuccess { _state.value = _state.value.copy(starting = false, shift = it); refresh() }
                .onFailure { e ->
                    val ambiguous = (e as? HttpException)?.code() == 422
                    _state.value = _state.value.copy(starting = false,
                        choosingShop = ambiguous && shops.size > 1,
                        error = if (ambiguous && shops.size <= 1) strings.get(R.string.drv_err_many_shops)
                                else e.say("start shift"))
                }
        }
    }

    fun cancelChooseShop() { _state.value = _state.value.copy(choosingShop = false) }

    // ---- Breaks (D18 / D28) and ending the shift ------------------------------------------

    /** No tasks are offered while on break; the server holds the clock. */
    fun startBreak(minutes: Int? = null) = shiftCall("start break") { platform.startBreak(StartBreakBody(minutes)) }
    fun endBreak() = shiftCall("end break") { platform.endBreak() }

    private fun shiftCall(what: String, call: suspend () -> DriverShiftState) {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _state.value = _state.value.copy(working = false, shiftState = it); syncOfferLoop() }
                .onFailure { _state.value = _state.value.copy(working = false, error = it.say(what)) }
        }
    }

    /** "End duty" — first ask the server what's still out (drops, cash) and show it. */
    fun askEndShift() {
        _state.value = _state.value.copy(endChecking = true, error = null)
        viewModelScope.launch {
            runCatching { platform.endCheck() }
                .onSuccess { _state.value = _state.value.copy(endChecking = false, endCheck = it) }
                .onFailure { _state.value = _state.value.copy(endChecking = false, error = it.say("end shift")) }
        }
    }

    fun cancelEndShift() { _state.value = _state.value.copy(endCheck = null) }

    /**
     * End duty. With drops still out the driver must hand them back to the shop ([handBack]);
     * the server answers 409 DRIVER_DROPS_PENDING otherwise.
     */
    fun finishShift(handBack: Boolean) {
        _state.value = _state.value.copy(ending = true, error = null)
        viewModelScope.launch {
            runCatching { platform.finish(FinishShiftBody(handBack = handBack, endedReason = "DRIVER_ENDED")) }
                .onSuccess { _state.value = _state.value.copy(ending = false, endCheck = null, shiftState = null); refresh() }
                .onFailure { e ->
                    val err = ServerError.of(e)
                    _state.value = _state.value.copy(ending = false, error = when (err?.code) {
                        "DRIVER_DROPS_PENDING" -> strings.get(R.string.drv_err_drops_pending)
                        else -> err?.message ?: e.say("end shift")
                    })
                }
        }
    }

    // ---- GPS / connection (D19) ---------------------------------------------------------

    /** The screen reports whether location is allowed and switched on. */
    fun onGpsStatus(permitted: Boolean, providerOn: Boolean) {
        _state.value = _state.value.copy(gps = when {
            !permitted -> GpsProblem.NO_PERMISSION
            !providerOn -> GpsProblem.OFF
            else -> null
        })
    }

    // ---- The active drop -----------------------------------------------------------------

    /** D04: every line ticked → tell the server the drop has started and ride. */
    fun loaded(orderId: String) {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { repo.startDelivery(orderId) }
                .onSuccess {
                    jobs.markLoaded(orderId)
                    _state.value = _state.value.copy(working = false, loadedOrders = _state.value.loadedOrders + orderId)
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(working = false, error = it.say("start drop")) }
        }
    }

    /** D05 "I'm at the door". */
    fun markArrived(orderId: String) {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { repo.markArrived(orderId) }
                .onSuccess {
                    jobs.markArrived(orderId, System.currentTimeMillis())
                    _state.value = _state.value.copy(working = false, photo = DoorPhoto.None, deliverError = null,
                        codeAttemptsLeft = null, codeLockedUntil = null)
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(working = false, error = it.say("mark arrived")) }
        }
    }

    fun newPhotoFile(): File = photos.newPhotoFile()

    /** The camera wrote [file]; upload it now so the hand-over doesn't wait on it. */
    fun photoTaken(file: File, ok: Boolean) {
        if (!ok) return
        _state.value = _state.value.copy(photo = DoorPhoto.Uploading)
        viewModelScope.launch {
            _state.value = _state.value.copy(photo = when (val r = photos.upload(file)) {
                is PhotoUploadResult.Uploaded -> DoorPhoto.Ready(r.s3Key)
                PhotoUploadResult.StorageDown -> DoorPhoto.StorageDown
                is PhotoUploadResult.Failed -> DoorPhoto.Failed(r.message)
            })
        }
    }

    /**
     * D06/D07 hand-over. Only a photo we actually uploaded is sent — never a placeholder key.
     * [settlement] is one of the three values the server accepts (CASH / UPI_AT_DOOR /
     * NOT_COLLECTED); anything else is refused *after* the server has marked the order
     * delivered, which would drop the settlement and the driver's pay for the drop.
     */
    fun deliver(code: String, emptiesCollected: Int, settlement: String?, cashTaken: BigDecimal?, emptiesShortReason: String? = null,
                photoRequired: Boolean = true) {
        val job = _state.value.activeAssignment ?: return
        val key = (_state.value.photo as? DoorPhoto.Ready)?.s3Key
        if (key == null && photoRequired) { _state.value = _state.value.copy(deliverError = strings.get(R.string.drv_err_photo_first)); return }
        if (_state.value.codeLocked) { _state.value = _state.value.copy(deliverError = strings.get(R.string.drv_err_code_locked)); return }
        if (code.length != CODE_LENGTH || !code.all(Char::isDigit)) {
            _state.value = _state.value.copy(deliverError = strings.get(R.string.drv_err_code_digits, CODE_LENGTH)); return
        }
        val isCash = job.paymentMethod == "COD"
        val safeSettlement = if (isCash) settlement?.takeIf { it in SETTLEMENTS } ?: "CASH" else null
        _state.value = _state.value.copy(delivering = true, deliverError = null)
        viewModelScope.launch {
            runCatching {
                val r = platform.deliver(job.orderId, DeliverBody(code, key,
                    codCollected = if (isCash && safeSettlement != "NOT_COLLECTED") cashTaken ?: job.codAmount else null,
                    emptiesCollected = emptiesCollected, codSettlement = safeSettlement,
                    emptiesShortReason = emptiesShortReason.takeIf { emptiesCollected < job.emptiesExpected }))
                if (!r.isSuccessful) throw HttpException(r)
            }.onSuccess {
                val earning = runCatching { ops.dropEarning(job.orderId) }.getOrNull()
                jobs.keepOnly(_state.value.assignments.map { it.orderId }.toSet() - job.orderId)
                _state.value = _state.value.copy(
                    delivering = false, photo = DoorPhoto.None,
                    done = DropDone(job.orderNumber, job.customerFirstName, earning,
                        offeredFee = _state.value.offerFees[job.orderId],
                        emptiesInAuto = emptiesCollected,
                        cashTaken = if (isCash && safeSettlement == "CASH") cashTaken ?: job.codAmount else null)
                )
                refresh()
            }.onFailure { e -> _state.value = deliverFailed(_state.value, e) }
        }
    }

    // ---- Not home (D22) / back to the shop (D27) ------------------------------------------

    fun openNotHome() {
        val job = _state.value.activeAssignment ?: return
        _state.value = _state.value.copy(notHome = NotHome(job.orderId, calls = 0,
            arrivedAtMs = jobs.arrivedAt(job.orderId) ?: System.currentTimeMillis()))
    }

    fun closeNotHome() { _state.value = _state.value.copy(notHome = null) }

    /** One call attempt: the Call button on the not-home screen, or the driver saying they rang another way. */
    fun recordCall() {
        val n = _state.value.notHome ?: return
        _state.value = _state.value.copy(notHome = n.copy(calls = n.calls + 1, problem = null))
    }

    /** Fail the drop. The server measures the wait from arrival and refuses NOT_HOME too soon. */
    fun failDrop(reason: String, note: String?) {
        val n = _state.value.notHome ?: return
        val job = _state.value.assignments.firstOrNull { it.orderId == n.orderId } ?: return
        _state.value = _state.value.copy(notHome = n.copy(sending = true, problem = null))
        viewModelScope.launch {
            runCatching {
                platform.failDrop(n.orderId, FailDropBody(reason, n.calls,
                    waitedSeconds = (System.currentTimeMillis() - n.arrivedAtMs) / 1000, note = note?.takeIf { it.isNotBlank() }))
            }.onSuccess { r ->
                jobs.keepOnly(_state.value.assignments.map { it.orderId }.toSet() - n.orderId)
                _state.value = _state.value.copy(notHome = null, failed = FailedDrop(job.orderNumber, job.customerFirstName, r,
                    shopName = job.shopName ?: _state.value.taskCards[job.orderId]?.vendorName,
                    shopLat = job.pickupLat ?: _state.value.taskCards[job.orderId]?.pickupLat,
                    shopLng = job.pickupLng ?: _state.value.taskCards[job.orderId]?.pickupLng))
                refresh()
            }.onFailure { e ->
                val err = ServerError.of(e)
                _state.value = _state.value.copy(notHome = n.copy(sending = false, problem = notHomeProblem(err) ?: e.say("fail drop")))
            }
        }
    }

    fun dismissFailed() { _state.value = _state.value.copy(failed = null) }

    fun dismissDone() { _state.value = _state.value.copy(done = null) }

    // ---- Task offers (D03 / D20) -------------------------------------------------------

    private fun syncOfferLoop() {
        val shouldPoll = visible && _state.value.isOnDuty && !_state.value.onBreak && _state.value.activeAssignment == null
        if (!shouldPoll) { offerJob?.cancel(); offerJob = null; _state.value = _state.value.copy(offer = null); return }
        if (offerJob?.isActive == true) return
        offerJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repo.pendingOffer() }.onSuccess(::onOffer)
                delay(OFFER_POLL_MS)
            }
        }
    }

    /** A shown offer that disappears without an answer went to someone else (D20). */
    internal fun onOffer(offer: DriverOffer?) {
        val previous = shownOffer
        if (offer == null && previous != null && answeredOffer != previous.orderId) {
            _state.value = _state.value.copy(missed = MissedTask(previous.driverFee,
                previous.task?.dropAddress ?: strings.get(R.string.drv_the_drop), previous.task?.vendorName ?: strings.get(R.string.drv_the_shop)))
        }
        shownOffer = offer
        _state.value = _state.value.copy(offer = offer, offerShownAtMs = if (offer?.orderId != previous?.orderId) System.currentTimeMillis() else _state.value.offerShownAtMs)
    }

    /** The countdown ran out on screen before the next poll. */
    fun offerExpired() { onOffer(null) }

    fun dismissMissed() { _state.value = _state.value.copy(missed = null) }

    fun acceptOffer() {
        val offer = _state.value.offer ?: return
        answeredOffer = offer.orderId
        _state.value = _state.value.copy(offerBusy = true, error = null)
        viewModelScope.launch {
            runCatching { repo.acceptOffer(offer.orderId) }
                .onSuccess {
                    _state.value = _state.value.copy(offer = null, offerBusy = false,
                        taskCards = offer.task?.let { _state.value.taskCards + (offer.orderId to it) } ?: _state.value.taskCards,
                        offerFees = _state.value.offerFees + (offer.orderId to offer.driverFee))
                    shownOffer = null
                    refresh()
                }
                .onFailure {
                    shownOffer = null
                    _state.value = _state.value.copy(offer = null, offerBusy = false,
                        error = it.say("accept run", strings.get(R.string.drv_err_task_gone)))
                }
        }
    }

    fun passOffer() {
        val offer = _state.value.offer ?: return
        answeredOffer = offer.orderId
        shownOffer = null
        _state.value = _state.value.copy(offer = null)
        viewModelScope.launch { runCatching { repo.passOffer(offer.orderId) } }
    }

    private fun Throwable.say(context: String, fallback: String = strings.get(R.string.drv_err_try_again)) =
        userMessage(context = context, fallback = fallback)

    /** D21 wrong code (attempts left) / D26 locked (until when) — from the error details. */
    internal fun deliverFailed(s: DriverHomeUiState, e: Throwable): DriverHomeUiState {
        val err = ServerError.of(e)
        return when (err?.code) {
            "ORDER_OTP_MISMATCH" -> {
                val left = err.int("attemptsLeft") ?: err.int("triesLeft")
                s.copy(delivering = false, codeAttemptsLeft = left,
                    deliverError = left?.let { strings.get(if (it == 1) R.string.drv_err_wrong_code_one else R.string.drv_err_wrong_code_many, it) }
                        ?: strings.get(R.string.drv_err_wrong_code))
            }
            "ORDER_OTP_LOCKED" -> s.copy(delivering = false, codeAttemptsLeft = 0, codeLockedUntil = err.text("lockedUntil"),
                deliverError = null)
            null -> s.copy(delivering = false, deliverError = e.say("deliver"))
            else -> s.copy(delivering = false, deliverError = err.message ?: strings.get(R.string.drv_err_try_again))
        }
    }

    internal fun notHomeProblem(err: ServerError?): String? = when (err?.code) {
        "DRIVER_NOT_HOME_TOO_SOON" -> {
            val calls = (err.int("callsNeeded") ?: 0) - (err.int("callsMade") ?: 0)
            val wait = (err.int("waitSecondsNeeded") ?: 0) - (err.int("waitedSeconds") ?: 0)
            val mins = (wait + 59) / 60
            val callPart = if (calls > 0) strings.get(if (calls == 1) R.string.drv_nothome_calls_one else R.string.drv_nothome_calls_many, calls) else null
            val waitPart = if (wait > 0) strings.get(if (mins == 1) R.string.drv_nothome_wait_one else R.string.drv_nothome_wait_many, mins) else null
            val what = when {
                callPart != null && waitPart != null -> strings.get(R.string.drv_nothome_and, callPart, waitPart)
                else -> callPart ?: waitPart
            }
            what?.let { strings.get(R.string.drv_nothome_not_yet, it) } ?: err.message
        }
        null -> null
        else -> err.message
    }

    companion object {
        const val OFFER_POLL_MS = 5_000L
        /** The server issues 6-digit delivery codes (the boards draw 4; the server is the source of truth). */
        const val CODE_LENGTH = 6
        val SETTLEMENTS = setOf("CASH", "UPI_AT_DOOR", "PARTIAL", "NOT_COLLECTED")
        /** Break lengths the driver can pick (DOnBreak); the server default is admin-set. */
        val BREAK_CHOICES = listOf(15, 30, 45)
    }
}

data class DriverHomeUiState(
    val loading: Boolean = true,
    val shift: DriverShiftResponse? = null,
    val assignments: List<DriverAssignment> = emptyList(),
    val error: String? = null,
    val offer: DriverOffer? = null,
    val offerShownAtMs: Long = 0,
    val offerBusy: Boolean = false,
    val delivering: Boolean = false,
    val deliverError: String? = null,
    val working: Boolean = false,
    val starting: Boolean = false,
    val shops: List<DriverShop> = emptyList(),
    val choosingShop: Boolean = false,
    val today: DriverToday? = null,
    val loadedOrders: Set<String> = emptySet(),
    /** Offer cards for tasks this driver accepted — they carry the shop address and pins. */
    val taskCards: Map<String, DriverTaskCard> = emptyMap(),
    val offerFees: Map<String, BigDecimal> = emptyMap(),
    val photo: DoorPhoto = DoorPhoto.None,
    val missed: MissedTask? = null,
    val done: DropDone? = null,
    val shiftState: DriverShiftState? = null,
    /** The last finished shift, loaded while off duty. */
    val lastShift: LastShift? = null,
    val endCheck: EndCheck? = null,
    val endChecking: Boolean = false,
    val ending: Boolean = false,
    val codeAttemptsLeft: Int? = null,
    /** ISO instant; set while the delivery code is locked (D26). */
    val codeLockedUntil: String? = null,
    val notHome: NotHome? = null,
    val failed: FailedDrop? = null,
    val gps: GpsProblem? = null,
    /** Last GPS fix, for "how far" on the ride screen. */
    val here: Pair<Double, Double>? = null
) {
    val isOnDuty: Boolean get() = shift != null && shift.endedAt == null
    val onBreak: Boolean get() = isOnDuty && shiftState?.onBreak == true
    val codeLocked: Boolean get() = codeLockedUntil?.let {
        runCatching { java.time.Instant.parse(it).isAfter(java.time.Instant.now()) }.getOrDefault(true) } == true
    val activeAssignment: DriverAssignment? get() = assignments.firstOrNull {
        it.status in setOf("PREPARING", "DISPATCHED", "ARRIVING")
    }
    val step: DropStep? get() = activeAssignment?.let {
        when {
            it.status == "ARRIVING" -> DropStep.HAND_OVER
            it.status == "DISPATCHED" && it.orderId in loadedOrders -> DropStep.RIDE
            else -> DropStep.LOAD
        }
    }
    /** Drops still to make after the active one, in the order the server lists them (D09/D32). */
    val upNext: List<DriverAssignment> get() = assignments.filter {
        it.status in setOf("PREPARING", "DISPATCHED", "ARRIVING") && it.orderId != activeAssignment?.orderId
    }
    val cashToCollect: BigDecimal get() = assignments.filter { it.paymentMethod == "COD" && it.status != "DELIVERED" }
        .fold(BigDecimal.ZERO) { a, j -> a + (j.codAmount ?: BigDecimal.ZERO) }
}

/** Straight-line km between two pins (no Maps key, so no road distance) — shown as "about". */
fun kmBetween(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.first - a.first); val dLng = Math.toRadians(b.second - a.second)
    val h = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(a.first)) * Math.cos(Math.toRadians(b.first)) * Math.sin(dLng / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(h))
}
