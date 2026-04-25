package com.neerly.mobile.feature.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.design.Role
import com.neerly.mobile.core.design.NeerlyTheme

/**
 * Tier-1 vendor onboarding wizard. 6 steps bundled into one composable with step state.
 * On final step, submits POST /api/v1/vendor/submit.
 *
 * State is local for Session 2 visual scaffold; ViewModel wiring is next slice.
 */
enum class OnboardingStep { PATH, BASICS, ADDRESS, SERVICE_AREA, FSSAI, REVIEW }

data class OnboardingState(
    val isTier2: Boolean = false,
    val businessName: String = "",
    val proprietor: String = "",
    val businessType: String = "PROPRIETOR",
    val pincode: String = "",
    val pincodes: Set<String> = emptySet(),
    val fssaiNumber: String = "",
    val fssaiExpiry: String = ""
)

@Composable
fun VendorOnboardingWizard(
    onSubmitted: () -> Unit,
    vm: VendorOnboardingViewModel = hiltViewModel()
) {
    NeerlyTheme(role = Role.VENDOR) {
        var step by remember { mutableStateOf(OnboardingStep.PATH) }
        var state by remember { mutableStateOf(OnboardingState()) }
        val submit by vm.state.collectAsState()

        Scaffold(
            containerColor = NeerlyColors.Paper,
            topBar = {
                Surface(color = NeerlyColors.Paper, shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Step ${step.ordinal + 1} of 6",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink500
                        )
                        Text(step.label(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (step.ordinal + 1) / 6f },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeerlyColors.VendorPrimary,
                            trackColor = NeerlyColors.Ink100
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (step) {
                    OnboardingStep.PATH ->
                        PathStep(state) { isTier2 ->
                            state = state.copy(isTier2 = isTier2)
                            step = OnboardingStep.BASICS
                        }

                    OnboardingStep.BASICS ->
                        BasicsStep(state, onNext = { newState ->
                            state = newState
                            step = OnboardingStep.ADDRESS
                        })

                    OnboardingStep.ADDRESS ->
                        AddressStep(state, onNext = { newState ->
                            state = newState
                            step = OnboardingStep.SERVICE_AREA
                        })

                    OnboardingStep.SERVICE_AREA ->
                        ServiceAreaStep(state, onNext = { newState ->
                            state = newState
                            step = if (state.isTier2) OnboardingStep.REVIEW else OnboardingStep.FSSAI
                        })

                    OnboardingStep.FSSAI ->
                        FssaiStep(state, onNext = { newState ->
                            state = newState
                            step = OnboardingStep.REVIEW
                        })

                    OnboardingStep.REVIEW ->
                        ReviewStep(
                            state = state,
                            submitting = submit.submitting,
                            error = submit.error,
                            onSubmit = { vm.submit(state, onSubmitted) }
                        )
                }
            }
        }
    }
}

private fun OnboardingStep.label(): String = when (this) {
    OnboardingStep.PATH -> "Onboarding path"
    OnboardingStep.BASICS -> "Business basics"
    OnboardingStep.ADDRESS -> "Business address"
    OnboardingStep.SERVICE_AREA -> "Service area"
    OnboardingStep.FSSAI -> "FSSAI license"
    OnboardingStep.REVIEW -> "Review & submit"
}

@Composable
private fun PathStep(state: OnboardingState, onPicked: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Do you have an FSSAI license?", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("We onboard licensed vendors in 48 hours. Unlicensed? We'll help you get one (3–4 weeks).",
            fontSize = 14.sp, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(NeerlySpacing.x6))

        PathOption("Tier-1 · I have FSSAI", "48-hour onboarding · 10% commission",
            onClick = { onPicked(false) })
        Spacer(Modifier.height(12.dp))
        PathOption("Tier-2 · Help me get FSSAI", "3–4 weeks · 12% commission for first 3 months",
            onClick = { onPicked(true) })
    }
}

@Composable
private fun PathOption(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NeerlyRadius.md),
        color = NeerlyColors.Paper,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(subtitle, fontSize = 12.sp, color = NeerlyColors.Ink500)
        }
    }
}

@Composable
private fun BasicsStep(state: OnboardingState, onNext: (OnboardingState) -> Unit) {
    var name by remember { mutableStateOf(state.businessName) }
    var proprietor by remember { mutableStateOf(state.proprietor) }
    val valid = name.length >= 3 && proprietor.length >= 2

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        WizardField("Business name (as on license)", name) { name = it.take(200) }
        WizardField("Proprietor name", proprietor) { proprietor = it.take(100) }

        Spacer(Modifier.height(NeerlySpacing.x6))
        NextButton(enabled = valid, onClick = {
            onNext(state.copy(businessName = name, proprietor = proprietor))
        })
    }
}

