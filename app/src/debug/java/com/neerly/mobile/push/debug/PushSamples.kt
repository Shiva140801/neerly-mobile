package com.neerly.mobile.push.debug

import android.content.Context
import com.neerly.mobile.push.PushPayload
import com.neerly.mobile.push.PushRole
import com.neerly.mobile.push.PushType
import java.time.Instant

/**
 * A realistic FCM payload for every catalog row (debug only). Wording comes from the
 * catalogs; ids can be real ones so Accept / Take it / Reply hit the local backend.
 */
object PushSamples {
    fun payload(context: Context, type: PushType, orderId: String? = null, extra: Map<String, String> = emptyMap()): Map<String, String> {
        val order = orderId?.ifBlank { null } ?: "lab-order-1"
        val role = when (type.role) { PushRole.CUSTOMER -> "CUSTOMER"; PushRole.SELLER -> "VENDOR_OWNER"; PushRole.DRIVER -> "DRIVER" }
        val data = mutableMapOf("orderId" to order)
        when (type) {
            PushType.SELLER_NEW_ORDER -> data += mapOf("amount" to "460", "earning" to "406", "customerFirst" to "Rahul V.",
                "km" to "2.3", "area" to "Jubilee Hills", "items" to "5 Plain + 2 Chilled, swap", "paidLabel" to "Paid online")
            PushType.SELLER_MISSED_ORDER -> data += mapOf("amount" to "240", "missedThisWeek" to "3")
            PushType.DRIVER_NEW_TASK -> data += mapOf("pay" to "42", "tripKm" to "2.5", "shopName" to "Sri Balaji Water", "shopKm" to "0.4",
                "dropArea" to "Road No. 12", "dropKm" to "2.1", "items" to "2 Plain jars + 1 Mineral case", "floorNote" to "2nd floor, no lift")
            PushType.DRIVER_MISSED_TASK -> data += mapOf("pay" to "42")
            PushType.CUST_OUT_FOR_DELIVERY -> data += mapOf("driverName" to "Ramesh", "etaMin" to "12", "progress" to "60", "driverPhone" to "+919100027452")
            PushType.CUST_AT_GATE -> data += mapOf("driverName" to "Ramesh", "driverPhone" to "+919100027452")
            PushType.DRIVER_NOTE_CHANGED -> data += mapOf("customerFirst" to "Rajesh K.", "customerPhone" to "+919876500123",
                "note" to "Ring once — I'm upstairs. Cash ₹180 ready.")
            PushType.SELLER_SOS, PushType.DRIVER_SOS_ACK -> data += mapOf("sosId" to "lab-sos-1", "driverName" to "Suresh",
                "driverPhone" to "+919100027452", "lat" to "17.4325", "lng" to "78.4071", "place" to "Road No. 10, Jubilee Hills")
            PushType.CUST_COMPLAINT_REPLY, PushType.CUST_COMPLAINT_UPDATE, PushType.SELLER_COMPLAINT,
            PushType.SELLER_COMPLAINT_REMINDER, PushType.SELLER_COMPLAINT_UPDATE -> data += mapOf("complaintId" to "lab-complaint-1")
            PushType.CUST_SUB_TOMORROW, PushType.CUST_SUB_SHOP_CLOSED, PushType.CUST_WALLET_SHORT -> data += mapOf("subscriptionId" to "lab-sub-1")
            PushType.SELLER_PAYOUT_SENT, PushType.SELLER_PAYOUT_HELD -> data += mapOf("payoutId" to "lab-payout-1", "amount" to "28756")
            PushType.SELLER_DRIVER_CLOSING -> data += mapOf("driverId" to "lab-driver-1", "cash" to "1240", "empties" to "11")
            else -> {}
        }
        data += extra
        val expires = when (type) {
            PushType.SELLER_NEW_ORDER -> Instant.now().plusSeconds(180).toString()
            PushType.DRIVER_NEW_TASK -> Instant.now().plusSeconds(45).toString()
            else -> null
        }
        return PushPayload(
            v = 1, type = type.name, role = role,
            nid = "lab-${type.name}-${System.currentTimeMillis()}",
            title = context.getString(type.sampleTitle),
            body = context.getString(type.sampleBody),
            expiresAt = expires,
            data = data
        ).toMap()
    }
}
