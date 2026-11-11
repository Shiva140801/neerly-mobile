package com.neerly.mobile.feature.review

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*

/** What the customer can tap instead of typing (board 16). Sent as part of the review text. */
internal val RATE_TAGS = listOf(R.string.cust_rv_t_on_time, R.string.cust_rv_t_polite, R.string.cust_rv_t_clean, R.string.cust_rv_t_seal, R.string.cust_rv_t_upstairs, R.string.cust_rv_t_swap)
internal val VERDICTS = listOf(0, R.string.cust_rv_v1, R.string.cust_rv_v2, R.string.cust_rv_v3, R.string.cust_rv_v4, R.string.cust_rv_v5)

/** The review text the backend stores: picked tags first, then whatever was typed. */
internal fun reviewText(tags: Collection<String>, note: String, order: List<String>): String? =
    listOfNotNull(order.filter { it in tags }.joinToString(" · ").ifBlank { null }, note.trim().ifBlank { null })
        .joinToString("\n").ifBlank { null }

/**
 * Board 16 — rate the delivery. The backend keeps two axes (the delivery and the water), so
 * both get stars; "what stood out" chips and an optional note become the review text.
 * The VM is the same instance the NavHost submits through (same back-stack entry), so the
 * screen can show its progress and errors.
 */
@Composable
fun ReviewSubmitScreen(
    orderId: String,
    vendorName: String,
    onSubmit: (rating: Int, vendorRating: Int, waterQualityRating: Int, text: String?) -> Unit,
    onBack: () -> Unit,
    vm: ReviewSubmitViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var vendorStars by remember { mutableIntStateOf(0) }
    var waterStars by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val tagLabels = RATE_TAGS.map { stringResource(it) }
    // The route carries the shop name, or the order number when that's all the caller had.
    val isOrderNumber = Regex("^[A-Z]{3}-\\d").containsMatchIn(vendorName)
    val shop = vendorName.takeUnless { isOrderNumber }

    BoondhPage(
        title = stringResource(R.string.cust_rv_title),
        subtitle = if (isOrderNumber) stringResource(R.string.cust_co_payment_desc, vendorName) else stringResource(R.string.cust_rv_from, vendorName),
        onBack = onBack, backLabel = stringResource(R.string.cust_skip),
        // Board RateOrder: "Skip" on the top row.
        topAction = { androidx.compose.material3.TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cust_skip), color = NeerlyColors.CustomerPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) } },
        footer = {
            state.error?.let { Notice(it, Tone.Bad) }
            PrimaryButton(if (state.submitting) stringResource(R.string.cust_rv_sending) else stringResource(R.string.cust_rv_send), {
                onSubmit((vendorStars + waterStars + 1) / 2, vendorStars, waterStars, reviewText(picked, note, tagLabels))
            }, enabled = vendorStars in 1..5 && waterStars in 1..5 && !state.submitting)
        }
    ) {
        item { StarRow(stringResource(R.string.cust_rv_delivery), stringResource(R.string.cust_rv_delivery_hint), vendorStars) { vendorStars = it } }
        item { StarRow(stringResource(R.string.cust_rv_water), stringResource(R.string.cust_rv_water_hint), waterStars) { waterStars = it } }
        item {
            // Board: "What stood out?" in sentence case; selected chips filled ink.
            Text(stringResource(R.string.cust_rv_stood_out).lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, modifier = Modifier.padding(bottom = 8.dp))
            FlowRowCompat {
                tagLabels.forEach { t ->
                    val on = t in picked
                    FilterChip(selected = on, onClick = { picked = if (on) picked - t else picked + t },
                        label = { Text(t, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeerlyColors.Ink900, selectedLabelColor = NeerlyColors.Paper,
                            containerColor = NeerlyColors.Paper, labelColor = NeerlyColors.Ink900),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(enabled = true, selected = on,
                            borderColor = NeerlyColors.Ink300, selectedBorderColor = NeerlyColors.Ink900),
                        modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        item {
            val optional = stringResource(R.string.cust_rv_optional)
            val label = if (shop != null) stringResource(R.string.cust_rv_anything_for, shop) else stringResource(R.string.cust_rv_anything_else)
            Text(androidx.compose.ui.text.buildAnnotatedString {
                val cut = label.indexOf(optional).takeIf { it >= 0 } ?: label.length
                append(label.substring(0, cut))
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal, color = NeerlyColors.Ink600)); append(label.substring(cut)); pop()
            }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = { if (it.length <= 1500) note = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.cust_rv_note_hint), color = NeerlyColors.Ink600) })
        }
    }
}

@Composable
private fun StarRow(title: String, hint: String, rating: Int, onRate: (Int) -> Unit) {
    // Board RateOrder: no card — a bold line, big amber stars, the verdict word under them.
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Text(hint, fontSize = 14.sp, color = NeerlyColors.Ink600)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-8).dp)) {
            for (i in 1..5) {
                val on = i <= rating
                val cd = androidx.compose.ui.res.pluralStringResource(R.plurals.cust_rv_star_cd, i, i, title)
                IconButton(onClick = { onRate(i) }, modifier = Modifier.size(52.dp).semantics {
                    role = Role.RadioButton; selected = i == rating
                    contentDescription = cd
                }) {
                    Text(if (on) "★" else "☆", fontSize = 40.sp, color = if (on) Boondh.Amber else NeerlyColors.Ink400)
                }
            }
        }
        if (rating > 0) Text(stringResource(VERDICTS[rating]), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink600)
    }
}

/** Wrapping chip row without pulling the experimental FlowRow API into callers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
