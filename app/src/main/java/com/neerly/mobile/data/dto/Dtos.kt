package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

// ------------------------------ Reviews ------------------------------

@JsonClass(generateAdapter = true)
data class CreateReviewRequest(
    val orderId: String,
    val rating: Int,                         // overall (averages vendor + waterQuality if both present)
    val vendorRating: Int? = null,           // PRD §13: 2-axis rating
    val waterQualityRating: Int? = null,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ReferralEntryRequest(
    val referrerCode: String
)

@JsonClass(generateAdapter = true)
data class ReviewResponse(
    val id: String,
    val customerId: String,
    val vendorId: String,
    val orderId: String,
    val rating: Int,
    val text: String?,
    val status: String,
    val createdAt: String,
    val vendorResponse: ReviewVendorReply? = null
)

@JsonClass(generateAdapter = true)
data class ReviewVendorReply(
    val id: String,
    val text: String,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class VendorReplyRequest(val text: String)

@JsonClass(generateAdapter = true)
data class VendorRatingSummary(
    val vendorId: String,
    val averageRating: BigDecimal,
    val totalReviews: Long
)

// ------------------------------ Complaints ----------------------------

@JsonClass(generateAdapter = true)
data class FileComplaintRequest(
    val orderId: String? = null,
    val category: String,
    val subject: String,
    val description: String,
    val evidencePhotos: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ComplaintResponse(
    val id: String,
    val customerId: String,
    val vendorId: String?,
    val orderId: String?,
    val category: String,
    val subject: String,
    val description: String,
    val evidencePhotos: List<String>,
    val status: String,
    val priority: String,
    val slaDeadline: String,
    val slaBreached: Boolean,
    val filedAt: String,
    val resolutionAction: String? = null,
    val resolutionNotes: String? = null,
    val awardedAmount: java.math.BigDecimal? = null,
    val resolvedAt: String? = null,
    val closedAt: String? = null,
    val messages: List<ComplaintMessageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ComplaintMessageDto(
    val id: String,
    val authorId: String,
    val authorRole: String,
    val message: String,
    val attachments: List<String> = emptyList(),
    val isInternal: Boolean,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AppendComplaintMessageRequest(
    val message: String,
    val attachments: List<String> = emptyList(),
    val isInternal: Boolean = false
)

// ------------------------------ Notifications -------------------------

@JsonClass(generateAdapter = true)
data class NotificationResponse(
    val id: String,
    val userId: String,
    val channel: String,
    val category: String,
    val subject: String?,
    val body: String,
    val status: String,
    val queuedAt: String,
    val readAt: String? = null
)

// ------------------------------ Promos --------------------------------

@JsonClass(generateAdapter = true)
data class QuotePromoRequest(
    val code: String,
    val orderSubtotal: BigDecimal,
    val isFirstOrder: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PromoQuoteResponse(
    val code: String,
    val eligible: Boolean,
    val reason: String?,
    val discount: BigDecimal,
    val finalTotal: BigDecimal
)
