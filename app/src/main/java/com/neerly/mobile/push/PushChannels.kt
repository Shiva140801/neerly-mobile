package com.neerly.mobile.push

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.neerly.mobile.R

/** Design loudness levels (catalog column "loudness") — each maps to one Android importance. */
enum class Loudness { RINGS, CRITICAL, HIGH, DEFAULT, QUIET, ONGOING }

/** The three app "faces" — the designs show them as separate apps; here they are channel groups. */
enum class PushRole(val groupId: String, @StringRes val groupName: Int) {
    CUSTOMER("grp_customer", R.string.push_grp_customer),
    SELLER("grp_seller", R.string.push_grp_seller),
    DRIVER("grp_driver", R.string.push_grp_driver);

    companion object {
        /** Server role string → face. Staff and managers of a shop get the seller face. */
        fun of(role: String?): PushRole? = when (role?.uppercase()) {
            "CUSTOMER" -> CUSTOMER
            "VENDOR_OWNER", "VENDOR_STAFF", "VENDOR_MANAGER", "VENDOR" -> SELLER
            "DRIVER" -> DRIVER
            else -> null
        }
    }
}

/**
 * Every channel the app posts to (plan §b). Importance can't change after a channel exists,
 * so ids carry a version: a change means a new id plus an entry in [RETIRED].
 */
