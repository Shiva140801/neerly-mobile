package com.neerly.mobile.feature.notification

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

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
/** Board NotifPrefs — "What we tell you about": each topic, each channel. */
@Composable
fun NotificationPrefsScreen(
    onBack: () -> Unit,
    vm: NotificationPrefsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val channelNames = mapOf("PUSH" to stringResource(R.string.acct_ntf_ch_push), "SMS" to "SMS", "EMAIL" to stringResource(R.string.acct_ntf_ch_email), "WHATSAPP" to "WhatsApp")
    com.neerly.mobile.core.design.BoondhPage(
        title = stringResource(R.string.acct_ntf_prefs_title),
        onBack = onBack,
        subtitle = stringResource(R.string.acct_ntf_prefs_sub)
    ) {
        if (s.loading) { item { com.neerly.mobile.core.design.LoadState(true, null, vm::refresh) }; return@BoondhPage }
        s.error?.let { e -> item { com.neerly.mobile.core.design.Notice(e, com.neerly.mobile.core.design.Tone.Bad) } }
        vm.categories.forEach { (key, label) ->
            item {
                com.neerly.mobile.core.design.BoondhCard {
                    Text(stringResource(label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    vm.channels.forEach { ch ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(channelNames[ch] ?: ch, Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink800)
                            Switch(checked = vm.isEnabled(ch, key), onCheckedChange = { vm.toggle(ch, key, it) })
                        }
                    }
                }
            }
        }
    }
}
