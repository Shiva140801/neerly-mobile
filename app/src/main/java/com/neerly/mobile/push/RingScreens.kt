package com.neerly.mobile.push

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R

/** Ring-screen palette from NPVRing / NPDRing. */
private object RingColors {
    val SellerBg = Color(0xFF1F2C66)
    val DriverBg = Color(0xFF1A2238)
    val Paper = Color(0xFFFFFCF6)
    val Muted = Color(0xFFC9CEE6)
    val Turmeric = Color(0xFFE6A321)
    val Ink = Color(0xFF1A2238)
    val Red = Color(0xFFB23A2B)
    val AvatarBg = Color(0xFFE3E6F3)
    val Indigo = Color(0xFF233580)
    val Track = Color(0xFF2C3350)
    val PassBorder = Color(0xFF5664A3)
}

private fun mmss(ms: Long): String {
    val s = ((ms + 999) / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

private fun initials(name: String?): String =
    name?.split(' ', '.')?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.take(1).uppercase() }?.ifBlank { null } ?: "B"

@Composable
fun SellerRingScreen(
    p: PushPayload, leftMs: Long, totalMs: Long, working: Boolean, problem: String?,
    onAccept: () -> Unit, onDecline: () -> Unit
) {
    val urgent = leftMs < 30_000
    val ringColor = if (urgent) RingColors.Red else RingColors.Turmeric
    val customer = p["customerFirst"]
    val amount = p["amount"]?.let { "₹$it" } ?: p.title.orEmpty()
    val km = p["km"]?.let { "$it km" }
    val left = stringResource(R.string.push_ring_left, mmss(leftMs))
    Column(
        Modifier.fillMaxSize().background(RingColors.SellerBg).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(RingColors.SellerBg, CircleShape), contentAlignment = Alignment.Center) {
                DropMark(RingColors.Turmeric, 10.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.push_ring_seller_label), color = RingColors.Muted, fontSize = 14.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The ring is the clock (note 3): drains over 3 minutes, red under 30 s.
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                val fraction = (leftMs.toFloat() / totalMs).coerceIn(0f, 1f)
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2
                    drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset),
                        Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
                    drawArc(ringColor, -90f, 360f * fraction, false, Offset(inset, inset),
                        Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Box(Modifier.size(120.dp).background(RingColors.AvatarBg, CircleShape), contentAlignment = Alignment.Center) {
                    Text(initials(customer), color = RingColors.Indigo, fontSize = 40.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                }
            }
            // Money, then distance, then items (note 2).
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(amount, color = RingColors.Paper, fontSize = 46.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, lineHeight = 46.sp)
                listOfNotNull(customer, km).joinToString(" · ").ifBlank { null }?.let {
                    Text(it, color = RingColors.Paper, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                val detail = listOfNotNull(
                    listOfNotNull(p["area"], p["items"]).joinToString(" · ").ifBlank { null },
                    listOfNotNull(p["paidLabel"], p["earning"]?.let { stringResource(R.string.push_ring_you_keep, "₹$it") }).joinToString(" · ").ifBlank { null }
                ).joinToString("\n").ifBlank { p.body }
                detail?.let { Text(it, color = RingColors.Muted, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center) }
            }
            Text(problem ?: left, color = if (problem != null) RingColors.Paper else ringColor, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Row(Modifier.fillMaxWidth().padding(start = 44.dp, end = 44.dp, bottom = 54.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            CallButton(stringResource(R.string.push_a_decline), RingColors.Red, Color.White, enabled = !working, onClick = onDecline) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            CallButton(if (working) stringResource(R.string.push_ring_working) else stringResource(R.string.push_a_accept),
                RingColors.Turmeric, RingColors.Ink, enabled = !working, onClick = onAccept) {
                Icon(Icons.Filled.Check, null, tint = RingColors.Ink, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun CallButton(label: String, bg: Color, fg: Color, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        Modifier.semantics { role = Role.Button; contentDescription = label }.clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(76.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) { icon() }
        Text(label, color = RingColors.Paper, fontSize = 14.sp)
    }
}

@Composable
fun DriverRingScreen(
    p: PushPayload, leftMs: Long, totalMs: Long, working: Boolean, problem: String?,
    onTake: () -> Unit, onPass: () -> Unit
) {
    val pay = (p["pay"] ?: p["amount"])?.let { "₹$it" } ?: p.title.orEmpty()
    val tripKm = p["tripKm"] ?: p["km"]
    Column(Modifier.fillMaxSize().background(RingColors.DriverBg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).background(RingColors.Turmeric, CircleShape), contentAlignment = Alignment.Center) {
                DropMark(RingColors.Ink, 11.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.push_ring_driver_label), color = RingColors.Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
            // Time is shown twice: big digits and a bar (note 3).
            Text(mmss(leftMs), color = RingColors.Turmeric, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp).fillMaxWidth().height(6.dp).background(RingColors.Track, RoundedCornerShape(3.dp))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((leftMs.toFloat() / totalMs).coerceIn(0f, 1f)).background(RingColors.Turmeric, RoundedCornerShape(3.dp)))
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp), verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically)) {
            // One number, as big as it gets (note 1).
            Row(verticalAlignment = Alignment.Bottom) {
                Text(pay, color = RingColors.Paper, fontSize = 84.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, lineHeight = 80.sp)
                tripKm?.let {
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.push_ring_for_km, it), color = RingColors.Muted, fontSize = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            // A route, not a map (note 2).
            val shop = p["shopName"]
            val drop = p["dropArea"]
            if (shop != null || drop != null) Column {
                RouteStop(shop, p["shopKm"]?.let { "$it km" }, start = true)
                Box(Modifier.padding(start = 7.dp).width(3.dp).height(28.dp).background(RingColors.PassBorder.copy(alpha = 0.6f)))
                RouteStop(drop, p["dropKm"]?.let { "$it km" }, start = false)
            } else p.body?.let { Text(it, color = RingColors.Paper, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            listOfNotNull(p["items"], p["floorNote"]).joinToString("\n").ifBlank { null }?.let {
                Text(it, color = RingColors.Muted, fontSize = 17.sp, lineHeight = 25.sp)
            }
            problem?.let { Text(it, color = RingColors.Paper, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        }
        // Take it fills the thumb zone (note 4): 96 dp; Pass smaller but glove-sized.
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 96.dp).background(RingColors.Turmeric, RoundedCornerShape(26.dp))
                .semantics { role = Role.Button }.clickable(enabled = !working, onClick = onTake), contentAlignment = Alignment.Center) {
                Text(if (working) stringResource(R.string.push_ring_working) else stringResource(R.string.push_a_take),
                    color = RingColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).border(2.dp, RingColors.PassBorder, RoundedCornerShape(22.dp))
                .semantics { role = Role.Button }.clickable(enabled = !working, onClick = onPass), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.push_a_pass), color = RingColors.Paper, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RouteStop(name: String?, km: String?, start: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
            if (start) Box(Modifier.size(14.dp).border(3.dp, RingColors.Turmeric, CircleShape))
            else Box(Modifier.size(14.dp).background(RingColors.Turmeric, RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.width(12.dp))
        Text(name.orEmpty(), color = RingColors.Paper, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        km?.let { Text(it, color = RingColors.Muted, fontSize = 17.sp) }
    }
}

/** The Boondh drop mark. */
@Composable
private fun DropMark(color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2, 0f)
            cubicTo(w / 2, 0f, 0f, h * 0.45f, 0f, h * 0.63f)
            cubicTo(0f, h * 0.85f, w * 0.22f, h, w / 2, h)
            cubicTo(w * 0.78f, h, w, h * 0.85f, w, h * 0.63f)
            cubicTo(w, h * 0.45f, w / 2, 0f, w / 2, 0f)
            close()
        }
        drawPath(path, color)
    }
}
