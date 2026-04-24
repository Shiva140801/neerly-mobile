package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.cart.AddOutcome
import com.neerly.mobile.data.dto.ProductResponse

/**
 * Vendor detail + product catalog. Tapping "Add" either:
 *   - adds to the cart if empty or same vendor
 *   - shows a "Switch vendor?" dialog if the cart has another vendor
 */
@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: () -> Unit,
    onAddToCart: (productId: String) -> Unit,
    vm: VendorDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var pendingSwitch by remember { mutableStateOf<ProductResponse?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = NeerlyColors.CustomerPrimary) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("← Back", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.height(NeerlySpacing.x3))
                val name = state.vendor?.businessName ?: "Vendor #${vendorId.take(8)}"
                Text(name, color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                state.vendor?.let { v ->
                    val rating = v.avgRating?.toPlainString() ?: "—"
                    Text(
                        "$rating ★  ·  ${v.businessCity} · ${v.businessPincode}" +
                            (if (!v.fssaiNumber.isNullOrBlank()) "  ·  FSSAI ✓" else ""),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x4))

        when {
            state.loading -> Text("Loading…",
                Modifier.padding(horizontal = 16.dp), color = NeerlyColors.Ink500)
            state.error != null -> Text(state.error!!,
                Modifier.padding(horizontal = 16.dp), color = NeerlyColors.Err)
            state.products.isEmpty() -> Text("No products listed yet.",
                Modifier.padding(horizontal = 16.dp), color = NeerlyColors.Ink500)
            else -> {
                Text("Products",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                    modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(NeerlySpacing.x2))
                state.products.forEach { product ->
                    ProductRow(product, onAdd = {
                        when (val outcome = vm.addToCart(product)) {
                            is AddOutcome.Added -> onAddToCart(product.id)
                            is AddOutcome.VendorMismatch -> pendingSwitch = product
                        }
                    })
                }
            }
        }

        Spacer(Modifier.height(NeerlySpacing.x6))
    }

    pendingSwitch?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text("Switch vendor?") },
            text = { Text("You already have items from another vendor in your cart. Replace them with items from ${state.vendor?.businessName ?: "this vendor"}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmReplaceWithNewVendor(product)
                    pendingSwitch = null
                    onAddToCart(product.id)
                }) { Text("Replace", color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitch = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProductRow(product: ProductResponse, onAdd: () -> Unit) {
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
            Text("${product.brand.orEmpty()} · ${product.categoryCode}",
                fontSize = 12.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(4.dp))
            Text("₹${product.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            product.depositAmount?.takeIf { it.signum() > 0 }?.let {
                Text("+ ₹$it refundable deposit", fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
        }
        OutlinedButton(
            onClick = onAdd,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.CustomerPrimary)
        ) { Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}
