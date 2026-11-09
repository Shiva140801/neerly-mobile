package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.neerly.mobile.core.design.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VendorAlertSettingsViewModel @Inject constructor(private val store: VendorAlertPrefsStore) : ViewModel() {
    private val _prefs = MutableStateFlow(store.load())
    val prefs = _prefs.asStateFlow()
    fun update(f: AlertPrefs.() -> AlertPrefs) { _prefs.value = _prefs.value.f().also(store::save) }
}

/** Board V44 — how this phone rings for a new order. Saved on this phone only. */
@Composable
fun VendorAlertSettingsScreen(onBack: () -> Unit, vm: VendorAlertSettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsState()
    val context = LocalContext.current
    val player = remember { AlertPlayer(context.applicationContext) }
    var testing by remember { mutableStateOf(false) }
    LaunchedEffect(testing) { if (testing) { player.start(p.copy(repeatUntilAnswered = false)); delay(4_000); player.stop(); testing = false } }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    BoondhPage(
        title = stringResource(R.string.vend_alert_title), onBack = onBack,
        subtitle = stringResource(R.string.vend_alert_sub)
    ) {
        // What new-order rings and driver SOS need from Android (plan §e).
        item { com.neerly.mobile.push.RingReadinessCard(com.neerly.mobile.push.PushRole.SELLER) }
        item {
            BoondhCard {
                Toggle(stringResource(R.string.vend_alert_silent), stringResource(R.string.vend_alert_silent_sub), p.ringOnSilent) { v -> vm.update { copy(ringOnSilent = v) } }
                HorizontalDivider(color = NeerlyColors.Ink200)
                Toggle(stringResource(R.string.vend_alert_repeat), stringResource(R.string.vend_alert_repeat_sub), p.repeatUntilAnswered) { v -> vm.update { copy(repeatUntilAnswered = v) } }
                HorizontalDivider(color = NeerlyColors.Ink200)
                Toggle(stringResource(R.string.vend_alert_vibrate), stringResource(R.string.vend_alert_vibrate_sub), p.vibrate) { v -> vm.update { copy(vibrate = v) } }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_alert_sound)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertSound.entries.forEach { snd ->
                    FilterChip(selected = p.sound == snd, onClick = { vm.update { copy(sound = snd) } },
                        label = { Text(stringResource(snd.label), fontSize = 15.sp) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        item { SecondaryButton(stringResource(if (testing) R.string.vend_alert_playing else R.string.vend_alert_test), { testing = true }, enabled = !testing) }
        item { Text(stringResource(R.string.vend_alert_saved_here), fontSize = 13.sp, color = NeerlyColors.Ink600) }
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(sub, fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}
