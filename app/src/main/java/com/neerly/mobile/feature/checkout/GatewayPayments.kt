package com.neerly.mobile.feature.checkout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.CapturePaymentRequest
import com.neerly.mobile.data.dto.InitiatePaymentResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** What Razorpay Checkout reported when its overlay closed. */
sealed interface GatewayResult {
    data class Paid(val razorpayPaymentId: String, val razorpayOrderId: String?, val signature: String?) : GatewayResult
    data class NotPaid(val code: Int, val description: String?) : GatewayResult
}

/**
 * Razorpay reports to the host Activity, not to whoever opened it. MainActivity drops
 * each result here and the caller awaiting its payment picks it up.
 */
@Singleton
class GatewayResults @Inject constructor() {
    private val results = MutableSharedFlow<GatewayResult>(extraBufferCapacity = 1)
    val stream = results.asSharedFlow()
    fun publish(result: GatewayResult) { results.tryEmit(result) }
}

/** The end of one online payment, from the app's side. */
sealed interface PaymentOutcome {
    /** The backend verified the signature and captured the money. */
    data object Captured : PaymentOutcome
    /** The customer backed out, or the bank declined. Nothing was taken. */
    data class Declined(val message: String) : PaymentOutcome
    /**
     * Razorpay says paid but we couldn't confirm it with our server (network).
     * The webhook settles it; the app must not offer to pay again.
     */
    data class Unconfirmed(val message: String) : PaymentOutcome
}

/**
 * Opens Razorpay for a payment the backend has already initiated, waits for the
 * customer, and has the backend verify and capture it. Shared by checkout and wallet
 * top-up. Before this the launcher was never called: checkout went straight to "Order
 * placed" for a UPI order nobody had paid.
 */
@Singleton
class GatewayPayments @Inject constructor(
    private val launcher: PaymentLauncher,
    private val results: GatewayResults,
    private val api: NeerlyApi
) {
    suspend fun pay(
        activity: Activity,
        init: InitiatePaymentResult,
        customerName: String,
        customerContact: String?,
        description: String
    ): PaymentOutcome {
        val keyId = init.keyId
        val rzpOrderId = init.razorpayOrderId
        if (keyId.isNullOrBlank() || rzpOrderId.isNullOrBlank()) {
            return PaymentOutcome.Declined("Online payment isn't available right now. Try again in a moment.")
        }
        launcher.launch(
            activity = activity, keyId = keyId, razorpayOrderId = rzpOrderId,
            amountPaise = init.amountPaise, customerName = customerName,
            customerEmail = null, customerContact = customerContact, description = description
        )
        return when (val r = results.stream.first()) {
            is GatewayResult.NotPaid -> PaymentOutcome.Declined(declineCopy(r))
            is GatewayResult.Paid -> {
                val signature = r.signature
                    ?: return PaymentOutcome.Unconfirmed("Payment received. We're confirming it with your bank.")
                runCatching {
                    api.capturePayment(init.paymentId, CapturePaymentRequest(r.razorpayPaymentId, signature))
                }.fold(
                    onSuccess = { PaymentOutcome.Captured },
                    onFailure = { PaymentOutcome.Unconfirmed("Payment received. We're confirming it — this can take a minute.") }
                )
            }
        }
    }

    private fun declineCopy(r: GatewayResult.NotPaid): String = when (r.code) {
        // Checkout.PAYMENT_CANCELED
        0 -> "Payment cancelled. Nothing was taken."
        // Checkout.NETWORK_ERROR
        2 -> "No internet while paying. Nothing was taken — try again."
        else -> r.description?.takeIf { it.isNotBlank() && !it.trimStart().startsWith("{") }
            ?: "The payment didn't go through. Nothing was taken."
    }
}

/** The Activity behind a Compose context, for SDKs that need one. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
