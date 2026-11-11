package com.neerly.mobile.feature.customer

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
    // Board Product opens on "Swap at the door · No deposit": the cheaper, usual choice.
    // Defaulting to keep quoted a ₹300 deposit on a ₹50 jar unless the customer noticed.
    if (allowTransfer) ContainerMode.TRANSFER_AND_RETURN else ContainerMode.KEEP

private const val MAX_QTY = 10

@Composable
fun ProductSheetContent(
    product: ProductResponse,
    vendorName: String?,
    initialQuantity: Int,
    alreadyInCart: Boolean,
    onConfirm: (ProductSheetSelection) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
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
        SheetHeader(product, vendorName, onClose)

        if (bothModes) {
            Text(stringResource(R.string.cust_ps_jar_q), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                modifier = Modifier.padding(top = NeerlySpacing.x4, bottom = NeerlySpacing.x2))
            ModeCard(
                title = stringResource(R.string.cust_ps_swap_door),
                subtitle = stringResource(R.string.cust_ps_swap_body),
                trailing = stringResource(R.string.cust_ps_no_deposit).replaceFirstChar { it.uppercase() }, trailingColor = NeerlyColors.Ok,
                selected = mode == ContainerMode.TRANSFER_AND_RETURN,
                onClick = { mode = ContainerMode.TRANSFER_AND_RETURN }
            )
            Spacer(Modifier.height(NeerlySpacing.x2))
            ModeCard(
                title = stringResource(R.string.cust_ps_keep_jar),
                subtitle = if (hasDeposit) stringResource(R.string.cust_ps_keep_body) else stringResource(R.string.cust_ps_keep_free),
                trailing = if (hasDeposit) stringResource(R.string.cust_ps_per_jar, deposit.asRupees()) else null,
                selected = mode == ContainerMode.KEEP,
                onClick = { mode = ContainerMode.KEEP }
            )
        } else if (!allowsKeep && !allowsTransfer && !product.template.allowsDeposit) {
            // Ice bags, sealed cases, tankers: nothing comes back, so no jar talk at all.
            Spacer(Modifier.height(NeerlySpacing.x4))
            Text(stringResource(R.string.cust_ps_one_way), fontSize = 13.sp, color = NeerlyColors.Ink600)
        } else {
            Spacer(Modifier.height(NeerlySpacing.x4))
            SingleModeNotice(
                keepOnly = mode == ContainerMode.KEEP,
                hasDeposit = hasDeposit
            )
        }

        // Board Product: the deposit lives in the "Keep the jar" option and the bill below;
        // the separate deposit card the old sheet showed isn't on the board.
        Spacer(Modifier.height(NeerlySpacing.x3))
        BillCard(qty = qty, price = product.price, waterTotal = waterTotal, depositTotal = depositTotal,
            payToday = payToday, showDeposit = hasDeposit || allowsKeep)

        Spacer(Modifier.height(NeerlySpacing.x4))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            BoxStepper(
                quantity = qty,
                onDecrease = { if (qty > 1) qty-- },
                onIncrease = { if (qty < MAX_QTY) qty++ }
            )
            Button(
                onClick = {
                    onConfirm(ProductSheetSelection(product, qty, mode))
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(NeerlyRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                val label = when {
                    alreadyInCart && depositTotal.signum() > 0 -> stringResource(R.string.cust_ps_update_today, qty, payToday.asRupees())
                    alreadyInCart -> stringResource(R.string.cust_ps_update, qty, payToday.asRupees())
                    depositTotal.signum() > 0 -> stringResource(R.string.cust_ps_add_today, qty, payToday.asRupees())
                    else -> stringResource(R.string.cust_ps_add, qty, payToday.asRupees())
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
    highlightSubtitle: String? = null,
    trailing: String? = null,
    trailingColor: androidx.compose.ui.graphics.Color = NeerlyColors.Ink900
) {
    val borderColor = if (selected) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200
    Surface(
        color = NeerlyColors.Paper,
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
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        title, Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeerlyColors.Ink900
                    )
                    trailing?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = trailingColor) }
                }
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
                    Text(subtitle, fontSize = 15.sp, lineHeight = 21.sp, color = NeerlyColors.Ink600)
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
    val text = when {
        keepOnly && hasDeposit -> stringResource(R.string.cust_ps_keep_only_deposit)
        keepOnly -> stringResource(R.string.cust_ps_keep_only)
        else -> stringResource(R.string.cust_ps_swap_only)
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
                    stringResource(R.string.cust_ps_refundable_deposit),
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
                        androidx.compose.ui.res.pluralStringResource(R.plurals.cust_ps_return_within, retentionHours, retentionHours),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink700
                    )
                }
            }
            Spacer(Modifier.height(NeerlySpacing.x1))
            Text(
                stringResource(R.string.cust_ps_deposit_bigger),
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
            SectionLabel(stringResource(R.string.cust_ps_pay_today))
            Spacer(Modifier.height(NeerlySpacing.x2))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.cust_ps_water_jars, quantity, quantity),
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
                        Text(stringResource(R.string.cust_ps_deposit), fontSize = 14.sp, color = NeerlyColors.Ink700)
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
                            stringResource(R.string.cust_ps_zero_swap),
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
                Text(stringResource(R.string.cust_total), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
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
                    stringResource(R.string.cust_ps_comes_back, depositTotal.asRupees()),
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
            stringResource(R.string.cust_ps_refundable_pill),
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
                description = stringResource(R.string.cust_ps_decrease),
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
                description = stringResource(R.string.cust_ps_increase),
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

/** Board Product header: jar tile, serif name, shop line, the "₹60 A JAR" chip, close ✕. */
@Composable
private fun SheetHeader(product: ProductResponse, vendorName: String?, onClose: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = NeerlySpacing.x2), verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(NeerlyRadius.md)).background(NeerlyColors.WarnSoft),
            contentAlignment = Alignment.Center) {
            val photo = product.photoUrl
            if (!photo.isNullOrBlank()) AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
            else Text("🫙", fontSize = 34.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(listOfNotNull(product.brand?.takeIf { it.isNotBlank() }, product.name).joinToString(" · "),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 22.sp, lineHeight = 26.sp,
                fontWeight = FontWeight.Medium, color = NeerlyColors.Ink900)
            vendorName?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600) }
            Text(stringResource(R.string.cust_ps_a_jar, product.price.asRupees()).uppercase(), fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = NeerlyColors.CustomerPrimary,
                modifier = Modifier.background(NeerlyColors.CustomerSofter, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        onClose?.let {
            IconButton(onClick = it, modifier = Modifier.size(48.dp)) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Close, contentDescription = stringResource(R.string.cust_ps_close), tint = NeerlyColors.Ink700)
            }
        }
    }
}

