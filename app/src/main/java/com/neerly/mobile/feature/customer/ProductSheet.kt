package com.neerly.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.neerly.mobile.core.design.JetBrainsMono
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.dto.ProductResponse
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * `S-CUST-VEN-PROD-01` — the product sheet.
 *
 * This is where the customer picks how the container comes back: **keep** it
 * against a refundable deposit, or **transfer-and-return** — hand the driver an
 * empty jar at the door and pay no deposit at all. PRD §8.1 calls this a
 * pillar; until now the app decided silently on the customer's behalf
 * (`keepContainer = product.allowKeepContainer`) and buried the deposit in 11sp
 * grey, which is how someone could tap "Add" on a ₹85 jar and be charged ₹385.
 *
 * Three states, all built from fields the API already serves:
 *
 *  1. **Both modes** — two radio cards; the "you pay today" panel splits water
 *     from the refundable deposit and re-totals as the choice changes.
 *  2. **Transfer selected** — deposit line reads ₹0 in the success colour.
 *  3. **One mode only** — no radios. An explainer says why, and the deposit is
 *     given its own display-size card, because a jar deposit routinely exceeds
 *     the price of the water inside it and that must not be a surprise.
 *
 * Nothing here is invented: `allowKeepContainer`, `allowTransferAndReturn`,
 * `depositAmount`, `retentionHours`, `price`, `brand`, `description` and
 * `photoUrl` are all on `ProductResponse`.
 */
enum class ContainerMode { KEEP, TRANSFER_AND_RETURN }

data class ProductSheetSelection(
    val product: ProductResponse,
    val quantity: Int,
    val mode: ContainerMode
) {
    val keepContainer: Boolean get() = mode == ContainerMode.KEEP
}

/**
 * What the customer is charged at checkout, split so the refundable part is
 * never mistaken for the price of the water.
 */
data class PayTodayBreakdown(
    val water: BigDecimal,
    val deposit: BigDecimal,
    val total: BigDecimal
)

/**
 * Pure so it can be tested without a composition — this is the arithmetic the
 * customer is agreeing to, and the case that matters (deposit > price) is easy
 * to get wrong in a `@Composable`.
 *
 * A deposit is charged only for keep-container. Transfer-and-return swaps an
 * empty jar at the door, so the deposit is genuinely zero, not merely hidden.
 */
fun payTodayFor(
    price: BigDecimal,
    depositAmount: BigDecimal?,
    quantity: Int,
    mode: ContainerMode
): PayTodayBreakdown {
    val qty = BigDecimal(quantity.coerceAtLeast(0))
    val water = price.multiply(qty).setScale(2, RoundingMode.HALF_UP)
    val deposit = when (mode) {
        ContainerMode.KEEP -> (depositAmount ?: BigDecimal.ZERO).multiply(qty)
        ContainerMode.TRANSFER_AND_RETURN -> BigDecimal.ZERO
    }.setScale(2, RoundingMode.HALF_UP)
    return PayTodayBreakdown(water, deposit, water.add(deposit).setScale(2, RoundingMode.HALF_UP))
}

/**
 * The mode the sheet opens on. A product that allows only transfer must not
 * default to keep (it would quote a deposit the vendor never charges); a
 * product with neither flag is treated as keep, mirroring the backend.
 */
fun defaultModeFor(allowKeep: Boolean, allowTransfer: Boolean): ContainerMode =
    if (allowTransfer && !allowKeep) ContainerMode.TRANSFER_AND_RETURN else ContainerMode.KEEP

private const val MAX_QTY = 10

