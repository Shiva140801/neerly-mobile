package com.neerly.mobile.feature.subscription

import androidx.compose.runtime.Composable
import com.neerly.mobile.R
import com.neerly.mobile.core.util.AppLocale
import com.neerly.mobile.core.util.Strings
import com.neerly.mobile.core.util.rememberStrings
import com.neerly.mobile.data.dto.SubscriptionResponse
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.format.TextStyle

/** How the boards say a plan out loud: "every other day", "Mon, Wed, Fri" — in the app's language. */
internal fun SubscriptionResponse.howOften(s: Strings): String = when (frequency) {
    "DAILY" -> s.get(R.string.acct_sub_freq_daily)
    "ALT_DAY" -> s.get(R.string.acct_sub_freq_alt)
    "WEEKLY" -> daysOfWeek?.firstOrNull()?.let { s.get(R.string.acct_sub_freq_weekly_on, dayName(it)) } ?: s.get(R.string.acct_sub_freq_weekly)
    "BIWEEKLY" -> s.get(R.string.acct_sub_freq_biweekly)
    "MONTHLY" -> s.get(R.string.acct_sub_freq_monthly)
    else -> daysOfWeek?.takeIf { it.isNotEmpty() }?.joinToString(", ") { dayName(it) } ?: s.get(R.string.acct_sub_freq_chosen)
}

@Composable internal fun SubscriptionResponse.howOften(): String = howOften(rememberStrings())

/** "2 × 20L Plain jar" */
internal fun SubscriptionResponse.what(s: Strings): String = s.get(R.string.acct_sub_what, quantity, productName ?: s.get(R.string.acct_sub_jar))

@Composable internal fun SubscriptionResponse.what(): String = what(rememberStrings())

/** Per-delivery charge, when the server told us the unit price. */
internal fun SubscriptionResponse.eachTime(): BigDecimal? = unitPrice?.multiply(BigDecimal(quantity))

internal fun SubscriptionResponse.paysFrom(s: Strings): String = when (paymentMethod) {
    "WALLET" -> s.get(R.string.acct_wallet_title)
    "UPI_AUTOPAY" -> s.get(R.string.acct_sub_pay_autopay)
    "COD" -> s.get(R.string.acct_sub_pay_cash)
    else -> paymentMethod.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable internal fun SubscriptionResponse.paysFrom(): String = paysFrom(rememberStrings())

internal fun initials(name: String?) =
    (name ?: "?").split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()

/** "MON" / "MONDAY" → "Mon" (or సోమ / सोम) from the app's locale — no word lists to translate. */
internal fun dayName(d: String): String =
    DayOfWeek.entries.firstOrNull { it.name.startsWith(d.take(3).uppercase()) }
        ?.getDisplayName(TextStyle.SHORT, AppLocale.locale) ?: d
