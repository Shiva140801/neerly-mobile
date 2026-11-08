package com.neerly.mobile.feature.vendor.jars

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

/** Backend `InventorySnapshot` — one row per product the shop owns jars for. */
@JsonClass(generateAdapter = true)
data class JarStock(
    val id: String,
    val productId: String,
    val totalInStock: Int,
    val outWithCustomers: Int,
    val damagedCount: Int,
    val lostCount: Int,
    val totalOwned: Int,
    val lastAdjustedAt: String? = null
)

/** Backend `AdjustStockRequest`. RECOUNT_ADJUST sets the in-shop count to [count]. */
@JsonClass(generateAdapter = true)
data class AdjustStock(val productId: String, val count: Int, val changeType: String, val note: String? = null)

/** Backend `ReturnResponse`. */
@JsonClass(generateAdapter = true)
data class JarReturn(
    val id: String,
    val customerId: String,
    val depositId: String? = null,
    val orderId: String? = null,
    val returnMode: String,            // SCHEDULED_PICKUP / DROPOFF
    val containerCount: Int,
    val customerName: String? = null,
    val addressLine: String? = null,
    val lat: java.math.BigDecimal? = null,
    val lng: java.math.BigDecimal? = null,
    val status: String,                // REQUESTED / SCHEDULED / EN_ROUTE / COMPLETED / FAILED / CANCELLED
    val scheduledFor: String? = null,
    val completedAt: String? = null,
    val pickupPhotoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ScheduleReturn(val scheduledFor: String, val driverId: String? = null)

@JsonClass(generateAdapter = true)
data class CompleteReturn(val pickupPhotoUrl: String? = null, val proofNotes: String? = null)

/** Backend `ClaimResponse`. */
@JsonClass(generateAdapter = true)
data class DamageClaim(
    val id: String,
    val customerId: String,
    val depositId: String,
    val returnId: String? = null,
    val claimAmount: BigDecimal,
    val awardedAmount: BigDecimal? = null,
    val reason: String,
    val evidencePhotos: List<String> = emptyList(),
    val status: String,                // FILED / UNDER_REVIEW / APPROVED / REJECTED / WITHDRAWN
    val adminNote: String? = null,
    val filedAt: String,
    val resolvedAt: String? = null
) {
    val open: Boolean get() = status == "FILED" || status == "UNDER_REVIEW"
}

@JsonClass(generateAdapter = true)
data class ClaimPage(val content: List<DamageClaim> = emptyList())

/** Backend `FileClaimRequest` — evidence is storage keys (the driver's pickup photo). */
@JsonClass(generateAdapter = true)
data class FileClaim(val depositId: String, val returnId: String?, val claimAmount: BigDecimal, val reason: String, val evidencePhotos: List<String>)

interface VendorJarsApi {
    @GET("api/v1/vendor/containers")
    suspend fun stock(): List<JarStock>

    @POST("api/v1/vendor/containers/adjust")
    suspend fun adjust(@Body body: AdjustStock): JarStock

    /** No status = everything still open (requested, scheduled, on the way). */
    @GET("api/v1/vendor/returns")
    suspend fun returns(@Query("status") status: List<String>? = null, @Query("size") size: Int = 50): List<JarReturn>

    @POST("api/v1/vendor/returns/{id}/schedule")
    suspend fun schedule(@Path("id") id: String, @Body body: ScheduleReturn): JarReturn

    @POST("api/v1/vendor/returns/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body body: CompleteReturn): JarReturn

    @GET("api/v1/vendor/claims")
    suspend fun claims(@Query("size") size: Int = 50): ClaimPage

    @GET("api/v1/vendor/claims/{id}")
    suspend fun claim(@Path("id") id: String): DamageClaim

    @POST("api/v1/vendor/claims")
    suspend fun file(@Body body: FileClaim): DamageClaim

    @POST("api/v1/vendor/claims/{id}/withdraw")
    suspend fun withdraw(@Path("id") id: String, @Body body: Map<String, String> = emptyMap()): DamageClaim
}

@Module
@InstallIn(SingletonComponent::class)
object VendorJarsApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorJarsApi = retrofit.create(VendorJarsApi::class.java)
}
