package com.neerly.mobile.feature.vendor.settings

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.serverErrorCode
import com.neerly.mobile.core.util.userMessage
import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class DeliveryArea(val serviceablePincodes: List<String> = emptyList(), val serviceAreaRadiusM: Int? = null)

interface VendorAreaApi {
    @GET("api/v1/vendor/business/area") suspend fun area(): DeliveryArea
    @PUT("api/v1/vendor/business/area") suspend fun save(@Body body: DeliveryArea): DeliveryArea
}

@Module
@InstallIn(SingletonComponent::class)
object VendorAreaModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorAreaApi = retrofit.create(VendorAreaApi::class.java)
}

data class AreaUiState(
    val loading: Boolean = true,
    val pincodes: List<String> = emptyList(),
    val radiusKm: Float = 5f,
    val draft: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    val draftValid get() = PINCODE.matches(draft) && draft !in pincodes
    val canSave get() = !saving && pincodes.isNotEmpty() && pincodes.size <= 50

    companion object {
        val PINCODE = Regex("^[1-9]\\d{5}$")
        const val MIN_KM = 0.5f
        const val MAX_KM = 20f
    }
}

/** Where the shop delivers — pincodes it serves and how far a driver goes (server: 500–20,000 m). */
@HiltViewModel
class VendorAreaViewModel @Inject constructor(
    private val api: VendorAreaApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(AreaUiState())
    val state: StateFlow<AreaUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.area() }
                .onSuccess { a -> _state.value = _state.value.copy(loading = false, pincodes = a.serviceablePincodes,
                    radiusKm = (a.serviceAreaRadiusM?.div(1000f) ?: 5f).coerceIn(AreaUiState.MIN_KM, AreaUiState.MAX_KM)) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("delivery area", strings.get(R.string.vend_err_load_area))) }
        }
    }

    fun type(v: String) { _state.value = _state.value.copy(draft = v.filter(Char::isDigit).take(6), saved = false) }
    fun add() { val s = _state.value; if (s.draftValid) _state.value = s.copy(pincodes = s.pincodes + s.draft, draft = "", saved = false) }
    fun remove(p: String) { _state.value = _state.value.copy(pincodes = _state.value.pincodes - p, saved = false) }
    fun radius(km: Float) { _state.value = _state.value.copy(radiusKm = (Math.round(km * 2) / 2f).coerceIn(AreaUiState.MIN_KM, AreaUiState.MAX_KM), saved = false) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { api.save(DeliveryArea(s.pincodes, (s.radiusKm * 1000).toInt())) }
                .onSuccess { a -> _state.value = _state.value.copy(saving = false, saved = true, pincodes = a.serviceablePincodes) }
                .onFailure { e -> _state.value = _state.value.copy(saving = false,
                    error = if (e.serverErrorCode() == "VENDOR_AREA_INVALID")
                        e.userMessage("delivery area", strings.get(R.string.vend_area_invalid))
                    else e.userMessage("delivery area", strings.get(R.string.vend_err_save_try2))) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VendorAreaScreen(onBack: () -> Unit, vm: VendorAreaViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    BoondhPage(
        title = stringResource(R.string.vend_delivery_area), onBack = onBack,
        subtitle = stringResource(R.string.vend_area_sub),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.saved) Notice(stringResource(R.string.vend_area_saved), Tone.Ok)
            PrimaryButton(stringResource(if (s.saving) R.string.vend_saving else R.string.vend_save_area), vm::save, enabled = s.canSave)
        }
    ) {
        if (s.loading) { item { LoadState(true, null, vm::load) }; return@BoondhPage }
        item { SectionLabel(stringResource(R.string.vend_pincodes_n, s.pincodes.size)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.pincodes.forEach { p ->
                    InputChip(selected = false, onClick = { vm.remove(p) }, label = { Text(p, fontSize = 15.sp) },
                        trailingIcon = { Text("✕", fontSize = 13.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
            if (s.pincodes.isEmpty()) Text(stringResource(R.string.vend_add_one_pincode), fontSize = 13.sp, color = NeerlyColors.Warn)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = s.draft, onValueChange = vm::type, label = { Text(stringResource(R.string.vend_add_pincode)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.vend_add), vm::add, enabled = s.draftValid, modifier = Modifier.width(96.dp))
            }
        }
        item { SectionLabel(stringResource(R.string.vend_how_far)) }
        item {
            Text(stringResource(R.string.vend_km_from_shop, "%.1f".format(s.radiusKm)), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Slider(value = s.radiusKm, onValueChange = vm::radius, valueRange = AreaUiState.MIN_KM..AreaUiState.MAX_KM, steps = 38)
            Text(stringResource(R.string.vend_farther_fee), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
    }
}
