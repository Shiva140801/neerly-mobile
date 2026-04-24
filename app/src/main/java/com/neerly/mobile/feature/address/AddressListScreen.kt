package com.neerly.mobile.feature.address

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.AddressResponse

/**
 * Profile → Addresses list. Shows each saved address with Primary / Edit / Delete actions.
 * "+ Add new" kicks off AddressScreen in create mode via the passed onAddNew lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit,
    vm: AddressListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Saved addresses", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = NeerlyColors.CustomerPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNew,
                containerColor = NeerlyColors.CustomerPrimary,
                contentColor = NeerlyColors.Paper,
                text = { Text("Add new") },
                icon = { Text("+") }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x4)
        ) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
                state.error != null -> Text(
                    state.error ?: "", color = NeerlyColors.Err
                )
                state.addresses.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No addresses yet",
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink700
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add a delivery address to start ordering.",
                            fontSize = 13.sp, color = NeerlyColors.Ink500
                        )
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.addresses, key = { it.id }) { addr ->
                        AddressRow(
                            addr,
                            onEdit = { onEdit(addr.id) },
                            onSetPrimary = { vm.setPrimary(addr.id) },
                            onDelete = { vm.delete(addr.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressRow(
    addr: AddressResponse,
    onEdit: () -> Unit,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    addr.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeerlyColors.Ink900
                )
                if (addr.isPrimary) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = NeerlyColors.CustomerSofter,
                        shape = RoundedCornerShape(NeerlyRadius.pill)
                    ) {
                        Text(
                            "PRIMARY",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.CustomerDark
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${addr.flatNo}, ${addr.streetArea}",
                fontSize = 14.sp, color = NeerlyColors.Ink800
            )
            Text(
                "${addr.city} · ${addr.pincode}",
                fontSize = 12.sp, color = NeerlyColors.Ink500
            )
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit", color = NeerlyColors.CustomerPrimary)
                }
                if (!addr.isPrimary) {
                    TextButton(onClick = onSetPrimary) {
                        Text("Set primary", color = NeerlyColors.CustomerPrimary)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text("Delete", color = NeerlyColors.Err)
                }
            }
        }
    }
}
