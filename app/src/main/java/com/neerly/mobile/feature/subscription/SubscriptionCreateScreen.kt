package com.neerly.mobile.feature.subscription

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import java.math.BigDecimal
import java.time.LocalDate

private val FREQUENCIES = listOf(
    Triple("DAILY", R.string.acct_new_freq_daily, R.string.acct_new_freq_daily_sub),
    Triple("ALT_DAY", R.string.acct_new_freq_alt, R.string.acct_new_freq_alt_sub),
    Triple("WEEKLY", R.string.acct_new_freq_weekly, R.string.acct_new_freq_weekly_sub),
    Triple("CUSTOM", R.string.acct_new_freq_custom, R.string.acct_new_freq_custom_sub)
)
/** Slot codes the server takes, labelled in the app's language ("6–8 AM"). */
private val SLOTS get() = listOf("6-8AM" to (6 to 8), "7-9AM" to (7 to 9), "8-10AM" to (8 to 10), "5-7PM" to (17 to 19), "7-9PM" to (19 to 21))
    .map { (code, h) -> code to slotWords(h.first, h.second) }
private fun slotWords(a: Int, b: Int): String {
    val end = java.time.LocalTime.of(b, 0).format(java.time.format.DateTimeFormatter.ofPattern("h a", com.neerly.mobile.core.util.AppLocale.locale))
    return "${if (a % 12 == 0) 12 else a % 12}–$end"
}
private val WEEKDAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

/**
 * Board 19. Shop → water → one plan page (how often, when, where, how to pay). Starts on the
 * first day the cut-off still allows, which is what the CTA promises.
 */
