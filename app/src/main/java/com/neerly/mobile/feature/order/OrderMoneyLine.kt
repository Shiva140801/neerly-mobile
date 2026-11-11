package com.neerly.mobile.feature.order

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neerly.mobile.R
import com.neerly.mobile.core.design.rupees
import com.neerly.mobile.data.dto.OrderResponse

/**
 * What happened to the money on an order that ended undelivered, from the server's
 * payment and refund rows — never guessed from the payment method. An online order whose
 * payment never captured says "Nothing was charged", not "Refund on its way".
 * Falls back to the old method-based line only when talking to a server without the field.
 */
@Composable
fun orderMoneyLine(o: OrderResponse): String = when (o.moneyState) {
    "NOT_CHARGED" -> stringResource(R.string.cust_oh_nothing_charged)
    "REFUNDED" -> o.refundedAmount?.let { a ->
        if (o.refundMode == "WALLET") stringResource(R.string.cust_money_refunded_wallet, a.rupees())
        else stringResource(R.string.cust_money_refunded_source, a.rupees())
    } ?: stringResource(R.string.cust_money_refunded)
    "REFUND_FAILED" -> stringResource(R.string.cust_money_refund_failed)
    "REFUND_PENDING" -> stringResource(R.string.cust_oh_refund_line)
    else -> if (o.paymentMethod == "COD") stringResource(R.string.cust_oh_nothing_charged) else stringResource(R.string.cust_oh_refund_line)
}
