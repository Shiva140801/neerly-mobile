package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.CancelOrderRequest
import com.neerly.mobile.data.dto.CapturePaymentRequest
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.CreateReviewRequest
import com.neerly.mobile.data.dto.ExchangeRequest
import com.neerly.mobile.data.dto.ExchangeResponse
import com.neerly.mobile.data.dto.FileComplaintRequest
import com.neerly.mobile.data.dto.InitiatePaymentRequest
import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.LogoutRequest
import com.neerly.mobile.data.dto.NotificationResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.PaymentSnapshot
import com.neerly.mobile.data.dto.PlaceOrderRequest
import com.neerly.mobile.data.dto.PresignRequest
import com.neerly.mobile.data.dto.PresignResponse
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.PromoQuoteResponse
import com.neerly.mobile.data.dto.QuotePromoRequest
import com.neerly.mobile.data.dto.RefreshRequest
import com.neerly.mobile.data.dto.RegisterDeviceRequest
import com.neerly.mobile.data.dto.ReviewResponse
import com.neerly.mobile.data.dto.SwitchRoleRequest
import com.neerly.mobile.data.dto.SwitchRoleResponse
import com.neerly.mobile.data.dto.TokenPair
import com.neerly.mobile.data.dto.UpdateProfileRequest
import com.neerly.mobile.data.dto.UserSummary
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.VendorRatingSummary
import com.neerly.mobile.data.dto.VendorReplyRequest
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.neerly.mobile.data.dto.WalletTransaction
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface covering every endpoint the mobile app calls. Grouped by feature
 * area. The OkHttp Authenticator handles 401 → refresh → retry transparently, so view
 * models treat these as plain suspend functions.
 */
interface NeerlyApi {

    // ------------------------------ Auth ------------------------------

    @POST("api/v1/auth/exchange")
    suspend fun exchange(@Body body: ExchangeRequest): ExchangeResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPair

    @POST("api/v1/auth/switch-role")
    suspend fun switchRole(@Body body: SwitchRoleRequest): SwitchRoleResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @POST("api/v1/auth/logout-all")
    suspend fun logoutAll()

    // ------------------------------ Profile ------------------------------

    @GET("api/v1/customer/me")
    suspend fun me(): UserSummary

    @PATCH("api/v1/customer/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserSummary

    @HTTP(method = "DELETE", path = "api/v1/customer/account", hasBody = false)
    suspend fun deleteAccount()

    @POST("api/v1/customer/account/reactivate")
    suspend fun reactivateAccount()

    // ------------------------------ Addresses ------------------------------

    @GET("api/v1/customer/addresses")
    suspend fun addresses(): List<AddressResponse>

    @POST("api/v1/customer/addresses")
    suspend fun createAddress(@Body body: CreateAddressRequest): AddressResponse

    @PATCH("api/v1/customer/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: String, @Body body: CreateAddressRequest): AddressResponse

    @POST("api/v1/customer/addresses/{id}/primary")
    suspend fun setPrimaryAddress(@Path("id") id: String): AddressResponse

    @DELETE("api/v1/customer/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: String)

    // ------------------------------ Vendors + Catalog ------------------------------

    @GET("api/v1/customer/vendors")
    suspend fun vendors(
        @Query("pincode") pincode: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<VendorCardResponse>

    @GET("api/v1/customer/vendors/{id}")
    suspend fun vendor(@Path("id") id: String): VendorCardResponse

    @GET("api/v1/customer/vendors/{id}/products")
    suspend fun vendorProducts(@Path("id") vendorId: String): List<ProductResponse>

    @GET("api/v1/customer/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<ProductResponse>

    // ------------------------------ Preferences / Favourites ------------------------------

    @POST("api/v1/customer/favourites/{vendorId}")
    suspend fun favourite(@Path("vendorId") vendorId: String)

    @DELETE("api/v1/customer/favourites/{vendorId}")
    suspend fun unfavourite(@Path("vendorId") vendorId: String)

    @GET("api/v1/customer/favourites")
    suspend fun favouriteIds(): List<String>

    // ------------------------------ Orders ------------------------------

    @POST("api/v1/customer/orders")
    suspend fun placeOrder(@Body body: PlaceOrderRequest): OrderResponse

    @GET("api/v1/customer/orders")
    suspend fun myOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<OrderResponse>

    @GET("api/v1/customer/orders/{id}")
    suspend fun order(@Path("id") id: String): OrderResponse

    @POST("api/v1/customer/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String, @Body body: CancelOrderRequest): OrderResponse

    // ------------------------------ Payments ------------------------------

    @POST("api/v1/customer/payments/initiate")
    suspend fun initiatePayment(@Body body: InitiatePaymentRequest): InitiatePaymentResult

    @POST("api/v1/customer/payments/{id}/capture")
    suspend fun capturePayment(@Path("id") id: String, @Body body: CapturePaymentRequest): PaymentSnapshot

    // ------------------------------ Wallet ------------------------------

    @GET("api/v1/customer/wallet")
    suspend fun wallet(): WalletResponse

    @GET("api/v1/customer/wallet/transactions")
    suspend fun walletTransactions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<WalletTransaction>

    @POST("api/v1/customer/wallet/topup")
    suspend fun walletTopup(@Body body: WalletTopupRequest): InitiatePaymentResult

    // ------------------------------ Reviews ------------------------------

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

    // ------------------------------ Complaints ------------------------------

    @POST("api/v1/customer/complaints")
    suspend fun fileComplaint(@Body body: FileComplaintRequest): ComplaintResponse

    @GET("api/v1/customer/complaints")
    suspend fun myComplaints(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<ComplaintResponse>

    @GET("api/v1/customer/complaints/{id}")
    suspend fun complaint(@Path("id") id: String): ComplaintResponse

    // ------------------------------ Notifications ------------------------------

    @GET("api/v1/notifications")
    suspend fun notifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<NotificationResponse>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    // ------------------------------ Devices (FCM) ------------------------------

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest)

    @DELETE("api/v1/devices/{deviceId}")
    suspend fun revokeDevice(@Path("deviceId") deviceId: String)

    // ------------------------------ Promos ------------------------------

    @POST("api/v1/customer/promos/quote")
    suspend fun quotePromo(@Body body: QuotePromoRequest): PromoQuoteResponse

    // ------------------------------ Uploads ------------------------------

    @POST("api/v1/uploads/presign")
    suspend fun presign(@Body body: PresignRequest): PresignResponse
}
