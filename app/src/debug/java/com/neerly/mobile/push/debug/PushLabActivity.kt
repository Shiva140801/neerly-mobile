package com.neerly.mobile.push.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.neerly.mobile.R
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.push.DriverDutyService
import com.neerly.mobile.push.DriverDutyState
import com.neerly.mobile.push.DutyCard
import com.neerly.mobile.push.PushRouter
import com.neerly.mobile.push.PushType
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Debug-only screen: every catalog push, posted through [PushRouter] exactly as an FCM data
 * message would be. "Ring as if closed" forces the full-screen ring even with the app open.
 */
@AndroidEntryPoint
class PushLabActivity : ComponentActivity() {
    @Inject lateinit var router: PushRouter
    @Inject lateinit var duty: DriverDutyState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var orderId by remember { mutableStateOf("") }
            var background by remember { mutableStateOf(true) }
            var last by remember { mutableStateOf<String?>(null) }
            Column(Modifier.fillMaxSize().background(NeerlyColors.Canvas).systemBarsPadding().padding(16.dp)) {
                Text(stringResource(R.string.push_lab_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                Text(stringResource(R.string.push_lab_sub), fontSize = 13.sp, color = NeerlyColors.Ink600)
                OutlinedTextField(orderId, { orderId = it.trim() }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("orderId (optional, real id makes Accept/Take work)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.push_lab_as_bg), Modifier.weight(1f), fontSize = 14.sp)
                    Switch(background, { background = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        duty.update(true, DutyCard("Sri Balaji", System.currentTimeMillis() - 4 * 3_600_000L - 20 * 60_000L, 9, 14,
                            BigDecimal("460"), "Rajesh K.", 17.4325, 78.4071))
                        DriverDutyService.start(this@PushLabActivity)
                    }) { Text(stringResource(R.string.push_lab_duty)) }
                    TextButton(onClick = { DriverDutyService.end(this@PushLabActivity, BigDecimal("460")); duty.update(false, null) }) {
                        Text(stringResource(R.string.push_lab_duty_end))
                    }
                    TextButton(onClick = { NotificationManagerCompat.from(this@PushLabActivity).cancelAll() }) {
                        Text(stringResource(R.string.push_lab_cancel_all))
                    }
                }
                last?.let { Text(it, fontSize = 12.sp, color = NeerlyColors.Ink600) }
                HorizontalDivider()
                LazyColumn(Modifier.fillMaxSize()) {
                    items(PushType.entries.toList()) { t ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable {
                            val d = router.handle(PushSamples.payload(this@PushLabActivity, t, orderId), forceBackground = background)
                            last = "${t.name} → $d"
                        }.padding(vertical = 8.dp)) {
                            Text("${t.row} · ${t.name}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                            Text("${stringResource(t.sampleTitle)} — ${t.channel.id}", fontSize = 12.sp, color = NeerlyColors.Ink600)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
