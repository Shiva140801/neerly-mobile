package com.neerly.mobile.core.design

import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Board VTabBar — the seller app's five tabs. Orders carries a red count of offers waiting. */
enum class VendorTab(@androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    Today(R.string.core_tab_today, Icons.Filled.WbSunny),
    Orders(R.string.core_tab_orders, Icons.AutoMirrored.Filled.ReceiptLong),
    Jars(R.string.core_tab_jars, Icons.Filled.WaterDrop),
    Earnings(R.string.core_tab_earnings, Icons.Filled.BarChart),
    Shop(R.string.core_tab_shop, Icons.Filled.Storefront)
}

@Composable
fun VendorTabBar(current: VendorTab, onSelect: (VendorTab) -> Unit, ordersBadge: Int = 0) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper)) {
        HorizontalDivider(color = NeerlyColors.Ink100)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 2.dp)) {
            VendorTab.entries.forEach { tab ->
                val on = tab == current
                val tint = if (on) NeerlyColors.Ink900 else NeerlyColors.Ink600
                val badge = if (tab == VendorTab.Orders && ordersBadge > 0) ordersBadge else 0
                val label = stringResource(tab.labelRes)
                val described = if (badge > 0) stringResource(R.string.core_tab_badge_waiting, label, badge) else label
                Column(
                    Modifier.weight(1f).heightIn(min = 52.dp).clickable { onSelect(tab) }
                        .semantics {
                            role = Role.Tab; selected = on
                            contentDescription = described
                        },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                        if (badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)
                            .clip(CircleShape).background(NeerlyColors.Err).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("$badge", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Paper)
                        }
                    }
                    Text(label, fontSize = 11.sp, color = tint, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (on) NeerlyColors.Ok else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
    }
}
