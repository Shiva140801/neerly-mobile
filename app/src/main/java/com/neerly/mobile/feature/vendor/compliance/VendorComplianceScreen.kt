package com.neerly.mobile.feature.vendor.compliance

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
import com.neerly.mobile.data.dto.ComplianceDocResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorComplianceScreen(
    onBack: () -> Unit,
    vm: VendorComplianceViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Compliance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
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
            state.docs.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No documents on file.", fontSize = 13.sp, color = NeerlyColors.Ink500)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(NeerlySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.docs, key = { it.id }) { DocRow(it) }
            }
        }
    }
}

@Composable
private fun DocRow(doc: ComplianceDocResponse) {
    val (bg, fg) = when (doc.status) {
        "APPROVED" -> NeerlyColors.OkSoft to NeerlyColors.Ok
        "REJECTED" -> NeerlyColors.ErrSoft to NeerlyColors.Err
        "EXPIRED"  -> NeerlyColors.ErrSoft to NeerlyColors.Err
        else       -> NeerlyColors.WarnSoft to NeerlyColors.Warn
    }
    Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(NeerlySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(doc.docType.replace("_", " "), fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Text(doc.docNumber ?: "—", fontSize = 12.sp, color = NeerlyColors.Ink500)
                doc.expiresDate?.let {
                    Text("Expires $it", fontSize = 11.sp, color = NeerlyColors.Ink500)
                }
                doc.rejectionReason?.let {
                    Text("Reason: $it", fontSize = 11.sp, color = NeerlyColors.Err)
                }
            }
            Surface(color = bg, shape = RoundedCornerShape(NeerlyRadius.pill)) {
                Text(doc.status,
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp, color = fg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
