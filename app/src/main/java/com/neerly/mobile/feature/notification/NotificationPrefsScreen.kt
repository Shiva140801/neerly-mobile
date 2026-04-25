package com.neerly.mobile.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * Notification preferences screen — channel × category matrix of switches.
 * Optimistic updates with auto-rollback on backend error (handled by VM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPrefsScreen(
    onBack: () -> Unit,
    vm: NotificationPrefsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
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
            Text(
                "Choose how you'd like to hear from us. We'll always show critical " +
                    "delivery updates in the in-app feed.",
                fontSize = 13.sp, color = NeerlyColors.Ink600
            )
            Spacer(Modifier.height(NeerlySpacing.x4))

            if (s.loading) {
                Text("Loading…", color = NeerlyColors.Ink500)
                return@Column
            }

            vm.categories.forEach { (key, label) ->
                Surface(
                    color = NeerlyColors.Paper,
                    shape = RoundedCornerShape(NeerlyRadius.md),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(NeerlySpacing.x4)) {
                        Text(label, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Spacer(Modifier.height(8.dp))
                        vm.channels.forEach { ch ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(ch, Modifier.weight(1f),
                                    fontSize = 13.sp, color = NeerlyColors.Ink800)
                                Switch(
                                    checked = vm.isEnabled(ch, key),
                                    onCheckedChange = { vm.toggle(ch, key, it) }
                                )
                            }
                        }
                    }
                }
            }

            if (s.error != null) {
                Text(s.error!!, color = NeerlyColors.Err, fontSize = 13.sp)
            }
        }
    }
}
