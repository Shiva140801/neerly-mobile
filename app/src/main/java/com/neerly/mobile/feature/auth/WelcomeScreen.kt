package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.AuthPageScaffold
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SecondaryButton
import com.neerly.mobile.core.util.AppLocale

/** Boondh board 01 — Welcome. The language row switches the app before anything else. */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onVendorRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    AuthPageScaffold {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("boondh", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = NeerlyColors.CustomerPrimary,
                modifier = Modifier.weight(1f))
            listOf("en" to "EN", "te" to "తె", "hi" to "हि").forEach { (tag, label) ->
                TextButton(onClick = { AppLocale.set(context, tag) }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(72.dp).background(NeerlyColors.CustomerDark, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Text("💧", fontSize = 34.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.cust_welcome_title), fontFamily = FontFamily.Serif,
            fontSize = 34.sp, lineHeight = 40.sp, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.cust_welcome_body),
            fontSize = 16.sp, lineHeight = 24.sp, color = NeerlyColors.Ink600)
        Spacer(Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.cust_welcome_continue_phone), onGetStarted)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(stringResource(R.string.cust_welcome_sell), onVendorRegister)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.cust_welcome_terms), fontSize = 13.sp, color = NeerlyColors.Ink600,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
