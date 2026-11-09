package com.neerly.mobile.core.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.neerly.mobile.BuildConfig
import com.neerly.mobile.core.design.NeerlyRadius

/** True when a Maps key is configured — callers fall back to GPS + typed address otherwise. */
val pinPickerAvailable: Boolean get() = BuildConfig.MAPS_ENABLED

/**
 * "Drag the map so the pin sits on the gate" (boards Address, AddressPin, AddressPinEdit,
 * VShopPin). The pin is fixed in the middle and the map moves under it, so the spot the
 * customer means is always exactly under the pin's tip.
 *
 * [target] is where the pin should be when something other than a drag moves it — a GPS
 * fix or a search pick; the camera flies there. [onSettled] fires once a drag ends with
 * the point under the pin (not on every frame), so the caller can reverse-geocode it.
 */
@Composable
fun PinPicker(
    target: LatLng,
    onSettled: (lat: Double, lng: Double) -> Unit,
    description: String,
    pinColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    zoom: Float = 17f
) {
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(target, zoom) }
    var draggedByUser by remember { mutableStateOf(false) }

    // Follow outside moves (GPS, search pick) unless the camera is already there.
    LaunchedEffect(target.latitude, target.longitude) {
        val at = camera.position.target
        if (distanceMetres(at, target) > 1.0) {
            runCatching { camera.animate(CameraUpdateFactory.newLatLngZoom(target, maxOf(camera.position.zoom, zoom)), 500) }
        }
    }
    // Report where a drag settled — only drags; programmatic moves already know their spot.
    LaunchedEffect(camera.isMoving) {
        if (camera.isMoving) {
            if (camera.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) draggedByUser = true
        } else if (draggedByUser) {
            draggedByUser = false
            val p = camera.position.target
            onSettled(p.latitude, p.longitude)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(NeerlyRadius.md))
            .semantics { contentDescription = description }
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false,
                myLocationButtonEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false)
        )
        // The tip of the icon is its bottom-centre: lift it by half its height so the tip,
        // not the middle, marks the centre of the map.
        Icon(
            Icons.Filled.Place, contentDescription = null, tint = pinColor,
            modifier = Modifier.align(Alignment.Center).size(44.dp).offset(y = (-22).dp)
        )
    }
}

/** Haversine distance — enough to tell "already there" from "fly there". */
internal fun distanceMetres(a: LatLng, b: LatLng): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val h = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * Math.sin(dLng / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(h))
}
