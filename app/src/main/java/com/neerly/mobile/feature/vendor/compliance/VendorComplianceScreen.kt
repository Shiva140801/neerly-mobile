package com.neerly.mobile.feature.vendor.compliance

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*

/**
 * Licences & papers. One screen covers boards V03 (the checklist), V36/V48 (documents to fix,
 * with the reviewer's reason) and V37 (licence renewal) — the mode follows the documents.
 */
@Composable
fun VendorComplianceScreen(onBack: () -> Unit, vm: VendorComplianceViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    s.draft?.let { DocEditor(it, s, vm); return }
    OnReturn(vm::load)
    val fix = s.toFix
    val renew = s.toRenew
    BoondhPage(
        title = when {
            fix.isNotEmpty() -> pluralStringResource(R.plurals.vend_docs_to_fix, fix.size, fix.size)
            renew.isNotEmpty() -> stringResource(R.string.vend_renewal_due)
            else -> stringResource(R.string.vend_licences_papers)
        },
        onBack = onBack,
        subtitle = when {
            fix.isNotEmpty() -> stringResource(R.string.vend_fix_sub)
            else -> stringResource(R.string.vend_docs_sub)
        }
    ) {
        if (s.docs.isEmpty() && (s.loading || s.error != null)) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        s.sent?.let { item { Notice(it, Tone.Ok) } }
        docsChecklist(s, vm::edit)
    }
}

