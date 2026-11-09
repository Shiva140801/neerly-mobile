package com.neerly.mobile.feature.auth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neerly.mobile.R
import com.neerly.mobile.core.design.BoondhCard
import com.neerly.mobile.core.design.BoondhPage
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.Notice
import com.neerly.mobile.core.design.OnReturn
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.design.SectionLabel
import com.neerly.mobile.core.design.Tone

/**
 * Board LocationDenied — location is off for Boondh. Three steps, "Open settings" (this
 * app's own settings page), a check when the customer comes back, and a way out:
 * "Type my address instead". [onContinue] fires once location is allowed.
 */
@Composable
fun LocationDeniedScreen(onContinue: () -> Unit, onTypeAddress: () -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var opened by rememberSaveable { mutableStateOf(false) }
    var granted by rememberSaveable { mutableStateOf(locationAllowed(context)) }
    OnReturn { granted = locationAllowed(context) }

    BoondhPage(
        title = stringResource(R.string.core_locden_title),
        onBack = onBack,
        footer = {
            if (opened && !granted) Notice(stringResource(R.string.core_locden_still_off), Tone.Warn)
            if (granted) PrimaryButton(stringResource(R.string.core_locden_continue), onContinue)
            else PrimaryButton(stringResource(R.string.core_locden_open_settings), {
                opened = true
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            })
            SecondaryButton(stringResource(R.string.core_locden_type), onTypeAddress)
        }
    ) {
        item { Text(stringResource(R.string.core_locden_body), fontSize = 16.sp, lineHeight = 24.sp, color = NeerlyColors.Ink700) }
        item {
            BoondhCard {
                SectionLabel(stringResource(R.string.core_locden_how))
                listOf(R.string.core_locden_step1, R.string.core_locden_step2, R.string.core_locden_step3).forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(step), fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink700)
                    }
                }
            }
        }
        if (opened && !granted) item { Notice(stringResource(R.string.core_locden_opened), Tone.Info) }
    }
}

fun locationAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
