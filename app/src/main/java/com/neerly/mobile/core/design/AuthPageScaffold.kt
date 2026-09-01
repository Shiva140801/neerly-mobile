package com.neerly.mobile.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Shared page container for the sign-up flow (Welcome / Phone / OTP / Name).
 *
 * Two things every one of those screens needs and none of them had:
 *
 *  * **Keyboard safety** — [Modifier.imePadding] shrinks the page to the space
 *    above the IME, so a primary CTA pinned to the bottom stays on screen and
 *    tappable while the user is still typing.
 *  * **Scroll** — the column is scrollable but held to at least the height of
 *    the visible area, so `Modifier.weight(1f)` still pushes the CTA to the
 *    bottom on a tall screen, while a short screen (or an open keyboard) simply
 *    scrolls instead of clipping the button off the bottom.
 *
 * Status-bar insets are handled once for the whole app in `MainActivity`.
 */
@Composable
fun AuthPageScaffold(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = NeerlySpacing.x6,
    verticalPadding: Dp = NeerlySpacing.x5,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .imePadding()
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content
        )
    }
}
