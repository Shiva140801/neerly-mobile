package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.design.Role

/** S-AUTH-ROL-01 — Role picker for multi-role users. */
@Composable
fun RolePickerScreen(
    grantedRoles: List<String>,
    onRoleSelected: (role: String, remember: Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Canvas)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(
            stringResource(R.string.cust_role_title),
            fontSize = 32.sp, lineHeight = 38.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        val can = listOfNotNull(
            "c".takeIf { grantedRoles.contains(PortalRole.CUSTOMER) },
            "v".takeIf { grantedRoles.contains(PortalRole.VENDOR) },
            "d".takeIf { grantedRoles.contains(PortalRole.DRIVER) }).joinToString("")
        // Whole sentences per combination — translations can't be stitched from fragments.
        val canText = when (can) {
            "cvd" -> R.string.cust_role_can_all; "cv" -> R.string.cust_role_can_cust_shop
            "cd" -> R.string.cust_role_can_cust_drive; "vd" -> R.string.cust_role_can_shop_drive
            else -> R.string.cust_role_can_one
        }
        Text(stringResource(canText), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (grantedRoles.contains(PortalRole.CUSTOMER)) {
                RoleCard(
                    title = stringResource(R.string.cust_role_order),
                    subtitle = stringResource(R.string.cust_role_order_sub),
                    color = NeerlyColors.CustomerPrimary,
                    icon = "🏠",
                    onClick = { onRoleSelected(PortalRole.CUSTOMER, rememberChoice) }
                )
            }
            if (grantedRoles.contains(PortalRole.VENDOR)) {
                RoleCard(
                    title = stringResource(R.string.cust_role_shop),
                    subtitle = stringResource(R.string.cust_role_shop_sub),
                    color = NeerlyColors.VendorPrimary,
                    icon = "🏪",
                    onClick = { onRoleSelected(PortalRole.VENDOR, rememberChoice) }
                )
            }
            if (grantedRoles.contains(PortalRole.DRIVER)) {
                RoleCard(
                    title = stringResource(R.string.cust_role_drive),
                    subtitle = stringResource(R.string.cust_role_drive_sub),
                    color = NeerlyColors.DriverPrimary,
                    icon = "🚚",
                    onClick = { onRoleSelected(PortalRole.DRIVER, rememberChoice) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { rememberChoice = !rememberChoice }
        ) {
            Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
            Text(stringResource(R.string.cust_role_remember), fontSize = 15.sp, color = NeerlyColors.Ink700)
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NeerlyRadius.lg),
        color = NeerlyColors.Paper,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink200),
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(subtitle, fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
    }
}
