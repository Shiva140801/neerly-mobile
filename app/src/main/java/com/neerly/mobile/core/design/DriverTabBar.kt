package com.neerly.mobile.core.design

import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocalShipping
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

/** Board DTabBar — the driver app's four tabs. Tasks carries a dot while an offer is waiting. */
enum class DriverTab(@androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    Tasks(R.string.core_tab_tasks, Icons.Filled.LocalShipping),
    Round(R.string.core_tab_round, Icons.AutoMirrored.Filled.FormatListBulleted),
    Money(R.string.core_tab_money, Icons.Filled.CurrencyRupee),
    Me(R.string.core_tab_me, Icons.Filled.AccountCircle)
}

/** Taller than the seller bar (60dp rows): drivers tap it on a bike, often with gloves. */
@Composable
fun DriverTabBar(current: DriverTab, onSelect: (DriverTab) -> Unit, offerWaiting: Boolean = false) {
    Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper)) {
        HorizontalDivider(color = NeerlyColors.Ink200)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 4.dp)) {
            DriverTab.entries.forEach { tab ->
                val on = tab == current
                val tint = if (on) NeerlyColors.Ink900 else NeerlyColors.Ink600
                val dot = tab == DriverTab.Tasks && offerWaiting
                val label = stringResource(tab.labelRes)
                val described = if (dot) stringResource(R.string.core_tab_task_waiting, label) else label
                Column(
                    Modifier.weight(1f).heightIn(min = 60.dp).clickable { onSelect(tab) }
                        .semantics {
                            role = Role.Tab; selected = on
                            contentDescription = described
                        },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box {
                        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                        if (dot) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)
                            .size(10.dp).clip(CircleShape).background(Boondh.Amber))
                    }
                    Text(label, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = tint)
                    Box(Modifier.size(width = 18.dp, height = 3.dp).clip(CircleShape)
                        .background(if (on) NeerlyColors.Ink900 else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
    }
}