@Composable
fun SubscriptionCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    vm: SubscriptionCreateViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val back = { if (s.step == 0) onBack() else if (s.step >= 2) { while (vm.state.value.step > 1) vm.back() } else vm.back() }
    when {
        s.loading || (s.error != null && s.step == 0 && s.vendors.isEmpty()) ->
            BoondhPage(stringResource(R.string.acct_sub_new), onBack = onBack) { item { LoadState(s.loading, s.error, onBack) } }
        s.step == 0 -> BoondhPage(stringResource(R.string.acct_new_who), onBack = onBack, subtitle = stringResource(R.string.acct_new_who_sub, s.selectedAddress?.label ?: stringResource(R.string.acct_new_your_address))) {
            if (s.vendors.isEmpty()) item { LoadState(false, null, onBack, stringResource(R.string.acct_new_no_shops), stringResource(R.string.acct_new_no_shops_body)) }
            items(s.vendors, key = { it.id }) { v ->
                BoondhCard(onClick = { vm.pickVendor(v) }) {
                    InfoRow(v.businessName, listOfNotNull(v.avgRating?.let { "${it.setScale(1, java.math.RoundingMode.HALF_UP)} ★" },
                        pluralStringResource(R.plurals.acct_new_orders_count, v.totalOrders.toInt(), v.totalOrders.toInt()), v.fssaiNumber?.let { stringResource(R.string.acct_new_fssai) }).joinToString(" · "))
                }
            }
        }
        s.step == 1 -> BoondhPage(stringResource(R.string.acct_new_which), onBack = back, backLabel = stringResource(R.string.acct_new_shops), subtitle = s.selectedVendor?.businessName) {
            if (s.loadingProducts || (s.error != null && s.products.isEmpty())) { item { LoadState(s.loadingProducts, s.error, back) }; return@BoondhPage }
            if (s.products.isEmpty()) item { LoadState(false, null, back, stringResource(R.string.acct_new_nothing), stringResource(R.string.acct_new_nothing_body)) }
            items(s.products, key = { it.id }) { p ->
                BoondhCard(onClick = { vm.pickProduct(p) }) {
                    InfoRow(listOfNotNull(p.brand, p.name).joinToString(" "), p.description, p.price.rupees())
                }
            }
        }
        else -> PlanPage(s, vm, back, onCreated)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PlanPage(s: SubscriptionCreateUiState, vm: SubscriptionCreateViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val autopayOn = com.neerly.mobile.data.config.LocalAppConfig.current.upiAutopay
    val cfg = LocalAppConfig.current
    val start = cfg.firstChangeableDate()
    val each = s.perDelivery
    val startWord = if (start == LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).plusDays(1)) stringResource(R.string.acct_new_tomorrow) else shortDay(start.toString())
    BoondhPage(
        title = stringResource(R.string.acct_new_how_often), onBack = onBack, backLabel = stringResource(R.string.acct_new_water),
        subtitle = listOf("${s.quantity} × ${s.selectedProduct?.name ?: stringResource(R.string.acct_sub_jar)}", s.selectedVendor?.businessName ?: "").filter { it.isNotBlank() }.joinToString(" · "),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            each?.let { e -> Text(stringResource(R.string.acct_new_month_estimate, s.deliveriesPerMonth, (e * BigDecimal(s.deliveriesPerMonth)).rupees()),
                fontSize = 13.sp, color = NeerlyColors.Ink600) }
            PrimaryButton(when {
                s.submitting -> stringResource(R.string.acct_new_starting)
                each != null -> stringResource(R.string.acct_new_start_price, startWord, each.rupees())
                else -> stringResource(R.string.acct_new_start, startWord)
            }, { vm.submit(start, onCreated) }, enabled = s.canSubmit && !s.submitting)
        }
    ) {
        FREQUENCIES.forEach { (code, title, sub) -> item(key = code) {
            val on = s.frequency == code
            BoondhCard(Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.CustomerPrimary else Color.Transparent, RoundedCornerShape(14.dp)),
                onClick = { vm.setFrequency(code) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary))
                    Spacer(Modifier.width(8.dp))
                    Column { Text(stringResource(title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(stringResource(sub), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                }
            }
        } }
        if (s.frequency == "CUSTOM") item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WEEKDAYS.forEach { d ->
                    FilterChip(selected = d in s.daysOfWeek, onClick = { vm.toggleDay(d) }, label = { Text(dayName(d), fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        if (s.frequency == "CUSTOM" && s.daysOfWeek.isEmpty()) item { Notice(stringResource(R.string.acct_new_pick_day), Tone.Warn) }
        item { SectionLabel(stringResource(R.string.acct_sub_jars_each)) }
        item {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listOfNotNull(s.selectedProduct?.name, s.selectedProduct?.price?.let { stringResource(R.string.acct_sub_price_each, it.rupees()) }).joinToString(" · "),
                        Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    TextButton(onClick = { vm.setQuantity(s.quantity - 1) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text("−", fontSize = 22.sp, color = NeerlyColors.CustomerPrimary) }
                    Text("${s.quantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    TextButton(onClick = { vm.setQuantity(s.quantity + 1) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text("+", fontSize = 22.sp, color = NeerlyColors.CustomerPrimary) }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.acct_new_morning_evening)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SLOTS.forEach { (code, label) ->
                    FilterChip(selected = s.slot == code, onClick = { vm.setSlot(code) }, label = { Text(label, fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item { SectionLabel(stringResource(R.string.acct_new_deliver_to)) }
        if (s.addresses.isEmpty()) item { Notice(stringResource(R.string.acct_new_add_address), Tone.Warn) }
        s.addresses.forEach { a -> item(key = a.id) {
            val on = s.selectedAddress?.id == a.id
            BoondhCard(Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.CustomerPrimary else Color.Transparent, RoundedCornerShape(14.dp)),
                onClick = { vm.selectAddress(a) }) {
                InfoRow(a.label, "${a.flatNumber}, ${a.street} · ${a.pincode}")
            }
        } }
        item { SectionLabel(stringResource(R.string.acct_sub_row_pays)) }
        listOf("WALLET" to (R.string.acct_refund_to_wallet to R.string.acct_new_pay_wallet_sub),
               "UPI_AUTOPAY" to (R.string.acct_sub_pay_autopay to R.string.acct_new_pay_autopay_sub))
            .filter { it.first != "UPI_AUTOPAY" || autopayOn }.forEach { (code, t) ->
            item(key = code) {
                val on = s.paymentMode == code
                BoondhCard(Modifier.border(if (on) 2.dp else 0.dp, if (on) NeerlyColors.CustomerPrimary else Color.Transparent, RoundedCornerShape(14.dp)),
                    onClick = { vm.setPaymentMode(code) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary))
                        Spacer(Modifier.width(8.dp))
                        Column { Text(stringResource(t.first), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            Text(stringResource(t.second), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.acct_new_footer_note, cfg.clock(cfg.subscriptionCutoff)), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}
