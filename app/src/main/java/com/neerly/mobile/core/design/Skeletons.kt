package com.neerly.mobile.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Per-screen loading skeletons, ported from the `SKEL-*` / `STATE-SHIMMER`
 * artboards. These replace the "Loading…" text each screen used to show.
 *
 * Each skeleton mirrors the *shape* of the screen it stands in for, so the
 * layout doesn't jump when real content arrives — that's the whole point of a
 * skeleton over a spinner.
 */

/** `STATE-SHIMMER` — Home. Tinted hero block, then the vendor list. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val heroBrush = rememberShimmerBrush(
        base = NeerlyColors.CustomerSoft,
        highlight = NeerlyColors.CustomerSofter
    )
    Column(modifier.fillMaxSize().loadingSemantics("Loading home")) {
        // Hero — greeting, address line, search pill.
        Column(
            Modifier
                .fillMaxWidth()
                .background(NeerlyColors.CustomerPrimary)
                .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
        ) {
            SkeletonBlock(height = 12.dp, width = 150.dp, brush = heroBrush)
            Spacer(Modifier.height(NeerlySpacing.x3))
            SkeletonBlock(height = 22.dp, width = 220.dp, radius = NeerlyRadius.xs, brush = heroBrush)
            Spacer(Modifier.height(NeerlySpacing.x4))
            SkeletonBlock(
                height = 46.dp,
                modifier = Modifier.fillMaxWidth(),
                radius = NeerlyRadius.pill,
                brush = heroBrush
            )
        }
        Column(
            Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            SkeletonBlock(height = 64.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
            SkeletonBlock(height = 16.dp, width = 160.dp, brush = brush)
            Row(horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)) {
                SkeletonBlock(height = 110.dp, modifier = Modifier.weight(1f), radius = NeerlyRadius.md, brush = brush)
                SkeletonBlock(height = 110.dp, modifier = Modifier.weight(1f), radius = NeerlyRadius.md, brush = brush)
            }
            SkeletonBlock(height = 16.dp, width = 180.dp, brush = brush)
            SkeletonBlock(height = 120.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.lg, brush = brush)
            SkeletonBlock(height = 120.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.lg, brush = brush)
        }
    }
}

/** `SKEL-VENDOR` — vendor detail. Photo banner, category strip, product rows. */
@Composable
fun VendorDetailSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val heroBrush = rememberShimmerBrush(
        base = NeerlyColors.CustomerSoft,
        highlight = NeerlyColors.CustomerSofter
    )
    Column(modifier.fillMaxSize().loadingSemantics("Loading vendor")) {
        Box(Modifier.fillMaxWidth().height(196.dp).background(heroBrush))
        Row(
            Modifier.fillMaxWidth().padding(vertical = NeerlySpacing.x4),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                SkeletonBlock(height = 26.dp, width = 64.dp, radius = NeerlyRadius.xs, brush = brush)
            }
        }
        Column(
            Modifier.padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            SkeletonBlock(height = 14.dp, width = 120.dp, brush = brush)
            repeat(3) {
                SkeletonBlock(
                    height = 84.dp,
                    modifier = Modifier.fillMaxWidth(),
                    radius = NeerlyRadius.md,
                    brush = brush
                )
            }
        }
    }
}

/**
 * `SKEL-CART` — cart. The canvas is explicit that the CTA bar stays anchored
 * while the list shimmers, so this renders the button placeholder pinned at the
 * bottom rather than as another list row.
 */
@Composable
fun CartSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val ctaBrush = rememberShimmerBrush(base = NeerlyColors.Ink200, highlight = NeerlyColors.Ink100)
    Column(modifier.fillMaxSize().loadingSemantics("Loading cart")) {
        Column(
            Modifier
                .weight(1f)
                .padding(NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            SkeletonBlock(height = 140.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.lg, brush = brush)
            SkeletonBlock(height = 56.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
            SkeletonBlock(height = 170.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.lg, brush = brush)
            SkeletonBlock(height = 64.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(NeerlyColors.Paper)
                .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3)
        ) {
            SkeletonBlock(
                height = 50.dp,
                modifier = Modifier.fillMaxWidth(),
                radius = NeerlyRadius.pill,
                brush = ctaBrush
            )
        }
    }
}

/** `SKEL-TRACK` — tracking. Header, progress rail, map, then the two cards. */
@Composable
fun OrderTrackingSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val railBrush = rememberShimmerBrush(
        base = NeerlyColors.CustomerSoft,
        highlight = NeerlyColors.CustomerSofter
    )
    val mapBrush = rememberShimmerBrush(base = NeerlyColors.OkSoft, highlight = NeerlyColors.Ink50)
    Column(modifier.fillMaxSize().loadingSemantics("Loading tracking")) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x5),
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBlock(height = 15.dp, width = 110.dp, brush = brush)
            SkeletonBlock(height = 11.dp, width = 70.dp, brush = brush)
        }
        Box(Modifier.padding(horizontal = NeerlySpacing.x4)) {
            SkeletonBlock(
                height = 5.dp,
                modifier = Modifier.fillMaxWidth(),
                radius = NeerlyRadius.pill,
                brush = railBrush
            )
        }
        Spacer(Modifier.height(NeerlySpacing.x5))
        Box(Modifier.fillMaxWidth().height(280.dp).background(mapBrush))
        Column(
            Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            SkeletonBlock(height = 100.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
            SkeletonBlock(height = 70.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
        }
    }
}

/** `SKEL-WALLET` — wallet. Balance card, then the transaction rows. */
@Composable
fun WalletSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val balanceBrush = rememberShimmerBrush(base = NeerlyColors.Ink200, highlight = NeerlyColors.Ink100)
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3)
            .loadingSemantics("Loading wallet"),
        verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
    ) {
        SkeletonBlock(
            height = 150.dp,
            modifier = Modifier.fillMaxWidth(),
            radius = NeerlyRadius.xl,
            brush = balanceBrush
        )
        Spacer(Modifier.height(NeerlySpacing.x1))
        SkeletonBlock(height = 12.dp, width = 150.dp, brush = brush)
        repeat(3) {
            SkeletonBlock(height = 62.dp, modifier = Modifier.fillMaxWidth(), radius = NeerlyRadius.md, brush = brush)
        }
    }
}

/** Convenience: a skeleton laid out inside a Scaffold's content padding. */
@Composable
fun SkeletonHost(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding)) { content() }
}