/** Board Product bill: dotted leaders, the deposit marked REFUNDABLE, "You pay today" in serif. */
@Composable
private fun BillCard(qty: Int, price: BigDecimal, waterTotal: BigDecimal, depositTotal: BigDecimal, payToday: BigDecimal, showDeposit: Boolean) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper, RoundedCornerShape(NeerlyRadius.md))
        .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md)).padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LeaderRow(stringResource(R.string.cust_ps_bill_water, qty, price.asRupees()), null, waterTotal.asRupees())
        if (showDeposit) LeaderRow(stringResource(R.string.cust_ps_jar_deposit), stringResource(R.string.cust_ps_refundable_pill), depositTotal.asRupees())
        HorizontalDivider(color = NeerlyColors.Ink200)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cust_ps_pay_today_line), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(payToday.asRupees(), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        }
    }
}

@Composable
private fun LeaderRow(label: String, tag: String?, amount: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, fontSize = 15.sp, color = NeerlyColors.Ink900)
        tag?.let { Text(" " + it.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = NeerlyColors.Ok) }
        androidx.compose.foundation.Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 6.dp)) {
            drawLine(NeerlyColors.Ink300, androidx.compose.ui.geometry.Offset(0f, size.height - 2f),
                androidx.compose.ui.geometry.Offset(size.width, size.height - 2f), strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 6f)))
        }
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
    }
}

/** Board Product stepper: an outlined box with − n +, not the pill with a filled plus. */
@Composable
private fun BoxStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(Modifier.height(56.dp).border(1.dp, NeerlyColors.Ink300, RoundedCornerShape(NeerlyRadius.md)),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrease, enabled = quantity > 1, modifier = Modifier.size(52.dp)) {
            Text("−", fontSize = 24.sp, color = if (quantity > 1) NeerlyColors.Ink900 else NeerlyColors.Ink300) }
        Text(quantity.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
            modifier = Modifier.widthIn(min = 28.dp).semantics { contentDescription = quantity.toString() }, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onIncrease, enabled = quantity < MAX_QTY, modifier = Modifier.size(52.dp)) {
            Text("+", fontSize = 24.sp, color = NeerlyColors.Ink900) }
    }
}
