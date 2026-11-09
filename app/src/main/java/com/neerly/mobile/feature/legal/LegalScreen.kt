package com.neerly.mobile.feature.legal

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.rupeesWhole
import com.neerly.mobile.data.config.LocalAppConfig

enum class LegalDoc { Terms, Privacy }

/**
 * Boards Terms / Privacy — the plain-words versions, reachable from Welcome and Profile
 * (Play needs a reachable privacy policy). Amounts and times come from admin settings,
 * so the page never promises a fee the app doesn't charge.
 */
@Composable
fun LegalScreen(initial: LegalDoc, onDone: () -> Unit) {
    var doc by rememberSaveable { mutableStateOf(initial) }
    val cfg = LocalAppConfig.current
    val context = LocalContext.current
    val sections: List<Pair<String, String>> = when (doc) {
        LegalDoc.Terms -> listOf(
            s(R.string.core_terms_h_marketplace) to s(R.string.core_terms_b_marketplace),
            s(R.string.core_terms_h_code) to s(R.string.core_terms_b_code),
            s(R.string.core_terms_h_deposit) to stringResource(R.string.core_terms_b_deposit, cfg.depositForfeitDays),
            s(R.string.core_terms_h_cancel) to stringResource(R.string.core_terms_b_cancel, cfg.lateCancelFee.rupeesWhole()),
            s(R.string.core_terms_h_subs) to stringResource(R.string.core_terms_b_subs, cfg.clock(cfg.subscriptionCutoff)),
            s(R.string.core_terms_h_wallet) to s(R.string.core_terms_b_wallet),
            s(R.string.core_terms_h_account) to s(R.string.core_terms_b_account),
            s(R.string.core_terms_h_problems) to s(R.string.core_terms_b_problems),
            s(R.string.core_terms_h_changes) to s(R.string.core_terms_b_changes),
            s(R.string.core_terms_h_law) to s(R.string.core_terms_b_law)
        )
        LegalDoc.Privacy -> listOf(
            s(R.string.core_privacy_h_number) to s(R.string.core_privacy_b_number),
            s(R.string.core_privacy_h_location) to s(R.string.core_privacy_b_location),
            s(R.string.core_privacy_h_payments) to s(R.string.core_privacy_b_payments),
            s(R.string.core_privacy_h_photos) to s(R.string.core_privacy_b_photos),
            s(R.string.core_privacy_h_calls) to s(R.string.core_privacy_b_calls),
            s(R.string.core_privacy_h_device) to s(R.string.core_privacy_b_device),
            s(R.string.core_privacy_h_notifications) to s(R.string.core_privacy_b_notifications),
            s(R.string.core_privacy_h_delete) to s(R.string.core_privacy_b_delete)
        )
    }
    BoondhPage(
        title = stringResource(if (doc == LegalDoc.Terms) R.string.core_terms_title else R.string.core_privacy_title),
        subtitle = stringResource(R.string.core_legal_updated, stringResource(R.string.core_legal_updated_on)),
        onBack = onDone,
        footer = { PrimaryButton(stringResource(R.string.core_got_it), onDone) }
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegalDoc.entries.forEach { d ->
                    FilterChip(
                        selected = doc == d, onClick = { doc = d },
                        label = { Text(stringResource(if (d == LegalDoc.Terms) R.string.core_legal_tab_terms else R.string.core_legal_tab_privacy),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeerlyColors.CustomerSoft,
                            selectedLabelColor = NeerlyColors.CustomerPrimary),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        sections.forEach { (h, b) ->
            item {
                BoondhCard {
                    Text(h, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900,
                        modifier = Modifier.semantics { heading() })
                    Text(b, fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700)
                }
            }
        }
        item {
            Text(stringResource(R.string.core_legal_footer, stringResource(R.string.core_legal_entity),
                cfg.clock(cfg.supportOpens), cfg.clock(cfg.supportCloses)),
                fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
            if (cfg.supportPhone.isNotBlank()) {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cfg.supportPhone}"))) }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.core_legal_call_support), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.CustomerPrimary)
                }
            }
        }
    }
}

@Composable
private fun s(@StringRes id: Int) = stringResource(id)
