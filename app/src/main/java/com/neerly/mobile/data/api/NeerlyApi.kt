package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.dto.CreateReviewRequest
import com.neerly.mobile.data.dto.FileComplaintRequest
import com.neerly.mobile.data.dto.NotificationResponse
import com.neerly.mobile.data.dto.PromoQuoteResponse
import com.neerly.mobile.data.dto.QuotePromoRequest
import com.neerly.mobile.data.dto.ReviewResponse
import com.neerly.mobile.data.dto.VendorRatingSummary
import com.neerly.mobile.data.dto.VendorReplyRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Session-7 endpoints. The auth interceptor injects
 * the Neerly JWT; role-based routing happens server-side.
 */
interface NeerlyApi {

    // ---- Reviews ----
    @POST("api/v1/customer/reviews")
    suspend fun createReview(@Body body: CreateReviewRequest): ReviewResponse

    @GET("api/v1/customer/reviews")
    suspend fun myReviews(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<ReviewResponse>

    @GET("api/v1/public/vendors/{vendorId}/reviews")
    suspend fun vendorReviews(
        @Path("vendorId") vendorId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<ReviewResponse>

    @GET("api/v1/public/vendors/{vendorId}/reviews/summary")
    suspend fun vendorRatingSummary(@Path("vendorId") vendorId: String): VendorRatingSummary

    @POST("api/v1/vendor/reviews/{reviewId}/reply")
    suspend fun vendorReply(
        @Path("reviewId") reviewId: String,
        @Body body: VendorReplyRequest
    ): ReviewResponse

    // ---- Complaints ----
    @POST("api/v1/customer/complaints")
    suspend fun fileComplaint(@Body body: FileComplaintRequest): ComplaintResponse

    @GET("api/v1/customer/complaints")
    suspend fun myComplaints(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<ComplaintResponse>

    // ---- Notifications (in-app feed) ----
    @GET("api/v1/notifications")
    suspend fun notifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<NotificationResponse>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    // ---- Promos ----
    @POST("api/v1/customer/promos/quote")
    suspend fun quotePromo(@Body body: QuotePromoRequest): PromoQuoteResponse
}
