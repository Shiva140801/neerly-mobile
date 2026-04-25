package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.*
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin repository facade in front of the Retrofit interface. Exists mostly so
 * view-models can swap the API for a fake in tests.
 */
@Singleton
class TrustRepository @Inject constructor(private val api: NeerlyApi) {

    suspend fun submitReview(orderId: String, rating: Int, text: String?): ReviewResponse =
        api.createReview(CreateReviewRequest(orderId, rating, text))

    suspend fun myReviews(): List<ReviewResponse> = api.myReviews()

    suspend fun vendorReviews(vendorId: String): List<ReviewResponse> = api.vendorReviews(vendorId)

    suspend fun vendorSummary(vendorId: String): VendorRatingSummary = api.vendorRatingSummary(vendorId)

    suspend fun fileComplaint(
        orderId: String?, category: String, subject: String, description: String,
        evidencePhotos: List<String> = emptyList()
    ): ComplaintResponse = api.fileComplaint(
        FileComplaintRequest(orderId, category, subject, description, evidencePhotos)
    )

    suspend fun myComplaints(): List<ComplaintResponse> = api.myComplaints()

    suspend fun notifications(): List<NotificationResponse> = api.notifications()

    suspend fun markNotificationRead(id: String) = api.markRead(id)

    suspend fun quotePromo(code: String, subtotal: BigDecimal, isFirstOrder: Boolean): PromoQuoteResponse =
        api.quotePromo(QuotePromoRequest(code, subtotal, isFirstOrder))
}
