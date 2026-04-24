package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class ProductCard(
    val id: String,
    val name: String,
    val brand: String?,
    val price: Int,
    val waterType: String,
    val size: String
)

private val SAMPLE_PRODUCTS = listOf(
    ProductCard("p1", "20L Jar (Normal)", "Aquafina", 85, "NORMAL", "20L"),
    ProductCard("p2", "20L Jar (Cool)", "Aquafina", 95, "COOL", "20L"),
    ProductCard("p3", "1L Bottle", "Bisleri", 20, "MINERAL", "1L")
)

@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: () -> Unit,
    onAddToCart: (productId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(color = NeerlyColors.CustomerPrimary) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("← Back", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.height(NeerlySpacing.x3))
                Text("Sri Ganesh Water Supply", color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("4.7 ★  ·  0.8 km  ·  FSSAI ✓", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp)
                Spacer(Modifier.height(NeerlySpacing.x2))
                AssistChip(
                    onClick = {},
                    label = { Text("Vendor #$vendorId") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                        labelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x4))

        Text("Products", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
            modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(NeerlySpacing.x2))

        SAMPLE_PRODUCTS.forEach { product ->
            ProductRow(product, onAdd = { onAddToCart(product.id) })
        }

        Spacer(Modifier.height(NeerlySpacing.x6))
    }
}

@Composable
private fun ProductRow(product: ProductCard, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeerlyColors.Water50),
            contentAlignment = Alignment.Center
        ) { Text("💧", fontSize = 26.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text("${product.brand ?: ""} · ${product.waterType}",
                fontSize = 12.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(4.dp))
            Text("₹${product.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
        OutlinedButton(
            onClick = onAdd,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.CustomerPrimary)
        ) { Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

