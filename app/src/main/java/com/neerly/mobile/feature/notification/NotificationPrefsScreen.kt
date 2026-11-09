package com.neerly.mobile.feature.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.InfoRow
import com.neerly.mobile.core.design.LoadState
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.OnReturn
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.Tone
import com.neerly.mobile.push.PushPermissions

/** Board NotifPrefs — "What we tell you about". Order updates are always on; offers start off. */
@Composable
fun NotificationPrefsScreen(
    onBack: () -> Unit,
    vm: NotificationPrefsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    OnReturn { tick++ }
    val systemOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    BoondhPage(title = stringResource(R.string.push_prefs_title), onBack = onBack) {
        if (!systemOn) item {
            Column {
                Notice(stringResource(R.string.push_prefs_blocked), Tone.Warn)
                SecondaryButton(stringResource(R.string.push_prefs_turn_on), { context.startActivity(PushPermissions.appNotificationSettings(context)) })
            }
        }
        if (s.loading) { item { LoadState(true, null, vm::refresh) }; return@BoondhPage }
        s.error?.let { e -> item { Notice(e, Tone.Bad) } }
        item {
            BoondhCard {
                NotificationPrefsViewModel.Topic.entries.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = NeerlyColors.Ink200)
                    TopicRow(stringResource(t.title), stringResource(t.sub), vm.isOn(t), t.locked) { vm.toggle(t, it) }
                }
            }
        }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.push_prefs_phone), stringResource(R.string.push_prefs_phone_sub),
                    onClick = { context.startActivity(PushPermissions.appNotificationSettings(context)) })
            }
        }
    }
}

@Composable
private fun TopicRow(title: String, sub: String, on: Boolean, locked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp).alpha(if (locked) 0.6f else 1f)
        .semantics { contentDescription = "$title. $sub" },
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
            Text(sub, fontSize = 13.sp, color = NeerlyColors.Ink600)
        }
        Switch(checked = on, onCheckedChange = onChange, enabled = !locked)
    }
}
