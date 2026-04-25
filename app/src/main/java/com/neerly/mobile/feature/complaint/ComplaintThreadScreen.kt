package com.neerly.mobile.feature.complaint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.dto.ComplaintMessageDto

/**
 * Conversation view between customer ↔ admin (vendor messages also visible).
 * Internal admin notes are filtered server-side by role, so customers never
 * see them here. Withdraw is only available while OPEN/TRIAGED.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintThreadScreen(
    onBack: () -> Unit,
    vm: ComplaintThreadViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var draft by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        state.complaint?.let {
                            Text(it.status, fontSize = 11.sp, color = NeerlyColors.Ink500)
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                actions = {
                    state.complaint?.takeIf { it.status in setOf("OPEN", "TRIAGED") }?.let {
                        TextButton(onClick = vm::withdraw) {
                            Text("Withdraw", color = NeerlyColors.Err)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        },
        bottomBar = {
            state.complaint?.takeIf { it.status !in setOf("CLOSED", "WITHDRAWN") }?.let {
                Surface(color = NeerlyColors.Paper, shadowElevation = 4.dp) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.padding(NeerlySpacing.x3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 2000) draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Reply…") },
                            singleLine = false,
                            maxLines = 4
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                vm.appendMessage(draft.trim())
                                draft = ""
                            },
                            enabled = draft.isNotBlank() && !state.sending,
                            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                        ) { Text("Send") }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Loading…", color = NeerlyColors.Ink500)
            }
            state.complaint == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error ?: "Complaint not found", color = NeerlyColors.Err)
            }
            else -> {
                val c = state.complaint!!
                LazyColumn(
                    contentPadding = PaddingValues(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item {
                        Surface(color = NeerlyColors.Paper, shape = RoundedCornerShape(NeerlyRadius.md),
                            shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(NeerlySpacing.x4)) {
                                Text(c.subject, fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                                Text(c.category, fontSize = 11.sp, color = NeerlyColors.Ink500,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(c.description, fontSize = 13.sp, color = NeerlyColors.Ink700)
                                Spacer(Modifier.height(NeerlySpacing.x2))
                                Text("Filed ${c.filedAt.take(10)} · SLA ${c.slaDeadline.take(16).replace("T", " ")}",
                                    fontSize = 11.sp, color = NeerlyColors.Ink500)
                                if (c.slaBreached) {
                                    Text("⚠ SLA breached", fontSize = 12.sp, color = NeerlyColors.Err)
                                }
                                c.resolutionAction?.let {
                                    Spacer(Modifier.height(NeerlySpacing.x2))
                                    Text(
                                        "Resolution: $it" + (c.awardedAmount?.let { a -> " · ₹$a" } ?: ""),
                                        fontSize = 13.sp, color = NeerlyColors.Ok,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    items(c.messages.filterNot { it.isInternal }, key = { it.id }) { MessageRow(it) }
                    state.error?.let { item { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) } }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(msg: ComplaintMessageDto) {
    val fromCustomer = msg.authorRole == "CUSTOMER"
    Surface(
        color = if (fromCustomer) NeerlyColors.CustomerSofter else NeerlyColors.Paper,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(if (fromCustomer) 0.85f else 0.95f)
    ) {
        Column(Modifier.padding(NeerlySpacing.x4)) {
            Text(msg.authorRole, fontSize = 11.sp, color = NeerlyColors.Ink500,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(msg.message, fontSize = 13.sp, color = NeerlyColors.Ink800)
            Text(msg.createdAt.take(16).replace("T", " "), fontSize = 10.sp, color = NeerlyColors.Ink400)
        }
    }
}
