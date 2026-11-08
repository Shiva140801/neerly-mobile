package com.neerly.mobile.feature.event

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

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
import com.neerly.mobile.core.design.*
import com.neerly.mobile.data.config.LocalAppConfig
import com.neerly.mobile.data.dto.ProductResponse
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IST = ZoneId.of("Asia/Kolkata")
private val DAY get() = DateTimeFormatter.ofPattern("EEE d MMM", com.neerly.mobile.core.util.AppLocale.locale).withZone(IST)
private val TIME get() = DateTimeFormatter.ofPattern("h:mm a", com.neerly.mobile.core.util.AppLocale.locale).withZone(IST)

/** Rough guide from the board: about one 20L jar per 15 guests. A hint only — the customer sets quantities. */
private const val GUESTS_PER_JAR = 15

/**
 * Boards 48 (water for a function) and 73 (request sent). The server books one shop the
 * customer picks; the shop confirms in its app and the advance is asked for then.
 */
@Composable
fun EventBookingScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    vm: EventBookingViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val cfg = LocalAppConfig.current
    LaunchedEffect(cfg.eventMinAdvanceHours) { vm.applyMinAdvance(cfg.eventMinAdvanceHours) }
    val back = { if (s.step == 0) onBack() else vm.back() }
    val title = when (s.step) {
        0 -> stringResource(R.string.acct_evt_t0)
        1 -> stringResource(R.string.acct_evt_t1)
        2 -> stringResource(R.string.acct_evt_t2)
        3 -> stringResource(R.string.acct_evt_t3)
        else -> stringResource(R.string.acct_evt_t4)
    }
    val subtitle = when (s.step) {
        0 -> stringResource(R.string.acct_evt_s0)
        1 -> s.selectedVendor?.businessName
        2 -> (cfg.eventMinAdvanceHours / 24).let { pluralStringResource(R.plurals.acct_evt_s2, it, it) }
        else -> null
    }
    BoondhPage(
        title = title, onBack = back, subtitle = subtitle,
        footer = footer@{
            s.error?.let { Notice(it, Tone.Bad) }
            when (s.step) {
                1 -> PrimaryButton(if (s.itemQty.isEmpty()) stringResource(R.string.acct_evt_add_water) else s.itemQty.values.sum().let { pluralStringResource(R.plurals.acct_evt_continue_items, it, it) },
                    vm::goToDetails, enabled = s.itemQty.isNotEmpty())
                2 -> PrimaryButton(stringResource(R.string.acct_evt_continue), vm::goToAddress,
                    enabled = s.eventEnd.isAfter(s.eventStart) && (s.minStart == null || !s.eventStart.isBefore(s.minStart)))
                4 -> PrimaryButton(if (s.submitting) stringResource(R.string.acct_evt_sending) else stringResource(R.string.acct_evt_send_to, s.selectedVendor?.businessName ?: stringResource(R.string.acct_evt_the_shop)),
                    { vm.submit(onCreated) }, enabled = !s.submitting)
                else -> {}
            }
        }
    ) {
        item { StepBar(s.step, 5) }
        if (s.loading) { item { LoadState(true, null, {}) }; return@BoondhPage }
        when (s.step) {
            0 -> if (s.vendors.isEmpty()) item {
                LoadState(false, null, {}, stringResource(R.string.acct_evt_no_shops),
                    if (s.addresses.isEmpty()) stringResource(R.string.acct_evt_no_shops_addr)
                    else stringResource(R.string.acct_evt_no_shops_area))
            } else items(s.vendors.size, key = { s.vendors[it].id }) { i ->
                val v = s.vendors[i]
                BoondhCard(onClick = { vm.pickVendor(v) }) {
                    InfoRow(v.businessName, listOfNotNull(v.avgRating?.let { "${it.setScale(1, java.math.RoundingMode.HALF_UP)} ★" },
                        pluralStringResource(R.plurals.acct_new_orders_count, v.totalOrders.toInt(), v.totalOrders.toInt()), v.businessCity).joinToString(" · "))
                }
            }
            1 -> {
                item { GuestsCard(s.expectedGuests, vm::setExpectedGuests, s.products, onSuggest = { p, n -> vm.setQty(p.id, n) }) }
                if (s.loadingProducts) item { LoadState(true, null, {}) }
                else if (s.products.isEmpty()) item { LoadState(false, null, {}, stringResource(R.string.acct_evt_nothing), stringResource(R.string.acct_evt_nothing_body)) }
                else items(s.products.size, key = { s.products[it].id }) { i ->
                    val p = s.products[i]
                    BoondhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(listOfNotNull(p.brand, p.name).joinToString(" "), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                                Text(p.depositAmount?.let { stringResource(R.string.acct_evt_price_deposit, p.price.rupees(), it.rupees()) } ?: stringResource(R.string.acct_sub_price_each, p.price.rupees()),
                                    fontSize = 13.sp, color = NeerlyColors.Ink600)
                            }
                            Stepper(s.itemQty[p.id] ?: 0) { vm.setQty(p.id, it) }
                        }
                    }
                }
            }
            2 -> {
                item { BoondhCard {
                    SectionLabel(stringResource(R.string.acct_evt_arrive))
                    DateTimeRow(s.eventStart) { vm.setStart(it) }
                    SectionLabel(stringResource(R.string.acct_evt_collect_after))
                    DateTimeRow(s.eventEnd) { vm.setEnd(it) }
                    if (!s.eventEnd.isAfter(s.eventStart)) Notice(stringResource(R.string.acct_evt_end_after_start), Tone.Warn)
                } }
                item { SectionLabel(stringResource(R.string.acct_evt_chilled_water)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 4, 6, 12).forEach { h ->
                        FilterChip(selected = s.chillingLeadHours == h, onClick = { vm.setChillingHours(h) },
                            label = { Text(if (h == 0) stringResource(R.string.acct_evt_chill_none) else stringResource(R.string.acct_evt_chill_h, h)) }, modifier = Modifier.heightIn(min = 44.dp))
                    }
                } }
                item { Field(stringResource(R.string.acct_evt_occasion), s.eventName, vm::setEventName) }
                item { Field(stringResource(R.string.acct_evt_notes), s.customerNotes, vm::setNotes, singleLine = false) }
            }
            3 -> if (s.addresses.isEmpty()) item {
                LoadState(false, null, {}, stringResource(R.string.acct_evt_no_addr), stringResource(R.string.acct_evt_no_addr_body))
            } else items(s.addresses.size, key = { s.addresses[it].id }) { i ->
                val a = s.addresses[i]
                BoondhCard(onClick = { vm.pickAddress(a) }) {
                    InfoRow(a.label, "${a.flatNumber}, ${a.buildingName}, ${a.street} · ${a.pincode}",
                        if (s.selectedAddress?.id == a.id) "✓" else null)
                }
            }
            else -> {
                val chosen = s.products.filter { it.id in s.itemQty }
                val water = chosen.fold(BigDecimal.ZERO) { a, p -> a + p.price * BigDecimal(s.itemQty[p.id] ?: 0) }
                val deposit = chosen.fold(BigDecimal.ZERO) { a, p -> a + (p.depositAmount ?: BigDecimal.ZERO) * BigDecimal(s.itemQty[p.id] ?: 0) }
                item { BoondhCard {
                    Text(s.eventName.ifBlank { stringResource(R.string.acct_evt_event) }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    InfoRow(stringResource(R.string.acct_evt_shop), trailing = s.selectedVendor?.businessName ?: "—")
                    InfoRow(stringResource(R.string.acct_evt_arrive), trailing = "${DAY.format(s.eventStart)}, ${TIME.format(s.eventStart)}")
                    InfoRow(stringResource(R.string.acct_evt_collected), trailing = "${DAY.format(s.eventEnd)}, ${TIME.format(s.eventEnd)}")
                    s.expectedGuests?.let { InfoRow(stringResource(R.string.acct_evt_guests), trailing = "$it") }
                    if (s.chillingLeadHours > 0) InfoRow(stringResource(R.string.acct_evt_chilled), trailing = stringResource(R.string.acct_evt_h_ahead, s.chillingLeadHours))
                    s.selectedAddress?.let { InfoRow(stringResource(R.string.acct_evt_venue), "${it.flatNumber}, ${it.buildingName}, ${it.street}") }
                } }
                item { BoondhCard {
                    chosen.forEach { p -> InfoRow("${s.itemQty[p.id]} × ${p.name}", trailing = (p.price * BigDecimal(s.itemQty[p.id] ?: 0)).rupees()) }
                    InfoRow(stringResource(R.string.acct_evt_list_price), trailing = water.rupees())
                    if (deposit.signum() > 0) InfoRow(stringResource(R.string.acct_evt_deposit), trailing = deposit.rupees())
                } }
                item { Text(stringResource(R.string.acct_evt_pay_note, s.selectedVendor?.businessName ?: stringResource(R.string.acct_dep_the_shop)),
                    fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600) }
            }
        }
    }
}

