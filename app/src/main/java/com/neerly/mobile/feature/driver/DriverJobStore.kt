package com.neerly.mobile.feature.driver

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the server doesn't track about a drop in progress: whether the driver ticked the load
 * checklist (D04). `POST /orders/{id}/start` leaves the status DISPATCHED either way, so without
 * this a restarted app would send the driver back to the shop checklist mid-ride.
 */
interface DriverJobStore {
    fun isLoaded(orderId: String): Boolean
    fun markLoaded(orderId: String)
    /** Forget finished drops so the set doesn't grow forever. */
    fun keepOnly(orderIds: Set<String>)
    /** When "I'm at the door" was tapped on this phone — the not-home wait timer (D22) starts there. */
    fun markArrived(orderId: String, atMs: Long) {}
    fun arrivedAt(orderId: String): Long? = null
}

@Singleton
class PrefsDriverJobStore @Inject constructor(@ApplicationContext context: Context) : DriverJobStore {
    private val prefs = context.getSharedPreferences("driver_jobs", Context.MODE_PRIVATE)
    private fun loaded(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()

    override fun isLoaded(orderId: String) = orderId in loaded()
    override fun markLoaded(orderId: String) { prefs.edit().putStringSet(KEY, loaded() + orderId).apply() }
    override fun keepOnly(orderIds: Set<String>) {
        val kept = loaded().intersect(orderIds)
        if (kept.size != loaded().size) prefs.edit().putStringSet(KEY, kept).apply()
        prefs.all.keys.filter { it.startsWith(ARRIVED) && it.removePrefix(ARRIVED) !in orderIds }
            .takeIf { it.isNotEmpty() }?.let { stale -> prefs.edit().apply { stale.forEach(::remove) }.apply() }
    }
    override fun markArrived(orderId: String, atMs: Long) { prefs.edit().putLong(ARRIVED + orderId, atMs).apply() }
    override fun arrivedAt(orderId: String): Long? = prefs.getLong(ARRIVED + orderId, -1).takeIf { it > 0 }

    private companion object { const val KEY = "loaded"; const val ARRIVED = "arrived:" }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class DriverJobStoreModule {
    @dagger.Binds abstract fun bind(impl: PrefsDriverJobStore): DriverJobStore
}
