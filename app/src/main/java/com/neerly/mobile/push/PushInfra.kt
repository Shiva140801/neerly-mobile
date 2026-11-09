package com.neerly.mobile.push

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Is any of our activities started? (rule 1: app open → in-app card, no ring.) */
object AppForeground : Application.ActivityLifecycleCallbacks {
    @Volatile private var started = 0
    /** Ring screens don't count — a second push while one rings should still ring. */
    @Volatile private var startedNonRing = 0

    val isForeground: Boolean get() = startedNonRing > 0

    fun install(app: Application) = app.registerActivityLifecycleCallbacks(this)

    override fun onActivityStarted(activity: Activity) {
        started++
        if (activity !is RingActivity) startedNonRing++
    }

    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (activity !is RingActivity) startedNonRing = (startedNonRing - 1).coerceAtLeast(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

/** Marker for the two full-screen ring activities. */
interface RingActivity

/**
 * A screen a notification asked to open. MainActivity's NavHost takes it once the user is
 * past sign-in, switching role first when the link belongs to another face.
 */
@Singleton
class DeepLinkBus @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun open(route: String) { _pending.value = route }
    fun consume(route: String) { if (_pending.value == route) _pending.value = null }
}

/** Orders whose new-order screen should open on the "Can't take" reason sheet. */
object PendingDecline {
    private val ids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    fun add(orderId: String) { ids += orderId }
    fun take(orderId: String): Boolean = ids.remove(orderId)
}

/** Accent colours from the brief (rule 13). */
object PushColors {
    const val INDIGO = 0xFF233580.toInt()
    const val TURMERIC = 0xFFE6A321.toInt()
    const val SOS_RED = 0xFFB23A2B.toInt()

    fun accent(role: PushRole, channel: PushChannel): Int = when {
        channel.bypassDnd -> SOS_RED
        role == PushRole.DRIVER -> TURMERIC
        else -> INDIGO
    }
}
