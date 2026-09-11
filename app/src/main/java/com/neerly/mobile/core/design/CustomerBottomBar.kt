package com.neerly.mobile.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The five customer tabs every artboard carries along the bottom, and which the
 * app was missing entirely — Orders, Subs, Wallet and Profile were only
 * reachable by digging through Profile, so most of them were effectively dead.
 *
 * Rendered as a plain Row rather than Material's `NavigationBar` so the active
 * state matches the canvas: accent-coloured label with a 5dp dot underneath,
 * not a pill-shaped indicator behind the icon.
 */
enum class CustomerTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Orders("Orders", Icons.AutoMirrored.Filled.ReceiptLong),
    Subs("Subs", Icons.Filled.Autorenew),
    Wallet("Wallet", Icons.Filled.AccountBalanceWallet),
    Profile("Profile", Icons.Filled.Person)
}

@Composable
fun CustomerBottomBar(
    current: CustomerTab,
    onSelect: (CustomerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(NeerlyColors.Paper)) {
        HorizontalDivider(color = NeerlyColors.Ink100)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = NeerlySpacing.x2, bottom = NeerlySpacing.x1)
        ) {
            CustomerTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: CustomerTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) NeerlyColors.CustomerPrimary else NeerlyColors.Ink500
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(vertical = NeerlySpacing.x1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            tab.label,
            fontSize = 10.5.sp,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
        // The canvas marks the active tab with a dot rather than an underline;
        // the inactive tabs reserve the same height so labels stay aligned.
        Spacer(Modifier.height(2.dp))
        if (selected) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(NeerlyColors.CustomerPrimary))
        } else {
            Spacer(Modifier.size(5.dp))
        }
    }
}
