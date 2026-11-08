package com.neerly.mobile.feature.vendor.shop

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

/**
 * The shop-side calls behind the Shop tab, reviews, complaints and event bookings
 * (boards V22–V25, V38, V45) that `NeerlyApi` doesn't carry. Same Retrofit (auth, request ids).
 */
interface VendorShopApi {
    @GET("api/v1/vendor/dashboard")
    suspend fun dashboard(): ShopDashboard

    @GET("api/v1/vendor/reviews")
    suspend fun reviews(@Query("page") page: Int = 0, @Query("size") size: Int = 50): List<ShopReview>

    @GET("api/v1/vendor/reviews/summary")
    suspend fun reviewSummary(): ShopRatingSummary

    @POST("api/v1/vendor/reviews/{id}/reply")
    suspend fun reply(@Path("id") id: String, @Body body: ReplyBody): ShopReview

    @GET("api/v1/vendor/complaints")
    suspend fun complaints(@Query("page") page: Int = 0, @Query("size") size: Int = 50): List<ShopComplaint>

    @GET("api/v1/vendor/complaints/{id}")
    suspend fun complaint(@Path("id") id: String): ShopComplaint

    @POST("api/v1/vendor/complaints/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body body: MessageBody): ShopComplaintMessage

    @GET("api/v1/vendor/events")
    suspend fun events(@Query("status") status: List<String>? = null,
                       @Query("page") page: Int = 0, @Query("size") size: Int = 50): List<ShopEvent>

    @GET("api/v1/vendor/events/{id}")
    suspend fun event(@Path("id") id: String): ShopEvent

    @POST("api/v1/vendor/events/{id}/confirm")
    suspend fun confirmEvent(@Path("id") id: String, @Body body: ConfirmEventBody): ShopEvent

    @POST("api/v1/vendor/events/{id}/reject")
    suspend fun rejectEvent(@Path("id") id: String, @Body body: RejectEventBody): ShopEvent

    @GET("api/v1/vendor/catalog/products")
    suspend fun products(): List<ShopProductRef>
}

@Module
@InstallIn(SingletonComponent::class)
object VendorShopApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorShopApi = retrofit.create(VendorShopApi::class.java)
}

@JsonClass(generateAdapter = true)
data class ShopDashboard(
    val businessName: String? = null,
    val status: String? = null,
    val avgRating: BigDecimal? = null,
    val pendingEvents: Int = 0,
    val licencesToRenew: Int = 0,
    val activeSubscriptions: Int = 0
)

@JsonClass(generateAdapter = true)
data class ShopRatingSummary(val averageRating: BigDecimal = BigDecimal.ZERO, val totalReviews: Long = 0)

@JsonClass(generateAdapter = true)
data class ShopReviewReply(val text: String, val createdAt: String? = null)

@JsonClass(generateAdapter = true)
data class ShopReview(
    val id: String,
    val orderId: String? = null,
    val rating: Int,
    val text: String? = null,
    val status: String? = null,
    val vendorResponse: ShopReviewReply? = null,
    val createdAt: String,
    val customerName: String? = null,      // "Priya M." — vendor list only
    val orderNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class ReplyBody(val text: String)

@JsonClass(generateAdapter = true)
data class ShopComplaintMessage(
    val id: String,
    val authorRole: String,
    val message: String,
    val attachments: List<String> = emptyList(),
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ShopComplaint(
    val id: String,
    val orderId: String? = null,
    val category: String,
    val subject: String,
    val description: String,
    val evidencePhotos: List<String> = emptyList(),
    val status: String,
    val resolutionNotes: String? = null,
    val resolutionAction: String? = null,
    val awardedAmount: BigDecimal? = null,
    val slaDeadline: String? = null,
    val slaBreached: Boolean = false,
    val filedAt: String,
    val resolvedAt: String? = null,
    val messages: List<ShopComplaintMessage> = emptyList(),
    val customerName: String? = null
) {
    val open: Boolean get() = status !in setOf("RESOLVED", "CLOSED", "WITHDRAWN")
}

@JsonClass(generateAdapter = true)
data class MessageBody(val message: String, val attachments: List<String> = emptyList())

@JsonClass(generateAdapter = true)
data class ShopEventItem(
    val productId: String,
    val productName: String? = null,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val containerMode: String? = null,
    val depositAmount: BigDecimal? = null
)

@JsonClass(generateAdapter = true)
data class ShopEventPickup(val scheduledFor: String, val status: String)

@JsonClass(generateAdapter = true)
data class ShopEvent(
    val id: String,
    val eventName: String,
    val expectedGuests: Int? = null,
    val eventStart: String,
    val eventEnd: String? = null,
    val chillingLeadHours: Int? = null,
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal = BigDecimal.ZERO,
    val depositTotal: BigDecimal = BigDecimal.ZERO,
    val taxes: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal,
    val advancePaidAmount: BigDecimal = BigDecimal.ZERO,
    val status: String,
    val items: List<ShopEventItem> = emptyList(),
    val customerNotes: String? = null,
    val vendorNotes: String? = null,
    val rejectionReason: String? = null,
    val pickup: ShopEventPickup? = null
) {
    /** The shop still has to say yes or no. */
    val awaitingShop: Boolean get() = status == "PENDING_CONFIRMATION"
}

@JsonClass(generateAdapter = true)
data class ConfirmEventBody(val vendorNotes: String? = null)

@JsonClass(generateAdapter = true)
data class RejectEventBody(val reason: String)

@JsonClass(generateAdapter = true)
data class ShopProductRef(val id: String, val template: ShopTemplateRef? = null, val brand: String? = null) {
    val name: String get() = listOfNotNull(brand, template?.displayName).joinToString(" ").ifBlank { "Product" }
}

@JsonClass(generateAdapter = true)
data class ShopTemplateRef(val displayName: String)
