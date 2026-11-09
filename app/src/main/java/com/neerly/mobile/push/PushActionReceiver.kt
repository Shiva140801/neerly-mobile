package com.neerly.mobile.push

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.neerly.mobile.R
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AppendComplaintMessageRequest
import com.neerly.mobile.data.dto.SkipSubscriptionRequest
import com.neerly.mobile.data.repo.DriverRepository
import com.neerly.mobile.feature.driver.DriverPlatformApi
import com.neerly.mobile.feature.driver.SosResolveBody
import com.neerly.mobile.feature.vendor.shop.MessageBody
import com.neerly.mobile.feature.vendor.shop.VendorShopApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Buttons that do their work without opening a screen (Pass, I'm safe, I'm back now,
 * Skip tomorrow, inline Reply) plus the ring time-out. Android 12+ forbids starting an
 * activity from here, so anything that needs a screen is an activity PendingIntent instead.
 */
@AndroidEntryPoint
class PushActionReceiver : BroadcastReceiver() {

    @Inject lateinit var api: NeerlyApi
    @Inject lateinit var drivers: DriverRepository
    @Inject lateinit var platform: DriverPlatformApi
    @Inject lateinit var shop: VendorShopApi
    @Inject lateinit var notifier: PushNotifier
    @Inject lateinit var duty: DriverDutyState

    override fun onReceive(context: Context, intent: Intent) {
        @Suppress("UNCHECKED_CAST")
        val map = (intent.getSerializableExtra(EXTRA_PAYLOAD) as? HashMap<String, String>) ?: return
        val p = PushPayload.parse(map)
        val tag = intent.getStringExtra(PushNotifier.EXTRA_TAG) ?: return
        val id = intent.getIntExtra(PushNotifier.EXTRA_ID, 0)

        if (intent.action == ACTION_REALERT) { notifier.reAlert(p, tag, id); return }
        if (intent.action == ACTION_RING_TIMEOUT) {
            // The ring screen handles its own clock; this covers the case where it never opened.
            if (!RingSession.wasAnswered(tag) && notifier.isShowing(tag, id)) notifier.postMissed(p)
            RingSession.stop(tag)
            return
        }

        val action = PushAction.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_ACTION) } ?: return
        val reply = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY)?.toString()?.trim()
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    PushAction.PASS -> {
                        RingSession.markAnswered(tag)
                        p["orderId"]?.let { drivers.passOffer(it) }
                        notifier.cancel(tag, id); RingSession.stop(tag)
                    }
                    PushAction.SAFE -> { p["sosId"]?.let { platform.sosResolve(it, SosResolveBody(null)) }; notifier.cancel(tag, id) }
                    PushAction.BACK_NOW -> { platform.endBreak(); notifier.cancel(tag, id) }
                    PushAction.TAKE_BREAK -> { platform.startBreak(); duty.markBreak(true) }
                    PushAction.SKIP_TOMORROW -> {
                        val date = p["date"] ?: LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString()
                        p["subscriptionId"]?.let { api.skipSubscription(it, SkipSubscriptionRequest(date)) }
                        notifier.cancel(tag, id)
                    }
                    PushAction.REPLY -> {
                        val ok = reply?.takeIf { it.isNotBlank() }?.let { text ->
                            val complaint = p["complaintId"] ?: return@let false
                            runCatching {
                                if (PushRole.of(p.role) == PushRole.SELLER) shop.sendMessage(complaint, MessageBody(text))
                                else api.appendComplaintMessage(complaint, AppendComplaintMessageRequest(text))
                            }.isSuccess
                        } ?: false
                        confirmReply(context, p, tag, id, ok)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.w(e, "push action %s failed", action)
            } finally {
                pending.finish()
            }
        }
    }

    /** RemoteInput leaves a spinner until the card is re-posted. */
    @SuppressLint("MissingPermission")
    private fun confirmReply(context: Context, p: PushPayload, tag: String, id: Int, ok: Boolean) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val channel = PushType.of(p.type)?.channel ?: PushChannel.CUST_ORDERS
        nm.notify(tag, id, NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(PushColors.INDIGO)
            .setContentTitle(p.title ?: context.getString(channel.title))
            .setContentText(context.getString(if (ok) R.string.push_reply_sent else R.string.push_reply_failed))
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(if (ok) 4_000 else 60_000)
            .build())
    }

    companion object {
        const val ACTION_PUSH = "com.neerly.mobile.PUSH_ACTION"
        const val ACTION_RING_TIMEOUT = "com.neerly.mobile.PUSH_RING_TIMEOUT"
        const val ACTION_REALERT = "com.neerly.mobile.PUSH_REALERT"
        const val EXTRA_ACTION = "push.action"
        const val EXTRA_PAYLOAD = "push.payload"
        const val KEY_REPLY = "push.reply"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun intent(context: Context, action: PushAction, p: PushPayload, tag: String, id: Int): Intent =
            Intent(context, PushActionReceiver::class.java)
                .setAction("$ACTION_PUSH.${action.name}")  // distinct actions keep PendingIntents apart
                .putExtra(EXTRA_ACTION, action.name)
                .putExtra(EXTRA_PAYLOAD, HashMap(p.toMap()))
                .putExtra(PushNotifier.EXTRA_TAG, tag)
                .putExtra(PushNotifier.EXTRA_ID, id)

        fun reAlertIntent(context: Context, p: PushPayload, tag: String, id: Int): Intent =
            Intent(context, PushActionReceiver::class.java)
                .setAction(ACTION_REALERT)
                .putExtra(EXTRA_PAYLOAD, HashMap(p.toMap()))
                .putExtra(PushNotifier.EXTRA_TAG, tag)
                .putExtra(PushNotifier.EXTRA_ID, id)

        fun timeoutIntent(context: Context, p: PushPayload, tag: String, id: Int): Intent =
            Intent(context, PushActionReceiver::class.java)
                .setAction(ACTION_RING_TIMEOUT)
                .putExtra(EXTRA_PAYLOAD, HashMap(p.toMap()))
                .putExtra(PushNotifier.EXTRA_TAG, tag)
                .putExtra(PushNotifier.EXTRA_ID, id)
    }
}
