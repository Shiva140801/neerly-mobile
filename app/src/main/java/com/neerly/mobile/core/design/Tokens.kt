package com.neerly.mobile.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Boondh design tokens — the redesign canvas ("new flows/Boondh Redesign") uses one brand
 * palette for all three apps: navy on warm cream paper, ink text, amber/green/red signals.
 * The role names are kept so every screen picks up the brand without a rewrite; they all
 * resolve to the same navy now (the old blue/teal/orange split is gone in the redesign).
 */
object NeerlyColors {
    private val Navy     = Color(0xFF233580)
    private val NavyDark = Color(0xFF1F2C66)
    private val NavySoft = Color(0xFFE6E9F4)
    private val NavySofter = Color(0xFFF2F4FA)

    // Role primaries — one brand
    val CustomerPrimary = Navy;  val CustomerDark = NavyDark;  val CustomerSoft = NavySoft;  val CustomerSofter = NavySofter
    val VendorPrimary   = Navy;  val VendorDark   = NavyDark;  val VendorSoft   = NavySoft;  val VendorSofter   = NavySofter
    val DriverPrimary   = Navy;  val DriverDark   = NavyDark;  val DriverSoft   = NavySoft;  val DriverSofter   = NavySofter
    val AdminPrimary    = Navy;  val AdminDark    = NavyDark;  val AdminSoft    = NavySoft;  val AdminSofter    = NavySofter

    // Neutrals — ink on cream
    val Ink900 = Color(0xFF1A2238)
    val Ink800 = Color(0xFF242C44)
    val Ink700 = Color(0xFF353D55)
    val Ink600 = Color(0xFF4F566B)   // body-secondary: ~7:1 on cream
    val Ink500 = Color(0xFF5E6478)   // smallest grey text still ≥4.5:1 on cream
    val Ink400 = Color(0xFF8E93A3)
    val Ink300 = Color(0xFFC9C3B4)
    val Ink200 = Color(0xFFE4DCCB)   // hairline
    val Ink100 = Color(0xFFEFE9DC)
    val Ink50  = Color(0xFFF8F4EC)
    val Paper  = Color(0xFFFFFCF6)
    val Canvas = Color(0xFFF6F1E7)   // cream

    // Semantic
    val Ok      = Color(0xFF2E6E4B); val OkSoft   = Color(0xFFE3EFE7)
    val Warn    = Color(0xFF9A6A0B); val WarnSoft = Color(0xFFFBEFD2)   // amber ink; #E6A321 is the fill
    val Err     = Color(0xFFB23A2B); val ErrSoft  = Color(0xFFF7E3DF)

    // Water accent
    val Water50  = Color(0xFFEEF1F8)
    val Water100 = Color(0xFFDDE3F2)
    val Water500 = Color(0xFF3B57B8)
}

object NeerlyRadius {
    val xs = 6.dp; val sm = 10.dp; val md = 14.dp; val lg = 18.dp; val xl = 24.dp
    val pill = 999.dp
}

object NeerlyElevation {
    val card   = 1.dp
    val raised = 4.dp
    val dialog = 16.dp
}

object NeerlySpacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x10 = 40.dp
}

/** Role-scoped accent tuple — NeerlyTheme chooses one at runtime. */
data class RoleAccent(
    val primary: Color,
    val dark: Color,
    val soft: Color,
    val softer: Color
) {
    companion object {
        fun of(role: Role): RoleAccent = when (role) {
            Role.CUSTOMER -> RoleAccent(NeerlyColors.CustomerPrimary, NeerlyColors.CustomerDark,
                NeerlyColors.CustomerSoft, NeerlyColors.CustomerSofter)
            Role.VENDOR   -> RoleAccent(NeerlyColors.VendorPrimary,   NeerlyColors.VendorDark,
                NeerlyColors.VendorSoft,   NeerlyColors.VendorSofter)
            Role.DRIVER   -> RoleAccent(NeerlyColors.DriverPrimary,   NeerlyColors.DriverDark,
                NeerlyColors.DriverSoft,   NeerlyColors.DriverSofter)
            Role.ADMIN    -> RoleAccent(NeerlyColors.AdminPrimary,    NeerlyColors.AdminDark,
                NeerlyColors.AdminSoft,    NeerlyColors.AdminSofter)
        }
    }
}
