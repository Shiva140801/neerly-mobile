package com.neerly.mobile.feature.checkout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.InfoRow
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.feature.vendor.orders.mmss
import com.neerly.mobile.feature.vendor.orders.rememberSecondsLeft

/**
 * Board PaymentPending: the UPI request is out; approve it in the UPI app. The page polls
 * by itself (CheckoutViewModel), so "I've approved it" only looks sooner.
 */
@Composable
fun PaymentPendingPage(
    page: PaymentPage.Pending,
    shopName: String,
    working: Boolean,
    onApproved: () -> Unit,
    onPayAnotherWay: () -> Unit,
    onCancel: () -> Unit
) {
    val left by rememberSecondsLeft(page.holdUntil)
    BoondhPage(
        title = stringResource(R.string.cust_payst_pending_title, page.order.totalAmount.asRupees()),
        onBack = onPayAnotherWay, backLabel = stringResource(R.string.cust_co_title),
        footer = {
            PrimaryButton(stringResource(R.string.cust_payst_approved), onApproved, enabled = !working)
            SecondaryButton(stringResource(R.string.cust_payst_another_way), onPayAnotherWay, enabled = !working)
            TextButton(onClick = onCancel, enabled = !working, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cust_payst_cancel), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    ) {
        item { Text(stringResource(R.string.cust_payst_pending_body), fontSize = 15.sp, color = NeerlyColors.Ink700) }
        left?.let { sec ->
            item {
                Text(if (sec > 0) mmss(sec) else stringResource(R.string.cust_payst_times_up),
                    fontSize = 40.sp, fontWeight = FontWeight.Bold, color = if (sec > 60) NeerlyColors.Ink900 else NeerlyColors.Err,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item { Text(stringResource(R.string.cust_payst_holds, shopName), fontSize = 14.sp, color = NeerlyColors.Ink600,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
        item { Notice(stringResource(R.string.cust_payst_dont_pay_twice), Tone.Info) }
    }
}

/**
 * Board PaymentFailed: nothing was taken, the order is held for a while, and three other
 * ways to pay. "Bank said" is Razorpay's own description of the refusal.
 */
@Composable
fun PaymentFailedPage(
    page: PaymentPage.Failed,
    working: Boolean,
    problem: String?,
    onOtherUpi: () -> Unit,
    onCard: () -> Unit,
    onCash: () -> Unit,
    onBack: () -> Unit
) {
    val left by rememberSecondsLeft(page.holdUntil)
    val minutes = left?.let { ((it + 59) / 60).toInt() }
    BoondhPage(
        title = stringResource(R.string.cust_payst_failed_title),
        onBack = onBack, backLabel = stringResource(R.string.cust_co_title),
        subtitle = stringResource(if (page.method == "CARD") R.string.cust_payst_failed_sub_card else R.string.cust_payst_failed_sub_upi),
        footer = {
            problem?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(R.string.cust_payst_other_upi), onOtherUpi, enabled = !working)
            SecondaryButton(stringResource(R.string.cust_payst_by_card), onCard, enabled = !working)
            SecondaryButton(stringResource(R.string.cust_payst_cash), onCash, enabled = !working)
        }
    ) {
        item { Notice(stringResource(R.string.cust_payst_nothing_taken), Tone.Ok) }
        minutes?.takeIf { it > 0 }?.let { m ->
            item { Text(pluralStringResource(R.plurals.cust_payst_holding, m, m), fontSize = 15.sp, color = NeerlyColors.Ink700) }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.cust_payst_amount), trailing = page.order.totalAmount.asRupees())
                InfoRow(stringResource(R.string.cust_payst_tried), trailing = stringResource(
                    if (page.method == "CARD") R.string.cust_method_card else R.string.cust_method_upi))
                page.bankSaid?.let { InfoRow(stringResource(R.string.cust_payst_bank_said), subtitle = it) }
            }
        }
        item { Text(stringResource(R.string.cust_payst_debit_back), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}
