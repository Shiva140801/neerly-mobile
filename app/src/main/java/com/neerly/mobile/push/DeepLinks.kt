package com.neerly.mobile.push

/**
 * `boondh://…` links (sent by the server, or built here when it sends none) → NavHost routes.
 * Pure string work so it is unit-tested; the route strings mirror `navigation.Routes`.
 */
object DeepLinks {
    const val SCHEME = "boondh://"

    /** Route for a link, or null when it isn't one of ours. */
    fun route(link: String?): String? {
        if (link.isNullOrBlank() || !link.startsWith(SCHEME)) return null
        val rest = link.removePrefix(SCHEME)
        val path = rest.substringBefore('?').trimEnd('/')
        val query = rest.substringAfter('?', "").split('&').filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        val p = path.split('/')
        fun at(i: Int) = p.getOrNull(i)?.ifBlank { null }
        return when (at(0)) {
            "order" -> at(1)?.let { "customer/order/$it" } ?: "customer/orders"
            "orders" -> "customer/orders"
            "shops", "home" -> "customer/home"
            "shop" -> at(1)?.let { "customer/vendor/$it" } ?: "customer/home"
            "subs" -> at(1)?.let { "customer/subscription/$it" } ?: "customer/subscriptions"
            "wallet" -> "customer/wallet"
            "jars" -> "customer/deposits"
            "refunds" -> at(1)?.let { "customer/refunds/$it" } ?: "customer/refunds"
            "complaint" -> at(1)?.let { "customer/complaint/$it" } ?: "customer/complaints"
            "complaint-new" -> "customer/complaint?orderId=${query["orderId"].orEmpty()}"
            "security", "profile" -> "customer/profile"
            "notifications" -> "notifications"
            "seller" -> when (at(1)) {
                "new-order" -> at(2)?.let { "vendor/new-order/$it" } ?: "vendor/orders"
                "order" -> at(2)?.let { "vendor/order/$it" } ?: "vendor/orders"
                "orders" -> "vendor/orders"
                "alert-settings" -> "vendor/alert-settings"
                "event", "events" -> "vendor/events"
                "complaint", "complaints" -> "vendor/complaints"
                "reviews" -> "vendor/reviews"
                "team" -> "vendor/team"
                "catalog" -> "vendor/catalog"
                "jars" -> "vendor/jars/pickups"
                "claims" -> at(2)?.let { "vendor/jars/claims/detail/$it" } ?: "vendor/jars/claims"
                "payouts", "earnings", "money" -> "vendor/earnings"
                "shop", "holidays" -> "vendor/business-config"
                "compliance" -> "vendor/compliance"
                "bank" -> "vendor/bank"
                else -> "vendor/today"
            }
            "driver" -> when (at(1)) {
                "documents" -> "driver/documents"
                "sos" -> "driver/sos?send=false"
                "pickups" -> "driver/pickups"
                else -> "driver/home"
            }
            else -> null
        }
    }

    /** `boondh://seller/new-order/{id}?decline=1` — "Can't take" opens the reason sheet straight away. */
    fun declineOrderId(link: String?): String? {
        if (link.isNullOrBlank() || !link.startsWith("${SCHEME}seller/new-order/")) return null
        if (!link.contains("decline=1") && !link.contains("decline=true")) return null
        return link.removePrefix("${SCHEME}seller/new-order/").substringBefore('?').ifBlank { null }
    }

    /** Which face a route belongs to — used to switch roles before opening it. */
    fun roleOf(route: String): PushRole? = when {
        route.startsWith("vendor/") -> PushRole.SELLER
        route.startsWith("driver/") -> PushRole.DRIVER
        route.startsWith("customer/") -> PushRole.CUSTOMER
        else -> null
    }