@Composable
private fun StepBar(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i -> LinearProgressIndicator(progress = { if (i <= current) 1f else 0f }, modifier = Modifier.weight(1f).height(4.dp),
            color = NeerlyColors.CustomerPrimary, trackColor = NeerlyColors.Ink200) }
    }
}

@Composable
private fun GuestsCard(guests: Int?, onGuests: (String) -> Unit, products: List<ProductResponse>, onSuggest: (ProductResponse, Int) -> Unit) {
    BoondhCard {
        Field(stringResource(R.string.acct_evt_how_many), guests?.toString() ?: "", { onGuests(it.filter(Char::isDigit).take(5)) }, KeyboardType.Number)
        val jar = products.firstOrNull { it.template.sizeLabel?.contains("20") == true } ?: products.firstOrNull()
        if (guests != null && jar != null) {
            val n = (guests + GUESTS_PER_JAR - 1) / GUESTS_PER_JAR
            Text(stringResource(R.string.acct_evt_suggest, GUESTS_PER_JAR, n, jar.name), fontSize = 14.sp, color = NeerlyColors.Ink700)
            SecondaryButton(pluralStringResource(R.plurals.acct_evt_use_jars, n, n), { onSuggest(jar, n) })
        }
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChange(value - 1) }, enabled = value > 0, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 18.sp) }
        Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 36.dp).padding(horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = { onChange(value + 1) }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 18.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(value: Instant, onChange: (Instant) -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val local = value.atZone(IST)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(DAY.format(value)) }
        OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(TIME.format(value)) }
    }
    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = {
                st.selectedDateMillis?.let { ms ->
                    val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onChange(d.atTime(local.toLocalTime()).atZone(IST).toInstant())
                }
                pickDate = false
            }) { Text(stringResource(R.string.acct_ok)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.acct_evt_cancel)) } }) { DatePicker(state = st) }
    }
    if (pickTime) {
        val st = rememberTimePickerState(initialHour = local.hour, initialMinute = local.minute, is24Hour = false)
        AlertDialog(onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = {
                onChange(local.toLocalDate().atTime(LocalTime.of(st.hour, st.minute)).atZone(IST).toInstant()); pickTime = false
            }) { Text(stringResource(R.string.acct_ok)) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.acct_evt_cancel)) } },
            text = { TimePicker(state = st) })
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), singleLine = singleLine,
            minLines = if (singleLine) 1 else 3, keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
    }
}

