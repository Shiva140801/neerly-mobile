package com.neerly.mobile.feature.checkout

import com.neerly.mobile.R
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
    /**
     * The customer backed out, or the bank declined. Nothing was taken. [code] and
     * [description] are Razorpay's own, when it said anything (board PaymentFailed).
     */
    data class Declined(val message: String, val code: Int? = null, val description: String? = null) : PaymentOutcome
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
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
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
            return PaymentOutcome.Declined(strings.get(R.string.cust_pay_unavailable))
        }
        launcher.launch(
            activity = activity, keyId = keyId, razorpayOrderId = rzpOrderId,
            amountPaise = init.amountPaise, customerName = customerName,
            // The server's own record of who is paying wins: without it Razorpay offers
            // whatever number it remembered on this phone — someone else's.
            customerEmail = init.prefillEmail, customerContact = init.prefillContact ?: customerContact, description = description
        )
        return when (val r = results.stream.first()) {
            is GatewayResult.NotPaid -> PaymentOutcome.Declined(declineCopy(r), r.code, r.description)
            is GatewayResult.Paid -> {
                val signature = r.signature
                    ?: return PaymentOutcome.Unconfirmed(strings.get(R.string.cust_pay_received_confirming))
                runCatching {
                    api.capturePayment(init.paymentId, CapturePaymentRequest(r.razorpayPaymentId, signature))
                }.fold(
                    onSuccess = { PaymentOutcome.Captured },
                    onFailure = { PaymentOutcome.Unconfirmed(strings.get(R.string.cust_pay_received_wait)) }
                )
            }
        }
    }

    private fun declineCopy(r: GatewayResult.NotPaid): String = when (r.code) {
        // Checkout.PAYMENT_CANCELED
        0 -> strings.get(R.string.cust_pay_cancelled)
        // Checkout.NETWORK_ERROR
        2 -> strings.get(R.string.cust_pay_no_internet)
        else -> r.description?.takeIf { it.isNotBlank() && !it.trimStart().startsWith("{") }
            ?: strings.get(R.string.cust_pay_failed)
    }
}

/** The Activity behind a Compose context, for SDKs that need one. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
