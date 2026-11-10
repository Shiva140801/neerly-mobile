package com.neerly.mobile.core.design

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.neerly.mobile.R

/**
 * The one Log out control for every role: a full-width outlined button in the error
 * colour that asks first. A stray tap at the bottom of a long settings page used to
 * sign people out with no way back.
 */
@Composable
fun LogoutButton(label: String, onConfirmed: () -> Unit, busy: Boolean = false, busyLabel: String? = null) {
    var asking by remember { mutableStateOf(false) }
    SecondaryButton(if (busy) busyLabel ?: label else label, { asking = true }, enabled = !busy, color = NeerlyColors.Err)
    if (asking) ConfirmLogoutDialog(onConfirm = { asking = false; onConfirmed() }, onDismiss = { asking = false })
}

@Composable
fun ConfirmLogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.core_logout_title)) },
        text = { Text(stringResource(R.string.core_logout_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.core_logout_yes), color = NeerlyColors.Err, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_logout_stay)) } }
    )
}
