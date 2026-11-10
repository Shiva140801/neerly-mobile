package com.neerly.mobile.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A checklist row that reads as done at a glance (DPickup, DJarPickup): ticked rows turn
 * green with a filled box, so a driver sees what is left without reading. The whole card
 * is the touch target (≥ 64dp), announced as a checkbox.
 */
@Composable
fun CheckCard(title: String, sub: String?, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .background(if (checked) NeerlyColors.OkSoft else NeerlyColors.Paper, shape)
            .border(if (checked) 2.dp else 1.dp, if (checked) NeerlyColors.Ok else NeerlyColors.Ink200, shape)
            .toggleable(value = checked, role = Role.Checkbox) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(32.dp)
                .background(if (checked) NeerlyColors.Ok else Color.Transparent, RoundedCornerShape(8.dp))
                .border(2.dp, if (checked) NeerlyColors.Ok else NeerlyColors.Ink400, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            sub?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600) }
        }
    }
}
