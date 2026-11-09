package com.neerly.mobile.push

import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.neerly.mobile.R

/**
 * One entry per catalog row (NPCCatalog C1–C18, NPVCatalog V1–V17, NPDCatalog D1–D12).
 * The server decides *when*; this table is how each one looks and behaves on the phone.
 *
 * @param thread what the stable Android tag is keyed on (`order:{id}` etc.), so a newer event
 *               about the same thing replaces the card instead of stacking (rule 9).
 * @param publicTitle/publicBody the lock-screen version for private rows (rule 5, A6).
 */
enum class PushType(
    val row: String,
    val role: PushRole,
    val channel: PushChannel,
    val defaultActions: List<PushAction> = emptyList(),
    val thread: Thread = Thread.ORDER,
    val ring: Boolean = false,
    val visibility: Int? = null,
    @StringRes val publicTitle: Int? = null,
    @StringRes val publicBody: Int? = null,
    val ongoing: Boolean = false,
    /** Auto-dismiss after this many ms (heads-up that gets out of the way). */
    val timeoutMs: Long? = null,
    /** Read the push aloud (driver rows, NPDRing / NPDHeadsUp). */
    val speak: Boolean = false,
    /** Hide action buttons while the phone moves faster than 10 km/h (NPDHeadsUp). */
    val speedGated: Boolean = false,
    /** Ask for the in-app sample in Push lab: catalog title/body (English). */
    @StringRes val sampleTitle: Int,
    @StringRes val sampleBody: Int
) {
    // ---- Customer ----
    CUST_ORDER_ACCEPTED("C1", PushRole.CUSTOMER, PushChannel.CUST_ORDERS, listOf(PushAction.TRACK),
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_order_title, publicBody = R.string.push_pub_order_body,
        sampleTitle = R.string.push_s_c1_t, sampleBody = R.string.push_s_c1_b),
    CUST_OUT_FOR_DELIVERY("C2", PushRole.CUSTOMER, PushChannel.CUST_LIVE, listOf(PushAction.CALL, PushAction.SHOW_CODE),
        ongoing = true, sampleTitle = R.string.push_s_c2_t, sampleBody = R.string.push_s_c2_b),
    CUST_AT_GATE("C3", PushRole.CUSTOMER, PushChannel.CUST_GATE, listOf(PushAction.CALL),
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_gate_title, publicBody = R.string.push_pub_gate_body,
        sampleTitle = R.string.push_s_c3_t, sampleBody = R.string.push_s_c3_b),
    CUST_DELIVERED("C4", PushRole.CUSTOMER, PushChannel.CUST_ORDERS, listOf(PushAction.RATE, PushAction.REPORT),
        sampleTitle = R.string.push_s_c4_t, sampleBody = R.string.push_s_c4_b),
    CUST_DELIVERY_FAILED("C5", PushRole.CUSTOMER, PushChannel.CUST_ALERTS, listOf(PushAction.NEW_TIME, PushAction.I_WAS_HOME),
        sampleTitle = R.string.push_s_c5_t, sampleBody = R.string.push_s_c5_b),
    CUST_SHOP_DECLINED("C6", PushRole.CUSTOMER, PushChannel.CUST_ALERTS, listOf(PushAction.OTHER_SHOPS),
        sampleTitle = R.string.push_s_c6_t, sampleBody = R.string.push_s_c6_b),
    CUST_PAYMENT_FAILED("C7", PushRole.CUSTOMER, PushChannel.CUST_ALERTS, listOf(PushAction.RETRY_PAY),
        sampleTitle = R.string.push_s_c7_t, sampleBody = R.string.push_s_c7_b),
    CUST_SUB_TOMORROW("C8", PushRole.CUSTOMER, PushChannel.CUST_SUBS, listOf(PushAction.SKIP_TOMORROW), thread = Thread.SUBSCRIPTION,
        sampleTitle = R.string.push_s_c8_t, sampleBody = R.string.push_s_c8_b),
    CUST_SUB_SHOP_CLOSED("C9", PushRole.CUSTOMER, PushChannel.CUST_SUBS, listOf(PushAction.CHANGE_PLAN), thread = Thread.SUBSCRIPTION,
        sampleTitle = R.string.push_s_c9_t, sampleBody = R.string.push_s_c9_b),
    CUST_WALLET_SHORT("C10", PushRole.CUSTOMER, PushChannel.CUST_SUBS, listOf(PushAction.ADD_MONEY), thread = Thread.SUBSCRIPTION,
        sampleTitle = R.string.push_s_c10_t, sampleBody = R.string.push_s_c10_b),
    CUST_JAR_DUE("C11", PushRole.CUSTOMER, PushChannel.CUST_JARS, listOf(PushAction.PLAN_RETURN), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_c11_t, sampleBody = R.string.push_s_c11_b),
    CUST_REFUND_SENT("C12", PushRole.CUSTOMER, PushChannel.CUST_MONEY, listOf(PushAction.VIEW), thread = Thread.REFUND,
        sampleTitle = R.string.push_s_c12_t, sampleBody = R.string.push_s_c12_b),
    CUST_WALLET_MOVED("C13", PushRole.CUSTOMER, PushChannel.CUST_MONEY, thread = Thread.TYPE,
        sampleTitle = R.string.push_s_c13_t, sampleBody = R.string.push_s_c13_b),
    CUST_COMPLAINT_REPLY("C14", PushRole.CUSTOMER, PushChannel.CUST_ORDERS, listOf(PushAction.REPLY), thread = Thread.COMPLAINT,
        sampleTitle = R.string.push_s_c14_t, sampleBody = R.string.push_s_c14_b),
    CUST_COMPLAINT_UPDATE("C15", PushRole.CUSTOMER, PushChannel.CUST_ORDERS, listOf(PushAction.OPEN), thread = Thread.COMPLAINT,
        sampleTitle = R.string.push_s_c15_t, sampleBody = R.string.push_s_c15_b),
    CUST_OFFER("C16", PushRole.CUSTOMER, PushChannel.CUST_OFFERS, listOf(PushAction.ORDER), thread = Thread.VENDOR,
        sampleTitle = R.string.push_s_c16_t, sampleBody = R.string.push_s_c16_b),
    CUST_BACK_IN_STOCK("C17", PushRole.CUSTOMER, PushChannel.CUST_OFFERS, listOf(PushAction.ORDER), thread = Thread.VENDOR,
        sampleTitle = R.string.push_s_c17_t, sampleBody = R.string.push_s_c17_b),
    SECURITY_NEW_SIGN_IN("C18", PushRole.CUSTOMER, PushChannel.CUST_SECURITY, listOf(PushAction.NOT_ME), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_c18_t, sampleBody = R.string.push_s_c18_b),

    // ---- Seller ----
    SELLER_NEW_ORDER("V1", PushRole.SELLER, PushChannel.SELLER_RING, listOf(PushAction.DECLINE, PushAction.ACCEPT), thread = Thread.OFFER, ring = true,
        sampleTitle = R.string.push_s_v1_t, sampleBody = R.string.push_s_v1_b),
    SELLER_MISSED_ORDER("V2", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.RING_SETTINGS), thread = Thread.OFFER,
        sampleTitle = R.string.push_s_v2_t, sampleBody = R.string.push_s_v2_b),
    SELLER_NO_DRIVER("V3", PushRole.SELLER, PushChannel.SELLER_TEAM, listOf(PushAction.ASSIGN),
        sampleTitle = R.string.push_s_v3_t, sampleBody = R.string.push_s_v3_b),
    SELLER_ORDER_CANCELLED("V4", PushRole.SELLER, PushChannel.SELLER_ORDERS,
        sampleTitle = R.string.push_s_v4_t, sampleBody = R.string.push_s_v4_b),
    SELLER_EVENT_REQUEST("V5", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.OPEN), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v5_t, sampleBody = R.string.push_s_v5_b),
    SELLER_COMPLAINT("V6", PushRole.SELLER, PushChannel.SELLER_COMPLAINTS, listOf(PushAction.REPLY, PushAction.FRESH_JAR), thread = Thread.COMPLAINT,
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_complaint_title, publicBody = R.string.push_pub_complaint_body,
        timeoutMs = null, sampleTitle = R.string.push_s_v6_t, sampleBody = R.string.push_s_v6_b),
    SELLER_COMPLAINT_REMINDER("V7", PushRole.SELLER, PushChannel.SELLER_COMPLAINTS, listOf(PushAction.REPLY), thread = Thread.COMPLAINT,
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_complaint_title, publicBody = R.string.push_pub_complaint_body,
        sampleTitle = R.string.push_s_v7_t, sampleBody = R.string.push_s_v7_b),
    SELLER_COMPLAINT_UPDATE("V8", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.OPEN), thread = Thread.COMPLAINT,
        sampleTitle = R.string.push_s_v8_t, sampleBody = R.string.push_s_v8_b),
    SELLER_LOW_REVIEW("V9", PushRole.SELLER, PushChannel.SELLER_QUIET, listOf(PushAction.REPLY), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v9_t, sampleBody = R.string.push_s_v9_b),
    SELLER_DRIVER_CLOSING("V10", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.COUNT_NOW), thread = Thread.DRIVER,
        sampleTitle = R.string.push_s_v10_t, sampleBody = R.string.push_s_v10_b),
    SELLER_SOS("V11", PushRole.SELLER, PushChannel.SELLER_SOS, listOf(PushAction.CALL, PushAction.SEE_MAP), thread = Thread.SOS,
        sampleTitle = R.string.push_s_v11_t, sampleBody = R.string.push_s_v11_b),
    SELLER_SOLD_OUT("V12", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.ADD_STOCK), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v12_t, sampleBody = R.string.push_s_v12_b),
    SELLER_LATE_JARS("V13", PushRole.SELLER, PushChannel.SELLER_QUIET, listOf(PushAction.PLAN_PICKUPS), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v13_t, sampleBody = R.string.push_s_v13_b),
    SELLER_PAYOUT_SENT("V14", PushRole.SELLER, PushChannel.SELLER_MONEY, listOf(PushAction.VIEW), thread = Thread.PAYOUT,
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_payout_title, publicBody = R.string.push_pub_money_body,
        sampleTitle = R.string.push_s_v14_t, sampleBody = R.string.push_s_v14_b),
    SELLER_PAYOUT_HELD("V15", PushRole.SELLER, PushChannel.SELLER_MONEY_QUIET, listOf(PushAction.FIX_BANK), thread = Thread.PAYOUT,
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_payout_held_title, publicBody = R.string.push_pub_money_body,
        sampleTitle = R.string.push_s_v15_t, sampleBody = R.string.push_s_v15_b),
    SELLER_CLOSED_TODAY("V16", PushRole.SELLER, PushChannel.SELLER_QUIET, listOf(PushAction.OPEN_EARLY), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v16_t, sampleBody = R.string.push_s_v16_b),
    SELLER_LICENCE_DUE("V17", PushRole.SELLER, PushChannel.SELLER_ORDERS, listOf(PushAction.UPLOAD), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_v17_t, sampleBody = R.string.push_s_v17_b),
    SELLER_CLAIM_DECIDED("A1", PushRole.SELLER, PushChannel.SELLER_MONEY, listOf(PushAction.VIEW), thread = Thread.TYPE,
        visibility = NotificationCompat.VISIBILITY_PRIVATE, publicTitle = R.string.push_pub_claim_title, publicBody = R.string.push_pub_claim_body,
        sampleTitle = R.string.push_s_a1_t, sampleBody = R.string.push_s_a1_b),

    // ---- Driver ----
    DRIVER_NEW_TASK("D1", PushRole.DRIVER, PushChannel.DRIVER_RING, listOf(PushAction.PASS, PushAction.TAKE), thread = Thread.OFFER, ring = true, speak = true,
        sampleTitle = R.string.push_s_d1_t, sampleBody = R.string.push_s_d1_b),
    DRIVER_MISSED_TASK("D2", PushRole.DRIVER, PushChannel.DRIVER_QUIET, thread = Thread.OFFER, visibility = NotificationCompat.VISIBILITY_SECRET,
        sampleTitle = R.string.push_s_d2_t, sampleBody = R.string.push_s_d2_b),
    DRIVER_NOTE_CHANGED("D3", PushRole.DRIVER, PushChannel.DRIVER_TASKS, listOf(PushAction.CALL), timeoutMs = 6_000, speak = true, speedGated = true,
        sampleTitle = R.string.push_s_d3_t, sampleBody = R.string.push_s_d3_b),
    DRIVER_HANDOVER_CONFIRMED("D4", PushRole.DRIVER, PushChannel.DRIVER_INFO,
        sampleTitle = R.string.push_s_d4_t, sampleBody = R.string.push_s_d4_b),
    DRIVER_BREAK_ENDING("D6", PushRole.DRIVER, PushChannel.DRIVER_INFO, listOf(PushAction.BACK_NOW), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_d6_t, sampleBody = R.string.push_s_d6_b),
    DRIVER_DAY_END("D7", PushRole.DRIVER, PushChannel.DRIVER_DAYEND, listOf(PushAction.CLOSE_DAY), thread = Thread.TYPE, ongoing = true,
        sampleTitle = R.string.push_s_d7_t, sampleBody = R.string.push_s_d7_b),
    DRIVER_WEEK_EARNINGS("D8", PushRole.DRIVER, PushChannel.DRIVER_QUIET, thread = Thread.TYPE,
        sampleTitle = R.string.push_s_d8_t, sampleBody = R.string.push_s_d8_b),
    DRIVER_SHIFT_TOMORROW("D9", PushRole.DRIVER, PushChannel.DRIVER_QUIET, thread = Thread.VENDOR,
        sampleTitle = R.string.push_s_d9_t, sampleBody = R.string.push_s_d9_b),
    DRIVER_SOS_ACK("D10", PushRole.DRIVER, PushChannel.DRIVER_SOS, listOf(PushAction.SAFE), thread = Thread.SOS,
        sampleTitle = R.string.push_s_d10_t, sampleBody = R.string.push_s_d10_b),
    DRIVER_ADDED_TO_SHOP("D11", PushRole.DRIVER, PushChannel.DRIVER_INFO, listOf(PushAction.OPEN), thread = Thread.VENDOR,
        sampleTitle = R.string.push_s_d11_t, sampleBody = R.string.push_s_d11_b),
    DRIVER_DOC_REJECTED("D12", PushRole.DRIVER, PushChannel.DRIVER_INFO, listOf(PushAction.UPLOAD), thread = Thread.TYPE,
        sampleTitle = R.string.push_s_d12_t, sampleBody = R.string.push_s_d12_b);

    /** What the stable tag is built from when the server doesn't send `thread`. */
    enum class Thread { ORDER, OFFER, COMPLAINT, SUBSCRIPTION, REFUND, PAYOUT, SOS, DRIVER, VENDOR, TYPE }

    companion object {
        fun of(type: String?): PushType? = type?.let { t -> entries.firstOrNull { it.name == t } }
    }
}
