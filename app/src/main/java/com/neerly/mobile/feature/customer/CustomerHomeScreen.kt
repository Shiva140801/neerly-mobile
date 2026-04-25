package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Customer home — browse vendors in area. Wired to GET /api/v1/customer/vendors?pincode=...
 * ViewModel + actual data layer arrive in the next slice; this renders the mock layout.
 */
data class VendorCard(
    val id: String,
    val name: String,
    val rating: Double,
    val distance: String,
    val categories: String
)

private val SAMPLE = listOf(
    VendorCard("1", "Sri Ganesh Water Supply", 4.7, "0.8 km", "Jars · Bottles"),
    VendorCard("2", "Pure Drops Madhapur", 4.5, "1.2 km", "Jars · Tanker"),
    VendorCard("3", "Aqua Life Suppliers", 4.3, "1.8 km", "Jars only"),
    VendorCard("4", "Balaji Tankers", 4.6, "2.4 km", "Tankers")
)

@Composable
fun CustomerHomeScreen(
    onVendorClick: (String) -> Unit
) {
    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            Surface(color = NeerlyColors.Paper, shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hi, Priya", fontSize = 12.sp, color = NeerlyColors.Ink500, fontWeight = FontWeight.SemiBold)
                    Text("Home · Gachibowli", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x4)
        ) {
            Text("Vendors near you", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Spacer(Modifier.height(NeerlySpacing.x3))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SAMPLE, key = { it.id }) { vendor ->
                    VendorRow(vendor, onClick = { onVendorClick(vendor.id) })
                }
            }
        }
    }
}

@Composable
private fun VendorRow(vendor: VendorCard, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NeerlyRadius.md),
        color = NeerlyColors.Paper,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeerlyColors.CustomerSofter),
                contentAlignment = Alignment.Center
            ) { Text("💧", fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vendor.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                Spacer(Modifier.height(2.dp))
                Text("${vendor.rating} ★  ·  ${vendor.distance}  ·  ${vendor.categories}",
                    fontSize = 12.sp, color = NeerlyColors.Ink500)
            }
            Text(">", fontSize = 20.sp, color = NeerlyColors.Ink400)
        }
    }
}