@Composable
fun ProductSheetContent(
    product: ProductResponse,
    vendorName: String?,
    initialQuantity: Int,
    alreadyInCart: Boolean,
    onConfirm: (ProductSheetSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val allowsKeep = product.allowKeepContainer
    val allowsTransfer = product.allowTransferAndReturn
    val bothModes = allowsKeep && allowsTransfer

    // A product with neither flag set still has to be orderable; the backend
    // treats that as keep-container, so mirror it rather than dead-ending.
    var mode by remember(product.id) { mutableStateOf(defaultModeFor(allowsKeep, allowsTransfer)) }
    var qty by remember(product.id) { mutableIntStateOf(initialQuantity.coerceIn(1, MAX_QTY)) }

    val deposit = product.depositAmount ?: BigDecimal.ZERO
    val hasDeposit = deposit.signum() > 0
    val breakdown = payTodayFor(product.price, product.depositAmount, qty, mode)
    val waterTotal = breakdown.water
    val depositTotal = breakdown.deposit
    val payToday = breakdown.total

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NeerlySpacing.x5)
            .padding(bottom = NeerlySpacing.x5)
    ) {
        ProductHero(product)

        Spacer(Modifier.height(NeerlySpacing.x4))
        TitleRow(product, vendorName)

        if (bothModes) {
            SectionLabel("Container mode", Modifier.padding(top = NeerlySpacing.x4, bottom = NeerlySpacing.x2))
            ModeCard(
                title = "Keep container",
                subtitle = if (hasDeposit) {
                    "${deposit.asRupees()} refundable deposit · return anytime"
                } else {
                    "Keep the jar until you're done with it"
                },
                selected = mode == ContainerMode.KEEP,
                onClick = { mode = ContainerMode.KEEP }
            )
            Spacer(Modifier.height(NeerlySpacing.x2))
            ModeCard(
                title = "Transfer-return · swap at door",
                subtitle = "Give your empty jar to the driver · no deposit",
                highlightSubtitle = "no deposit",
                selected = mode == ContainerMode.TRANSFER_AND_RETURN,
                onClick = { mode = ContainerMode.TRANSFER_AND_RETURN }
            )
        } else {
            Spacer(Modifier.height(NeerlySpacing.x4))
            SingleModeNotice(
                keepOnly = mode == ContainerMode.KEEP,
                hasDeposit = hasDeposit
            )
        }

        // The deposit gets display type whenever the customer is actually going
        // to be charged one — this is the number that can dwarf the water price.
        if (mode == ContainerMode.KEEP && hasDeposit) {
            Spacer(Modifier.height(NeerlySpacing.x3))
            DepositHighlight(deposit = deposit, retentionHours = product.retentionHours)
        }

        Spacer(Modifier.height(NeerlySpacing.x3))
        PayTodayPanel(
            quantity = qty,
            waterTotal = waterTotal,
            depositTotal = depositTotal,
            payToday = payToday,
            keepingContainer = mode == ContainerMode.KEEP,
            productHasDeposit = hasDeposit
        )

        Spacer(Modifier.height(NeerlySpacing.x4))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            QuantityStepper(
                quantity = qty,
                onDecrease = { if (qty > 1) qty-- },
                onIncrease = { if (qty < MAX_QTY) qty++ }
            )
            Button(
                onClick = {
                    onConfirm(ProductSheetSelection(product, qty, mode))
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                val verb = if (alreadyInCart) "Update" else "Add"
                val label = if (depositTotal.signum() > 0) {
                    "$verb $qty · ${payToday.asRupees()} today"
                } else {
                    "$verb $qty · ${payToday.asRupees()}"
                }
                Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Photo if the vendor uploaded one, otherwise the water gradient the canvas
 * uses. No BESTSELLER / ISI badges — neither has a backing field.
 */
@Composable
private fun ProductHero(product: ProductResponse) {
    val gradient = Brush.linearGradient(
        listOf(NeerlyColors.CustomerPrimary, NeerlyColors.Water500)
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(NeerlyRadius.xl))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        val photo = product.photoUrl
        if (!photo.isNullOrBlank()) {
            AsyncImage(
                model = photo,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
        } else {
            Text("💧", fontSize = 56.sp)
        }
    }
}

@Composable
private fun TitleRow(product: ProductResponse, vendorName: String?) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                product.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink900
            )
            // Sub-line built only from fields that exist: brand, description and
            // the vendor's own name.
            val subtitle = listOfNotNull(
                vendorName?.takeIf { it.isNotBlank() },
                product.brand?.takeIf { it.isNotBlank() },
                product.description?.takeIf { it.isNotBlank() }
                    ?: product.template.sizeLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.5.sp, color = NeerlyColors.Ink500)
            }
        }
        Text(
            product.price.asRupees(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 0.9.sp,
        fontWeight = FontWeight.SemiBold,
        color = NeerlyColors.Ink500,
        modifier = modifier
    )
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlightSubtitle: String? = null
) {
    val borderColor = if (selected) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200
    Surface(
        color = if (selected) NeerlyColors.CustomerSofter else NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeerlyRadius.md))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(NeerlyRadius.md)
            )
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            RadioDot(selected)
            Column {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) NeerlyColors.CustomerDark else NeerlyColors.Ink900
                )
                Spacer(Modifier.height(2.dp))
                if (highlightSubtitle != null && subtitle.endsWith(highlightSubtitle)) {
                    Row {
                        Text(
                            subtitle.removeSuffix(highlightSubtitle),
                            fontSize = 12.sp,
                            color = NeerlyColors.Ink600
                        )
                        Text(
                            highlightSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.Ok
                        )
                    }
                } else {
                    Text(subtitle, fontSize = 12.sp, color = NeerlyColors.Ink600)
                }
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier
            .padding(top = 1.dp)
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) NeerlyColors.CustomerPrimary else NeerlyColors.Ink300,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(NeerlyColors.CustomerPrimary))
        }
    }
}

