package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.AppendComplaintMessageRequest
import com.neerly.mobile.data.dto.CancelOrderRequest
import com.neerly.mobile.data.dto.CancelSubscriptionRequest
import com.neerly.mobile.data.dto.CapturePaymentRequest
import com.neerly.mobile.data.dto.ComplaintMessageDto
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.CreateReviewRequest
import com.neerly.mobile.data.dto.CreateSubscriptionRequest
import com.neerly.mobile.data.dto.CancelEventRequest
import com.neerly.mobile.data.dto.CreateEventBookingRequest
import com.neerly.mobile.data.dto.DepositResponse
import com.neerly.mobile.data.dto.EventBookingResponse
import com.neerly.mobile.data.dto.ExchangeRequest
import com.neerly.mobile.data.dto.ExchangeResponse
import com.neerly.mobile.data.dto.FileComplaintRequest
import com.neerly.mobile.data.dto.InitiatePaymentRequest
import com.neerly.mobile.data.dto.InitiatePaymentResult
import com.neerly.mobile.data.dto.LogoutRequest
import com.neerly.mobile.data.dto.ModifySubscriptionRequest
import com.neerly.mobile.data.dto.NotificationResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.PauseSubscriptionRequest
import com.neerly.mobile.data.dto.PaymentSnapshot
import com.neerly.mobile.data.dto.PlaceOrderRequest
import com.neerly.mobile.data.dto.PresignRequest
import com.neerly.mobile.data.dto.PresignResponse
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.PromoQuoteResponse
import com.neerly.mobile.data.dto.QuotePromoRequest
import com.neerly.mobile.data.dto.ReferralEntryRequest
import com.neerly.mobile.data.dto.RefreshRequest
import com.neerly.mobile.data.dto.RegisterDeviceRequest
import com.neerly.mobile.data.dto.ReturnRequest
import com.neerly.mobile.data.dto.ReturnResponse
import com.neerly.mobile.data.dto.ReviewResponse
import com.neerly.mobile.data.dto.SkipSubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionResponse
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
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface covering every endpoint the mobile app calls. Grouped by feature
 * area. The OkHttp Authenticator handles 401 → refresh → retry transparently, so view
 * models treat these as plain suspend functions.
 */
interface NeerlyApi {

    // ------------------------------ Auth ------------------------------

    /**
     * Local-only dev OTP login. Gated server-side by SPRING_PROFILES_ACTIVE=local.
     * In a release build (or non-local profile) the call 404s and the app falls
     * back to the real Firebase Phone Auth + /auth/exchange path.
     */
    @POST("api/v1/auth/dev/send-otp")
    suspend fun devSendOtp(
        @Body body: com.neerly.mobile.data.dto.DevSendOtpRequest
    ): com.neerly.mobile.data.dto.DevSendOtpResponse

    @POST("api/v1/auth/dev/verify-otp")
    suspend fun devVerifyOtp(
        @Body body: com.neerly.mobile.data.dto.DevVerifyOtpRequest
    ): ExchangeResponse

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

    // ------------------------------ Notification preferences ------------------------------

    @GET("api/v1/customer/profile/notification-prefs")
    suspend fun notificationPrefs(): List<com.neerly.mobile.data.dto.NotificationPrefDto>

    @PUT("api/v1/customer/profile/notification-prefs")
    suspend fun upsertNotificationPref(
        @Body body: com.neerly.mobile.data.dto.NotificationPrefDto
    ): com.neerly.mobile.data.dto.NotificationPrefDto

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

    @POST("api/v1/customer/complaints/{id}/messages")
    suspend fun appendComplaintMessage(
        @Path("id") id: String,
        @Body body: AppendComplaintMessageRequest
    ): ComplaintMessageDto

    @POST("api/v1/customer/complaints/{id}/withdraw")
    suspend fun withdrawComplaint(@Path("id") id: String): ComplaintResponse

    // ------------------------------ Subscriptions ------------------------------

    @GET("api/v1/customer/subscriptions")
    suspend fun mySubscriptions(): List<SubscriptionResponse>

    @GET("api/v1/customer/subscriptions/{id}")
    suspend fun subscription(@Path("id") id: String): SubscriptionResponse

    @POST("api/v1/customer/subscriptions")
    suspend fun createSubscription(@Body body: CreateSubscriptionRequest): SubscriptionResponse

