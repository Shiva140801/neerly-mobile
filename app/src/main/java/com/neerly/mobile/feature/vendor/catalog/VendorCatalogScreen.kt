package com.neerly.mobile.feature.vendor.catalog

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import java.time.LocalDate

/** Board V13 — "What you sell". Tap a product to edit it; the switch hides it from customers. */
@Composable
fun VendorCatalogScreen(
    onBack: () -> Unit,
    onEdit: (productId: String) -> Unit = {},
    onAdd: () -> Unit = {},
    vm: VendorCatalogViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    var asking by remember { mutableStateOf<ShopProduct?>(null) }
    BoondhPage(
        title = stringResource(R.string.vend_what_you_sell), onBack = onBack,
        subtitle = stringResource(R.string.vend_catalog_sub),
        footer = { PrimaryButton(stringResource(R.string.vend_add_product), onAdd, color = NeerlyColors.VendorPrimary) }
    ) {
        s.error?.let { e -> if (s.products.isNotEmpty()) item { Notice(e, Tone.Bad) } }
        if (s.products.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_catalog_empty), stringResource(R.string.vend_catalog_empty_sub)) }
            return@BoondhPage
        }
        items(s.products, key = { it.id }) { p ->
            val on = p.status != "PAUSED"
            val desc = stringResource(if (on) R.string.vend_on_sale_desc else R.string.vend_off_desc, p.name)
            BoondhCard(onClick = { onEdit(p.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(listOfNotNull(stringResource(R.string.vend_price_per_day, p.price.rupees(), p.dailyCapacity),
                            p.containersInStock?.let { stringResource(R.string.vend_in_stock, it) }).joinToString(" · "),
                            fontSize = 13.sp, color = NeerlyColors.Ink600)
                    }
                    Switch(
                        checked = on, enabled = s.saving != p.id,
                        onCheckedChange = { want -> if (want) vm.switchOn(p) else asking = p },
                        modifier = Modifier.semantics { contentDescription = desc }
                    )
                }
                when {
                    !on -> Pill(p.pausedUntil?.let { stringResource(R.string.vend_off_back_on, shortDay(it)) } ?: stringResource(R.string.vend_off_until_on), Tone.Warn)
                    p.status == "OUT_OF_STOCK" -> Pill(stringResource(R.string.vend_sold_out), Tone.Bad)
                    else -> p.soldThisMonth?.takeIf { it > 0 }?.let { Pill(stringResource(R.string.vend_sold_month, it), Tone.Ok) }
                }
            }
        }
    }
    asking?.let { p -> BackOnDialog(p, vm.today(), onDismiss = { asking = null }) { day -> vm.switchOff(p, day); asking = null } }
}

/** "Back on when?" — the server switches it back on by itself on that day. */
@Composable
private fun BackOnDialog(p: ShopProduct, today: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vend_switch_off_q, p.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.vend_switch_off_body), fontSize = 14.sp, color = NeerlyColors.Ink700)
                listOf(
                    stringResource(R.string.vend_back_tomorrow) to today.plusDays(1),
                    stringResource(R.string.vend_back_3d) to today.plusDays(3),
                    stringResource(R.string.vend_back_week) to today.plusDays(7),
                    stringResource(R.string.vend_back_manual) to null
                ).forEach { (label, day) ->
                    TextButton(onClick = { onPick(day) }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                        Text(day?.let { stringResource(R.string.vend_option_day, label, shortDay(it.toString())) } ?: label, color = NeerlyColors.VendorPrimary,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.vend_keep_on)) } }
    )
}
