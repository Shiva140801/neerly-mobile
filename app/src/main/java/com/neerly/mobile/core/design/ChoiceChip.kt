package com.neerly.mobile.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One pick from a short set (break length, vehicle type, why short) — the boards' tiles,
 * not Material's lavender FilterChip. Picked = filled in the role accent (turmeric for
 * drivers) with a thicker edge, so it reads without colour too. ≥ 48dp tall.
 */
@Composable
fun ChoiceChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier,
               accent: Color = Boondh.Amber) {
    Surface(onClick = onClick, selected = selected, shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else NeerlyColors.Paper,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else NeerlyColors.Ink200),
        modifier = modifier.heightIn(min = 48.dp)) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = NeerlyColors.Ink900)
        }
    }
}
