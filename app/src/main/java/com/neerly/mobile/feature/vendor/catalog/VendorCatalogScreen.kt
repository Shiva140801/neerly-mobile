package com.neerly.mobile.feature.vendor.catalog

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
import com.neerly.mobile.data.dto.VendorProductRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCatalogScreen(
    onBack: () -> Unit,
    vm: VendorCatalogViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Catalog", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.products.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No products yet — add one from the web admin to start receiving orders.",
                    fontSize = 13.sp, color = NeerlyColors.Ink500)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.products, key = { it.id }) { Row(it, onTogglePause = { vm.togglePause(it) }) }
            }
        }
    }
}

@Composable
private fun Row(p: VendorProductRow, onTogglePause: () -> Unit) {
    Surface(
        color = NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text("${p.brand.orEmpty()} · ₹${p.price} · cap ${p.dailyCapacity}/day",
                    fontSize = 11.sp, color = NeerlyColors.Ink500)
                Text(p.status,
                    fontSize = 11.sp, color = if (p.status == "PAUSED") NeerlyColors.Warn else NeerlyColors.Ok,
                    fontWeight = FontWeight.SemiBold)
            }
            Switch(checked = p.status != "PAUSED", onCheckedChange = { onTogglePause() })
        }
    }
}