    /** The link a row opens when the server sends none. */
    fun default(type: PushType, p: PushPayload): String {
        val order = p["orderId"]
        return when (type) {
            PushType.CUST_ORDER_ACCEPTED, PushType.CUST_OUT_FOR_DELIVERY, PushType.CUST_AT_GATE,
            PushType.CUST_DELIVERED, PushType.CUST_DELIVERY_FAILED, PushType.CUST_PAYMENT_FAILED ->
                order?.let { "${SCHEME}order/$it" } ?: "${SCHEME}orders"
            PushType.CUST_SHOP_DECLINED -> "${SCHEME}shops"
            PushType.CUST_SUB_TOMORROW, PushType.CUST_SUB_SHOP_CLOSED ->
                p["subscriptionId"]?.let { "${SCHEME}subs/$it" } ?: "${SCHEME}subs"
            PushType.CUST_WALLET_SHORT, PushType.CUST_WALLET_MOVED -> "${SCHEME}wallet"
            PushType.CUST_JAR_DUE -> "${SCHEME}jars"
            PushType.CUST_REFUND_SENT -> p["refundId"]?.let { "${SCHEME}refunds/$it" } ?: "${SCHEME}refunds"
            PushType.CUST_COMPLAINT_REPLY, PushType.CUST_COMPLAINT_UPDATE ->
                p["complaintId"]?.let { "${SCHEME}complaint/$it" } ?: "${SCHEME}complaint"
            PushType.CUST_OFFER, PushType.CUST_BACK_IN_STOCK -> p["vendorId"]?.let { "${SCHEME}shop/$it" } ?: "${SCHEME}shops"
            PushType.SECURITY_NEW_SIGN_IN -> "${SCHEME}security"

            PushType.SELLER_NEW_ORDER -> order?.let { "${SCHEME}seller/new-order/$it" } ?: "${SCHEME}seller/orders"
            PushType.SELLER_MISSED_ORDER -> "${SCHEME}seller/alert-settings"
            PushType.SELLER_NO_DRIVER, PushType.SELLER_ORDER_CANCELLED ->
                order?.let { "${SCHEME}seller/order/$it" } ?: "${SCHEME}seller/orders"
            PushType.SELLER_EVENT_REQUEST -> "${SCHEME}seller/events"
            PushType.SELLER_COMPLAINT, PushType.SELLER_COMPLAINT_REMINDER, PushType.SELLER_COMPLAINT_UPDATE -> "${SCHEME}seller/complaints"
            PushType.SELLER_LOW_REVIEW -> "${SCHEME}seller/reviews"
            PushType.SELLER_DRIVER_CLOSING -> "${SCHEME}seller/team"
            PushType.SELLER_SOS -> "${SCHEME}seller/team"
            PushType.SELLER_SOLD_OUT -> "${SCHEME}seller/catalog"
            PushType.SELLER_LATE_JARS -> "${SCHEME}seller/jars"
            PushType.SELLER_PAYOUT_SENT -> "${SCHEME}seller/payouts"
            PushType.SELLER_PAYOUT_HELD -> "${SCHEME}seller/bank"
            PushType.SELLER_CLOSED_TODAY -> "${SCHEME}seller/shop"
            PushType.SELLER_LICENCE_DUE -> "${SCHEME}seller/compliance"
            PushType.SELLER_CLAIM_DECIDED -> p["claimId"]?.let { "${SCHEME}seller/claims/$it" } ?: "${SCHEME}seller/claims"

            PushType.DRIVER_SOS_ACK -> "${SCHEME}driver/sos"
            PushType.DRIVER_DOC_REJECTED -> "${SCHEME}driver/documents"
            PushType.DRIVER_NEW_TASK, PushType.DRIVER_MISSED_TASK, PushType.DRIVER_NOTE_CHANGED,
            PushType.DRIVER_HANDOVER_CONFIRMED, PushType.DRIVER_BREAK_ENDING, PushType.DRIVER_DAY_END,
            PushType.DRIVER_WEEK_EARNINGS, PushType.DRIVER_SHIFT_TOMORROW, PushType.DRIVER_ADDED_TO_SHOP -> "${SCHEME}driver"
        }
    }

    /** Where a specific button goes, when it isn't simply the row's link. */
    fun forAction(action: PushAction, type: PushType, p: PushPayload): String {
        val order = p["orderId"]
        return when (action) {
            PushAction.REPORT, PushAction.I_WAS_HOME -> "${SCHEME}complaint-new?orderId=${order.orEmpty()}"
            PushAction.OTHER_SHOPS -> "${SCHEME}shops"
            PushAction.ADD_MONEY -> "${SCHEME}wallet"
            PushAction.DECLINE -> order?.let { "${SCHEME}seller/new-order/$it?decline=1" }
            PushAction.RING_SETTINGS -> "${SCHEME}seller/alert-settings"
            PushAction.ASSIGN, PushAction.FRESH_JAR -> order?.let { "${SCHEME}seller/order/$it" } ?: "${SCHEME}seller/complaints"
            PushAction.COUNT_NOW -> "${SCHEME}seller/team"
            PushAction.ADD_STOCK -> "${SCHEME}seller/catalog"
            PushAction.PLAN_PICKUPS -> "${SCHEME}seller/jars"
            PushAction.FIX_BANK -> "${SCHEME}seller/bank"
            PushAction.OPEN_EARLY -> "${SCHEME}seller/shop"
            PushAction.UPLOAD -> if (type.role == PushRole.DRIVER) "${SCHEME}driver/documents" else "${SCHEME}seller/compliance"
            PushAction.CLOSE_DAY -> "${SCHEME}driver"
            PushAction.NOT_ME -> "${SCHEME}security"
            else -> null
        } ?: p.deepLink ?: default(type, p)
    }
}
