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