    @POST("api/v1/customer/subscriptions/{id}/pause")
    suspend fun pauseSubscription(@Path("id") id: String, @Body body: PauseSubscriptionRequest): SubscriptionResponse

    @POST("api/v1/customer/subscriptions/{id}/skip")
    suspend fun skipSubscription(@Path("id") id: String, @Body body: SkipSubscriptionRequest): SubscriptionResponse

    @POST("api/v1/customer/subscriptions/{id}/cancel")
    suspend fun cancelSubscription(@Path("id") id: String, @Body body: CancelSubscriptionRequest): SubscriptionResponse

    @PATCH("api/v1/customer/subscriptions/{id}")
    suspend fun modifySubscription(@Path("id") id: String, @Body body: ModifySubscriptionRequest): SubscriptionResponse

    // ------------------------------ Deposits + Returns ------------------------------

    @GET("api/v1/customer/deposits")
    suspend fun myDeposits(
        @Query("status") status: String? = null
    ): List<DepositResponse>

    @POST("api/v1/customer/returns")
    suspend fun scheduleReturn(@Body body: ReturnRequest): ReturnResponse

    // ------------------------------ Event bookings ------------------------------

    @POST("api/v1/customer/events")
    suspend fun createEventBooking(@Body body: CreateEventBookingRequest): EventBookingResponse

    @GET("api/v1/customer/events/{id}")
    suspend fun eventBooking(@Path("id") id: String): EventBookingResponse

    @POST("api/v1/customer/events/{id}/cancel")
    suspend fun cancelEventBooking(
        @Path("id") id: String,
        @Body body: CancelEventRequest
    ): EventBookingResponse

    // ------------------------------ Referral ------------------------------

    @POST("api/v1/customer/referral")
    suspend fun submitReferralCode(@Body body: ReferralEntryRequest)

    // ============================== VENDOR APIs ==============================

    @POST("api/v1/vendor/submit")
    suspend fun submitVendorOnboarding(
        @Body body: com.neerly.mobile.data.dto.SubmitVendorOnboardingRequest
    ): com.neerly.mobile.data.dto.VendorResponse

    @GET("api/v1/vendor/me")
    suspend fun vendorMe(): com.neerly.mobile.data.dto.VendorResponse

    @GET("api/v1/vendor/dashboard")
    suspend fun vendorTodaySummary(): com.neerly.mobile.data.dto.VendorTodaySummary

