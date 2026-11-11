package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.PageTitle
import com.neerly.mobile.core.design.PrimaryButton
import com.neerly.mobile.core.design.SectionLabel

/** [glyph] is the tile on the left (board Language: "Aa"); [native] the language's own name. */
data class Language(val code: String, val label: String, val native: String, val glyph: String)

private val LANGUAGES = listOf(
    Language("en", "English", "English", "Aa"),
    Language("te", "Telugu", "తెలుగు", "తె"),
    Language("hi", "Hindi", "हिंदी", "हि")
)

/**
 * Board Language. During sign-up it is a "Before we start" step; from Profile ([onBack]
 * set) it gets the chevron and no eyebrow. Each option is a framed card: glyph tile,
 * the language in its own script, a check on the right when chosen.
 */
@Composable
fun LanguageScreen(
    onPicked: (String) -> Unit,
    languageSync: com.neerly.mobile.core.util.LanguageSync? = null,
    onBack: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(com.neerly.mobile.core.util.AppLocale.current(context)) }

    Scaffold(containerColor = NeerlyColors.Canvas, bottomBar = {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Kept and applied — the choice used to be dropped on the floor.
            PrimaryButton(stringResource(R.string.cust_continue), {
                com.neerly.mobile.core.util.AppLocale.set(context, selected)
                languageSync?.push()
                onPicked(selected)
            })
        }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) com.neerly.mobile.core.design.BackChevron(onBack, stringResource(R.string.core_back))
            else {
                Spacer(Modifier.height(36.dp))
                SectionLabel(stringResource(R.string.cust_language_eyebrow))
            }
            PageTitle(stringResource(R.string.cust_language_title), size = 32)
            Text(stringResource(R.string.cust_language_body), fontSize = 16.sp, lineHeight = 23.sp, color = NeerlyColors.Ink600)
            Spacer(Modifier.height(8.dp))
            LANGUAGES.forEach { lang ->
                val active = selected == lang.code
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(NeerlyColors.Paper)
                        .border(if (active) 2.dp else 1.dp, if (active) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200, RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton) { selected = lang.code }
                        .semantics { this.selected = active }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(Modifier.size(52.dp).background(if (active) NeerlyColors.WarnSoft else NeerlyColors.Ink100, RoundedCornerShape(12.dp))
                        .clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                        Text(lang.glyph, fontSize = 22.sp, color = NeerlyColors.Ink900)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(lang.native, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        Text(lang.label, fontSize = 14.sp, color = NeerlyColors.Ink600)
                    }
                    Box(Modifier.size(26.dp).clip(CircleShape)
                        .background(if (active) NeerlyColors.CustomerPrimary else NeerlyColors.Paper)
                        .border(if (active) 0.dp else 1.5.dp, NeerlyColors.Ink200, CircleShape)
                        .clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                        if (active) Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = NeerlyColors.Paper, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
