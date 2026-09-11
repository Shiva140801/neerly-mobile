package com.neerly.mobile.feature.order

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.data.dto.OrderTrackingResponse
import java.time.Instant

/**
 * The live map card shown while the order is DISPATCHED/ARRIVING: driver marker,
 * drop (and pickup) pins, a straight guide line driver→door, and a freshness chip.
 *
 * Camera policy (borrowed from a tracking app that learned it the hard way):
 * frame all pins once when the map opens, then follow the driver with at most
 * one camera move per [FOLLOW_MS] — and stand down for [PAN_GRACE_MS] after the
 * customer pans, so the map never fights their finger.
 */
@Composable
fun OrderLiveMap(tracking: OrderTrackingResponse, modifier: Modifier = Modifier) {
    val driver = tracking.driverLat?.let { lat -> tracking.driverLng?.let { LatLng(lat, it) } }
    val drop = tracking.dropLat?.let { lat -> tracking.dropLng?.let { LatLng(lat, it) } }
    val pickup = tracking.pickupLat?.let { lat -> tracking.pickupLng?.let { LatLng(lat, it) } }
    val points = listOfNotNull(driver, drop, pickup)
    if (points.isEmpty()) return

    val cameraPositionState = rememberCameraPositionState()
    var framed by remember { mutableLongStateOf(0L) }
    var lastFollowAt by remember { mutableLongStateOf(0L) }
    var pannedAt by remember { mutableLongStateOf(0L) }

    // A user pan pauses following; detect it from the camera's own move reason.
    LaunchedEffect(Unit) {
        snapshotFlow { cameraPositionState.cameraMoveStartedReason }
            .collect { if (it == CameraMoveStartedReason.GESTURE) pannedAt = System.currentTimeMillis() }
    }

    // Frame once when the pin set first becomes known — an initial camera position
    // computed before data arrives would open the map on 0,0 in the Gulf of Guinea.
    LaunchedEffect(points.size) {
        if (framed != 0L) return@LaunchedEffect
        framed = System.currentTimeMillis()
        runCatching {
            if (points.size == 1) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
            } else {
                val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 90))
            }
        }
    }

    LaunchedEffect(driver?.latitude, driver?.longitude) {
        val d = driver ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - pannedAt < PAN_GRACE_MS) return@LaunchedEffect
        if (now - lastFollowAt < FOLLOW_MS) return@LaunchedEffect
        lastFollowAt = now
        runCatching { cameraPositionState.animate(CameraUpdateFactory.newLatLng(d), 600) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(NeerlyRadius.md))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            if (driver != null && drop != null) {
                Polyline(
                    points = listOf(driver, drop),
                    color = NeerlyColors.CustomerPrimary,
                    width = 8f,
                    geodesic = true
                )
            }
            pickup?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Pickup",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
            drop?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Your address",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
            }
            // Driver drawn last so nothing occludes it.
            driver?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = tracking.driverName ?: "Driver",
                    snippet = tracking.vehicle,
                    rotation = tracking.driverHeadingDeg?.toFloat() ?: 0f,
                    flat = tracking.driverHeadingDeg != null,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }
        FreshnessChip(
            recordedAt = tracking.driverLocationAt,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        )
    }
}

/**
 * Freshness is first-class UI state: a marker is only trustworthy with a caption
 * saying how old it is. <60 s reads as live, up to 5 min as ageing, beyond that
 * (or with no fix at all) the wording flips so nobody waits at the gate for a
 * bus-stop marker from twenty minutes ago.
 */
@Composable
private fun FreshnessChip(recordedAt: String?, modifier: Modifier = Modifier) {
    val ageSec = recordedAt
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.let { (System.currentTimeMillis() - it.toEpochMilli()) / 1000 }
    val (label, fg, bg) = when {
        ageSec == null -> Triple("Locating driver…", NeerlyColors.Ink500, NeerlyColors.Ink100)
        ageSec < 60 -> Triple("LIVE", NeerlyColors.Ok, NeerlyColors.OkSoft)
        ageSec < 300 -> Triple("Updated ${ageSec / 60} min ago", NeerlyColors.Warn, NeerlyColors.WarnSoft)
        else -> Triple("Last seen ${ageSec / 60} min ago", NeerlyColors.Ink500, NeerlyColors.Ink100)
    }
    Surface(color = bg, shape = RoundedCornerShape(NeerlyRadius.pill), modifier = modifier) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

private const val FOLLOW_MS = 5_000L
private const val PAN_GRACE_MS = 12_000L
