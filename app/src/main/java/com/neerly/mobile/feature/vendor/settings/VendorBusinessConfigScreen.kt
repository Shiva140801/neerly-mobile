package com.neerly.mobile.feature.vendor.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.VendorHoursRow

private val DAY_LABELS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

/**
 * Vendor business config — business hours per day, holidays, and emergency
 * close. Single screen so the vendor sees their full availability picture
 * at once. Each section saves independently; we don't have a "save all" CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorBusinessConfigScreen(
    onBack: () -> Unit,
    vm: VendorBusinessConfigViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Business config", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.VendorPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(NeerlySpacing.x4)
        ) {
            if (s.loading) {
                Text("Loading…", color = NeerlyColors.Ink500)
                return@Column
            }

            // ---- Status banner ----
            val st = s.status
            val open = st?.isOpen ?: true
            Surface(
                color = if (open) NeerlyColors.OkSoft else NeerlyColors.WarnSoft,
                shape = RoundedCornerShape(NeerlyRadius.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(NeerlySpacing.x4)) {
                    Text(
                        if (open) "Open for orders" else "Closed for new orders",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (open) NeerlyColors.Ok else NeerlyColors.Warn
                    )
                    if (!open) {
                        Text(
                            "Until ${st?.pausedUntil ?: "—"}" +
                                (st?.pausedReason?.let { " — $it" } ?: ""),
                            fontSize = 12.sp, color = NeerlyColors.Ink700
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = vm::reopen,
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ok)) {
                            Text("Reopen now")
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.emergencyClose(24, "Family emergency") }) {
                                Text("Close 24h")
                            }
                            OutlinedButton(onClick = { vm.emergencyClose(72, "Vacation") }) {
                                Text("Close 3 days")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(NeerlySpacing.x4))

            // ---- Hours editor ----
            Text("Weekly hours", fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text("Times in 24h format (e.g. 06:00, 22:00). Leave blank to mark closed.",
                fontSize = 11.sp, color = NeerlyColors.Ink500)
            Spacer(Modifier.height(8.dp))

            // Local editable copy keyed off s.hours; rebuilds when refreshed.
            val openTimes = remember(s.hours) {
                mutableStateMapOf<Int, String>().apply {
                    DAY_LABELS.indices.forEach { i ->
                        val dow = i + 1
                        put(dow, s.hours.firstOrNull { it.dayOfWeek == dow }?.openTime ?: "")
                    }
                }
            }
            val closeTimes = remember(s.hours) {
                mutableStateMapOf<Int, String>().apply {
                    DAY_LABELS.indices.forEach { i ->
                        val dow = i + 1
                        put(dow, s.hours.firstOrNull { it.dayOfWeek == dow }?.closeTime ?: "")
                    }
                }
            }
            DAY_LABELS.forEachIndexed { idx, label ->
                val dow = idx + 1
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(label, modifier = Modifier.width(40.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink700)
                    OutlinedTextField(
                        value = openTimes[dow] ?: "",
                        onValueChange = { openTimes[dow] = it.take(8) },
                        placeholder = { Text("06:00", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    OutlinedTextField(
                        value = closeTimes[dow] ?: "",
                        onValueChange = { closeTimes[dow] = it.take(8) },
                        placeholder = { Text("22:00", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                }
            }
            Button(
                onClick = {
                    val rows = (1..7).mapNotNull { dow ->
                        val o = openTimes[dow]?.trim().orEmpty()
                        val c = closeTimes[dow]?.trim().orEmpty()
                        if (o.isBlank() || c.isBlank()) null
                        else VendorHoursRow(dow, paddedTime(o), paddedTime(c))
                    }
                    vm.saveHours(rows)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
            ) { Text("Save hours") }

            Spacer(Modifier.height(NeerlySpacing.x5))

            // ---- Holidays ----
            Text("Holidays", fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Spacer(Modifier.height(8.dp))
            s.holidays.forEach { h ->
                Surface(
                    color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(NeerlySpacing.x3),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(h.date, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            h.label?.let {
                                Text(it, fontSize = 11.sp, color = NeerlyColors.Ink500)
                            }
                        }
                        TextButton(onClick = { vm.removeHoliday(h.date) }) {
                            Text("Remove", color = NeerlyColors.Err)
                        }
                    }
                }
            }
            var newHolidayDate by remember { mutableStateOf("") }
            var newHolidayLabel by remember { mutableStateOf("") }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newHolidayDate,
                onValueChange = { newHolidayDate = it.take(10) },
                placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newHolidayLabel,
                onValueChange = { newHolidayLabel = it.take(120) },
                placeholder = { Text("Label (optional)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Button(
                onClick = {
                    if (newHolidayDate.isNotBlank()) {
                        vm.addHoliday(newHolidayDate, newHolidayLabel.trim().ifBlank { null })
                        newHolidayDate = ""; newHolidayLabel = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.VendorPrimary)
            ) { Text("Add holiday") }

            if (s.error != null) {
                Spacer(Modifier.height(NeerlySpacing.x3))
                Text(s.error!!, color = NeerlyColors.Err, fontSize = 13.sp)
            }
        }
    }
}

/** Coerce "6", "06", "06:0", "6:00" → "06:00:00" so the backend's @JsonFormat parses cleanly. */
private fun paddedTime(input: String): String {
    val parts = input.split(":")
    val h = parts.getOrNull(0)?.padStart(2, '0') ?: "00"
    val m = parts.getOrNull(1)?.padStart(2, '0') ?: "00"
    return "$h:$m:00"
}