@Composable
private fun AddressStep(state: OnboardingState, onNext: (OnboardingState) -> Unit) {
    var pin by remember { mutableStateOf(state.pincode) }
    val valid = pin.matches(Regex("^[1-9]\\d{5}\$"))

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(NeerlyRadius.md))
                .background(NeerlyColors.Ink100),
            contentAlignment = Alignment.Center
        ) { Text("📍 Business location pin", fontSize = 12.sp, color = NeerlyColors.Ink500) }

        Spacer(Modifier.height(NeerlySpacing.x4))
        WizardField("Pincode", pin, keyboardType = KeyboardType.Number) { pin = it.take(6).filter(Char::isDigit) }

        Spacer(Modifier.height(NeerlySpacing.x6))
        NextButton(enabled = valid, onClick = { onNext(state.copy(pincode = pin)) })
    }
}

@Composable
private fun ServiceAreaStep(state: OnboardingState, onNext: (OnboardingState) -> Unit) {
    var csv by remember { mutableStateOf(state.pincodes.joinToString(",")) }
    val pins = csv.split(",").map { it.trim() }.filter { it.matches(Regex("^[1-9]\\d{5}\$")) }.toSet()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Which pincodes do you deliver to?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        WizardField("Pincodes (comma-separated)", csv) { csv = it }
        if (pins.isNotEmpty()) {
            Spacer(Modifier.height(NeerlySpacing.x2))
            Text("${pins.size} valid pincode(s)", fontSize = 12.sp, color = NeerlyColors.Ok)
        }

        Spacer(Modifier.height(NeerlySpacing.x6))
        NextButton(enabled = pins.isNotEmpty(), onClick = { onNext(state.copy(pincodes = pins)) })
    }
}

@Composable
private fun FssaiStep(state: OnboardingState, onNext: (OnboardingState) -> Unit) {
    var num by remember { mutableStateOf(state.fssaiNumber) }
    var exp by remember { mutableStateOf(state.fssaiExpiry) }
    val valid = num.matches(Regex("^\\d{14}\$"))

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        WizardField("FSSAI number (14 digits)", num, keyboardType = KeyboardType.Number) {
            num = it.take(14).filter(Char::isDigit)
        }
        WizardField("Expiry (YYYY-MM-DD)", exp) { exp = it.take(10) }

        Spacer(Modifier.height(NeerlySpacing.x3))
        OutlinedButton(
            onClick = { /* TODO presigned S3 upload in next slice */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NeerlyRadius.md)
        ) { Text("📷  Upload FSSAI photo") }

        Spacer(Modifier.height(NeerlySpacing.x6))
        NextButton(enabled = valid, onClick = {
            onNext(state.copy(fssaiNumber = num, fssaiExpiry = exp))
        })
    }
}

@Composable
private fun ReviewStep(
    state: OnboardingState,
    submitting: Boolean,
    error: String?,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Review your application", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x4))
        ReviewRow("Path", if (state.isTier2) "Tier-2 (license pending)" else "Tier-1 (licensed)")
        ReviewRow("Business", state.businessName)
        ReviewRow("Proprietor", state.proprietor)
        ReviewRow("Business pincode", state.pincode)
        ReviewRow("Serves pincodes", state.pincodes.joinToString(", "))
        if (!state.isTier2) {
            ReviewRow("FSSAI", state.fssaiNumber)
            ReviewRow("FSSAI expiry", state.fssaiExpiry)
        }

        Spacer(Modifier.height(NeerlySpacing.x6))
        Text("Commission: ${if (state.isTier2) "12% (first 3 months)" else "10% standard"}",
            fontSize = 13.sp, color = NeerlyColors.Ink600)

        if (error != null) {
            Spacer(Modifier.height(NeerlySpacing.x3))
            Text(error, fontSize = 13.sp, color = NeerlyColors.Err)
        }

        Spacer(Modifier.height(NeerlySpacing.x6))

        Button(
            onClick = onSubmit,
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
        ) {
            Text(
                if (submitting) "Submitting…" else "Submit for verification",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WizardField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text,
                        onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeerlyColors.VendorPrimary,
                unfocusedBorderColor = NeerlyColors.Ink200
            )
        )
    }
}

@Composable
private fun NextButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(NeerlyRadius.pill),
        colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
    ) { Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = NeerlyColors.Ink500)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900,
            modifier = Modifier.weight(2f))
    }
}
