package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorOrderDetailViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val ops: VendorOpsApi,
    savedState: SavedStateHandle,
    private val override: CodeOverrideApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val orderId: String = savedState["orderId"] ?: error("orderId required")

    private val _state = MutableStateFlow(VendorOrderDetailUiState())
    val state: StateFlow<VendorOrderDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.order == null, error = null)
        viewModelScope.launch {
            runCatching { ops.order(orderId) }
                .onSuccess { o ->
                    _state.value = _state.value.copy(loading = false, order = o)
                    // The fee lines exist once the order is settled (delivered, or cancelled with trip pay).
                    if (o.status in setOf("DELIVERED", "CANCELLED")) runCatching { ops.earning(orderId) }
                        .onSuccess { e -> _state.value = _state.value.copy(earning = e) }
                    if (o.driverId == null && o.status in setOf("VENDOR_ACCEPTED", "PREPARING"))
                        runCatching { ops.team() }.onSuccess { t -> _state.value = _state.value.copy(team = t) }
                }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage(context = "vendor order", fallback = strings.get(R.string.vend_err_load_order))) }
        }
    }

    fun pickDriver(id: String) { _state.value = _state.value.copy(pickedDriver = id) }

    /** Packed: send with the driver the owner picked (board V43 "Who's taking it?"). */
    fun send() {
        val d = _state.value.pickedDriver ?: return
        act(strings.get(R.string.vend_err_hand_driver)) {
            if (_state.value.order?.status == "VENDOR_ACCEPTED") repo.ready(orderId)
            repo.dispatch(orderId, d)
        }
    }

    fun markReady() = act(strings.get(R.string.vend_err_mark_packed)) { repo.ready(orderId) }

    /** Let the app offer it to whoever is on shift; they accept on their phone. */
    fun offerDriver() {
        _state.value = _state.value.copy(working = true, error = null, note = null)
        viewModelScope.launch {
            runCatching { repo.offerDriver(orderId) }
                .onSuccess { r ->
                    _state.value = _state.value.copy(working = false,
                        note = strings.get(if (r.offered) R.string.vend_offered_shift else R.string.vend_no_driver_shift))
                    load()
                }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "offer driver", fallback = strings.get(R.string.vend_err_offer_run))) }
        }
    }

    /**
     * Driver at the door, delivery code locked after wrong tries (driver board D26): the owner
     * confirms after speaking to the customer. The server refuses unless the code is locked,
     * tells the customer, and flags shops that override often.
     */
    fun confirmWithoutCode(reason: String) {
        if (reason.isBlank()) { _state.value = _state.value.copy(error = strings.get(R.string.vend_override_need_reason)); return }
        _state.value = _state.value.copy(working = true, error = null, note = null)
        viewModelScope.launch {
            runCatching {
                val r = override.codeOverride(orderId, CodeOverrideBody(reason.trim()))
                if (!r.isSuccessful) throw retrofit2.HttpException(r)
            }
                .onSuccess { _state.value = _state.value.copy(working = false, note = strings.get(R.string.vend_override_done)); load() }
                .onFailure { e ->
                    val err = com.neerly.mobile.feature.driver.ServerError.of(e)
                    _state.value = _state.value.copy(working = false, error = when (err?.code) {
                        "ORDER_CODE_NOT_LOCKED" -> strings.get(R.string.vend_override_not_locked)
                        else -> err?.message ?: strings.get(R.string.vend_err_override)
                    })
                }
        }
    }

    private fun act(fallback: String, call: suspend () -> Unit) {
        _state.value = _state.value.copy(working = true, error = null)
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _state.value = _state.value.copy(working = false, pickedDriver = null); load() }
                .onFailure { _state.value = _state.value.copy(working = false,
                    error = it.userMessage(context = "vendor order action", fallback = fallback)) }
        }
    }
}

data class VendorOrderDetailUiState(
    val loading: Boolean = true,
    val order: VendorOrderView? = null,
    val earning: VendorOrderEarning? = null,
    val team: List<VendorTeamLive> = emptyList(),
    val pickedDriver: String? = null,
    val working: Boolean = false,
    val note: String? = null,
    val error: String? = null
) {
    val needsDriver get() = order?.let { it.driverId == null && it.status in setOf("VENDOR_ACCEPTED", "PREPARING") } == true
    /** Drivers who can go now first: on shift, then off; someone mid-drop last. */
    val driversByReadiness get() = team.sortedBy { when (it.liveStatus) { "ON_SHIFT" -> 0; "OFF" -> 1; else -> 2 } }
}


@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CodeOverrideBody(val reason: String)

/** `POST /vendor/orders/{id}/code-override` — only while the delivery code is locked. */
interface CodeOverrideApi {
    @retrofit2.http.POST("api/v1/vendor/orders/{id}/code-override")
    suspend fun codeOverride(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: CodeOverrideBody): retrofit2.Response<Unit>
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
object CodeOverrideApiModule {
    @dagger.Provides @javax.inject.Singleton
    fun provide(retrofit: retrofit2.Retrofit): CodeOverrideApi = retrofit.create(CodeOverrideApi::class.java)
}
