package com.neerly.mobile.feature.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PageTitle

/** SOS red is reserved for SOS (brief rule 13). */
internal val SosRed = Color(0xFFB23A2B)

/**
 * The red "⚠ SOS" chip every driver step board carries top-right. One tap opens the SOS
 * screen, where the real alert needs a 3-second hold — so a pocket tap can't send it.
 */
@Composable
internal fun SosChip(onSos: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.drv_sos_short)
    Surface(onClick = onSos, shape = RoundedCornerShape(28.dp), color = SosRed,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = label }) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * A driver step page as the D-boards lay it out: the step label (caps) and its sub-line
 * on top with SOS beside them, then the big serif title, then the content; the primary
 * action sits in the footer above the tab bar.
 */
@Composable
internal fun DriverStepPage(
    eyebrow: String,
    title: String,
    eyebrowSub: String? = null,
    onSos: (() -> Unit)? = null,
    eyebrowColor: Color = NeerlyColors.Ink600,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Scaffold(containerColor = NeerlyColors.Canvas, bottomBar = {
        if (footer != null) Column(Modifier.fillMaxWidth().background(NeerlyColors.Paper).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(eyebrow.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = eyebrowColor)
                        eyebrowSub?.let { Text(it, fontSize = 15.sp, color = NeerlyColors.Ink600) }
                    }
                    onSos?.let { Box(Modifier.padding(start = 12.dp)) { SosChip(it) } }
                }
            }
            item { PageTitle(title) }
            content()
        }
    }
}

/**
 * DGpsOff "Signal trouble": checks the two things a driver can lose — the phone's
 * location and the internet — and says what still works without each. Opened from
 * "Signal trouble?" on duty (it used to only re-ask the location permission).
 */
@Composable
internal fun SignalCheckScreen(onBack: () -> Unit, onAskPermission: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    // Re-check when the driver comes back from Settings.
    com.neerly.mobile.core.design.OnReturn { tick++ }
    val permitted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    val gpsOn = tick >= 0 && permitted && androidx.core.location.LocationManagerCompat.isLocationEnabled(lm)
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val online = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    com.neerly.mobile.core.design.BoondhPage(title = stringResource(R.string.drv_signal_title), onBack = onBack) {
        item {
            com.neerly.mobile.core.design.BoondhCard {
                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.drv_signal_gps),
                    if (gpsOn) stringResource(R.string.drv_signal_gps_ok) else stringResource(R.string.drv_signal_gps_off),
                    if (gpsOn) "✓" else "!", trailingColor = if (gpsOn) NeerlyColors.Ok else NeerlyColors.Err)
                if (!gpsOn) com.neerly.mobile.core.design.PrimaryButton(
                    if (!permitted) stringResource(R.string.drv_allow_location) else stringResource(R.string.drv_turn_on_location),
                    { if (!permitted) onAskPermission() else runCatching {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
            }
        }
        item {
            com.neerly.mobile.core.design.BoondhCard {
                com.neerly.mobile.core.design.InfoRow(stringResource(R.string.drv_signal_net),
                    if (online) stringResource(R.string.drv_signal_net_ok) else stringResource(R.string.drv_signal_net_off),
                    if (online) "✓" else "!", trailingColor = if (online) NeerlyColors.Ok else NeerlyColors.Err)
            }
        }
        item { com.neerly.mobile.core.design.Notice(stringResource(R.string.drv_signal_calls_work), com.neerly.mobile.core.design.Tone.Info) }
    }
}
