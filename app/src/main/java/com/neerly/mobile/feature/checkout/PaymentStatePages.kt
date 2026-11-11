package com.neerly.mobile.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
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
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.feature.vendor.orders.mmss
import com.neerly.mobile.feature.vendor.orders.rememberSecondsLeft

/**
 * Board PaymentPending: the UPI request is out; approve it in the UPI app. The page polls
 * by itself (CheckoutViewModel), so "I've approved it" only looks sooner. Board layout:
 * centred amber clock, serif title and timer, the "don't pay twice" note just above the
 * buttons, then Pay another way | Cancel as a pair. No back chevron; system Back = pay another way.
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
    androidx.activity.compose.BackHandler(enabled = !working, onBack = onPayAnotherWay)
    StatePage(
        badge = { Badge(NeerlyColors.WarnSoft) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Schedule, null,
            tint = NeerlyColors.Warn, modifier = Modifier.size(34.dp)) } },
        centred = true,
        title = stringResource(R.string.cust_payst_pending_title, page.order.totalAmount.asRupees()),
        footer = {
            Notice(stringResource(R.string.cust_payst_dont_pay_twice), Tone.Info)
            PrimaryButton(stringResource(R.string.cust_payst_approved), onApproved, enabled = !working)
            PairButtons(stringResource(R.string.cust_payst_another_way), onPayAnotherWay,
                stringResource(R.string.cust_payst_cancel), onCancel, enabled = !working)
        }
    ) {
        Text(stringResource(R.string.cust_payst_pending_body), fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink700,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        left?.let { sec ->
            Text(if (sec > 0) mmss(sec) else stringResource(R.string.cust_payst_times_up),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                color = if (sec > 60) NeerlyColors.Ink900 else NeerlyColors.Err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.cust_payst_holds, shopName), fontSize = 15.sp, color = NeerlyColors.Ink600,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Board PaymentFailed: red "!" badge, serif title, "Your bank declined … **No money was taken.**
 * We're holding your order for N minutes.", a dashed-row card (Amount / Tried / Bank said),
 * then one primary and a pair of outlined ways to pay, and the small debit note.
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
    androidx.activity.compose.BackHandler(enabled = !working, onBack = onBack)
    val holding = minutes?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.cust_payst_holding, it, it) }
    val lead = stringResource(if (page.method == "CARD") R.string.cust_payst_failed_sub_card else R.string.cust_payst_failed_sub_upi)
    val nothing = stringResource(R.string.cust_payst_nothing_taken)
    StatePage(
        badge = { Badge(NeerlyColors.ErrSoft) { Text("!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Err) } },
        centred = false,
        title = stringResource(R.string.cust_payst_failed_title),
        footer = {
            problem?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(R.string.cust_payst_other_upi), onOtherUpi, enabled = !working)
            PairButtons(stringResource(R.string.cust_payst_by_card), onCard, stringResource(R.string.cust_payst_cash), onCash, enabled = !working)
            Text(stringResource(R.string.cust_payst_debit_back), fontSize = 13.sp, lineHeight = 18.sp, color = NeerlyColors.Ink600,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Text(androidx.compose.ui.text.buildAnnotatedString {
            append(lead); append(" ")
            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)); append(nothing); pop()
            holding?.let { append(" "); append(it) }
        }, fontSize = 16.sp, lineHeight = 24.sp, color = NeerlyColors.Ink700)
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()
            .background(NeerlyColors.Paper, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .border(1.dp, NeerlyColors.Ink200, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)) {
            FactLine(stringResource(R.string.cust_payst_amount), page.order.totalAmount.asRupees())
            Dashed()
            FactLine(stringResource(R.string.cust_payst_tried), stringResource(if (page.method == "CARD") R.string.cust_method_card else R.string.cust_method_upi))
            page.bankSaid?.let { Dashed(); FactLine(stringResource(R.string.cust_payst_bank_said), it) }
        }
    }
}

@Composable
private fun StatePage(
    badge: @Composable () -> Unit,
    centred: Boolean,
    title: String,
    footer: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.Scaffold(containerColor = NeerlyColors.Canvas, bottomBar = {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), content = footer)
    }) { pad ->
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = if (centred) androidx.compose.ui.Alignment.CenterHorizontally else androidx.compose.ui.Alignment.Start,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            badge()
            Text(title, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 30.sp, lineHeight = 34.sp,
                color = NeerlyColors.Ink900, textAlign = if (centred) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth().semantics { heading() })
            content()
        }
    }
}

@Composable
private fun Badge(bg: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.size(72.dp).background(bg, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center) { content() }
}

@Composable
private fun PairButtons(a: String, onA: () -> Unit, b: String, onB: () -> Unit, enabled: Boolean) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        for ((label, go) in listOf(a to onA, b to onB)) {
            androidx.compose.material3.OutlinedButton(onClick = go, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeerlyColors.Ink300)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FactLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink600)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, textAlign = TextAlign.End)
    }
}

@Composable
private fun Dashed() {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(NeerlyColors.Ink200, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    }
}
