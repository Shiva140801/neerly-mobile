package com.neerly.mobile.feature.vendor.team

import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import javax.inject.Singleton

/** Backend team row with today's live figures. liveStatus: ON_DROP / ON_SHIFT / OFF. */
@JsonClass(generateAdapter = true)
data class TeamDriver(
    val membershipId: String,
    val driverId: String,
    val name: String? = null,
    val phoneMasked: String? = null,
    /** Full number for the call button — only for drivers who have joined. */
    val phone: String? = null,
    val notes: String? = null,
    val liveStatus: String = "OFF",
    val dropsToday: Int = 0,
    val cashHeld: BigDecimal = BigDecimal.ZERO,
    val cashConfirmed: Boolean = false
)

/** `GET /vendor/team/lookup` — is there a Boondh driver on this number, before adding. */
@JsonClass(generateAdapter = true)
data class DriverLookup(val exists: Boolean, val nameMasked: String? = null, val alreadyOnTeam: Boolean = false)

@JsonClass(generateAdapter = true)
data class CashConfirmation(
    val id: String,
    val day: String,
    val expectedCash: BigDecimal,
    val cashReceived: BigDecimal,
    val cashShortfall: BigDecimal,
    val expectedEmpties: Int,
    val emptiesReceived: Int,
    val emptiesShortfall: Int,
    val shortfall: Boolean,
    val note: String? = null,
    val confirmedAt: String
)

/** One driver's evening hand-over: what the day's cash orders and swaps say they should bring. */
@JsonClass(generateAdapter = true)
data class CashToday(
    val driverId: String,
    val name: String? = null,
    val day: String,
    val drops: Int,
    val cashExpected: BigDecimal,
    val emptiesExpected: Int,
    val driverDeclaredCash: BigDecimal? = null,
    val confirmation: CashConfirmation? = null,
    /** The deliveries inside [cashExpected], one per customer (board VCashConfirm). */
    val lines: List<CashLine> = emptyList(),
    /** Cash on delivery still to collect on drops not delivered yet. */
    val stillToCollect: BigDecimal = BigDecimal.ZERO,
    val dropsLeft: Int = 0
)

@JsonClass(generateAdapter = true)
data class CashLine(
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val settlement: String,      // CASH / PARTIAL
    val bill: BigDecimal,
    val collected: BigDecimal
)

@JsonClass(generateAdapter = true)
data class CashConfirmRequest(val cashReceived: BigDecimal, val emptiesReceived: Int, val note: String? = null)

interface VendorTeamApi {
    @GET("api/v1/vendor/team")
    suspend fun team(): List<TeamDriver>

    @GET("api/v1/vendor/team/lookup")
    suspend fun lookup(@Query("phone") phone: String): DriverLookup

    @GET("api/v1/vendor/team/cash-today")
    suspend fun cashToday(): List<CashToday>

    @POST("api/v1/vendor/team/{driverId}/cash-confirm")
    suspend fun confirmCash(@Path("driverId") driverId: String, @Body body: CashConfirmRequest): CashConfirmation
}

@Module
@InstallIn(SingletonComponent::class)
object VendorTeamApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorTeamApi = retrofit.create(VendorTeamApi::class.java)
}
