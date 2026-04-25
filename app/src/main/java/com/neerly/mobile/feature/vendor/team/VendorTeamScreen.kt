package com.neerly.mobile.feature.vendor.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.VendorTeamMember

/**
 * Vendor team list + add-driver form. Tapping a row opens a confirm-remove
 * dialog. Drivers must already be registered Neerly users with the DRIVER role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorTeamScreen(
    onBack: () -> Unit,
    vm: VendorTeamViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    var pickedRemove by remember { mutableStateOf<VendorTeamMember?>(null) }
    var newPhone by remember { mutableStateOf("+91") }
    var newNotes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Drivers", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(NeerlySpacing.x4)) {

            // ---- Add driver ----
            Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Text("Invite a driver",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900)
                    Text("They must already have a Neerly Driver account on this number.",
                        fontSize = 11.sp, color = NeerlyColors.Ink500)
                    Spacer(Modifier.height(NeerlySpacing.x2))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it.take(13) },
                        placeholder = { Text("+919XXXXXXXXX", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it.take(500) },
                        placeholder = { Text("Notes (optional)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Button(
                        onClick = {
                            vm.add(newPhone.trim(), newNotes.trim().ifBlank { null })
                        },
                        enabled = !s.adding,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(NeerlyRadius.pill),
                        colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
                    ) { Text(if (s.adding) "Adding…" else "Add driver") }
                    if (s.error != null) {
                        Text(s.error!!, color = NeerlyColors.Err,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            Spacer(Modifier.height(NeerlySpacing.x4))

            Text("Active drivers (${s.members.size})", fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)

            if (s.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
            } else if (s.members.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No drivers on your team yet.", fontSize = 13.sp, color = NeerlyColors.Ink500)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)) {
                    items(s.members, key = { it.membershipId }) { m ->
                        Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(NeerlySpacing.x4),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.name ?: "Driver",
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = NeerlyColors.Ink900)
                                    Text(m.phoneMasked ?: "—",
                                        fontSize = 12.sp, color = NeerlyColors.Ink500)
                                    m.notes?.let {
                                        Text(it, fontSize = 11.sp, color = NeerlyColors.Ink400)
                                    }
                                }
                                TextButton(onClick = { pickedRemove = m }) {
                                    Text("Remove", color = NeerlyColors.Err)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pickedRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { pickedRemove = null },
            title = { Text("Remove ${m.name ?: "driver"}?") },
            text = { Text("They'll lose access to this vendor's orders. You can re-add them later.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.remove(m.driverId)
                    pickedRemove = null
                }) { Text("Remove", color = NeerlyColors.Err) }
            },
            dismissButton = {
                TextButton(onClick = { pickedRemove = null }) { Text("Cancel") }
            }
        )
    }
}
