package com.neerly.mobile.feature.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import timber.log.Timber

/**
 * Board Otp: "Reading the SMS for you…". Uses the SMS User Consent API — Android shows
 * its own one-tap "Allow Boondh to read this message?" sheet for the single SMS that
 * arrives, so the app never holds READ_SMS. When Firebase Phone Auth is switched on it
 * auto-verifies on its own; this stays as the fallback for any code SMS.
 *
 * [key] restarts listening (e.g. after "Resend"). Returns true while listening, so the
 * screen can say so.
 */
@Composable
fun rememberSmsConsent(key: Any?, onCode: (String) -> Unit): Boolean {
    val context = LocalContext.current
    val latestOnCode by rememberUpdatedState(onCode)
    var listening by remember { mutableStateOf(false) }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        if (result.resultCode == Activity.RESULT_OK) {
            otpFromSms(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))?.let { latestOnCode(it) }
        }
    }

    DisposableEffect(key) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val status = intent.extras?.let {
                    @Suppress("DEPRECATION") it.get(SmsRetriever.EXTRA_STATUS) as? Status
                } ?: return
                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val ask = intent.extras?.let {
                            @Suppress("DEPRECATION") it.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        }
                        // Only ever launch Google Play services' own consent sheet, and never
                        // hand it URI grants — the extra comes from a broadcast.
                        val safe = ask?.takeIf {
                            it.resolveActivity(ctx.packageManager)?.packageName == GMS_PACKAGE &&
                                it.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0
                        }
                        if (safe != null) runCatching { consent.launch(safe) }.onFailure { listening = false }
                        else listening = false
                    }
                    else -> listening = false // TIMEOUT after 5 minutes
                }
            }
        }
        val started = runCatching {
            SmsRetriever.getClient(context).startSmsUserConsent(null)
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                SmsRetriever.SEND_PERMISSION, null, ContextCompat.RECEIVER_EXPORTED
            )
        }.onFailure { Timber.w(it, "SMS consent unavailable") }.isSuccess
        listening = started
        onDispose {
            if (started) runCatching { context.unregisterReceiver(receiver) }
            listening = false
        }
    }
    return listening
}

private const val GMS_PACKAGE = "com.google.android.gms"
