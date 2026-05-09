package com.neerly.mobile.feature.subscription

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.SubscriptionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onNew: () -> Unit = {},
    vm: SubscriptionListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = NeerlyColors.CustomerPrimary,
                contentColor = NeerlyColors.Paper,
                text = { Text("New") },
                icon = { Text("+") }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.subscriptions.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No subscriptions yet",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Spacer(Modifier.height(6.dp))
                    Text("Subscribe to never run out of water — start from any vendor.",
                        fontSize = 13.sp, color = NeerlyColors.Ink500)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (state.active.isNotEmpty()) {
                    item { SectionHeader("Active") }
                    items(state.active, key = { it.id }) { Row(it, onOpen) }
                }
                if (state.paused.isNotEmpty()) {
                    item { SectionHeader("Paused") }
                    items(state.paused, key = { it.id }) { Row(it, onOpen) }
                }
                if (state.pending.isNotEmpty()) {
                    item { SectionHeader("Awaiting mandate") }
                    items(state.pending, key = { it.id }) { Row(it, onOpen) }
                }
                if (state.ended.isNotEmpty()) {
                    item { SectionHeader("Ended") }
                    items(state.ended, key = { it.id }) { Row(it, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 11.sp, color = NeerlyColors.Ink500,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun Row(s: SubscriptionResponse, onOpen: (String) -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpen(s.id) }
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.productName, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text("${s.vendorName} · ${s.frequency} · ${s.deliverySlot}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
                s.nextDeliveryDate?.let {
                    Text("Next: ${it.take(10)}", fontSize = 11.sp, color = NeerlyColors.CustomerDark)
                }
            }
            StatusPill(s.status)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        "ACTIVE"           -> NeerlyColors.OkSoft to NeerlyColors.Ok
        "PAUSED"           -> NeerlyColors.WarnSoft to NeerlyColors.Warn
        "PENDING_MANDATE"  -> NeerlyColors.CustomerSoft to NeerlyColors.CustomerDark
        else               -> NeerlyColors.Ink100 to NeerlyColors.Ink500
    }
    Surface(color = bg, shape = RoundedCornerShape(NeerlyRadius.pill)) {
        Text(status,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
