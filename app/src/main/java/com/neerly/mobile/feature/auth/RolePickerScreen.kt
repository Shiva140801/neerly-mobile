package com.neerly.mobile.feature.auth

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
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(
            "Continue as",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text("You have access to multiple roles.", fontSize = 14.sp, color = NeerlyColors.Ink600)

        Spacer(Modifier.height(NeerlySpacing.x8))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (grantedRoles.contains("CUSTOMER")) {
                RoleCard(
                    title = "Customer",
                    subtitle = "Browse vendors and order water",
                    color = NeerlyColors.CustomerPrimary,
                    icon = "🏠",
                    onClick = { onRoleSelected("CUSTOMER", rememberChoice) }
                )
            }
            if (grantedRoles.contains("VENDOR")) {
                RoleCard(
                    title = "Vendor",
                    subtitle = "Manage your business",
                    color = NeerlyColors.VendorPrimary,
                    icon = "🏪",
                    onClick = { onRoleSelected("VENDOR", rememberChoice) }
                )
            }
            if (grantedRoles.contains("DRIVER")) {
                RoleCard(
                    title = "Driver",
                    subtitle = "Deliver for your vendor",
                    color = NeerlyColors.DriverPrimary,
                    icon = "🚚",
                    onClick = { onRoleSelected("DRIVER", rememberChoice) }
                )
            }
            if (grantedRoles.contains("ADMIN")) {
                RoleCard(
                    title = "Admin",
                    subtitle = "Platform operations",
                    color = NeerlyColors.AdminPrimary,
                    icon = "🛡️",
                    onClick = { onRoleSelected("ADMIN", rememberChoice) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { rememberChoice = !rememberChoice }
        ) {
            Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
            Text("Remember my choice", fontSize = 14.sp, color = NeerlyColors.Ink700)
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
        color = Color.White,
        modifier = Modifier.fillMaxWidth().height(88.dp),
        shadowElevation = 2.dp
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
