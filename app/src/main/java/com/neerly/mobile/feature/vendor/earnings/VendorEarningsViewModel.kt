package com.neerly.mobile.feature.vendor.earnings

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.EarningsBucket
import com.neerly.mobile.data.dto.EarningsSummary
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.money.EarningLine
import com.neerly.mobile.feature.vendor.money.UnpaidSummary
import com.neerly.mobile.feature.vendor.money.VendorMoneyApi
import com.neerly.mobile.feature.vendor.money.VendorPayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

enum class EarningsPeriod(@androidx.annotation.StringRes val label: Int) { TODAY(R.string.vend_ep_today), WEEK(R.string.vend_ep_week), MONTH(R.string.vend_ep_month) }

/** Boards V16 (earnings), V39 (payouts), V31 (payout detail), V32 (payout on hold). */
@HiltViewModel
class VendorEarningsViewModel @Inject constructor(
    private val repo: VendorRepository,
    private val money: VendorMoneyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorEarningsUiState())
    val state: StateFlow<VendorEarningsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val overview = async { repo.earnings() }
                val unpaid = async { runCatching { money.unpaid() }.getOrNull() }
                val payouts = async { runCatching { money.payouts() }.getOrDefault(emptyList()) }
                val lines = async { runCatching { money.lines().content }.getOrDefault(emptyList()) }
                val bank = async { runCatching { repo.bank() }.getOrNull() }
                _state.value.copy(loading = false, summary = overview.await(), unpaid = unpaid.await(),
                    payouts = payouts.await(), lines = lines.await(), bank = bank.await(), error = null)
            }.onSuccess { _state.value = it }
             .onFailure { _state.value = _state.value.copy(loading = false,
                 error = it.userMessage("vendor earnings", strings.get(R.string.vend_err_load_earnings))) }
        }
    }

    fun select(period: EarningsPeriod) { _state.value = _state.value.copy(period = period) }

    /** Fresh copy of one payout (status can change from on-hold to paid while you look). */
    fun openPayout(id: String) {
        _state.value = _state.value.copy(openPayout = _state.value.payouts.firstOrNull { it.id == id }, payoutError = null)
        viewModelScope.launch {
            runCatching { money.payout(id) }
                .onSuccess { p -> _state.value = _state.value.copy(openPayout = p,
                    payouts = _state.value.payouts.map { if (it.id == p.id) p else it }) }
                .onFailure { _state.value = _state.value.copy(payoutError = it.userMessage("payout detail", strings.get(R.string.vend_err_load_payout))) }
        }
    }

    fun closePayout() { _state.value = _state.value.copy(openPayout = null, payoutError = null, invoiceHtml = null) }

    /** VPayoutDetail "Order list (CSV)": fetched here, saved and opened by the screen. */
    fun downloadCsv() {
        val p = _state.value.openPayout ?: return
        _state.value = _state.value.copy(fileBusy = true, payoutError = null)
        viewModelScope.launch {
            runCatching { money.payoutCsv(p.id).string() }
                .onSuccess { _state.value = _state.value.copy(fileBusy = false,
                    csv = PayoutCsv(payoutCsvName(p.periodStart, p.periodEnd), it)) }
                .onFailure { _state.value = _state.value.copy(fileBusy = false,
                    payoutError = it.userMessage("payout csv", strings.get(R.string.vend_payout_file_failed))) }
        }
    }

    fun csvHandled() { _state.value = _state.value.copy(csv = null) }

    /** VPayoutDetail "GST invoice": the server renders it; the screen shows it and can share it. */
    fun openInvoice() {
        val p = _state.value.openPayout ?: return
        _state.value = _state.value.copy(fileBusy = true, payoutError = null)
        viewModelScope.launch {
            runCatching { money.payoutFeeInvoice(p.id).string() }
                .onSuccess { _state.value = _state.value.copy(fileBusy = false, invoiceHtml = it) }
                .onFailure { _state.value = _state.value.copy(fileBusy = false,
                    payoutError = it.userMessage("payout invoice", strings.get(R.string.vend_payout_file_failed))) }
        }
    }

    fun closeInvoice() { _state.value = _state.value.copy(invoiceHtml = null) }
}

/** A downloaded payout order list, waiting for the screen to save and open it. */
data class PayoutCsv(val fileName: String, val text: String)

/** "boondh-payout-2026-11-02-2026-11-08.csv" — dates only, nothing personal in the name. */
fun payoutCsvName(start: String?, end: String?): String =
    "boondh-payout-${listOfNotNull(start, end).joinToString("-").filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "orders" }}.csv"

data class VendorEarningsUiState(
    val loading: Boolean = true,
    val summary: EarningsSummary? = null,
    val unpaid: UnpaidSummary? = null,
    val payouts: List<VendorPayout> = emptyList(),
    val lines: List<EarningLine> = emptyList(),
    val bank: VendorBankAccountResponse? = null,
    val period: EarningsPeriod = EarningsPeriod.TODAY,
    val openPayout: VendorPayout? = null,
    val payoutError: String? = null,
    /** Payout file download in flight (CSV or invoice). */
    val fileBusy: Boolean = false,
    val csv: PayoutCsv? = null,
    val invoiceHtml: String? = null,
    val error: String? = null
) {
    val bucket: EarningsBucket? get() = summary?.let {
        when (period) { EarningsPeriod.TODAY -> it.today; EarningsPeriod.WEEK -> it.week; EarningsPeriod.MONTH -> it.month }
    }
    /** Payouts the bank sent back or Boondh is holding — they lead the screen (V32). */
    val stuck: List<VendorPayout> get() = payouts.filter { it.stuck }
    /** Trip pay for orders cancelled after pickup, kept apart from water sales. */
    val tripPayLines: List<EarningLine> get() = lines.filter { it.isTripPay }
    val tripPayTotal: BigDecimal get() = tripPayLines.fold(BigDecimal.ZERO) { a, l -> a + l.netPayable }
}
