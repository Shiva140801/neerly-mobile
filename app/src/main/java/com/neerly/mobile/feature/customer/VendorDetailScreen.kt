package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.neerly.mobile.core.design.OfflineBanner
import com.neerly.mobile.core.design.VendorDetailSkeleton
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.cart.AddOutcome
import com.neerly.mobile.data.dto.ProductResponse
import kotlinx.coroutines.launch

/**
 * Vendor detail + product catalogue.
 *
 * Tapping a product no longer adds it silently — it opens
 * `S-CUST-VEN-PROD-01`, where the customer chooses keep-container vs
 * transfer-and-return and sees what that choice costs today. The
 * "Switch vendor?" dialog still guards a cart that already belongs to someone
 * else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: () -> Unit,
    onAddToCart: (productId: String) -> Unit,
    vm: VendorDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    var pendingSwitch by remember { mutableStateOf<ProductSheetSelection?>(null) }
    var sheetProduct by remember { mutableStateOf<ProductResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetProduct = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
    ) {
        if (!online) OfflineBanner(onRetry = vm::refresh)

        when {
            state.loading -> VendorDetailSkeleton()

            state.error != null -> VendorDetailError(
                message = state.error!!,
                onRetry = vm::refresh,
                onBack = onBack
            )

            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Surface(color = NeerlyColors.CustomerPrimary) {
                    Column(modifier = Modifier.padding(NeerlySpacing.x4).fillMaxWidth()) {
                        Text(
                            "← Back", color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.clickable { onBack() }
                        )
                        Spacer(Modifier.height(NeerlySpacing.x3))
                        val name = state.vendor?.businessName ?: "Vendor #${vendorId.take(8)}"
                        Text(
                            name, color = Color.White,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
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

                if (state.products.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(NeerlySpacing.x6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💧", fontSize = 40.sp)
                        Spacer(Modifier.height(NeerlySpacing.x3))
                        Text(
                            "No products listed yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.Ink900
                        )
                        Spacer(Modifier.height(NeerlySpacing.x2))
                        Text(
                            "This vendor hasn't published a catalogue. Try another vendor near you.",
                            fontSize = 13.sp,
                            color = NeerlyColors.Ink500
                        )
                    }
                } else {
                    Text(
                        "Products",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                        modifier = Modifier.padding(horizontal = NeerlySpacing.x4)
                    )
                    Spacer(Modifier.height(NeerlySpacing.x2))
                    state.products.forEach { product ->
                        ProductRow(product, onOpen = { sheetProduct = product })
                    }
                }

                Spacer(Modifier.height(NeerlySpacing.x6))
            }
        }
    }

    sheetProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = { sheetProduct = null },
            sheetState = sheetState,
            containerColor = NeerlyColors.Paper,
            shape = RoundedCornerShape(topStart = NeerlyRadius.xl, topEnd = NeerlyRadius.xl)
        ) {
            ProductSheetContent(
                product = product,
                vendorName = state.vendor?.businessName,
                initialQuantity = vm.quantityInCart(product.id),
                alreadyInCart = vm.quantityInCart(product.id) > 0,
                onConfirm = { selection ->
                    when (vm.setCartLine(selection)) {
                        is AddOutcome.Added -> {
                            closeSheet()
                            onAddToCart(selection.product.id)
                        }
                        is AddOutcome.VendorMismatch -> {
                            pendingSwitch = selection
                            closeSheet()
                        }
                    }
                }
            )
        }
    }

    pendingSwitch?.let { selection ->
        AlertDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text("Switch vendor?") },
            text = {
                Text(
                    "You already have items from another vendor in your cart. Replace them with " +
                        "items from ${state.vendor?.businessName ?: "this vendor"}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmReplaceWithNewVendor(selection)
                    pendingSwitch = null
                    onAddToCart(selection.product.id)
                }) {
                    Text("Replace", color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitch = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VendorDetailError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(NeerlySpacing.x6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Couldn't load this vendor",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(message, fontSize = 13.sp, color = NeerlyColors.Ink500)
        Spacer(Modifier.height(NeerlySpacing.x5))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text("Try again") }
        TextButton(onClick = onBack) { Text("Go back", color = NeerlyColors.Ink500) }
    }
}

/**
 * A catalogue row. The deposit is no longer whispered in 11sp grey here either
 * — it's a chip, and the row opens the sheet where the full breakdown lives.
 */
@Composable
private fun ProductRow(product: ProductResponse, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = NeerlySpacing.x4, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(NeerlyRadius.sm))
                .background(NeerlyColors.Water50),
            contentAlignment = Alignment.Center
        ) { Text("💧", fontSize = 26.sp) }
        Spacer(Modifier.width(NeerlySpacing.x3))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            val sub = listOfNotNull(
                product.brand?.takeIf { it.isNotBlank() },
                product.template.sizeLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" · ").ifBlank { product.categoryCode }
            Text(sub, fontSize = 12.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
            ) {
                Text(
                    product.price.asRupees(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink900
                )
                product.depositAmount?.takeIf { it.signum() > 0 }?.let { deposit ->
                    Surface(
                        color = NeerlyColors.CustomerSoft,
                        shape = RoundedCornerShape(NeerlyRadius.pill)
                    ) {
                        Text(
                            "+ ${deposit.asRupees()} deposit",
                            modifier = Modifier.padding(horizontal = NeerlySpacing.x2, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.CustomerDark
                        )
                    }
                }
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Choose options",
            tint = NeerlyColors.Ink400
        )
    }
}
