package com.neerly.mobile.push

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.InfoRow
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.OnReturn
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.Tone

/** Runtime checks for the permissions in plan §e. */
object PushPermissions {
    fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Android 14+ grants full-screen intents only to calling/alarm apps by default. */
    fun fullScreenAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true

    fun dndAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun fullScreenSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        else appNotificationSettings(context)

    fun appNotificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun channelSettings(context: Context, channel: PushChannel): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channel.id)

    private fun prefs(context: Context) = context.getSharedPreferences("push_permissions", Context.MODE_PRIVATE)
    internal fun alreadyAsked(context: Context, key: String) = prefs(context).getBoolean(key, false)
    internal fun markAsked(context: Context, key: String) = prefs(context).edit().putBoolean(key, true).apply()
}

/**
 * Asks for POST_NOTIFICATIONS once, the first time [ask] becomes true — at the moment the
 * user can see why (first order placed, shop onboarding done, "Start duty"), never at install.
 */
@Composable
fun AskNotificationsOnce(key: String, ask: Boolean) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(ask) {
        if (!ask || Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        if (PushPermissions.notificationsAllowed(context) || PushPermissions.alreadyAsked(context, key)) return@LaunchedEffect
        PushPermissions.markAsked(context, key)
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Seller and driver settings: what's missing for rings and SOS to work, each with the
 * button that fixes it. Shows nothing when all is granted (except the DND line).
 */
@Composable
fun RingReadinessCard(role: PushRole) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    OnReturn { tick++ }  // re-check after the user comes back from Settings
    val notifications = remember(tick) { PushPermissions.notificationsAllowed(context) }
    val fullScreen = remember(tick) { PushPermissions.fullScreenAllowed(context) }
    val dnd = remember(tick) { PushPermissions.dndAccess(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!notifications) {
            Notice(stringResource(if (role == PushRole.DRIVER) R.string.push_perm_driver else R.string.push_perm_seller), Tone.Warn)
            SecondaryButton(stringResource(R.string.push_perm_allow), {
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(PushPermissions.appNotificationSettings(context))
            })
        }
        if (notifications && !fullScreen) {
            BoondhCard {
                InfoRow(stringResource(R.string.push_fsi_title), stringResource(R.string.push_fsi_body))
            }
            SecondaryButton(stringResource(R.string.push_fsi_open), { context.startActivity(PushPermissions.fullScreenSettings(context)) })
        }
        if (role == PushRole.SELLER || role == PushRole.DRIVER) {
            if (dnd) Notice(stringResource(R.string.push_dnd_on), Tone.Ok)
            else BoondhCard {
                InfoRow(stringResource(R.string.push_dnd_title), stringResource(R.string.push_dnd_body),
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) })
            }
        }
    }
}

/** Debug builds only: a row that opens Push lab (src/debug). Renders nothing in release. */
@Composable
fun PushLabEntry() {
    if (!com.neerly.mobile.BuildConfig.DEBUG) return
    val context = LocalContext.current
    BoondhCard {
        InfoRow(stringResource(R.string.push_lab_title), stringResource(R.string.push_lab_sub), onClick = {
            runCatching { context.startActivity(Intent().setClassName(context, PUSH_LAB_CLASS)) }
        })
    }
}

/** Lives in src/debug so it never ships. */
const val PUSH_LAB_CLASS = "com.neerly.mobile.push.debug.PushLabActivity"
