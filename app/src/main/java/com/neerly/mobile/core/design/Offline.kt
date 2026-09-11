package com.neerly.mobile.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * `S-COMMON-OFFLINE` — the two offline surfaces the canvas specifies.
 *
 *  * [OfflineScreen] blocks a cold start that has nothing to show. The app has
 *    no on-disk cache, so there is genuinely no content behind this — the
 *    canvas's greyed "last synced home" is not rendered, because grey boxes
 *    standing in for data we never stored would be fiction.
 *  * [OfflineBanner] is the non-blocking mid-session strip: it never hides
 *    what's already on screen.
 *
 * Both lead with the same honest split — what keeps working without a
 * connection and what does not. That split is the point of the screen; a bare
 * "no internet" toast is what it replaces.
 */

/** What still works when the connection drops, and what doesn't. */
private val WorksOffline = listOf(
    "The order status you already have on screen",
    "Everything you've already loaded this session"
)

private val NeedsInternet = listOf(
    "Placing a new order",
    "Payments and wallet top-ups",
    "Live status updates and new deliveries"
)

/**
 * Full-screen blocker for a cold start with no data.
 *
 * @param onRetry re-checks connectivity and re-runs the failed load.
 */
@Composable
fun OfflineScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "You're offline",
    body: String = "Neerly needs a connection to load this. Check your Wi-Fi or mobile data and try again."
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeerlyColors.Canvas)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(NeerlyColors.Ink50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = NeerlyColors.Ink400,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(Modifier.height(NeerlySpacing.x6))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(
            body,
            fontSize = 13.5.sp,
            color = NeerlyColors.Ink500,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x6))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text("Try again", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.height(NeerlySpacing.x6))
        OfflineCapabilityCard(Modifier.fillMaxWidth())
    }
}

/**
 * Mid-session strip. Sits above the screen's own content and pushes nothing
 * off — the customer keeps whatever already loaded.
 */
@Composable
fun OfflineBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "Tracking stays on screen · payments paused"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NeerlyColors.Ink800)
            .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
    ) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = NeerlyColors.Paper,
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                "Connection lost",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = NeerlyColors.Paper
            )
            Text(subtitle, fontSize = 11.sp, color = NeerlyColors.Ink400)
        }
        Surface(
            color = NeerlyColors.Paper,
            shape = RoundedCornerShape(NeerlyRadius.pill)
        ) {
            TextButton(onClick = onRetry, contentPadding = PaddingValuesRetry) {
                Text(
                    "Retry",
                    color = NeerlyColors.Ink900,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private val PaddingValuesRetry = androidx.compose.foundation.layout.PaddingValues(
    horizontal = NeerlySpacing.x3,
    vertical = NeerlySpacing.x1
)

/**
 * The "works offline / needs internet" explainer. Shown inside the full-screen
 * state and, while offline, on tracking — the one screen a customer is most
 * likely to be staring at when their signal drops.
 */
@Composable
fun OfflineCapabilityCard(modifier: Modifier = Modifier) {
    Surface(
        color = NeerlyColors.WarnSoft,
        shape = RoundedCornerShape(NeerlyRadius.md),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(NeerlySpacing.x4),
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = NeerlyColors.Warn,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x1)) {
                Text(
                    "Works offline",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink800
                )
                WorksOffline.forEach { Bullet(it) }
                Spacer(Modifier.height(NeerlySpacing.x2))
                Text(
                    "Needs internet",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeerlyColors.Ink800
                )
                NeedsInternet.forEach { Bullet(it) }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)) {
        Text("·", fontSize = 12.sp, color = NeerlyColors.Ink600, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 12.sp, color = NeerlyColors.Ink600, lineHeight = 17.sp)
    }
}

/**
 * "Last updated 9:38 AM · showing cached status" — the stale-data stamp the
 * canvas puts under the order number while offline. Only rendered when we
 * actually have a fetch timestamp to quote.
 */
@Composable
fun StaleDataStamp(lastUpdatedLabel: String, modifier: Modifier = Modifier) {
    Text(
        "Last updated $lastUpdatedLabel · showing the status we already had",
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = NeerlyColors.Warn,
        modifier = modifier
    )
}
