package com.neerly.mobile.feature.driver

import com.neerly.mobile.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.PresignRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

private fun Throwable.say(strings: com.neerly.mobile.core.util.Strings, ctx: String, fallback: String = strings.get(R.string.drv_err_try_again)) =
    userMessage(ctx, fallback)

// ---- D17 Me · D31 joined ------------------------------------------------------------------

data class DriverMeState(
    val loading: Boolean = true,
    val me: DriverMe? = null,
    val error: String? = null,
    val savingUpi: Boolean = false,
    val upiSaved: Boolean = false,
    val logoutCheck: LogoutCheck? = null,
    val checkingLogout: Boolean = false
)

@HiltViewModel
class DriverMeViewModel @Inject constructor(private val api: DriverPlatformApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _state = MutableStateFlow(DriverMeState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.me == null, error = null)
        viewModelScope.launch {
            runCatching { api.me() }
                .onSuccess { _state.value = _state.value.copy(loading = false, me = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(strings, "driver me", strings.get(R.string.drv_err_load_profile))) }
        }
    }

    /** Weekly pay goes to this UPI ID. */
    fun saveUpi(upi: String) {
        val v = upi.trim()
        if (!UPI.matches(v)) { _state.value = _state.value.copy(error = strings.get(R.string.drv_err_upi_format)); return }
        _state.value = _state.value.copy(savingUpi = true, error = null, upiSaved = false)
        viewModelScope.launch {
            runCatching { api.setPayoutUpi(PayoutUpiBody(v)) }
                .onSuccess { _state.value = _state.value.copy(savingUpi = false, me = it, upiSaved = true) }
                .onFailure { _state.value = _state.value.copy(savingUpi = false, error = it.say(strings, "payout upi")) }
        }
    }

    /** Logging out with cash in hand or drops on the round strands both — ask the server first. */
    fun checkLogout() {
        _state.value = _state.value.copy(checkingLogout = true, error = null)
        viewModelScope.launch {
            runCatching { api.logoutCheck() }
                .onSuccess { _state.value = _state.value.copy(checkingLogout = false, logoutCheck = it) }
                .onFailure { _state.value = _state.value.copy(checkingLogout = false, error = it.say(strings, "logout check")) }
        }
    }

    fun dismissLogout() { _state.value = _state.value.copy(logoutCheck = null) }

    companion object { val UPI = Regex("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.]{1,63}$") }
}

// ---- D15 / D30 vehicle ------------------------------------------------------------------

data class VehicleState(val loading: Boolean = true, val vehicle: DriverVehicle = DriverVehicle(),
                        val saving: Boolean = false, val saved: Boolean = false, val error: String? = null)

@HiltViewModel
class DriverVehicleViewModel @Inject constructor(private val api: DriverPlatformApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _state = MutableStateFlow(VehicleState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { api.vehicle() }
                .onSuccess { _state.value = _state.value.copy(loading = false, vehicle = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(strings, "vehicle", strings.get(R.string.drv_err_load_vehicle))) }
        }
    }

    fun save(type: String, plate: String, model: String) {
        if (type != "CYCLE" && plate.isBlank()) { _state.value = _state.value.copy(error = strings.get(R.string.drv_err_add_plate)); return }
        _state.value = _state.value.copy(saving = true, error = null, saved = false)
        viewModelScope.launch {
            runCatching { api.saveVehicle(DriverVehicle(type, plate.trim().ifBlank { null }, model.trim().ifBlank { null })) }
                .onSuccess { _state.value = _state.value.copy(saving = false, vehicle = it, saved = true) }
                .onFailure { e -> _state.value = _state.value.copy(saving = false,
                    error = ServerError.of(e)?.message ?: e.say(strings, "vehicle")) }
        }
    }

    companion object {
        val TYPES = listOf("BIKE" to R.string.drv_vehicle_bike, "SCOOTER" to R.string.drv_vehicle_scooter, "AUTO" to R.string.drv_vehicle_auto,
            "CARGO_AUTO" to R.string.drv_vehicle_cargo_auto, "VAN" to R.string.drv_vehicle_van, "CYCLE" to R.string.drv_vehicle_cycle)
    }
}

