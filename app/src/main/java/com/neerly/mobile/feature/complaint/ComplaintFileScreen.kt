package com.neerly.mobile.feature.complaint

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Complaint filing screen — category, subject, description, optional evidence.
 * Backend: POST /api/v1/customer/complaints. 24-hour L1 SLA starts on submit.
 *
 * Evidence photos are uploaded separately via the S3 presign flow (Session 5);
 * this screen just collects the S3 keys once upload completes.
 */
private val CATEGORIES = listOf(
    "LATE_DELIVERY" to R.string.acct_cmp_cat_late,
    "WRONG_PRODUCT" to R.string.acct_cmp_cat_wrong,
    "DAMAGED_CONTAINER" to R.string.acct_cmp_cat_damaged,
    "WATER_QUALITY" to R.string.acct_cmp_cat_quality,
    "DRIVER_BEHAVIOUR" to R.string.acct_cmp_cat_driver,
    "SHORT_QUANTITY" to R.string.acct_cmp_cat_short,
    "OVERCHARGED" to R.string.acct_cmp_cat_overcharged,
    "OTHER" to R.string.acct_cmp_cat_other
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintFileScreen(
    orderId: String?,
    onSubmit: (category: String, subject: String, description: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acct_cmp_report), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.core_back), color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(NeerlySpacing.x5)
        ) {
            if (orderId != null) {
                Text(
                    stringResource(R.string.acct_cmp_order_no, orderId.take(8)),
                    fontSize = 13.sp, color = NeerlyColors.Ink500
                )
                Spacer(Modifier.height(NeerlySpacing.x2))
            }
            Text(stringResource(R.string.acct_cmp_what), fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NeerlySpacing.x3))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORIES) { (code, label) ->
                    val isSelected = selectedCategory == code
                    val bg = if (isSelected) NeerlyColors.CustomerPrimary else NeerlyColors.Paper
                    val fg = if (isSelected) NeerlyColors.Paper else NeerlyColors.Ink700
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NeerlyRadius.pill))
                            .background(bg)
                            .clickable { selectedCategory = code }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text(stringResource(label), fontSize = 13.sp, color = fg, fontWeight = FontWeight.SemiBold) }
                }
            }

            Spacer(Modifier.height(NeerlySpacing.x5))
            Text(stringResource(R.string.acct_cmp_subject), fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NeerlySpacing.x2))
            OutlinedTextField(
                value = subject,
                onValueChange = { if (it.length <= 200) subject = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.acct_cmp_subject_hint), color = NeerlyColors.Ink400) }
            )

            Spacer(Modifier.height(NeerlySpacing.x4))
            Text(stringResource(R.string.acct_cmp_details), fontSize = 14.sp, color = NeerlyColors.Ink700, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NeerlySpacing.x2))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) description = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                placeholder = { Text(stringResource(R.string.acct_cmp_details_hint), color = NeerlyColors.Ink400) },
                supportingText = { Text("${description.length} / 2000", color = NeerlyColors.Ink500, fontSize = 12.sp) }
            )

            Spacer(Modifier.height(NeerlySpacing.x4))
            Text(
                com.neerly.mobile.data.config.LocalAppConfig.current.complaintFirstReplyHours.let { pluralStringResource(R.plurals.acct_cmp_reply_within, it, it) },
                fontSize = 12.sp, color = NeerlyColors.Ink500
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    submitting = true
                    onSubmit(selectedCategory!!, subject, description)
                },
                enabled = selectedCategory != null && subject.isNotBlank() && description.isNotBlank() && !submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) {
                Text(if (submitting) stringResource(R.string.acct_cmp_filing) else stringResource(R.string.acct_cmp_file),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(NeerlySpacing.x3))
        }
    }
}
