package com.neerly.mobile.feature.promo

import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import java.math.BigDecimal

/**
 * Inline promo-code input for the cart screen. Calls
 * POST /api/v1/customer/promos/quote through [onQuote] and renders the
 * discount / rejection reason coming back.
 *
 * Intentionally presentational — the caller wires the Retrofit call via
 * its view-model; this keeps the composable Hilt-free so preview-friendly.
 */
@Composable
fun PromoCodeField(
    orderSubtotal: BigDecimal,
    isFirstOrder: Boolean,
    currentDiscount: BigDecimal,
    currentReason: String?,
    onQuote: (code: String) -> Unit,
    onRemove: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column {
        Text(stringResource(R.string.cust_promo_label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
        Spacer(Modifier.height(NeerlySpacing.x2))

        if (currentDiscount > BigDecimal.ZERO) {
            Surface(
                color = NeerlyColors.OkSoft,
                shape = RoundedCornerShape(NeerlyRadius.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(NeerlySpacing.x4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.cust_promo_off, currentDiscount.asRupees()), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ok)
                        Text(stringResource(R.string.cust_promo_applied_to, orderSubtotal.asRupees()),
                            fontSize = 12.sp, color = NeerlyColors.Ink700)
                    }
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.cust_remove), color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.cust_promo_enter)) },
                    singleLine = true
                )
                Spacer(Modifier.width(NeerlySpacing.x2))
                Button(
                    onClick = { if (code.isNotBlank()) onQuote(code.trim()) },
                    enabled = code.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                ) { Text(stringResource(R.string.cust_apply)) }
            }
            if (!currentReason.isNullOrBlank()) {
                Spacer(Modifier.height(NeerlySpacing.x2))
                Text(currentReason, color = NeerlyColors.Err, fontSize = 12.sp)
            }
            if (isFirstOrder) {
                Spacer(Modifier.height(NeerlySpacing.x2))
                Text(stringResource(R.string.cust_promo_first_order_tip),
                    color = NeerlyColors.Ink500, fontSize = 12.sp)
            }
        }
    }
}
