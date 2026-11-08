package com.neerly.mobile.feature.vendor

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import com.neerly.mobile.feature.vendor.compliance.DocEditor
import com.neerly.mobile.feature.vendor.compliance.VendorComplianceViewModel
import com.neerly.mobile.feature.vendor.compliance.docsChecklist
import com.neerly.mobile.feature.vendor.onboarding.SetupStep
import com.neerly.mobile.feature.vendor.onboarding.SundayMode
import com.neerly.mobile.feature.vendor.onboarding.VendorOnboardingViewModel
import com.neerly.mobile.feature.vendor.onboarding.VendorSetupUiState

/**
 * Seller onboarding, boards V01–V07 (+ V47 pin). Entry point kept for the nav graph.
 * [onOpenBank] opens the bank screen (V04 lives there); [onSignInAsSeller] is for an account
 * that is signed in as a customer — the server only lets seller accounts submit.
 */
@Composable
fun VendorOnboardingWizard(
    onSubmitted: () -> Unit,
    onExit: () -> Unit,
    onOpenBank: () -> Unit = {},
    onSignInAsSeller: () -> Unit = {},
    onAddDrivers: () -> Unit = {},
    vm: VendorOnboardingViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    BackHandler(enabled = !s.submitting) { if (!vm.back()) onExit() }
    if (s.checking) { Box(Modifier.fillMaxSize().background(NeerlyColors.Canvas), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    when (s.step) {
        SetupStep.WELCOME -> WelcomeStep(onStart = { vm.go(SetupStep.SHOP) }, onExit = onExit)
        SetupStep.SHOP -> ShopStep(s, vm)
        SetupStep.AREA -> AreaStep(s, vm, onSignInAsSeller)
        SetupStep.DOCS -> DocsStep(s, vm)
        SetupStep.BANK -> BankStep(vm, onOpenBank)
        SetupStep.PRODUCTS -> ProductsStep(s, vm)
        SetupStep.REVIEW -> ReviewStep(s, vm, onDone = onSubmitted, onAddDrivers = onAddDrivers)
    }
}

private fun stepOf(s: SetupStep) = when (s) {
    SetupStep.SHOP -> "1/5"; SetupStep.AREA -> "2/5"; SetupStep.DOCS -> "3/5"; SetupStep.BANK -> "4/5"; SetupStep.PRODUCTS -> "5/5"; else -> ""
}

// ---------------------------------------------------------------- V01

@Composable
private fun WelcomeStep(onStart: () -> Unit, onExit: () -> Unit) {
    BoondhPage(
        title = stringResource(R.string.vend_wz_title),
        onBack = onExit,
        subtitle = stringResource(R.string.vend_wz_sub),
        footer = { PrimaryButton(stringResource(R.string.vend_start_reg), onStart) }
    ) {
        item { SectionLabel(stringResource(R.string.vend_keep_handy)) }
        item {
            BoondhCard {
                listOf(R.string.vend_need_fssai, R.string.vend_need_bis, R.string.vend_need_pan,
                    R.string.vend_need_water, R.string.vend_need_bank).forEachIndexed { i, t ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(min = 44.dp)) {
                        Box(Modifier.size(28.dp).background(NeerlyColors.CustomerSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary) }
                        Text(stringResource(t), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    }
                }
            }
        }
        item { Text(stringResource(R.string.vend_no_fssai_yet), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}

// ---------------------------------------------------------------- V02 + V47

@Composable
private fun ShopStep(s: VendorSetupUiState, vm: VendorOnboardingViewModel) {
    val f = s.form
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { g ->
        if (g.values.any { it }) vm.useMyLocation() else vm.locationDenied()
    }
    BoondhPage(
        title = stringResource(R.string.vend_wz_shop_title), onBack = { vm.back() },
        subtitle = stepOf(SetupStep.SHOP),
        footer = { s.error?.let { Notice(it, Tone.Bad) }; PrimaryButton(stringResource(R.string.vend_next_area), vm::nextFromShop) }
    ) {
        item { Field(stringResource(R.string.vend_shop_name_field), f.name) { v -> vm.edit { copy(name = v.take(200)) } } }
        item { Field(stringResource(R.string.vend_owner_name_field), f.owner, caps = KeyboardCapitalization.Words) { v -> vm.edit { copy(owner = v.take(100)) } } }
        item { SectionLabel(stringResource(R.string.vend_where_shop)) }
        item {
            BoondhCard {
                if (f.lat != null) {
                    Pill(stringResource(R.string.vend_pin_set), Tone.Ok)
                    Text(f.foundAt ?: "%.5f, %.5f".format(f.lat, f.lng), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.vend_pin_note), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    SecondaryButton(stringResource(if (s.locating) R.string.vend_finding_you else R.string.vend_use_loc_again), { ask.launch(LOCATION) })
                } else {
                    Text(stringResource(R.string.vend_put_pin), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    Text(stringResource(R.string.vend_put_pin_sub), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    PrimaryButton(stringResource(if (s.locating) R.string.vend_finding_you else R.string.vend_use_loc), { ask.launch(LOCATION) }, enabled = !s.locating)
                }
                s.locationError?.let { Notice(it, Tone.Warn) }
            }
        }
        item { Field(stringResource(R.string.vend_shop_building), f.flat) { v -> vm.edit { copy(flat = v.take(50)) } } }
        item { Field(stringResource(R.string.vend_street_area), f.street) { v -> vm.edit { copy(street = v.take(200)) } } }
        item { Field(stringResource(R.string.vend_landmark_opt), f.landmark) { v -> vm.edit { copy(landmark = v.take(200)) } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Field(stringResource(R.string.vend_city), f.city) { v -> vm.edit { copy(city = v.take(100)) } } }
                Box(Modifier.weight(1f)) { Field(stringResource(R.string.vend_pincode), f.pincode, KeyboardType.Number) { v -> vm.edit { copy(pincode = v.filter(Char::isDigit).take(6)) } } }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_fssai_licence)) }
        item { Field(stringResource(R.string.vend_licence_14), f.fssaiNumber, KeyboardType.Number) { v -> vm.edit { copy(fssaiNumber = v.filter(Char::isDigit).take(14)) } } }
        if (f.fssaiNumber.isNotBlank()) item {
            Field(stringResource(R.string.vend_valid_till_fmt), f.fssaiExpiry) { v -> vm.edit { copy(fssaiExpiry = v.take(10)) } }
        } else item {
            Text(stringResource(R.string.vend_fssai_empty_note), fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
    }
}

private val LOCATION = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

// ---------------------------------------------------------------- V06

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaStep(s: VendorSetupUiState, vm: VendorOnboardingViewModel, onSignInAsSeller: () -> Unit) {
    val f = s.form
    var newPin by remember { mutableStateOf("") }
    BoondhPage(
        title = stringResource(R.string.vend_how_far_when), onBack = { vm.back() }, subtitle = stringResource(R.string.vend_step_then_send, stepOf(SetupStep.AREA)),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (s.needsSellerSignIn) SecondaryButton(stringResource(R.string.vend_sign_in_seller), onSignInAsSeller)
            PrimaryButton(stringResource(if (s.submitting) R.string.vend_sending else R.string.vend_submit_review), vm::submit, enabled = !s.submitting)
        }
    ) {
        item { SectionLabel(stringResource(R.string.vend_delivery_distance)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 5, 8).forEach { km ->
                    FilterChip(selected = f.radiusKm == km, onClick = { vm.edit { copy(radiusKm = km) } },
                        label = { Text(stringResource(R.string.vend_km_n, km), fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_pincodes_deliver)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                f.pincodes.sorted().forEach { p ->
                    InputChip(selected = true, onClick = { if (p != f.pincode) vm.edit { copy(extraPincodes = extraPincodes - p) } },
                        label = { Text(stringResource(if (p == f.pincode) R.string.vend_pin_shop else R.string.vend_pin_remove, p)) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field(stringResource(R.string.vend_add_pincode), newPin, KeyboardType.Number) { newPin = it.filter(Char::isDigit).take(6) } }
                TextButton(onClick = { if (newPin.length == 6) { vm.edit { copy(extraPincodes = extraPincodes + newPin) }; newPin = "" } },
                    enabled = newPin.length == 6, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.vend_add)) }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_open_on)) }
        item {
            BoondhCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { Field(stringResource(R.string.vend_monsat_opens), f.opens) { v -> vm.edit { copy(opens = v.take(5)) } } }
                    Box(Modifier.weight(1f)) { Field(stringResource(R.string.vend_monsat_closes), f.closes) { v -> vm.edit { copy(closes = v.take(5)) } } }
                }
                Text(stringResource(R.string.vend_sunday), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SundayMode.values().forEach { m ->
                        FilterChip(selected = f.sunday == m, onClick = { vm.edit { copy(sunday = m) } }, label = { Text(stringResource(m.label)) },
                            modifier = Modifier.heightIn(min = 44.dp))
                    }
                }
                if (f.sunday == SundayMode.HALF) Text(stringResource(R.string.vend_half_day_1pm), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
    }
}

// ---------------------------------------------------------------- V03

@Composable
private fun DocsStep(s: VendorSetupUiState, vm: VendorOnboardingViewModel) {
    val docs: VendorComplianceViewModel = hiltViewModel()
    val d by docs.state.collectAsState()
    d.draft?.let { DocEditor(it, d, docs); return }
    BoondhPage(
        title = stringResource(R.string.vend_licences_papers), subtitle = stringResource(R.string.vend_step_docs_sub, stepOf(SetupStep.DOCS)),
        footer = {
            PrimaryButton(stringResource(R.string.vend_next_bank), { vm.go(SetupStep.BANK) })
            if (d.missing.isNotEmpty()) Text(pluralStringResource(R.plurals.vend_still_to_add, d.missing.size, d.missing.size),
                fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
    ) {
        s.notice?.let { item { Notice(it, Tone.Warn) } }
        if (s.vendor != null && s.notice == null) item { Notice(stringResource(R.string.vend_app_sent_for, s.vendor.businessName), Tone.Ok) }
        d.sent?.let { item { Notice(it, Tone.Ok) } }
        if (d.loading && d.docs.isEmpty()) item { LoadState(true, null, docs::load) } else docsChecklist(d, docs::edit)
    }
}

// ---------------------------------------------------------------- V04

@Composable
private fun BankStep(vm: VendorOnboardingViewModel, onOpenBank: () -> Unit) {
    BoondhPage(
        title = stringResource(R.string.vend_bank_first_title), onBack = { vm.back() },
        subtitle = stringResource(R.string.vend_step_bank_sub, stepOf(SetupStep.BANK)),
        footer = {
            PrimaryButton(stringResource(R.string.vend_add_bank), onOpenBank)
            SecondaryButton(stringResource(R.string.vend_next_sell), { vm.go(SetupStep.PRODUCTS) })
        }
    ) {
        item { Text(stringResource(R.string.vend_bank_check_note),
            fontSize = 14.sp, lineHeight = 21.sp, color = NeerlyColors.Ink600) }
    }
}

// ---------------------------------------------------------------- V05

@Composable
private fun ProductsStep(s: VendorSetupUiState, vm: VendorOnboardingViewModel) {
    val count = s.picks.count { it.value.on }
    BoondhPage(
        title = stringResource(R.string.vend_what_do_you_sell), onBack = { vm.back() },
        subtitle = stringResource(R.string.vend_step_products_sub, stepOf(SetupStep.PRODUCTS)),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(when { s.saving -> stringResource(R.string.vend_adding); count == 0 -> stringResource(R.string.vend_skip_now)
                else -> pluralStringResource(R.plurals.vend_next_add_products, count, count) },
                vm::saveProducts, enabled = !s.saving)
        }
    ) {
        if (s.existingProducts > 0) item { Notice(pluralStringResource(R.plurals.vend_already_products, s.existingProducts, s.existingProducts), Tone.Info) }
        if (s.templates.isEmpty()) item { LoadState(s.error == null, null, vm::loadTemplates) }
        s.templates.forEach { t -> item(key = t.categoryCode) {
            val p = s.picks[t.categoryCode]
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = p?.on == true, onCheckedChange = { on -> vm.pick(t.categoryCode) { copy(on = on) } })
                    Column(Modifier.weight(1f)) {
                        Text(t.displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(stringResource(if (t.allowsDeposit) R.string.vend_jar_deposit_swap else R.string.vend_no_jar_return), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                    if (p?.on == true) OutlinedTextField(value = p.price, onValueChange = { v -> vm.pick(t.categoryCode) { copy(price = v.filter { c -> c.isDigit() || c == '.' }.take(7)) } },
                        modifier = Modifier.width(110.dp), singleLine = true, prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        } }
        if (s.templates.any { it.allowsDeposit }) {
            item { SectionLabel(stringResource(R.string.vend_jar_rules)) }
            item {
                BoondhCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.vend_accept_swaps), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            Text(stringResource(R.string.vend_accept_swaps_sub), fontSize = 13.sp, color = NeerlyColors.Ink600)
                        }
                        Switch(checked = s.acceptSwaps, onCheckedChange = vm::setSwaps)
                    }
                    OutlinedTextField(value = s.deposit, onValueChange = vm::setDeposit, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(stringResource(R.string.vend_deposit_per_jar)) }, prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- V07

@Composable
private fun ReviewStep(s: VendorSetupUiState, vm: VendorOnboardingViewModel, onDone: () -> Unit, onAddDrivers: () -> Unit) {
    val docs: VendorComplianceViewModel = hiltViewModel()
    val d by docs.state.collectAsState()
    OnReturn { vm.refreshVendor(); docs.load() }
    val v = s.vendor
    val live = v?.status == "ACTIVE"
    val owner = v?.proprietorName?.substringBefore(' ') ?: s.form.owner.substringBefore(' ')
    BoondhPage(
        title = stringResource(if (live) R.string.vend_youre_live else R.string.vend_thanks_checking, owner),
        subtitle = stringResource(if (live) R.string.vend_homes_see_now else R.string.vend_live_in_2_days),
        footer = { PrimaryButton(stringResource(if (live) R.string.vend_open_dashboard else R.string.vend_done), onDone); SecondaryButton(stringResource(R.string.vend_add_staff), onAddDrivers) }
    ) {
        item { Pill(stringResource(if (live) R.string.vend_live_caps else R.string.vend_under_review_caps), if (live) Tone.Ok else Tone.Warn) }
        item {
            BoondhCard {
                Step(stringResource(R.string.vend_app_sent_step), v?.joinedAt?.let { shortDay(it) } ?: stringResource(R.string.vend_done), done = true)
                val papersDone = d.missing.isEmpty() && d.toFix.isEmpty() && d.latest.values.all { it.status == "APPROVED" }
                Step(stringResource(R.string.vend_checking_papers),
                    when {
                        d.toFix.isNotEmpty() -> stringResource(R.string.vend_n_to_fix, d.toFix.size)
                        d.missing.isNotEmpty() -> stringResource(R.string.vend_n_still_send, d.missing.size)
                        papersDone -> stringResource(R.string.vend_all_approved)
                        else -> stringResource(R.string.vend_fssai_bis_offices)
                    }, done = papersDone, warn = d.toFix.isNotEmpty())
                Step(stringResource(R.string.vend_short_visit), stringResource(R.string.vend_short_visit_sub), done = live)
                Step(stringResource(R.string.vend_youre_live_step), stringResource(R.string.vend_homes_start_seeing), done = live)
            }
        }
        if (d.toFix.isNotEmpty() || d.missing.isNotEmpty()) item {
            SecondaryButton(stringResource(R.string.vend_finish_papers), { vm.go(SetupStep.DOCS) })
        }
    }
}

@Composable
private fun Step(title: String, sub: String, done: Boolean, warn: Boolean = false) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.size(22.dp).background(when { done -> NeerlyColors.Ok; warn -> NeerlyColors.Warn; else -> NeerlyColors.Ink200 }, CircleShape),
            contentAlignment = Alignment.Center) { if (done) Text("✓", color = NeerlyColors.Paper, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(sub, fontSize = 13.sp, color = if (warn) NeerlyColors.Warn else NeerlyColors.Ink600)
        }
    }
}

@Composable
private fun Field(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text,
                  caps: KeyboardCapitalization = KeyboardCapitalization.Sentences, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps))
}
