package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

data class Language(val code: String, val label: String, val native: String)

private val LANGUAGES = listOf(
    Language("en", "English", "English"),
    Language("te", "Telugu", "తెలుగు"),
    Language("hi", "Hindi", "हिंदी")
)

@Composable
fun LanguageScreen(onPicked: (String) -> Unit, languageSync: com.neerly.mobile.core.util.LanguageSync? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(com.neerly.mobile.core.util.AppLocale.current(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .padding(horizontal = NeerlySpacing.x6, vertical = NeerlySpacing.x5)
    ) {
        Text(stringResource(R.string.cust_language_title), fontSize = 32.sp, lineHeight = 38.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(stringResource(R.string.cust_language_body), fontSize = 14.sp, color = NeerlyColors.Ink600)
        Spacer(Modifier.height(NeerlySpacing.x6))

        LANGUAGES.forEach { lang ->
            val active = selected == lang.code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(NeerlyRadius.md))
                    .background(if (active) NeerlyColors.CustomerSofter else NeerlyColors.Paper)
                    .then(
                        if (active) Modifier.padding(1.dp)
                        else Modifier.padding(1.dp)
                    )
                    .clickable { selected = lang.code }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = active,
                    onClick = { selected = lang.code },
                    colors = RadioButtonDefaults.colors(selectedColor = NeerlyColors.CustomerPrimary)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(lang.native, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                    Text(lang.label, fontSize = 12.sp, color = NeerlyColors.Ink500)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            // Kept and applied — the choice used to be dropped on the floor.
            onClick = {
                com.neerly.mobile.core.util.AppLocale.set(context, selected)
                languageSync?.push()
                onPicked(selected)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) {
            Text(stringResource(R.string.cust_continue), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
