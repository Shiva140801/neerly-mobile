package com.neerly.mobile.data.auth

import com.neerly.mobile.data.cart.CartStore
import com.neerly.mobile.data.local.RecentSearchStore
import com.neerly.mobile.feature.driver.DriverJobStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What one person leaves on the phone that the next one must not see. Device settings
 * (the phone's id, alert sound, language) stay; per-order delivery codes are keyed by
 * order and only shown on that order's own screen, so they stay too.
 */
@Singleton
class SignedOutCleanup @Inject constructor(
    private val cart: CartStore,
    private val searches: RecentSearchStore,
    private val driverJobs: DriverJobStore
) {
    suspend fun clear() {
        cart.clear()
        runCatching { searches.clear() }
        driverJobs.keepOnly(emptySet())
    }
}
