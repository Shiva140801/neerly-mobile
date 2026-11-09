package com.neerly.mobile.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.neerly.mobile.R
import com.neerly.mobile.core.design.rupees
import com.neerly.mobile.feature.driver.DriverLocationTicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * The driver's one ongoing card for the whole shift (NPDLock, rule 3): a location foreground
 * service so GPS keeps flowing with the screen off. It updates silently after each drop and
 * once a minute for the time on duty, and can't be swiped away until the shift ends — then
 * it becomes a swipeable "Shift ended · hand ₹… to the shop".
 */
@AndroidEntryPoint
class DriverDutyService : Service() {
    @Inject lateinit var duty: DriverDutyState
    @Inject lateinit var ticker: DriverLocationTicker
    @Inject lateinit var notifier: PushNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watch: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            val cash = intent.getStringExtra(EXTRA_CASH)?.toBigDecimalOrNull()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            postEnded(cash)
            stopSelf()
            return START_NOT_STICKY
        }
        val ok = runCatching {
            ServiceCompat.startForeground(this, NOTIF_ID, build(duty.card.value),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        }.onFailure { Timber.w(it, "duty service couldn't start in the foreground") }.isSuccess
        if (!ok) { stopSelf(); return START_NOT_STICKY }
        if (hasLocation()) ticker.start()
        if (watch?.isActive != true) watch = scope.launch {
            launch { duty.card.collect { refresh() } }
            while (isActive) { delay(60_000); refresh() }  // "On duty · 4h 20m" ticks without sound
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun refresh() {
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) nm.notify(NOTIF_ID, build(duty.card.value))
    }

    private fun hasLocation() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun build(card: DutyCard?): Notification {
        val title = when {
            card == null || card.dropsTotal == 0 -> getString(R.string.push_duty_title_none)
            else -> getString(R.string.push_duty_title, card.dropsDone, card.dropsTotal)
        }
        val mins = card?.startedAtMs?.let { ((System.currentTimeMillis() - it) / 60_000).coerceAtLeast(0) } ?: 0
        val line = getString(R.string.push_duty_line, getString(R.string.push_duty_hm, (mins / 60).toInt(), (mins % 60).toInt()),
            (card?.cashInHand ?: BigDecimal.ZERO).rupees())
        val next = card?.nextCustomer?.let { getString(R.string.push_duty_next, it) }
        val text = listOfNotNull(line, next).joinToString("\n")
        val open = notifier.openIntent("${DeepLinks.SCHEME}driver", TAG, NOTIF_ID, 0)
        val b = NotificationCompat.Builder(this, PushChannel.DRIVER_DUTY.id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(PushColors.TURMERIC)
            .setContentTitle(title)
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSubText(card?.shopName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setGroup(PushRole.DRIVER.groupId)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // Cash and next customer are readable on the lock screen on purpose (NPDLock).
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
        if (card?.nextLat != null && card.nextLng != null) {
            val nav = PendingIntent.getActivity(this, 1, Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=${card.nextLat},${card.nextLng}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            b.addAction(R.drawable.ic_stat_drop, getString(R.string.push_duty_navigate), nav)
        }
        if (card != null && !card.onBreak) {
            val p = PushPayload(v = 1, type = null, role = "DRIVER")
            b.addAction(R.drawable.ic_stat_drop, getString(R.string.push_duty_break),
                PendingIntent.getBroadcast(this, 2, PushActionReceiver.intent(this, PushAction.TAKE_BREAK, p, TAG, NOTIF_ID),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        return b.build()
    }

    @SuppressLint("MissingPermission")
    private fun postEnded(cash: BigDecimal?) {
        val nm = NotificationManagerCompat.from(this)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(this, PushChannel.DRIVER_QUIET.id)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(PushColors.TURMERIC)
            .setContentTitle(getString(R.string.push_duty_ended_title))
            .setContentText(cash?.takeIf { it.signum() > 0 }?.let { getString(R.string.push_duty_ended_body, it.rupees()) })
            .setGroup(PushRole.DRIVER.groupId)
            .setAutoCancel(true)
            .setContentIntent(notifier.openIntent("${DeepLinks.SCHEME}driver", TAG_ENDED, NOTIF_ID, 0))
            .build()
        nm.notify(TAG_ENDED, NOTIF_ID, n)
    }

    companion object {
        const val NOTIF_ID = 4201
        const val TAG = "duty"
        const val TAG_ENDED = "duty:ended"
        private const val ACTION_END = "com.neerly.mobile.DUTY_END"
        private const val EXTRA_CASH = "cash"

        /** Start (or refresh) the on-duty card. Must be called while the app is in the foreground. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, DriverDutyService::class.java)) }
                .onFailure { Timber.w(it, "couldn't start duty service") }
        }

        /** Shift over: the ongoing card becomes the swipeable "Shift ended" one. */
        fun end(context: Context, cashInHand: BigDecimal?) {
            runCatching {
                context.startService(Intent(context, DriverDutyService::class.java).setAction(ACTION_END)
                    .putExtra(EXTRA_CASH, cashInHand?.toPlainString()))
            }
        }
    }
}
