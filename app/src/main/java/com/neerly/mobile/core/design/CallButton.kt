package com.neerly.mobile.core.design

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.R

/**
 * "Call" — opens the phone's dialer with the number filled in (ACTION_DIAL, so no call
 * permission and the person still presses the green button themselves). Renders nothing
 * when [phone] is null: the backend only hands out a number while the order is live.
 */
@Composable
fun CallButton(phone: String?, label: String = stringResource(R.string.core_call), modifier: Modifier = Modifier,
               color: Color = NeerlyColors.CustomerPrimary, onDialed: () -> Unit = {}) {
    val number = phone?.filter { it.isDigit() || it == '+' }?.ifBlank { null } ?: return
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            // A tablet with no dialer has nothing to open — say nothing rather than crash.
            val opened = runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.exceptionOrNull() !is ActivityNotFoundException
            if (opened) onDialed()
        },
        modifier = modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(NeerlyRadius.pill),
        border = BorderStroke(1.dp, color),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Filled.Call, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
