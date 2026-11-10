package com.neerly.mobile.feature.order.redeliver

import com.neerly.mobile.data.dto.OrderResponse
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

/** Mirrors backend `RedeliverRequest`: a time (slot picker) and/or another address. */
@JsonClass(generateAdapter = true)
data class RedeliverRequest(
    val addressId: String? = null,
    val requestedSlotStart: String? = null,
    val requestedSlotEnd: String? = null
)

@JsonClass(generateAdapter = true)
data class CloseFailedResponse(
    val orderId: String,
    val paymentMethod: String,
    val refund: BigDecimal,
    val keptForTrip: BigDecimal
)

/** Mirrors backend `SlotCalendarResponse` (GET /customer/vendors/{id}/slot-days). */
@JsonClass(generateAdapter = true)
data class SlotCalendar(val vendorId: String, val days: List<SlotDayRow> = emptyList())

@JsonClass(generateAdapter = true)
data class SlotDayRow(
    val date: String,
    val open: Boolean,
    val closedReason: String? = null,
    val label: String? = null,
    val slots: List<SlotOptionRow> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SlotOptionRow(
    /** "12:00:00" */
    val start: String,
    val end: String,
    /** OPEN | FILLING | FULL | PASSED | CLOSED */
    val state: String,
    val remaining: Int? = null
) {
    val bookable: Boolean get() = state == "OPEN" || state == "FILLING"
}

/** Board DeliveryFailed and its two pickers. */
interface RedeliveryApi {
    @POST("api/v1/customer/orders/{id}/redeliver")
    suspend fun redeliver(@Path("id") orderId: String, @Body body: RedeliverRequest): OrderResponse

    @POST("api/v1/customer/orders/{id}/close-failed")
    suspend fun closeFailed(@Path("id") orderId: String): CloseFailedResponse

    @GET("api/v1/customer/vendors/{id}/slot-days")
    suspend fun slotDays(@Path("id") vendorId: String, @Query("days") days: Int = 3): SlotCalendar
}

@Module
@InstallIn(SingletonComponent::class)
object RedeliveryModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): RedeliveryApi = retrofit.create(RedeliveryApi::class.java)
}
