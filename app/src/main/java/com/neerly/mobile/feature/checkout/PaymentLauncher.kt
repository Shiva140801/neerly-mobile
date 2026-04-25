package com.neerly.mobile.feature.checkout

import android.app.Activity
import com.razorpay.Checkout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Razorpay Checkout. The host Activity implements
 * `PaymentResultListener` (from Razorpay) to receive success/failure callbacks;
 * this launcher only handles "open the overlay with the right options".
 *
 * The backend already verifies the HMAC signature at `/payments/{id}/capture`
 * so we don't need to inspect the response here — just forward the payment
 * id + signature to the capture call from the Activity's callback.
 */
@Singleton
class PaymentLauncher @Inject constructor() {

    /**
     * Opens Razorpay Checkout in the provided activity.
     *
     * @param keyId  Razorpay publishable key (from /payments/initiate response)
     * @param razorpayOrderId order id from /payments/initiate
     * @param amountPaise amount in paise
     * @param customerName / email / contact — prefilled to reduce friction
     */
    fun launch(
        activity: Activity,
        keyId: String,
        razorpayOrderId: String,
        amountPaise: Long,
        customerName: String,
        customerEmail: String?,
        customerContact: String?,
        description: String = "Neerly water delivery"
    ) {
        val checkout = Checkout().apply {
            setKeyID(keyId)
        }

        val options = JSONObject().apply {
            put("name", "Neerly")
            put("description", description)
            put("order_id", razorpayOrderId)
            put("currency", "INR")
            put("amount", amountPaise)
            put("prefill", JSONObject().apply {
                put("name", customerName)
                if (!customerEmail.isNullOrBlank()) put("email", customerEmail)
                if (!customerContact.isNullOrBlank()) put("contact", customerContact)
            })
            put("theme", JSONObject().apply {
                put("color", "#2E75B6") // customer role primary
            })
            // Method toggles — UPI + card only; Razorpay wallet UI off
            put("method", JSONObject().apply {
                put("upi", true)
                put("card", true)
                put("netbanking", true)
                put("wallet", false)
                put("emi", false)
            })
        }

        checkout.open(activity, options)
    }
}
