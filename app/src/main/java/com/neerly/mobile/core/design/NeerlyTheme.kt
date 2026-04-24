package com.neerly.mobile.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRoleAccent = staticCompositionLocalOf { RoleAccent.of(Role.CUSTOMER) }

@Composable
fun NeerlyTheme(
    role: Role = Role.CUSTOMER,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accent = RoleAccent.of(role)

    val colors = lightColorScheme(
        primary = accent.primary,
        onPrimary = NeerlyColors.Paper,
        primaryContainer = accent.soft,
        onPrimaryContainer = accent.dark,
        secondary = NeerlyColors.Ink700,
        onSecondary = NeerlyColors.Paper,
        background = NeerlyColors.Canvas,
        onBackground = NeerlyColors.Ink900,
        surface = NeerlyColors.Paper,
        onSurface = NeerlyColors.Ink900,
        surfaceVariant = NeerlyColors.Ink50,
        onSurfaceVariant = NeerlyColors.Ink700,
        outline = NeerlyColors.Ink300,
        error = NeerlyColors.Err,
        errorContainer = NeerlyColors.ErrSoft
    )

    CompositionLocalProvider(LocalRoleAccent provides accent) {
        MaterialTheme(
            colorScheme = colors,
            typography = NeerlyType,
            content = content
        )
    }
}
