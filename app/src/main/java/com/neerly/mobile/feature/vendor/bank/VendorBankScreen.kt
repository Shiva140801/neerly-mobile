package com.neerly.mobile.feature.vendor.bank

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.AddVendorBankAccountRequest

/**
 * Vendor bank account screen — shows the active account (masked) plus an
 * "add new account" form. Adding an account deactivates the existing one
 * server-side; we re-fetch so the UI reflects the freshly-added row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorBankScreen(
    onBack: () -> Unit,
    vm: VendorBankViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Bank account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(NeerlySpacing.x4)
        ) {
            if (s.loading) {
                Text("Loading…", color = NeerlyColors.Ink500)
                return@Column
            }

            // ---- Current account ----
            s.current?.let { c ->
                Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(NeerlySpacing.x4)) {
                        Text("Active account", fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink500)
                        Text(c.accountHolderName, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(c.accountNumberMasked, fontSize = 14.sp, color = NeerlyColors.Ink800)
                        Text("${c.ifsc} · ${c.bankName ?: "Bank on file"}",
                            fontSize = 12.sp, color = NeerlyColors.Ink500)
                        Spacer(Modifier.height(8.dp))
                        if (c.verifiedAt != null) {
                            Text("✓ Verified (${c.verificationMethod ?: "manual"})",
                                fontSize = 12.sp, color = NeerlyColors.Ok)
                        } else {
                            Button(onClick = vm::verify,
                                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)) {
                                Text("Verify (penny-drop)")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(NeerlySpacing.x4))
            }

            // ---- Add / replace ----
            Text(
                if (s.current == null) "Add bank account" else "Replace with a new account",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900
            )
            Text("Adding a new account will deactivate the existing one.",
                fontSize = 11.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(NeerlySpacing.x3))

            var holder by remember { mutableStateOf("") }
            var account by remember { mutableStateOf("") }
            var ifsc by remember { mutableStateOf("") }
            var bankName by remember { mutableStateOf("") }
            var branch by remember { mutableStateOf("") }
            var type by remember { mutableStateOf("SAVINGS") }

            Field("Account holder name", holder) { holder = it.take(200) }
            Field("Account number", account, KeyboardType.Number) {
                account = it.take(18).filter(Char::isDigit)
            }
            Field("IFSC", ifsc) { ifsc = it.take(11).uppercase() }
            Field("Bank name (optional)", bankName) { bankName = it.take(200) }
            Field("Branch (optional)", branch) { branch = it.take(200) }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                listOf("SAVINGS", "CURRENT").forEach { t ->
                    val active = type == t
                    OutlinedButton(
                        onClick = { type = t },
                        colors = if (active)
                            ButtonDefaults.outlinedButtonColors(containerColor = NeerlyColors.VendorSofter)
                        else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(t) }
                }
            }

            if (s.error != null) {
                Text(s.error!!, color = NeerlyColors.Err, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    vm.add(AddVendorBankAccountRequest(
                        accountHolderName = holder.trim(),
                        accountNumber = account,
                        ifsc = ifsc.trim(),
                        bankName = bankName.trim().ifBlank { null },
                        branchName = branch.trim().ifBlank { null },
                        accountType = type
                    ))
                },
                enabled = !s.submitting,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
            ) {
                Text(if (s.submitting) "Saving…" else "Save account")
            }

            Spacer(Modifier.height(NeerlySpacing.x4))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(NeerlyRadius.md)
        )
    }
}
