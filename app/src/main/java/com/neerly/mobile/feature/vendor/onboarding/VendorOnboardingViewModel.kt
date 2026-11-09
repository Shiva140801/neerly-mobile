package com.neerly.mobile.feature.vendor.onboarding

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.SubmitVendorOnboardingRequest
import com.neerly.mobile.data.dto.VendorHoursRow
import com.neerly.mobile.data.dto.VendorResponse
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.address.AddressLocator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Boards V01–V07. Before submit: welcome → shop (+ GPS pin, V47) → area & hours. Submit creates
 * the application; documents, bank and first products need the shop to exist, so they come
 * after — then the under-review timeline. Re-opening the wizard with an application on file
 * skips straight to what's left.
 */
enum class SetupStep { WELCOME, SHOP, AREA, DOCS, BANK, PRODUCTS, REVIEW }

enum class SundayMode(@androidx.annotation.StringRes val label: Int) { CLOSED(R.string.vend_sun_closed), HALF(R.string.vend_sun_half), FULL(R.string.vend_sun_full) }

data class ShopForm(
    val name: String = "",
    val owner: String = "",
    val flat: String = "",
    val street: String = "",
    val landmark: String = "",
    val city: String = "Hyderabad",
    val pincode: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val foundAt: String? = null,
    val fssaiNumber: String = "",
    val fssaiExpiry: String = "",
    val extraPincodes: Set<String> = emptySet(),
    val radiusKm: Int = 3,
    val opens: String = "06:00",
    val closes: String = "21:00",
    val sunday: SundayMode = SundayMode.HALF
) {
    val pincodes: Set<String> get() = (extraPincodes + pincode).filter { it.matches(PIN) }.toSet()
    val tier2: Boolean get() = fssaiNumber.isBlank()

    /** The first problem on the shop step, as a string id; null when it's complete. */
    @androidx.annotation.StringRes
    fun shopProblem(): Int? = when {
        name.trim().length < 2 -> R.string.vend_op_name
        owner.trim().length < 2 -> R.string.vend_op_owner
        lat == null || lng == null -> R.string.vend_op_location
        street.isBlank() -> R.string.vend_op_street
        !pincode.matches(PIN) -> R.string.vend_op_pincode
        fssaiNumber.isNotBlank() && !fssaiNumber.matches(Regex("^\\d{14}$")) -> R.string.vend_op_fssai
        fssaiNumber.isNotBlank() && runCatching { java.time.LocalDate.parse(fssaiExpiry.trim()) }.isFailure -> R.string.vend_op_fssai_expiry
        else -> null
    }

    @androidx.annotation.StringRes
    fun areaProblem(): Int? = when {
        pincodes.isEmpty() -> R.string.vend_op_pincodes
        runCatching { java.time.LocalTime.parse(opens) < java.time.LocalTime.parse(closes) }.getOrDefault(false).not() ->
            R.string.vend_op_hours
        else -> null
    }

    /** Mon–Sat at the chosen hours, Sunday per [sunday] (1 = Monday on the server). */
    fun hoursRows(): List<VendorHoursRow> {
        val o = "$opens:00"; val c = "$closes:00"
        val week = (1..6).map { VendorHoursRow(it, o, c) }
        return week + when (sunday) {
            SundayMode.CLOSED -> emptyList()
            SundayMode.HALF -> listOf(VendorHoursRow(7, o, "13:00:00"))
            SundayMode.FULL -> listOf(VendorHoursRow(7, o, c))
        }
    }

    companion object { val PIN = Regex("^[1-9]\\d{5}$") }
}

data class ProductPick(val on: Boolean = false, val price: String = "")

data class VendorSetupUiState(
    val checking: Boolean = true,
    val step: SetupStep = SetupStep.WELCOME,
    val form: ShopForm = ShopForm(),
    val locating: Boolean = false,
    val locationError: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    /** The signed-in account can't sell yet — it needs a seller sign-in (see wizard). */
    val needsSellerSignIn: Boolean = false,
    val vendor: VendorResponse? = null,
    val templates: List<ProductTemplate> = emptyList(),
    val picks: Map<String, ProductPick> = emptyMap(),
    val acceptSwaps: Boolean = true,
    val deposit: String = "",
    val existingProducts: Int = 0,
    val saving: Boolean = false,
    val notice: String? = null
)

