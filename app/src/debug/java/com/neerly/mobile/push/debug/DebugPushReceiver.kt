package com.neerly.mobile.push.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neerly.mobile.push.PushRouter
import com.neerly.mobile.push.PushType
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Debug builds only. Posts a catalog push through the same [PushRouter] FCM uses:
 *
 *   adb shell am broadcast -a com.neerly.mobile.DEBUG_PUSH -p com.neerly.mobile \
 *       --es type SELLER_NEW_ORDER [--es orderId <uuid>] [--ez bg true] [--es op cancel]
 *
 * `bg true` rings as if the app were closed (full-screen) even while it is open.
 */
@AndroidEntryPoint
class DebugPushReceiver : BroadcastReceiver() {
    @Inject lateinit var router: PushRouter

    override fun onReceive(context: Context, intent: Intent) {
        val type = PushType.of(intent.getStringExtra("type")) ?: run {
            Timber.w("DEBUG_PUSH: unknown type %s — one of %s", intent.getStringExtra("type"), PushType.entries.joinToString { it.name })
            return
        }
        val map = PushSamples.payload(context, type, intent.getStringExtra("orderId")).toMutableMap()
        intent.getStringExtra("op")?.let { map["op"] = it }
        // Real ids from the caller replace the sample's placeholders, so a tap opens a real record.
        listOf("subscriptionId", "complaintId", "refundId", "vendorId", "claimId").forEach { k ->
            intent.getStringExtra(k)?.let { map[k] = it }
        }
        val decision = router.handle(map, forceBackground = intent.getBooleanExtra("bg", false))
        Timber.i("DEBUG_PUSH %s → %s", type, decision)
    }
}