/** `S-CUST-VEN-PROD-01C` — the explainer shown when only one mode is offered. */
@Composable
private fun SingleModeNotice(keepOnly: Boolean, hasDeposit: Boolean) {
    val text = if (keepOnly) {
        buildString {
            append("This vendor only offers keep-container for this product — no swap option.")
            if (hasDeposit) append(" The deposit is fully refunded when you return the container.")
        }
    } else {
        "This vendor only offers transfer-and-return for this product — hand your empty jar to " +
            "the driver at the door. No deposit is charged."
    }
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = NeerlyColors.CustomerPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text,
                fontSize = 12.5.sp,
                color = NeerlyColors.CustomerDark,
                lineHeight = 19.sp
            )
        }
    }
}

/**
 * The deposit, at display size. `retentionHours` becomes the "return within"
 * line; it's omitted rather than guessed when the vendor didn't set one.
 */
@Composable
private fun DepositHighlight(deposit: BigDecimal, retentionHours: Int?) {
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.lg),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, NeerlyColors.CustomerPrimary, RoundedCornerShape(NeerlyRadius.lg))
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Refundable deposit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.CustomerDark
                )
                Text(
                    deposit.asRupees(),
                    fontSize = 26.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.CustomerDark
                )
            }
            if (retentionHours != null && retentionHours > 0) {
                Spacer(Modifier.height(NeerlySpacing.x2))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = NeerlyColors.Ink600,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "Return within $retentionHours hours · money back to your wallet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink700
                    )
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x1))
            Text(
                "The deposit can be bigger than the water — that's normal for jars, and you get it back.",
                fontSize = 11.5.sp,
                color = NeerlyColors.Ink500,
                lineHeight = 16.sp
            )
        }
    }
}

/** "You pay today", with water and the refundable deposit kept visibly apart. */
@Composable
private fun PayTodayPanel(
    quantity: Int,
    waterTotal: BigDecimal,
    depositTotal: BigDecimal,
    payToday: BigDecimal,
    keepingContainer: Boolean,
    productHasDeposit: Boolean
) {
    Surface(
        color = NeerlyColors.Ink50,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md))
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            SectionLabel("You pay today")
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Water · $quantity ${if (quantity == 1) "jar" else "jars"}",
                    fontSize = 14.sp,
                    color = NeerlyColors.Ink700
                )
                Text(
                    waterTotal.asRupees(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink900
                )
            }
            if (productHasDeposit) {
                Spacer(Modifier.height(NeerlySpacing.x1))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
                    ) {
                        Text("Deposit", fontSize = 14.sp, color = NeerlyColors.Ink700)
                        if (keepingContainer) RefundablePill()
                    }
                    if (keepingContainer) {
                        Text(
                            depositTotal.asRupees(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.Ink900
                        )
                    } else {
                        Text(
                            "₹0 — you swap jars",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeerlyColors.Ok
                        )
                    }
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x2))
            HorizontalDivider(color = NeerlyColors.Ink200)
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(
                    payToday.asRupees(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink900
                )
            }
            if (keepingContainer && depositTotal.signum() > 0) {
                Spacer(Modifier.height(NeerlySpacing.x1))
                Text(
                    "${depositTotal.asRupees()} of this comes back when the container does.",
                    fontSize = 11.5.sp,
                    color = NeerlyColors.Ink500
                )
            }
        }
    }
}

@Composable
private fun RefundablePill() {
    Surface(color = NeerlyColors.CustomerSoft, shape = RoundedCornerShape(NeerlyRadius.pill)) {
        Text(
            "REFUNDABLE",
            modifier = Modifier.padding(horizontal = NeerlySpacing.x2, vertical = 2.dp),
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.CustomerDark
        )
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Surface(
        color = NeerlyColors.Ink50,
        shape = RoundedCornerShape(NeerlyRadius.pill),
        modifier = Modifier.border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.pill))
    ) {
        Row(
            Modifier.padding(NeerlySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                description = "Decrease quantity",
                enabled = quantity > 1,
                filled = false,
                onClick = onDecrease
            )
            Text(
                quantity.toString(),
                modifier = Modifier.width(26.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink900,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            StepperButton(
                icon = Icons.Filled.Add,
                description = "Increase quantity",
                enabled = quantity < MAX_QTY,
                filled = true,
                onClick = onIncrease
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> NeerlyColors.Ink300
        filled -> NeerlyColors.Paper
        else -> NeerlyColors.CustomerPrimary
    }
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (filled && enabled) NeerlyColors.CustomerPrimary else NeerlyColors.Paper.copy(alpha = 0f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