enum class PushChannel(
    val id: String,
    val role: PushRole,
    val loudness: Loudness,
    @StringRes val title: Int,
    /** Lock-screen visibility for the whole channel; rows can still go stricter. */
    val lockscreen: Int = NotificationCompat.VISIBILITY_PUBLIC,
    val bypassDnd: Boolean = false,
    /** True where the board turns sound off by default (offers). */
    val startsBlocked: Boolean = false
) {
    CUST_ORDERS("cust_orders_v1", PushRole.CUSTOMER, Loudness.DEFAULT, R.string.push_ch_cust_orders),
    CUST_GATE("cust_gate_v1", PushRole.CUSTOMER, Loudness.HIGH, R.string.push_ch_cust_gate),
    CUST_ALERTS("cust_alerts_v1", PushRole.CUSTOMER, Loudness.HIGH, R.string.push_ch_cust_alerts),
    CUST_LIVE("cust_live_v1", PushRole.CUSTOMER, Loudness.ONGOING, R.string.push_ch_cust_live),
    CUST_SUBS("cust_subs_v1", PushRole.CUSTOMER, Loudness.DEFAULT, R.string.push_ch_cust_subs),
    CUST_JARS("cust_jars_v1", PushRole.CUSTOMER, Loudness.DEFAULT, R.string.push_ch_cust_jars),
    CUST_MONEY("cust_money_v1", PushRole.CUSTOMER, Loudness.DEFAULT, R.string.push_ch_cust_money),
    CUST_OFFERS("cust_offers_v1", PushRole.CUSTOMER, Loudness.QUIET, R.string.push_ch_cust_offers, startsBlocked = true),
    CUST_SECURITY("cust_security_v1", PushRole.CUSTOMER, Loudness.HIGH, R.string.push_ch_cust_security),

    SELLER_RING("seller_ring_v1", PushRole.SELLER, Loudness.RINGS, R.string.push_ch_seller_ring),
    SELLER_COMPLAINTS("seller_complaints_v1", PushRole.SELLER, Loudness.HIGH, R.string.push_ch_seller_complaints,
        lockscreen = NotificationCompat.VISIBILITY_PRIVATE),
    SELLER_TEAM("seller_team_v1", PushRole.SELLER, Loudness.HIGH, R.string.push_ch_seller_team),
    SELLER_ORDERS("seller_orders_v1", PushRole.SELLER, Loudness.DEFAULT, R.string.push_ch_seller_orders),
    SELLER_MONEY("seller_money_v1", PushRole.SELLER, Loudness.DEFAULT, R.string.push_ch_seller_money,
        lockscreen = NotificationCompat.VISIBILITY_PRIVATE),
    SELLER_MONEY_QUIET("seller_money_quiet_v1", PushRole.SELLER, Loudness.QUIET, R.string.push_ch_seller_money_quiet,
        lockscreen = NotificationCompat.VISIBILITY_PRIVATE),
    SELLER_QUIET("seller_quiet_v1", PushRole.SELLER, Loudness.QUIET, R.string.push_ch_seller_quiet),
    SELLER_SOS("seller_sos_v1", PushRole.SELLER, Loudness.CRITICAL, R.string.push_ch_seller_sos, bypassDnd = true),

    DRIVER_RING("driver_ring_v1", PushRole.DRIVER, Loudness.RINGS, R.string.push_ch_driver_ring),
    DRIVER_TASKS("driver_tasks_v1", PushRole.DRIVER, Loudness.HIGH, R.string.push_ch_driver_tasks),
    DRIVER_INFO("driver_info_v1", PushRole.DRIVER, Loudness.DEFAULT, R.string.push_ch_driver_info),
    DRIVER_DAYEND("driver_dayend_v1", PushRole.DRIVER, Loudness.HIGH, R.string.push_ch_driver_dayend),
    DRIVER_DUTY("driver_duty_v1", PushRole.DRIVER, Loudness.ONGOING, R.string.push_ch_driver_duty),
    DRIVER_QUIET("driver_quiet_v1", PushRole.DRIVER, Loudness.QUIET, R.string.push_ch_driver_quiet),
    DRIVER_SOS("driver_sos_v1", PushRole.DRIVER, Loudness.CRITICAL, R.string.push_ch_driver_sos, bypassDnd = true);

    /** NotificationCompat priority for pre-O devices (minSdk 26 means channels always win; kept for heads-up). */
    val priority: Int get() = when (loudness) {
        Loudness.RINGS, Loudness.CRITICAL -> NotificationCompat.PRIORITY_MAX
        Loudness.HIGH -> NotificationCompat.PRIORITY_HIGH
        Loudness.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
        Loudness.QUIET, Loudness.ONGOING -> NotificationCompat.PRIORITY_LOW
    }

    companion object {
        fun byId(id: String): PushChannel? = entries.firstOrNull { it.id == id }

        /** Older channel ids to delete on upgrade (none yet). */
        val RETIRED: List<String> = emptyList()

        /** Default channel of each face, for unknown types and newer payload versions. */
        fun defaultFor(role: PushRole) = when (role) {
            PushRole.CUSTOMER -> CUST_ORDERS
            PushRole.SELLER -> SELLER_ORDERS
            PushRole.DRIVER -> DRIVER_INFO
        }

        /** Creates groups and channels; safe to call on every start (existing ones are left alone). */
        fun ensure(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            PushRole.entries.forEach { nm.createNotificationChannelGroup(NotificationChannelGroup(it.groupId, context.getString(it.groupName))) }
            RETIRED.forEach { nm.deleteNotificationChannel(it) }
            entries.forEach { c -> nm.createNotificationChannel(c.build(context)) }
        }

        private fun PushChannel.build(context: Context): NotificationChannel {
            val importance = when (loudness) {
                Loudness.RINGS, Loudness.CRITICAL, Loudness.HIGH -> NotificationManager.IMPORTANCE_HIGH
                Loudness.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                Loudness.QUIET, Loudness.ONGOING -> NotificationManager.IMPORTANCE_LOW
            }.let { if (startsBlocked) NotificationManager.IMPORTANCE_LOW else it }
            return NotificationChannel(id, context.getString(title), importance).apply {
                group = role.groupId
                lockscreenVisibility = lockscreen
                setShowBadge(loudness != Loudness.ONGOING)
                when (loudness) {
                    // The ring screens play and loop the ringtone themselves; the channel only
                    // needs to be loud enough to fire the full-screen intent.
                    Loudness.RINGS -> {
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        enableVibration(true)
                        vibrationPattern = RING_VIBRATION
                    }
                    Loudness.CRITICAL -> {
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 1000, 300, 1000, 300, 1000)
                        enableLights(true)
                        lightColor = 0xFFB23A2B.toInt()
                        // Only SOS may come through Do Not Disturb (rule 4); needs notification-policy access.
                        setBypassDnd(bypassDnd)
                    }
                    Loudness.HIGH -> enableVibration(true)
                    Loudness.DEFAULT -> {}
                    Loudness.QUIET, Loudness.ONGOING -> { setSound(null, null); enableVibration(false) }
                }
            }
        }

        /** Long, short, short (NPVRing). */
        val RING_VIBRATION = longArrayOf(0, 900, 250, 300, 250, 300, 900)
    }
}
