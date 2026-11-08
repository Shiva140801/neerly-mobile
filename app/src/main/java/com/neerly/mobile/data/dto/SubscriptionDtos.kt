package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mirror of backend `SubscriptionResponse`. Field names are the server's: the previous
 * shape (`quantity`, `deliverySlot`, `createdAt`, non-null names) matched nothing the
 * backend sends, so Moshi rejected every subscription payload and the list, detail,
 * create, pause and cancel screens all failed after the server had already acted.
 * Display fields (names, unit price) are nullable — the server fills them best-effort.
 */
@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    val id: String,
    val customerId: String,
    val vendorId: String,
    val productId: String,
    val quantityPerDelivery: Int,
    val containerMode: String = "KEEP",
    val frequency: String,                 // DAILY / ALT_DAY / WEEKLY / BIWEEKLY / MONTHLY / CUSTOM
    val daysOfWeek: List<String>? = null,
    val slotStart: String,                 // "06:00:00"
    val slotEnd: String,
    val startDate: String,
    val endDate: String? = null,
    val paymentMethod: String,
    val mandateMaxAmount: BigDecimal? = null,
    val status: String,                    // PENDING_MANDATE / ACTIVE / PAUSED / CANCELLED
    val pausedUntil: String? = null,
    val nextDeliveryDate: String? = null,
    val lastDeliveryDate: String? = null,
    val addressId: String? = null,
    val vendorName: String? = null,
    val productName: String? = null,
    val unitPrice: BigDecimal? = null,
    /** Set when the customer ended the plan after the cut-off: this last delivery still comes. */
    val finalDeliveryDate: String? = null,
    val cancelledAt: String? = null
) {
    val quantity: Int get() = quantityPerDelivery
    /** Ending after [finalDeliveryDate] — no more pause/skip/change. */
    val ending: Boolean get() = finalDeliveryDate != null

    /** "6–9 AM" from "06:00:00" / "09:00:00". */
    /** "7–9 AM", in the app's language. */
    val deliverySlot: String get() = com.neerly.mobile.core.design.timeWindow(slotStart, slotEnd)

    private fun hour(t: String): String {
        val h = t.substringBefore(':').toIntOrNull() ?: return t
        val m = t.split(':').getOrNull(1)?.toIntOrNull() ?: 0
        val h12 = if (h % 12 == 0) 12 else h % 12
        val mm = if (m == 0) "" else ":%02d".format(m)
        return "$h12$mm ${if (h < 12) "AM" else "PM"}"
    }
}

/** One scheduled delivery of a subscription — what `skip` returns. */
@JsonClass(generateAdapter = true)
data class SubscriptionInstanceResponse(
    val id: String,
    val subscriptionId: String,
    val deliveryDate: String,
    val status: String,                    // SCHEDULED / SKIPPED / ORDERED / DELIVERED / ...
    val amount: BigDecimal,
    val orderId: String? = null,
    val skipReason: String? = null,
    val skippedBy: String? = null
)

/**
 * Mirrors backend `CreateSubscriptionRequest` exactly. Slots are HH:mm local
 * times, dates are yyyy-MM-dd, frequency ∈ DAILY|ALT_DAY|WEEKLY|BIWEEKLY|MONTHLY|CUSTOM.
 */
@JsonClass(generateAdapter = true)
data class CreateSubscriptionRequest(
    val vendorId: String,
    val addressId: String,
    val productId: String,
    val quantityPerDelivery: Int,
    val containerMode: String = "KEEP",    // KEEP | TRANSFER_AND_RETURN
    val frequency: String,
    val daysOfWeek: List<String> = emptyList(),
    val slotStart: String,                 // e.g. "07:00"
    val slotEnd: String,                   // e.g. "09:00"
    val startDate: String,                 // yyyy-MM-dd
    val endDate: String? = null,
    val paymentMethod: String,             // UPI_AUTOPAY / WALLET
    val mandateMaxAmount: BigDecimal       // customer-approved per-charge cap
)

@JsonClass(generateAdapter = true)
data class PauseSubscriptionRequest(
    val pausedFrom: String,                // yyyy-MM-dd (backend LocalDate — an instant was a 400)
    val pausedUntil: String? = null,       // null = until the customer resumes
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class SkipSubscriptionRequest(
    val deliveryDate: String               // YYYY-MM-DD
)

@JsonClass(generateAdapter = true)
data class CancelSubscriptionRequest(
    val reason: String? = null
)

/** Backend PATCH reads exactly these; anything else was silently ignored. */
@JsonClass(generateAdapter = true)
data class ModifySubscriptionRequest(
    val quantityPerDelivery: Int? = null,
    val slotStart: String? = null,          // "HH:mm"
    val slotEnd: String? = null,
    val paymentMethod: String? = null,      // WALLET / COD — starts a plan stuck on PENDING_MANDATE
    val frequency: String? = null,
    val daysOfWeek: List<String>? = null
)