@HiltViewModel
class VendorOnboardingViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val setup: VendorSetupApi,
    private val locator: AddressLocator,
    private val auth: com.neerly.mobile.data.auth.AuthRepository,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorSetupUiState())
    val state: StateFlow<VendorSetupUiState> = _state.asStateFlow()

    init {
        // An application already on file: carry on from documents instead of a second submit (409).
        viewModelScope.launch {
            val me = runCatching { repo.me() }.getOrNull()
            _state.value = _state.value.copy(checking = false, vendor = me,
                step = if (me != null) (if (me.status == "ACTIVE") SetupStep.REVIEW else SetupStep.DOCS) else SetupStep.WELCOME)
        }
    }

    fun go(step: SetupStep) {
        _state.value = _state.value.copy(step = step, error = null, notice = null)
        if (step == SetupStep.PRODUCTS && _state.value.templates.isEmpty()) loadTemplates()
    }

    fun back(): Boolean {
        val s = _state.value
        val prev = when (s.step) {
            SetupStep.WELCOME -> return false
            SetupStep.SHOP -> SetupStep.WELCOME
            SetupStep.AREA -> SetupStep.SHOP
            SetupStep.DOCS -> return false          // submitted — nothing before this to go back to
            SetupStep.BANK -> SetupStep.DOCS
            SetupStep.PRODUCTS -> SetupStep.BANK
            SetupStep.REVIEW -> SetupStep.PRODUCTS
        }
        go(prev); return true
    }

    fun edit(transform: ShopForm.() -> ShopForm) {
        _state.value = _state.value.copy(form = _state.value.form.transform(), error = null)
    }

    /** V47 — the shop's pin from a real GPS fix (never a made-up city-centre point). */
    fun useMyLocation() {
        _state.value = _state.value.copy(locating = true, locationError = null)
        viewModelScope.launch {
            val fix = locator.currentFix()
            if (fix == null) {
                _state.value = _state.value.copy(locating = false,
                    locationError = strings.get(R.string.vend_loc_failed))
                return@launch
            }
            if (fix.first !in 6.0..37.5 || fix.second !in 68.0..97.5) {
                _state.value = _state.value.copy(locating = false, locationError = strings.get(R.string.vend_loc_outside))
                return@launch
            }
            val place = locator.reverse(fix.first, fix.second)
            val f = _state.value.form
            _state.value = _state.value.copy(locating = false, form = f.copy(
                lat = fix.first, lng = fix.second, foundAt = place?.line,
                street = f.street.ifBlank { place?.street.orEmpty() },
                pincode = f.pincode.ifBlank { place?.pincode.orEmpty() },
                city = place?.city ?: f.city))
        }
    }

    fun locationDenied() {
        _state.value = _state.value.copy(locationError = strings.get(R.string.vend_loc_off))
    }

    private var pinJob: kotlinx.coroutines.Job? = null

    /**
     * Board VShopPin — the map settled under the pin after a drag. The pin counts at once;
     * the address line follows after a short pause (one reverse geocode per burst of drags).
     */
    fun pinMoved(lat: Double, lng: Double) {
        if (lat !in 6.0..37.5 || lng !in 68.0..97.5) {
            _state.value = _state.value.copy(locationError = strings.get(R.string.vend_loc_outside))
            return
        }
        _state.value = _state.value.copy(locationError = null, form = _state.value.form.copy(lat = lat, lng = lng))
        pinJob?.cancel()
        pinJob = viewModelScope.launch {
            kotlinx.coroutines.delay(PIN_SETTLE_MS)
            val place = locator.reverse(lat, lng) ?: return@launch
            val f = _state.value.form
            _state.value = _state.value.copy(form = f.copy(foundAt = place.line,
                street = f.street.ifBlank { place.street.orEmpty() },
                pincode = f.pincode.ifBlank { place.pincode.orEmpty() },
                city = place.city ?: f.city))
        }
    }

    fun nextFromShop() {
        _state.value.form.shopProblem()?.let { _state.value = _state.value.copy(error = strings.get(it)); return }
        go(SetupStep.AREA)
    }

    /** Send the application, then save the opening hours (best effort — they can be set later). */
    fun submit() {
        val f = _state.value.form
        (f.shopProblem() ?: f.areaProblem())?.let { _state.value = _state.value.copy(error = strings.get(it)); return }
        if (_state.value.submitting) return
        _state.value = _state.value.copy(submitting = true, error = null, needsSellerSignIn = false)
        val req = SubmitVendorOnboardingRequest(
            businessName = f.name.trim(),
            proprietorName = f.owner.trim(),
            businessFlatNumber = f.flat.trim().ifBlank { null },
            businessStreet = f.street.trim(),
            businessLandmark = f.landmark.trim().ifBlank { null },
            businessCity = f.city.trim().ifBlank { "Hyderabad" },
            businessPincode = f.pincode,
            businessLatitude = f.lat?.let { BigDecimal.valueOf(it).setScale(6, java.math.RoundingMode.HALF_UP) },
            businessLongitude = f.lng?.let { BigDecimal.valueOf(it).setScale(6, java.math.RoundingMode.HALF_UP) },
            fssaiNumber = f.fssaiNumber.ifBlank { null },
            fssaiExpiresAt = f.fssaiExpiry.trim().ifBlank { null },
            serviceablePincodes = f.pincodes,
            serviceAreaRadiusM = f.radiusKm * 1000,
            isTier2 = f.tier2
        )
        viewModelScope.launch {
            runCatching { repo.submitOnboarding(req) }
                .onSuccess { v ->
                    // Submitting makes this number a seller (the server grants the role); switch the
                    // session to it so documents, bank and products — all seller-only — work next.
                    runCatching { auth.switchRole("VENDOR_OWNER") }
                    val hoursSaved = runCatching { repo.replaceHours(f.hoursRows()) }.isSuccess
                    _state.value = _state.value.copy(submitting = false, vendor = v, step = SetupStep.DOCS,
                        notice = if (hoursSaved) null else strings.get(R.string.vend_app_sent_hours))
                }
                .onFailure { e ->
                    val code = (e as? HttpException)?.code()
                    _state.value = _state.value.copy(submitting = false,
                        needsSellerSignIn = code == 403,
                        error = when (code) {
                            403 -> strings.get(R.string.vend_buyer_account)
                            409 -> strings.get(R.string.vend_app_on_file)
                            else -> e.userMessage("vendor submit", strings.get(R.string.vend_err_send_app))
                        })
                    if (code == 409) runCatching { repo.me() }.getOrNull()?.let {
                        _state.value = _state.value.copy(vendor = it, step = SetupStep.DOCS, error = null)
                    }
                }
        }
    }

    // ---------------------------------------------------------------- V05 first products

    fun loadTemplates() {
        viewModelScope.launch {
            val t = runCatching { setup.templates() }.getOrDefault(emptyList())
            val have = runCatching { setup.products() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                templates = t.filter { !it.categoryCode.startsWith("TANKER") }.sortedBy { it.sortOrder },
                existingProducts = have.size,
                error = if (t.isEmpty()) strings.get(R.string.vend_err_product_list) else null)
        }
    }

    fun pick(code: String, transform: ProductPick.() -> ProductPick) {
        val cur = _state.value.picks[code] ?: ProductPick()
        _state.value = _state.value.copy(picks = _state.value.picks + (code to cur.transform()), error = null)
    }

    fun setSwaps(on: Boolean) { _state.value = _state.value.copy(acceptSwaps = on) }
    fun setDeposit(v: String) { _state.value = _state.value.copy(deposit = v.filter { it.isDigit() }.take(5)) }

    fun saveProducts() {
        val s = _state.value
        val chosen = s.picks.filterValues { it.on }
        if (chosen.isEmpty()) { go(SetupStep.REVIEW); return }
        chosen.entries.firstOrNull { (_, p) -> p.price.toBigDecimalOrNull()?.let { it.signum() > 0 } != true }?.let { (code, _) ->
            val name = s.templates.firstOrNull { it.categoryCode == code }?.displayName ?: strings.get(R.string.vend_a_product)
            _state.value = s.copy(error = strings.get(R.string.vend_set_price_for, name)); return
        }
        val deposit = s.deposit.toBigDecimalOrNull()
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            val failed = chosen.mapNotNull { (code, p) ->
                val tpl = s.templates.firstOrNull { it.categoryCode == code }
                runCatching {
                    setup.createProduct(CreateProductBody(
                        templateCategoryCode = code,
                        price = p.price.toBigDecimal(),
                        dailyCapacity = 100,
                        allowKeepContainer = true,
                        allowTransferAndReturn = tpl?.allowsDeposit == true && s.acceptSwaps,
                        depositAmount = if (tpl?.allowsDeposit == true) deposit else null
                    ))
                }.exceptionOrNull()?.let { tpl?.displayName ?: code }
            }
            _state.value = _state.value.copy(saving = false,
                error = if (failed.isEmpty()) null else strings.get(R.string.vend_couldnt_add_x, failed.joinToString()))
            if (failed.isEmpty()) go(SetupStep.REVIEW)
        }
    }

    fun refreshVendor() {
        viewModelScope.launch { runCatching { repo.me() }.onSuccess { _state.value = _state.value.copy(vendor = it) } }
    }
}

private const val PIN_SETTLE_MS = 400L
