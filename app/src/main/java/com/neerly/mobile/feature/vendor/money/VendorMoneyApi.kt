package com.neerly.mobile.feature.vendor.money

import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import javax.inject.Singleton

/** Backend `PayoutResponse`. status: PENDING / PROCESSING / COMPLETED / FAILED / ON_HOLD. */
@JsonClass(generateAdapter = true)
data class VendorPayout(
    val id: String,
    val periodStart: String,
    val periodEnd: String,
    val grossTotal: BigDecimal,
    val commissionTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val netTotal: BigDecimal,
    val status: String,
    val utr: String? = null,
    val holdReason: String? = null,
    val failureReason: String? = null,
    val createdAt: String,
    val completedAt: String? = null,
    /** The account this payout went to, as recorded when it was made — not today's account. */
    val bankAccountId: String? = null,
    val bankName: String? = null,
    val accountMasked: String? = null
) {
    val stuck: Boolean get() = status == "FAILED" || status == "ON_HOLD"
}

/** Backend `EarningsSummaryResponse` — what the next payout will carry so far. */
@JsonClass(generateAdapter = true)
data class UnpaidSummary(
    val unpaidCount: Int,
    val unpaidGross: BigDecimal,
    val unpaidNet: BigDecimal,
    val periodStart: String? = null,
    val periodEnd: String? = null
)

/** One earnings line. entryType ORDER, or LATE_CANCEL_TRIP when a customer cancelled after pickup. */
@JsonClass(generateAdapter = true)
data class EarningLine(
    val id: String,
    val orderId: String? = null,
    val grossAmount: BigDecimal,
    val commissionAmount: BigDecimal,
    val netPayable: BigDecimal,
    val deliveryDate: String,
    val paidOut: Boolean = false,
    val entryType: String = "ORDER"
) {
    val isTripPay: Boolean get() = entryType == "LATE_CANCEL_TRIP"
}

@JsonClass(generateAdapter = true)
data class EarningPage(val content: List<EarningLine> = emptyList(), val totalElements: Long = 0, val hasNext: Boolean = false)

/** Vendor money calls `NeerlyApi` doesn't carry: payouts, unpaid summary, line items, bank history. */
interface VendorMoneyApi {
    @GET("api/v1/vendor/payouts")
    suspend fun payouts(@Query("page") page: Int = 0, @Query("size") size: Int = 20): List<VendorPayout>

    @GET("api/v1/vendor/payouts/{id}")
    suspend fun payout(@Path("id") id: String): VendorPayout

    /** VPayoutDetail "Order list (CSV)" — every order this payout settled. */
    @GET("api/v1/vendor/payouts/{id}/orders.csv")
    suspend fun payoutCsv(@retrofit2.http.Path("id") id: String): okhttp3.ResponseBody

    /** VPayoutDetail "GST invoice" — Boondh's fee invoice for the payout (HTML). */
    @GET("api/v1/vendor/payouts/{id}/fee-invoice")
    suspend fun payoutFeeInvoice(@retrofit2.http.Path("id") id: String): okhttp3.ResponseBody

    @GET("api/v1/vendor/earnings/summary")
    suspend fun unpaid(): UnpaidSummary

    @GET("api/v1/vendor/earnings")
    suspend fun lines(@Query("page") page: Int = 0, @Query("size") size: Int = 30): EarningPage

    @GET("api/v1/vendor/bank/history")
    suspend fun bankHistory(): List<VendorBankAccountResponse>
}

@Module
@InstallIn(SingletonComponent::class)
object VendorMoneyModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorMoneyApi = retrofit.create(VendorMoneyApi::class.java)
}
