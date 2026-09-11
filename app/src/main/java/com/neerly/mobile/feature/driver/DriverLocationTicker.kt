package com.neerly.mobile.feature.driver

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects GPS fixes while the driver is on shift so the send loop in
 * [DriverHomeViewModel] can broadcast them. Collection and sending are split
 * on purpose: the OS callback fires on its own schedule, and the loop decides
 * what is fresh enough to put on the wire — a fix older than [FIX_MAX_AGE_MS]
 * must read as "no location", never be restamped as now.
 */
@Singleton
class DriverLocationTicker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @Volatile
    private var last: Location? = null
    private var callback: LocationCallback? = null

    /** Caller must hold ACCESS_FINE_LOCATION before starting. */
    @SuppressLint("MissingPermission")
    fun start() {
        if (callback != null) return
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { last = it }
            }
        }
        callback = cb
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_MOVE_METERS)
            .build()
        runCatching { fused.requestLocationUpdates(request, cb, Looper.getMainLooper()) }
            .onFailure { Timber.w(it, "Could not start location updates") }
    }

    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        last = null
    }

    /** The freshest fix, or null when we have none young enough to trust. */
    fun freshFix(): Location? =
        last?.takeIf { System.currentTimeMillis() - it.time <= FIX_MAX_AGE_MS }

    /**
     * True when the vehicle reads as stationary. Android sometimes reports
     * speed 0/absent while moving, so this is only used to slow the cadence,
     * never to stop sending.
     */
    fun isParked(): Boolean {
        val fix = freshFix() ?: return false
        return !fix.hasSpeed() || fix.speed < PARKED_SPEED_MPS
    }

    fun batteryPct(): Int? =
        (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

    companion object {
        const val UPDATE_INTERVAL_MS = 5_000L
        const val MIN_MOVE_METERS = 5f
        const val FIX_MAX_AGE_MS = 30_000L
        const val PARKED_SPEED_MPS = 0.42f // ~1.5 km/h

        /** Send cadences — 10 s on the move (per ARCHITECTURE.md), 30 s when parked. */
        const val ACTIVE_SEND_MS = 10_000L
        const val PARKED_SEND_MS = 30_000L
    }
}
