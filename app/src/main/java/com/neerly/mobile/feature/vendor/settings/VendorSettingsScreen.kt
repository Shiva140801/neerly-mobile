package com.neerly.mobile.feature.vendor.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Vendor settings hub — links to compliance, subscriptions today, plus
 * Emergency Close action and Logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorSettingsScreen(
    onBack: () -> Unit,
    onCompliance: () -> Unit,
    onSubscriptionsToday: () -> Unit,
    onLogout: () -> Unit,
    vm: VendorSettingsViewModel = hiltViewModel()
) {
    var closeOpen by remember { mutableStateOf(false) }
    var closeReason by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("More", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Link("Today's subscriptions", onSubscriptionsToday)
            Link("Compliance", onCompliance)

            Spacer(Modifier.height(NeerlySpacing.x4))

            Button(
                onClick = { closeOpen = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.WarnSoft, contentColor = NeerlyColors.Warn)
            ) { Text("Emergency close", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.Ink700)
            ) { Text("Log out") }
        }
    }

    if (closeOpen) {
        AlertDialog(
            onDismissRequest = { closeOpen = false },
            title = { Text("Close shop for today?") },
            text = {
                Column {
                    Text("New orders will be blocked until you reopen. Existing orders are unaffected.",
                        fontSize = 13.sp, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = closeReason, onValueChange = { closeReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reason (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.emergencyClose(closeReason.takeIf { it.isNotBlank() })
                    closeOpen = false
                }) { Text("Close shop", color = NeerlyColors.Warn, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { closeOpen = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Link(label: String, onClick: () -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(NeerlySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(">", fontSize = 18.sp, color = NeerlyColors.Ink400)
        }
    }
}
