package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import javax.inject.Singleton

/** Backend `VendorDashboardResponse` — the Today counts, including Phase 4's events and licences. */
@JsonClass(generateAdapter = true)
data class VendorDashboard(
    val businessName: String? = null,
    val ordersToday: Int = 0,
    val deliveredToday: Int = 0,
    val pendingAccept: Int = 0,
    val activeOrders: Int = 0,
    val grossToday: BigDecimal = BigDecimal.ZERO,
    val earningToday: BigDecimal = BigDecimal.ZERO,
    val avgRating: BigDecimal? = null,
    val strikesLast30d: Int = 0,
    val missedOffersLast7d: Long = 0,
    val activeSubscriptions: Int = 0,
    val pendingEvents: Int = 0,
    val licencesToRenew: Int = 0,
    val jarsOut: Int = 0,                    // jars at customers' homes on a deposit
    val jarsLate: Int = 0,                   // …of those, past their return window
    val jarsSentToday: Int = 0               // full jars that left the shop today (board VToday)
)

@JsonClass(generateAdapter = true)
data class VendorOrderLine(val productId: String, val productName: String, val quantity: Int, val unitPrice: BigDecimal)

/** Backend `VendorOrderView`, with the late-cancel fields the shared DTO doesn't carry yet. */
@JsonClass(generateAdapter = true)
data class VendorOrderView(
    val id: String,
    val orderNumber: String,
    val customerFirstName: String = "Customer",
    val customerPhoneMask: String = "",
    val deliveryAddress: String = "",
    val distanceKm: BigDecimal? = null,
    val items: List<VendorOrderLine> = emptyList(),
    val orderValue: BigDecimal = BigDecimal.ZERO,
    val yourEarning: BigDecimal = BigDecimal.ZERO,
    val paymentStatus: String = "",
    val slotRequested: String = "",
    val notes: String? = null,
    val status: String,
    val placedAt: String,
    val acceptDeadlineAt: String? = null,
    val jarsGoingOut: Int = 0,
    val emptiesExpected: Int? = null,
    val depositTotal: BigDecimal = BigDecimal.ZERO,
    val driverId: String? = null,
    val cancellationFee: BigDecimal = BigDecimal.ZERO,
    val tripPay: BigDecimal = BigDecimal.ZERO,
    /** The delivery code locked after wrong tries — only then can the shop confirm without it. */
    val codeLocked: Boolean = false,
    val codeLockedUntil: String? = null,
    val driverName: String? = null,
    /** Full numbers for the call buttons — only once accepted and until the order is finished. */
    val customerPhone: String? = null,
    val driverPhone: String? = null,
    /** This order's place among the customer's orders with the shop ("12th order with you"). */
    val customerOrderCount: Int? = null,
    /** What the customer gave as the reason, when they cancelled (board VCustCancelled). */
    val cancelReason: String? = null,
    /** Status history, detail view only. */
    val timeline: List<VendorOrderStep> = emptyList()
) {
    val paidOnline get() = paymentStatus != "COD"
    val itemsLine get() = items.joinToString(", ") { "${it.quantity} × ${it.productName}" }
}

/** Backend earnings entry for one order — the fee lines on board V11. */
@JsonClass(generateAdapter = true)
data class VendorOrderEarning(
    val grossAmount: BigDecimal,
    val commissionRate: BigDecimal,
    val commissionAmount: BigDecimal,
    val netPayable: BigDecimal,
    val deliveryDate: String? = null,
    val paidOut: Boolean = false,
    val entryType: String = "ORDER"
)

@JsonClass(generateAdapter = true)
data class VendorOrderStep(val status: String, val at: String)

/** Team row with Phase 4's live status — who can take a packed order (board V43). */
@JsonClass(generateAdapter = true)
data class VendorTeamLive(
    val driverId: String,
    val name: String,
    val phoneMasked: String = "",
    val acceptedAt: String? = null,
    val liveStatus: String = "OFF",          // ON_DROP / ON_SHIFT / OFF
    val dropsToday: Int = 0,
    val cashHeld: BigDecimal = BigDecimal.ZERO
)

interface VendorOpsApi {
    @GET("api/v1/vendor/dashboard") suspend fun dashboard(): VendorDashboard
    @GET("api/v1/vendor/orders") suspend fun orders(@Query("status") status: String, @Query("size") size: Int = 50): List<VendorOrderView>
    @GET("api/v1/vendor/orders/{id}") suspend fun order(@Path("id") id: String): VendorOrderView
    @GET("api/v1/vendor/earnings/orders/{id}") suspend fun earning(@Path("id") id: String): VendorOrderEarning
    @GET("api/v1/vendor/team") suspend fun team(): List<VendorTeamLive>
    @GET("api/v1/vendor/business/status") suspend fun businessStatus(): VendorBusinessStatus
}

@Module
@InstallIn(SingletonComponent::class)
object VendorOpsModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorOpsApi = retrofit.create(VendorOpsApi::class.java)
}

/** Board V09 tabs. */
enum class OrderTab(@androidx.annotation.StringRes val label: Int) {
    New(R.string.vend_ot_new), Preparing(R.string.vend_ot_preparing), Out(R.string.vend_ot_out), Done(R.string.vend_ot_done)
}

fun tabOf(status: String): OrderTab = when (status) {
    "VENDOR_ASSIGNED" -> OrderTab.New
    "VENDOR_ACCEPTED", "PREPARING" -> OrderTab.Preparing
    "DISPATCHED", "ARRIVING" -> OrderTab.Out
    else -> OrderTab.Done
}
