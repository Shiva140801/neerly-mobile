package com.neerly.mobile.feature.checkout

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
import java.math.BigDecimal
import javax.inject.Singleton

/** Mirrors backend `PaymentStatusView` — boards PaymentPending / PaymentFailed. */
@JsonClass(generateAdapter = true)
data class PaymentStatusView(
    val paymentId: String,
    val orderId: String? = null,
    /** PAID | PENDING | FAILED */
    val state: String,
    val method: String,
    val amount: BigDecimal,
    /** What the bank or Razorpay said, in its words. */
    val failureReason: String? = null,
    val createdAt: String,
    /** When the unpaid order is cancelled and the shop stops holding the jars. */
    val holdUntil: String
)

@JsonClass(generateAdapter = true)
data class ClientPaymentError(val code: Int? = null, val description: String? = null)

interface PaymentStatusApi {
    @GET("api/v1/customer/payments/{id}") suspend fun status(@Path("id") paymentId: String): PaymentStatusView
    @POST("api/v1/customer/payments/{id}/client-error")
    suspend fun clientError(@Path("id") paymentId: String, @Body body: ClientPaymentError): PaymentStatusView
    /** PaymentFailed "Cash on delivery": the unpaid order becomes a cash order. */
    @POST("api/v1/customer/payments/by-order/{orderId}/pay-cash")
    suspend fun payCash(@Path("orderId") orderId: String): retrofit2.Response<Unit>
}

@Module
@InstallIn(SingletonComponent::class)
object PaymentStatusModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): PaymentStatusApi = retrofit.create(PaymentStatusApi::class.java)
}
