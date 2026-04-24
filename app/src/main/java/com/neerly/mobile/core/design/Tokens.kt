package com.neerly.mobile.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Neerly design tokens — ported 1:1 from design-reference/design.css.
 * Don't invent new colors here without updating neerly-backend/docs/DESIGN_SYSTEM.md first.
 */
object NeerlyColors {
    // Role: Customer
    val CustomerPrimary = Color(0xFF2E75B6)
    val CustomerDark    = Color(0xFF1E5A92)
    val CustomerSoft    = Color(0xFFE5F0F9)
    val CustomerSofter  = Color(0xFFF1F7FC)

    // Role: Vendor
    val VendorPrimary   = Color(0xFF17A2B8)
    val VendorDark      = Color(0xFF0B7C8E)
    val VendorSoft      = Color(0xFFE3F5F8)
    val VendorSofter    = Color(0xFFF0FAFB)

    // Role: Driver
    val DriverPrimary   = Color(0xFFF07820)
    val DriverDark      = Color(0xFFC5611A)
    val DriverSoft      = Color(0xFFFDECDF)
    val DriverSofter    = Color(0xFFFEF5EC)

    // Role: Admin
    val AdminPrimary    = Color(0xFF7B2CBF)
    val AdminDark       = Color(0xFF5E1F96)
    val AdminSoft       = Color(0xFFEFE5F8)
    val AdminSofter    = Color(0xFFF6F0FB)

    // Neutrals (ink scale)
    val Ink900 = Color(0xFF0E1A24)
    val Ink800 = Color(0xFF17252F)
    val Ink700 = Color(0xFF2B3A46)
    val Ink600 = Color(0xFF4B5A66)
    val Ink500 = Color(0xFF6B7B88)
    val Ink400 = Color(0xFF9AA8B3)
    val Ink300 = Color(0xFFC4CDD5)
    val Ink200 = Color(0xFFE1E7EC)
    val Ink100 = Color(0xFFEEF2F5)
    val Ink50  = Color(0xFFF6F8FA)
    val Paper  = Color(0xFFFFFFFF)
    val Canvas = Color(0xFFF0EEE8)

    // Semantic
    val Ok      = Color(0xFF1E9E6A); val OkSoft   = Color(0xFFE1F3EA)
    val Warn    = Color(0xFFD98E1B); val WarnSoft = Color(0xFFFBEED7)
    val Err     = Color(0xFFD73F3F); val ErrSoft  = Color(0xFFFAE3E3)

    // Water hero tint
    val Water50  = Color(0xFFEBF6FA)
    val Water100 = Color(0xFFD5EDF3)
    val Water500 = Color(0xFF2EA3C6)
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