// ---- D16 papers -------------------------------------------------------------------------

data class DocsState(val loading: Boolean = true, val docs: DriverDocs? = null, val uploading: String? = null,
                     val error: String? = null, val storageDown: Boolean = false)

@HiltViewModel
class DriverDocumentsViewModel @Inject constructor(
    private val api: DriverPlatformApi,
    private val shared: NeerlyApi,
    private val uploader: PhotoUploader,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(DocsState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { api.documents() }
                .onSuccess { _state.value = _state.value.copy(loading = false, docs = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(strings, "driver docs", strings.get(R.string.drv_err_load_papers))) }
        }
    }

    fun newPhotoFile(): File = uploader.newPhotoFile()

    /** Photo of the paper → presign DRIVER_DOCS (key is under driver-docs/{driverId}/) → PUT → submit. */
    fun upload(type: String, file: File, docNumber: String?, expiresOn: String?) {
        _state.value = _state.value.copy(uploading = type, error = null, storageDown = false)
        viewModelScope.launch {
            val key = when (val r = uploader.upload(file, scope = "DRIVER_DOCS", kind = type.lowercase())) {
                is PhotoUploadResult.Uploaded -> r.s3Key
                PhotoUploadResult.StorageDown -> { _state.value = _state.value.copy(uploading = null, storageDown = true); return@launch }
                is PhotoUploadResult.Failed -> { _state.value = _state.value.copy(uploading = null, error = r.message); return@launch }
            }
            runCatching { api.submitDocument(SubmitDocBody(type, key, docNumber?.trim()?.ifBlank { null }, expiresOn?.trim()?.ifBlank { null })) }
                .onSuccess { _state.value = _state.value.copy(uploading = null, docs = it) }
                .onFailure { e -> _state.value = _state.value.copy(uploading = null, error = ServerError.of(e)?.message ?: e.say(strings, "driver doc")) }
        }
    }

    companion object {
        val LABELS = mapOf(
            "DRIVING_LICENSE" to R.string.drv_doc_license, "VEHICLE_RC" to R.string.drv_doc_rc, "VEHICLE_INSURANCE" to R.string.drv_doc_insurance,
            "AADHAAR" to R.string.drv_doc_aadhaar, "POLICE_VERIFICATION" to R.string.drv_doc_police)
    }
}

// ---- D12 money · week ------------------------------------------------------------------

data class WeekState(val loading: Boolean = true, val week: DriverWeek? = null, val weekStart: LocalDate, val error: String? = null)

@HiltViewModel
class DriverWeekViewModel @Inject constructor(private val api: DriverPlatformApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val ist = ZoneId.of("Asia/Kolkata")
    private val thisWeek = LocalDate.now(ist).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val _state = MutableStateFlow(WeekState(weekStart = thisWeek))
    val state = _state.asStateFlow()

    init { load(thisWeek) }

    fun previous() = load(_state.value.weekStart.minusWeeks(1))
    fun next() { if (_state.value.weekStart < thisWeek) load(_state.value.weekStart.plusWeeks(1)) }
    val isThisWeek get() = _state.value.weekStart == thisWeek

    fun load(start: LocalDate = _state.value.weekStart) {
        _state.value = _state.value.copy(loading = true, weekStart = start, error = null)
        viewModelScope.launch {
            runCatching { api.week(start.toString()) }
                .onSuccess { _state.value = _state.value.copy(loading = false, week = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(strings, "driver week", strings.get(R.string.drv_err_load_week))) }
        }
    }
}

// ---- D10 jar pickups -------------------------------------------------------------------

data class PickupsState(val loading: Boolean = true, val pickups: List<DriverPickup> = emptyList(),
                        val working: String? = null, val photoFor: String? = null, val photoKey: String? = null,
                        val error: String? = null)

