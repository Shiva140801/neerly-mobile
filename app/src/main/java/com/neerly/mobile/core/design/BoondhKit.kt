package com.neerly.mobile.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Patterns the redesign boards repeat across screens. Screens used generic cards and
 * Material defaults where the boards draw rows, pill chips and serif section headers —
 * the visual RCA's most common cause — so these live in one place.
 */

/** Board Home header: "DELIVERING TO / Home · Road No. 12 ⌄" with the bell on the right. */
@Composable
fun AddressHeader(
    caption: String,
    line: String,
    onAddress: () -> Unit,
    bellLabel: String,
    unread: Int,
    onBell: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = onAddress).padding(vertical = 4.dp)) {
            Text(caption.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = NeerlyColors.Ink600)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(line, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = NeerlyColors.Ink900, modifier = Modifier.size(22.dp))
            }
        }
        BellButton(bellLabel, unread, onBell)
    }
}

/** 48dp round bell; a red dot when anything is unread. */
@Composable
fun BellButton(label: String, unread: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(NeerlyColors.Canvas).border(1.dp, NeerlyColors.Ink200, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Notifications, null, tint = NeerlyColors.Ink900, modifier = Modifier.size(22.dp))
        if (unread > 0) Box(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 12.dp).size(9.dp)
            .background(NeerlyColors.Err, RoundedCornerShape(2.dp)))
    }
}

/** Serif section title with an optional quiet count on the right ("Suppliers near you · 12 open now"). */
@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f).semantics { heading() }, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp, color = NeerlyColors.Ink900)
        trailing?.let { Text(it, fontSize = 14.sp, color = NeerlyColors.Ink600, modifier = Modifier.padding(bottom = 3.dp)) }
    }
}

/** Pill chip: ink fill when selected, hairline outline otherwise. */
@Composable
fun BoondhChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NeerlyRadius.pill),
        color = if (selected) NeerlyColors.Ink900 else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, NeerlyColors.Ink300),
        modifier = Modifier.heightIn(min = 44.dp).semantics { if (selected) contentDescription = "$label, selected" }
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) NeerlyColors.Paper else NeerlyColors.Ink900)
        }
    }
}

/** Search box that opens the search screen: magnifier + placeholder on paper. */
@Composable
fun SearchField(placeholder: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .background(NeerlyColors.Paper, RoundedCornerShape(NeerlyRadius.md))
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.Search, null, tint = NeerlyColors.Ink900, modifier = Modifier.size(22.dp))
        Text(placeholder, fontSize = 15.sp, color = NeerlyColors.Ink600)
    }
}

/** A soft coloured strip: icon, one-line message, optional trailing action (board Home subscription strip). */
@Composable
fun StripCard(
    tint: Color = NeerlyColors.WarnSoft,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(tint, RoundedCornerShape(NeerlyRadius.md))
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
        trailing?.invoke()
    }
}

/** A list row separated by hairlines rather than boxed in a card (boards Home, Orders, Profile). */
@Composable
fun ListRow(onClick: (() -> Unit)? = null, divider: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), content = content
        )
        if (divider) HorizontalDivider(color = NeerlyColors.Ink200)
    }
}

/** Square initials tile used for shops ("SB"), serif like the boards. */
@Composable
fun InitialsTile(initials: String, bg: Color = NeerlyColors.CustomerSoft, fg: Color = NeerlyColors.CustomerPrimary, size: Dp = 56.dp) {
    Box(Modifier.size(size).background(bg, RoundedCornerShape(NeerlyRadius.md)), contentAlignment = Alignment.Center) {
        Text(initials, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp, color = fg)
    }
}

/** Small ink tag ("NEW"). */
@Composable
fun InkTag(text: String) {
    Box(Modifier.background(NeerlyColors.Ink900, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = NeerlyColors.Paper)
    }
}

/** A 20L jar glyph (cap + neck + body + water line) — drawn, so it themes; [outline] for light backgrounds. */
@Composable
fun JarGlyph(modifier: Modifier = Modifier, body: Color = Color.White, water: Color = NeerlyColors.Water100,
             cap: Color = Boondh.Amber, outline: Color? = null) {
    Canvas(modifier.size(width = 26.dp, height = 36.dp)) {
        val w = size.width; val h = size.height
        val stroke = outline?.let { androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f) }
        drawRoundRect(cap, Offset(w * 0.34f, 0f), Size(w * 0.32f, h * 0.10f), CornerRadius(2f, 2f))
        drawRoundRect(body, Offset(w * 0.40f, h * 0.08f), Size(w * 0.20f, h * 0.12f))
        drawRoundRect(body, Offset(0f, h * 0.18f), Size(w, h * 0.82f), CornerRadius(w * 0.28f, w * 0.28f))
        drawRoundRect(water, Offset(w * 0.12f, h * 0.48f), Size(w * 0.76f, h * 0.42f), CornerRadius(w * 0.18f, w * 0.18f))
        if (outline != null && stroke != null) {
            drawRoundRect(outline, Offset(w * 0.40f, h * 0.08f), Size(w * 0.20f, h * 0.12f), style = stroke)
            drawRoundRect(outline, Offset(0f, h * 0.18f), Size(w, h * 0.82f), CornerRadius(w * 0.28f, w * 0.28f), style = stroke)
        }
    }
}