    @GET("api/v1/vendor/orders")
    suspend fun vendorOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): List<com.neerly.mobile.data.dto.VendorOrderResponse>

    @GET("api/v1/vendor/orders/{id}")
    suspend fun vendorOrder(@Path("id") id: String): com.neerly.mobile.data.dto.VendorOrderResponse

    @POST("api/v1/vendor/orders/{id}/accept")
    suspend fun vendorAcceptOrder(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.AcceptOrderRequest
    ): com.neerly.mobile.data.dto.VendorOrderResponse

    @POST("api/v1/vendor/orders/{id}/reject")
    suspend fun vendorRejectOrder(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.RejectOrderRequest
    ): com.neerly.mobile.data.dto.VendorOrderResponse

    @POST("api/v1/vendor/orders/{id}/ready")
    suspend fun vendorMarkReady(@Path("id") id: String): com.neerly.mobile.data.dto.VendorOrderResponse

    @POST("api/v1/vendor/orders/{id}/dispatch")
    suspend fun vendorDispatchOrder(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.DispatchOrderRequest
    ): com.neerly.mobile.data.dto.VendorOrderResponse

    @GET("api/v1/vendor/catalog/products")
    suspend fun vendorCatalog(): List<com.neerly.mobile.data.dto.VendorProductRow>

    @POST("api/v1/vendor/catalog/products/{id}/pause")
    suspend fun vendorPauseProduct(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.TogglePauseRequest
    ): com.neerly.mobile.data.dto.VendorProductRow

    @GET("api/v1/vendor/earnings")
    suspend fun vendorEarnings(): com.neerly.mobile.data.dto.EarningsSummary

    @GET("api/v1/vendor/compliance/docs")
    suspend fun vendorComplianceDocs(): List<com.neerly.mobile.data.dto.ComplianceDocResponse>

    @GET("api/v1/vendor/subscriptions/today")
    suspend fun vendorSubscriptionsToday(): List<com.neerly.mobile.data.dto.VendorSubscriptionTodayRow>

    @POST("api/v1/vendor/business/close")
    suspend fun vendorEmergencyClose(
        @Body body: com.neerly.mobile.data.dto.VendorEmergencyCloseRequest
    ): com.neerly.mobile.data.dto.VendorBusinessStatus

    @POST("api/v1/vendor/business/reopen")
    suspend fun vendorReopen(): com.neerly.mobile.data.dto.VendorBusinessStatus

    @GET("api/v1/vendor/business/status")
    suspend fun vendorBusinessStatus(): com.neerly.mobile.data.dto.VendorBusinessStatus

    @GET("api/v1/vendor/business/hours")
    suspend fun vendorHours(): List<com.neerly.mobile.data.dto.VendorHoursRow>

    @PUT("api/v1/vendor/business/hours")
    suspend fun replaceVendorHours(
        @Body body: com.neerly.mobile.data.dto.ReplaceVendorHoursRequest
    ): List<com.neerly.mobile.data.dto.VendorHoursRow>

    @GET("api/v1/vendor/business/holidays")
    suspend fun vendorHolidays(): List<com.neerly.mobile.data.dto.VendorHolidayRow>

    @POST("api/v1/vendor/business/holidays")
    suspend fun addVendorHoliday(
        @Body body: com.neerly.mobile.data.dto.AddVendorHolidayRequest
    ): com.neerly.mobile.data.dto.VendorHolidayRow

    @DELETE("api/v1/vendor/business/holidays/{date}")
    suspend fun removeVendorHoliday(@Path("date") date: String)

    @GET("api/v1/vendor/bank")
    suspend fun vendorBank(): com.neerly.mobile.data.dto.VendorBankAccountResponse?

    @POST("api/v1/vendor/bank")
    suspend fun addVendorBank(
        @Body body: com.neerly.mobile.data.dto.AddVendorBankAccountRequest
    ): com.neerly.mobile.data.dto.VendorBankAccountResponse

    @POST("api/v1/vendor/bank/verify")
    suspend fun verifyVendorBank(): com.neerly.mobile.data.dto.VendorBankAccountResponse

    @GET("api/v1/vendor/team")
    suspend fun vendorTeam(): List<com.neerly.mobile.data.dto.VendorTeamMember>

    @POST("api/v1/vendor/team")
    suspend fun addVendorDriver(
        @Body body: com.neerly.mobile.data.dto.AddVendorDriverRequest
    ): com.neerly.mobile.data.dto.VendorTeamMember

    @DELETE("api/v1/vendor/team/{driverId}")
    suspend fun removeVendorDriver(@Path("driverId") driverId: String)

    // ============================== DRIVER APIs ==============================

    @POST("api/v1/driver/shifts")
    suspend fun driverStartShift(
        @Body body: com.neerly.mobile.data.dto.StartShiftRequest
    ): com.neerly.mobile.data.dto.DriverShiftResponse

    @POST("api/v1/driver/shifts/{id}/end")
    suspend fun driverEndShift(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.EndShiftRequest
    ): com.neerly.mobile.data.dto.DriverShiftResponse

    @GET("api/v1/driver/shifts/current")
    suspend fun driverCurrentShift(): com.neerly.mobile.data.dto.DriverShiftResponse?

    @GET("api/v1/driver/assignments")
    suspend fun driverAssignments(): List<com.neerly.mobile.data.dto.DriverAssignment>

    @POST("api/v1/driver/gps")
    suspend fun driverGpsPing(@Body body: com.neerly.mobile.data.dto.GpsPingRequest)

    @POST("api/v1/driver/orders/{id}/start")
    suspend fun driverStartDelivery(@Path("id") id: String): com.neerly.mobile.data.dto.DriverAssignment

    @POST("api/v1/driver/orders/{id}/arrived")
    suspend fun driverMarkArrived(@Path("id") id: String): com.neerly.mobile.data.dto.DriverAssignment

    @POST("api/v1/driver/orders/{id}/deliver")
    suspend fun driverCompleteDelivery(
        @Path("id") id: String,
        @Body body: com.neerly.mobile.data.dto.CompleteDeliveryRequest
    ): com.neerly.mobile.data.dto.DriverAssignment

    @POST("api/v1/driver/cod-reconcile")
    suspend fun driverReconcileCod(@Body body: com.neerly.mobile.data.dto.CodReconcileRequest)

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
