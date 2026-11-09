package com.neerly.mobile.push

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.neerly.mobile.MainActivity
import com.neerly.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and posts the Android notification for a [PushPlanner.Planned] card. */
@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speaker: NoteSpeaker
) {
    private val nm get() = NotificationManagerCompat.from(context)

    fun cancel(tag: String, id: Int) = nm.cancel(tag, id)

    fun isShowing(tag: String, id: Int): Boolean =
        context.getSystemService(NotificationManager::class.java)?.activeNotifications
            ?.any { it.tag == tag && it.id == id } == true

    @SuppressLint("MissingPermission") // areNotificationsEnabled() is checked first
    fun post(n: PushPlanner.Planned, p: PushPayload, reAlerted: Boolean = false) {
        if (!nm.areNotificationsEnabled()) return
        val accent = PushColors.accent(n.role, n.channel)
        val title = n.title.ifBlank { context.getString(n.channel.title) }
        val b = NotificationCompat.Builder(context, n.channel.id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(accent)
            .setContentTitle(title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setPriority(n.channel.priority)
            .setGroup(n.role.groupId)
            .setAutoCancel(!n.ongoing && !n.ring)
            .setOngoing(n.ongoing || n.ring)
            .setOnlyAlertOnce(n.onlyAlertOnce)
            .setContentIntent(openIntent(n.deepLink, n.tag, n.id, 0, switchRole = n.otherRole))
            .setCategory(category(n))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
        if (n.otherRole) b.setSubText(context.getString(R.string.push_switch_role))
        n.timeoutMs?.let { b.setTimeoutAfter(it) }
        // C2 live card: a progress bar when the server sends one.
        p["progress"]?.toIntOrNull()?.takeIf { n.type == PushType.CUST_OUT_FOR_DELIVERY }?.let { b.setProgress(100, it.coerceIn(0, 100), false) }

        val visibility = n.visibility ?: n.channel.lockscreen
        b.setVisibility(visibility)
        if (visibility == NotificationCompat.VISIBILITY_PRIVATE) b.setPublicVersion(publicVersion(n, accent))

        n.actions.forEachIndexed { i, a -> action(a, n, p, i + 1)?.let(b::addAction) }

        if (n.ring) {
            val remaining = n.expiresAtMs?.let { it - System.currentTimeMillis() }
            remaining?.takeIf { it > 0 }?.let { b.setTimeoutAfter(it) }
            b.setFullScreenIntent(ringIntent(n, p), true)
            scheduleRingTimeout(n, p, remaining ?: DEFAULT_RING_MS[n.role] ?: 60_000L)
        }

        nm.notify(n.tag, n.id, b.build())
        postSummary(n.role)
        // A7: "at the gate" sounds once more after 60 s if the customer hasn't opened it.
        if (n.type == PushType.CUST_AT_GATE && !n.onlyAlertOnce && !reAlerted) scheduleGateReAlert(n, p)
        if (n.speak && !n.ring) speaker.speak("${n.title}. ${n.body}")
    }

    /** "Missed order" / "Missed task": the ring ran out (rule 1, 2). Replaces the ring card. */
    @SuppressLint("MissingPermission")
    fun postMissed(p: PushPayload) {
        if (!nm.areNotificationsEnabled()) return
        val type = PushType.of(p.type) ?: return
        val seller = type == PushType.SELLER_NEW_ORDER
        val missedType = if (seller) PushType.SELLER_MISSED_ORDER else PushType.DRIVER_MISSED_TASK
        val tag = PushPlanner.tagFor(p, type)
        val id = PushPlanner.idFor(tag)
        val amount = (if (seller) p["amount"] else p["pay"] ?: p["amount"])?.let { "₹$it" }
        val title = if (seller) amount?.let { context.getString(R.string.push_ring_missed_title, it) } ?: context.getString(R.string.push_ring_missed_title_plain)
                    else amount?.let { context.getString(R.string.push_ring_missed_task_title, it) } ?: context.getString(R.string.push_ring_missed_task_title_plain)
        val body = context.getString(if (seller) R.string.push_ring_missed_body else R.string.push_ring_missed_task_body)
        val planned = PushPlanner.Planned(type = missedType, role = missedType.role, channel = missedType.channel, tag = tag, id = id,
            title = title, body = body, publicTitle = null, publicBody = null, visibility = missedType.visibility,
            actions = missedType.defaultActions, deepLink = DeepLinks.default(missedType, p), ring = false, ongoing = false,
            timeoutMs = null, speak = false, onlyAlertOnce = true, otherRole = false, expiresAtMs = null)
        cancel(tag, id)
        post(planned, p)
    }

    private fun category(n: PushPlanner.Planned): String = when {
        n.ring -> NotificationCompat.CATEGORY_CALL
        n.channel.bypassDnd -> NotificationCompat.CATEGORY_ALARM
        n.channel == PushChannel.SELLER_COMPLAINTS || n.type == PushType.CUST_COMPLAINT_REPLY -> NotificationCompat.CATEGORY_MESSAGE
        n.type == PushType.SECURITY_NEW_SIGN_IN -> NotificationCompat.CATEGORY_ERROR
        n.ongoing -> NotificationCompat.CATEGORY_PROGRESS
        else -> NotificationCompat.CATEGORY_STATUS
    }

    private fun publicVersion(n: PushPlanner.Planned, accent: Int) =
        NotificationCompat.Builder(context, n.channel.id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(accent)
            .setContentTitle(n.publicTitle ?: n.type?.publicTitle?.let(context::getString) ?: context.getString(n.channel.title))
            .setContentText(n.publicBody ?: n.type?.publicBody?.let(context::getString) ?: context.getString(R.string.push_pub_money_body))
            .build()

    @SuppressLint("MissingPermission")
    private fun postSummary(role: PushRole) {
        val s = NotificationCompat.Builder(context, PushChannel.defaultFor(role).id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(if (role == PushRole.DRIVER) PushColors.TURMERIC else PushColors.INDIGO)
            .setContentTitle(context.getString(role.groupName))
            .setGroup(role.groupId)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSilent(true)
            .setAutoCancel(true)
            .build()
        nm.notify(SUMMARY_TAG, role.groupId.hashCode() and 0x7fffffff, s)
    }

    private fun action(a: PushAction, n: PushPlanner.Planned, p: PushPayload, index: Int): NotificationCompat.Action? {
        val label = context.getString(a.label)
        val type = n.type ?: return null
        val pi: PendingIntent = when (a.kind) {
            PushAction.Kind.OPEN -> openIntent(DeepLinks.forAction(a, type, p), n.tag, n.id, index, switchRole = n.otherRole)
            PushAction.Kind.DIAL -> PushPlanner.phoneOf(p)?.let { phone ->
                PendingIntent.getActivity(context, reqCode(n.id, index),
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), IMMUTABLE)
            } ?: openIntent(n.deepLink, n.tag, n.id, index, switchRole = n.otherRole)
            PushAction.Kind.MAP -> {
                val lat = p["lat"]; val lng = p["lng"]
                if (lat != null && lng != null) PendingIntent.getActivity(context, reqCode(n.id, index),
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), IMMUTABLE)
                else openIntent(n.deepLink, n.tag, n.id, index, switchRole = n.otherRole)
            }
            PushAction.Kind.BACKGROUND -> PendingIntent.getBroadcast(context, reqCode(n.id, index),
                PushActionReceiver.intent(context, a, p, n.tag, n.id), IMMUTABLE)
            PushAction.Kind.FOREGROUND -> PendingIntent.getActivity(context, reqCode(n.id, index),
                ringActivityIntent(n.role, p).putExtra(RingActivityBase.EXTRA_ANSWER, a.name), IMMUTABLE)
            PushAction.Kind.REPLY -> {
                val pi = PendingIntent.getBroadcast(context, reqCode(n.id, index),
                    PushActionReceiver.intent(context, a, p, n.tag, n.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0))
                return NotificationCompat.Action.Builder(R.drawable.ic_stat_drop, label, pi)
                    .addRemoteInput(RemoteInput.Builder(PushActionReceiver.KEY_REPLY).setLabel(context.getString(R.string.push_reply_hint)).build())
                    .setAllowGeneratedReplies(false)
                    .build()
            }
        }
        return NotificationCompat.Action.Builder(R.drawable.ic_stat_drop, label, pi).build()
    }

    /** Taps open MainActivity at the link; NeerlyNavHost routes it once past sign-in. */
    fun openIntent(link: String, tag: String, id: Int, index: Int, switchRole: Boolean = false): PendingIntent =
        PendingIntent.getActivity(context, reqCode(id, index), Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(link))
            .putExtra(EXTRA_TAG, tag)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_SWITCH_ROLE, switchRole)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), IMMUTABLE)

    private fun ringIntent(n: PushPlanner.Planned, p: PushPayload): PendingIntent =
        PendingIntent.getActivity(context, reqCode(n.id, 99), ringActivityIntent(n.role, p), IMMUTABLE)

    fun ringActivityIntent(role: PushRole, p: PushPayload): Intent =
        Intent(context, if (role == PushRole.DRIVER) DriverRingActivity::class.java else SellerRingActivity::class.java)
            .putExtra(RingActivityBase.EXTRA_PAYLOAD, HashMap(p.toMap()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

    /** If the ring screen never opened (full-screen intent not allowed), still turn it into "missed". */
    private fun scheduleRingTimeout(n: PushPlanner.Planned, p: PushPayload, afterMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, reqCode(n.id, 98),
            PushActionReceiver.timeoutIntent(context, p, n.tag, n.id), IMMUTABLE)
        // Inexact is fine: a missed card a few seconds late is harmless, and exact alarms need a permission.
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + afterMs, pi)
    }

    private fun scheduleGateReAlert(n: PushPlanner.Planned, p: PushPayload) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, reqCode(n.id, 97),
            PushActionReceiver.reAlertIntent(context, p, n.tag, n.id), IMMUTABLE)
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + GATE_REALERT_MS, pi)
    }

    /** Re-post the same card once so it sounds again — only if it's still in the shade. */
    fun reAlert(p: PushPayload, tag: String, id: Int) {
        if (!isShowing(tag, id)) return
        val n = (PushPlanner.decide(p.copy(op = PushPayload.Op.POST),
            PushPlanner.Env(PushRole.CUSTOMER, appForeground = false, nowMs = System.currentTimeMillis()))
            as? PushPlanner.Decision.Show)?.n ?: return
        cancel(tag, id)  // a fresh post is what makes Android sound again
        post(n, p, reAlerted = true)
    }

    fun cancelRingTimeout(id: Int, p: PushPayload, tag: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(PendingIntent.getBroadcast(context, reqCode(id, 98), PushActionReceiver.timeoutIntent(context, p, tag, id), IMMUTABLE))
    }

    companion object {
        const val EXTRA_TAG = "push.tag"
        const val EXTRA_ID = "push.id"
        const val EXTRA_SWITCH_ROLE = "push.switchRole"
        const val SUMMARY_TAG = "summary"
        private const val IMMUTABLE = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        /** Seller 3 min (orders.vendor_accept_window_sec), driver 45 s, when the push has no expiresAt. */
        val DEFAULT_RING_MS = mapOf(PushRole.SELLER to 180_000L, PushRole.DRIVER to 45_000L)

        const val GATE_REALERT_MS = 60_000L

        fun reqCode(id: Int, index: Int) = (id * 31 + index) and 0x7fffffff
    }
}
