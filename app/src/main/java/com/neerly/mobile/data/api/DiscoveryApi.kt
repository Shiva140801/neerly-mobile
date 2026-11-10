package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.JoinWaitlistRequest
import com.neerly.mobile.data.dto.OrderAlternativesResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.ProductAlternativesResponse
import com.neerly.mobile.data.dto.RerouteRequest
import com.neerly.mobile.data.dto.SlotCalendarResponse
import com.neerly.mobile.data.dto.WaitlistEntryDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Singleton

/** Ordering & discovery (WP-3): slots, no-shop alternatives, waitlist, sold-out alternatives. */
interface DiscoveryApi {
    @GET("api/v1/customer/vendors/{id}/slot-days")
    suspend fun slotDays(@Path("id") vendorId: String, @Query("days") days: Int = 5): SlotCalendarResponse

    @GET("api/v1/customer/orders/{id}/alternatives")
    suspend fun alternatives(@Path("id") orderId: String): OrderAlternativesResponse

    @POST("api/v1/customer/orders/{id}/reroute")
    suspend fun reroute(@Path("id") orderId: String, @Body body: RerouteRequest): OrderResponse

    @POST("api/v1/customer/waitlist")
    suspend fun joinWaitlist(@Body body: JoinWaitlistRequest): WaitlistEntryDto

    @GET("api/v1/customer/waitlist")
    suspend fun myWaitlist(): List<WaitlistEntryDto>

    @DELETE("api/v1/customer/waitlist/{id}")
    suspend fun leaveWaitlist(@Path("id") id: String)

    @GET("api/v1/customer/products/{id}/alternatives")
    suspend fun productAlternatives(@Path("id") productId: String, @Query("pincode") pincode: String?): ProductAlternativesResponse
}

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): DiscoveryApi = retrofit.create(DiscoveryApi::class.java)
}
