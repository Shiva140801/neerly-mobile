package com.neerly.mobile.feature.vendor.subscriptions

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
import com.neerly.mobile.data.dto.VendorSubscriptionTodayRow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorSubscriptionsTodayViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VendorSubsTodayUiState())
    val state: StateFlow<VendorSubsTodayUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.subscriptionsToday() }
                .onSuccess { _state.value = VendorSubsTodayUiState(loading = false, rows = it) }
                .onFailure { _state.value = VendorSubsTodayUiState(loading = false, error = it.message) }
        }
    }
}

data class VendorSubsTodayUiState(
    val loading: Boolean = true,
    val rows: List<VendorSubscriptionTodayRow> = emptyList(),
    val error: String? = null
) {
    val grouped: Map<String, List<VendorSubscriptionTodayRow>>
        get() = rows.groupBy { it.slot }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorSubscriptionsTodayScreen(
    onBack: () -> Unit,
    vm: VendorSubscriptionsTodayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Today's subs", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
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
            state.rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Nothing scheduled today.",
                    fontSize = 13.sp, color = NeerlyColors.Ink500)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                state.grouped.forEach { (slot, list) ->
                    item {
                        Text("$slot · ${list.size} deliveries",
                            fontSize = 12.sp, color = NeerlyColors.Ink500,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(list, key = { it.subscriptionId }) { Row(it) }
                }
            }
        }
    }
}

@Composable
private fun Row(row: VendorSubscriptionTodayRow) {
    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(Modifier.padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${row.quantity} × ${row.productName}",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(row.customerName, fontSize = 12.sp, color = NeerlyColors.Ink700)
                Text(row.deliveryAddress, fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
            Surface(
                color = when (row.status) {
                    "DELIVERED" -> NeerlyColors.OkSoft
                    "DISPATCHED" -> NeerlyColors.VendorSoft
                    "SKIPPED" -> NeerlyColors.Ink100
                    else -> NeerlyColors.WarnSoft
                },
                shape = RoundedCornerShape(NeerlyRadius.pill)
            ) {
                Text(row.status,
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink700)
            }
        }
    }
}