/** The V03 checklist rows — shared with the onboarding wizard's documents step. */
fun LazyListScope.docsChecklist(s: VendorComplianceUiState, onEdit: (String) -> Unit) {
    s.toFix.forEach { d -> item(key = "fix-" + d.id) {
        BoondhCard {
            Pill(stringResource(R.string.vend_needs_fixing), Tone.Bad)
            Text(SHOP_DOCS.firstOrNull { it.type == d.docType }?.let { stringResource(it.label) } ?: d.docType, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            d.rejectionReason?.let { Text(stringResource(R.string.vend_quoted, it), fontSize = 15.sp, color = NeerlyColors.Ink900) }
            Text(tipFor(d.docType), fontSize = 13.sp, color = NeerlyColors.Ink600)
            PrimaryButton(stringResource(R.string.vend_send_again), { onEdit(d.docType) })
        }
    } }
    s.toRenew.forEach { d -> item(key = "renew-" + d.id) {
        val days = d.expiresInDays ?: 0
        BoondhCard {
            Pill(if (days < 0) pluralStringResource(R.plurals.vend_lapsed_days_ago, -days, -days)
                else pluralStringResource(R.plurals.vend_expires_in, days, days, shortDay(d.expiresDate)),
                if (days < 0) Tone.Bad else Tone.Warn)
            Text(stringResource(R.string.vend_renew_your, SHOP_DOCS.firstOrNull { it.type == d.docType }?.let { stringResource(it.label) } ?: stringResource(R.string.vend_licence)), fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
            Text(stringResource(R.string.vend_lapse_note),
                fontSize = 14.sp, color = NeerlyColors.Ink600)
            PrimaryButton(stringResource(R.string.vend_upload_renewal), { onEdit(d.docType) })
        }
    } }
    if (s.toFix.isNotEmpty() || s.toRenew.isNotEmpty()) item { SectionLabel(stringResource(R.string.vend_everything_else)) }
    item {
        BoondhCard {
            SHOP_DOCS.forEachIndexed { i, k ->
                if (i > 0) HorizontalDivider(color = NeerlyColors.Ink200)
                val d = s.latest[k.type]
                val (sub, trail, color) = when (d?.status) {
                    null -> Triple(k.hint?.let { stringResource(it) }, stringResource(R.string.vend_add), NeerlyColors.CustomerPrimary)
                    "APPROVED" -> Triple(listOfNotNull(d.docNumber, d.expiresDate?.let { stringResource(R.string.vend_valid_till, shortDay(it)) }).joinToString(" · ")
                        .ifBlank { stringResource(R.string.vend_approved) }, stringResource(R.string.vend_approved), NeerlyColors.Ok)
                    "SUBMITTED", "PENDING", "UNDER_REVIEW" -> Triple(stringResource(R.string.vend_sent_checking, shortDay(d.uploadedAt)), stringResource(R.string.vend_checking), NeerlyColors.Warn)
                    "REJECTED" -> Triple(d.rejectionReason ?: stringResource(R.string.vend_needs_fixing), stringResource(R.string.vend_fix), NeerlyColors.Err)
                    "EXPIRED" -> Triple(stringResource(R.string.vend_lapsed_on, shortDay(d.expiresDate)), stringResource(R.string.vend_renew), NeerlyColors.Err)
                    else -> Triple(d.status.lowercase(), stringResource(R.string.vend_change), NeerlyColors.CustomerPrimary)
                }
                val name = stringResource(k.label)
                InfoRow(if (k.required) name else stringResource(R.string.vend_optional_suffix, name), sub, trailing = trail, trailingColor = color, onClick = { onEdit(k.type) })
            }
        }
    }
    if (s.missing.isEmpty() && s.toFix.isEmpty()) item {
        Text(stringResource(R.string.vend_approved_no_resend), fontSize = 13.sp, color = NeerlyColors.Ink600)
    } else item {
        Text(stringResource(R.string.vend_stuck_doc), fontSize = 13.sp, color = NeerlyColors.Ink600)
    }
}

@Composable
private fun tipFor(type: String) = when (type) {
    "WATER_TEST" -> stringResource(R.string.vend_tip_water)
    else -> stringResource(R.string.vend_tip_flat)
}

@Composable
internal fun DocEditor(d: DocDraft, s: VendorComplianceUiState, vm: VendorComplianceViewModel) {
    BackHandler(onBack = vm::cancel)
    val context = LocalContext.current
    val documentWord = stringResource(R.string.vend_document)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            vm.update { copy(file = uri, fileName = name ?: documentWord) }
        }
    }
    BoondhPage(
        title = stringResource(d.kind.label), onBack = vm::cancel, backLabel = stringResource(R.string.vend_documents),
        subtitle = d.kind.hint?.let { stringResource(it) },
        footer = {
            s.sendError?.let { Notice(it, Tone.Bad) }
            PrimaryButton(stringResource(if (s.sending) R.string.vend_sending else R.string.vend_send_for_checking), vm::send, enabled = !s.sending)
        }
    ) {
        s.latest[d.kind.type]?.rejectionReason?.takeIf { s.latest[d.kind.type]?.status == "REJECTED" }?.let {
            item { Notice(stringResource(R.string.vend_last_time, it), Tone.Warn) }
        }
        if (d.kind.hasNumber) item {
            OutlinedTextField(value = d.number, onValueChange = { v -> vm.update { copy(number = v.take(20)) } },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(if (d.kind.type == "FSSAI_LICENSE") R.string.vend_licence_number else R.string.vend_number)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = if (d.kind.type == "FSSAI_LICENSE") KeyboardType.Number else KeyboardType.Text))
        }
        if (d.kind.hasExpiry) item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = d.issued, onValueChange = { v -> vm.update { copy(issued = v.take(10)) } },
                    modifier = Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.vend_valid_from)) },
                    placeholder = { Text("2026-12-19") })
                OutlinedTextField(value = d.expires, onValueChange = { v -> vm.update { copy(expires = v.take(10)) } },
                    modifier = Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.vend_valid_till_label)) },
                    placeholder = { Text("2031-12-18") })
            }
        }
        item {
            BoondhCard {
                if (d.file == null) {
                    Text(stringResource(R.string.vend_photo_or_pdf), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                    Text(tipFor(d.kind.type), fontSize = 13.sp, color = NeerlyColors.Ink600)
                    PrimaryButton(stringResource(R.string.vend_choose_photo), { pick.launch("*/*") })
                } else {
                    InfoRow(d.fileName ?: documentWord, stringResource(R.string.vend_ready_to_send), trailing = stringResource(R.string.vend_replace), trailingColor = NeerlyColors.CustomerPrimary,
                        onClick = { pick.launch("*/*") })
                }
            }
        }
        item { Text(stringResource(R.string.vend_checks_1_day),
            fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}