@HiltViewModel
class DriverPickupsViewModel @Inject constructor(private val api: DriverPlatformApi, private val uploader: PhotoUploader,
    private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _state = MutableStateFlow(PickupsState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { api.pickups() }
                .onSuccess { _state.value = _state.value.copy(loading = false, pickups = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.say(strings, "driver pickups", strings.get(R.string.drv_err_load_pickups))) }
        }
    }

    fun start(id: String) = act(id) { api.startPickup(id) }

    fun newPhotoFile(): File = uploader.newPhotoFile()

    /** Photo of the collected jars (it backs any deposit claim), then complete. */
    fun photoTaken(id: String, file: File, ok: Boolean) {
        if (!ok) return
        _state.value = _state.value.copy(working = id, error = null)
        viewModelScope.launch {
            when (val r = uploader.upload(file, scope = "ORDER_PROOF", kind = "pickup")) {
                is PhotoUploadResult.Uploaded -> _state.value = _state.value.copy(working = null, photoFor = id, photoKey = r.s3Key)
                PhotoUploadResult.StorageDown -> _state.value = _state.value.copy(working = null,
                    error = strings.get(R.string.drv_err_pickup_storage))
                is PhotoUploadResult.Failed -> _state.value = _state.value.copy(working = null, error = r.message)
            }
        }
    }

    fun complete(id: String, note: String?) {
        val key = _state.value.photoKey.takeIf { _state.value.photoFor == id }
        act(id) { api.completePickup(id, CompletePickupBody(key, note?.trim()?.ifBlank { null })) }
    }

    private fun act(id: String, call: suspend () -> DriverPickup) {
        _state.value = _state.value.copy(working = id, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { p -> _state.value = _state.value.copy(working = null,
                    pickups = _state.value.pickups.map { if (it.id == p.id) p else it }) }
                .onFailure { e -> _state.value = _state.value.copy(working = null, error = ServerError.of(e)?.message ?: e.say(strings, "pickup")) }
        }
    }
}

// ---- D29 SOS ---------------------------------------------------------------------------

data class SosState(val sending: Boolean = false, val sos: DriverSos? = null, val error: String? = null,
                    val lastShared: Long? = null, val resolved: Boolean = false)

/**
 * Hold-to-send is the confirmation, so [send] fires straight away. While open, the position
 * is shared every [SHARE_MS] until the driver says they're safe.
 */
@HiltViewModel
class DriverSosViewModel @Inject constructor(private val api: DriverPlatformApi, private val ticker: DriverLocationTicker,
    private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _state = MutableStateFlow(SosState())
    val state = _state.asStateFlow()
    private var shareJob: Job? = null

    fun send(note: String? = null) {
        if (_state.value.sending || _state.value.sos != null) return
        _state.value = _state.value.copy(sending = true, error = null)
        viewModelScope.launch {
            ticker.start()
            val fix = ticker.freshFix() ?: run { delay(1500); ticker.freshFix() }
            runCatching { api.sos(SosBody(fix?.latitude, fix?.longitude, note)) }
                .onSuccess { _state.value = _state.value.copy(sending = false, sos = it, lastShared = System.currentTimeMillis()); share(it.id) }
                .onFailure { _state.value = _state.value.copy(sending = false,
                    error = it.say(strings, "sos", strings.get(R.string.drv_err_sos_failed))) }
        }
    }

    private fun share(id: String) {
        shareJob?.cancel()
        shareJob = viewModelScope.launch {
            while (isActive) {
                delay(SHARE_MS)
                ticker.freshFix()?.let { f ->
                    runCatching { api.sosLocation(id, SosLocationBody(f.latitude, f.longitude)) }
                        .onSuccess { _state.value = _state.value.copy(lastShared = System.currentTimeMillis()) }
                }
            }
        }
    }

    fun safe() {
        val id = _state.value.sos?.id ?: return
        viewModelScope.launch {
            runCatching { api.sosResolve(id, SosResolveBody("Driver marked safe")) }
                .onSuccess { shareJob?.cancel(); _state.value = _state.value.copy(resolved = true) }
                .onFailure { _state.value = _state.value.copy(error = it.say(strings, "sos resolve")) }
        }
    }

    override fun onCleared() { shareJob?.cancel(); super.onCleared() }

    companion object { const val SHARE_MS = 15_000L }
}
