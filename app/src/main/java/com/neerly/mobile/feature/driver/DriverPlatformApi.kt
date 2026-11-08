package com.neerly.mobile.feature.driver

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import javax.inject.Singleton

// Phase 5 driver platform — every shape below was checked against :8081.

/** `GET /driver/shifts/state`, `POST /driver/shifts/break` and `/break/end`. */
@JsonClass(generateAdapter = true)
data class DriverShiftState(
    val shiftId: String,
    val vendorId: String? = null,
    val startedAt: String? = null,
    val state: String = "ON_DUTY",            // ON_DUTY / ON_BREAK
    val onBreakSince: String? = null,
    val breakSecondsToday: Long = 0
) { val onBreak get() = state == "ON_BREAK" }

@JsonClass(generateAdapter = true)
data class PendingDrop(val orderId: String, val orderNumber: String, val status: String, val shopName: String? = null)

/** `GET /driver/shifts/end-check`. */
@JsonClass(generateAdapter = true)
data class EndCheck(
    val pendingDrops: List<PendingDrop> = emptyList(),
    val cashHeld: BigDecimal = BigDecimal.ZERO,
    val canEndCleanly: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FinishShiftBody(val handBack: Boolean, val endedReason: String? = null, val notes: String? = null)

@JsonClass(generateAdapter = true)
data class FinishShiftResult(val handedBack: Int = 0)

@JsonClass(generateAdapter = true)
data class DriverVehicle(val type: String? = null, val plateNumber: String? = null, val model: String? = null)

/** `GET /driver/me`. `verification` is VERIFIED / IN_REVIEW / ACTION_NEEDED. */
@JsonClass(generateAdapter = true)
data class DriverMe(
    val driverId: String,
    val name: String = "",
    val phoneMasked: String = "",
    val shops: List<DriverShop> = emptyList(),
    val vehicle: DriverVehicle? = null,
    val verification: String = "ACTION_NEEDED",
    val payoutUpi: String? = null,
    val cashHeld: BigDecimal = BigDecimal.ZERO
)

@JsonClass(generateAdapter = true)
data class PayoutUpiBody(val upiId: String)

/** `GET /driver/logout-check`. */
@JsonClass(generateAdapter = true)
data class LogoutCheck(
    val cashHeld: BigDecimal = BigDecimal.ZERO,
    val pendingDrops: Int = 0,
    val onShift: Boolean = false,
    val canLogout: Boolean = true
)

@JsonClass(generateAdapter = true)
data class DriverDoc(
    val type: String,
    val required: Boolean = false,
    val status: String = "MISSING",           // MISSING / SUBMITTED / APPROVED / REJECTED / EXPIRED
    val id: String? = null,
    val docNumber: String? = null,
    val expiresOn: String? = null,
    val rejectionReason: String? = null,
    val uploadedAt: String? = null
)

/** `GET /driver/documents`. */
@JsonClass(generateAdapter = true)
data class DriverDocs(val overall: String = "ACTION_NEEDED", val documents: List<DriverDoc> = emptyList())

@JsonClass(generateAdapter = true)
data class SubmitDocBody(val type: String, val s3Key: String, val docNumber: String? = null, val expiresOn: String? = null)

@JsonClass(generateAdapter = true)
data class WeekDay(val date: String, val drops: Int = 0, val failedTrips: Int = 0,
                   val earned: BigDecimal = BigDecimal.ZERO, val bonus: BigDecimal = BigDecimal.ZERO)

/** `GET /driver/earnings/week`. There is no driver payout run yet, so status is NOT_PAID_YET. */
@JsonClass(generateAdapter = true)
data class DriverWeek(
    val weekStart: String,
    val weekEnd: String,
    val days: List<WeekDay> = emptyList(),
    val totalEarned: BigDecimal = BigDecimal.ZERO,
    val totalDrops: Int = 0,
    val totalBonus: BigDecimal = BigDecimal.ZERO,
    val payoutStatus: String = "NOT_PAID_YET",
    val payoutUpi: String? = null
)

/** A jar pickup (`GET /driver/returns`), with who and where to collect from. */
@JsonClass(generateAdapter = true)
data class DriverPickup(
    val id: String,
    val orderId: String? = null,
    val containerCount: Int = 0,
    val customerName: String? = null,
    val addressLine: String? = null,
    val lat: java.math.BigDecimal? = null,
    val lng: java.math.BigDecimal? = null,
    val status: String,                       // REQUESTED / SCHEDULED / EN_ROUTE / COMPLETED / CANCELLED
    val scheduledFor: String? = null,
    val completedAt: String? = null,
    val pickupPhotoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CompletePickupBody(val pickupPhotoUrl: String? = null, val proofNotes: String? = null)

@JsonClass(generateAdapter = true)
data class SosBody(val lat: Double?, val lng: Double?, val note: String? = null)

@JsonClass(generateAdapter = true)
data class SosLocationBody(val lat: Double, val lng: Double)

@JsonClass(generateAdapter = true)
data class SosResolveBody(val note: String? = null)

@JsonClass(generateAdapter = true)
data class DriverSos(val id: String, val status: String = "OPEN", val raisedAt: String? = null,
                     val notified: Int = 0, val resolvedAt: String? = null)

/** `POST /driver/orders/{id}/fail`. The server counts the wait from arrival; waitedSeconds is advisory. */
@JsonClass(generateAdapter = true)
data class FailDropBody(val reason: String, val callAttempts: Int, val waitedSeconds: Long, val note: String? = null)

@JsonClass(generateAdapter = true)
data class FailDropResult(val orderId: String, val status: String = "FAILED",
                          val tripPay: BigDecimal = BigDecimal.ZERO, val jarsToShop: Int = 0)

/** `POST /driver/orders/{id}/deliver` with the Phase 5 `emptiesShortReason`. */
@JsonClass(generateAdapter = true)
data class DeliverBody(
    val otp: String,
    val photoS3Key: String?,
    val codCollected: BigDecimal?,
    val emptiesCollected: Int?,
    val codSettlement: String?,
    val emptiesShortReason: String? = null
)

@JsonClass(generateAdapter = true)
data class CodLine(val orderId: String, val collected: BigDecimal, val note: String? = null)

@JsonClass(generateAdapter = true)
data class ReconcileBody(val collected: BigDecimal, val handedOver: BigDecimal, val notes: String?, val lines: List<CodLine>? = null)

interface DriverPlatformApi {
    @GET("api/v1/driver/shifts/state") suspend fun shiftState(): DriverShiftState
    @POST("api/v1/driver/shifts/break") suspend fun startBreak(@Body body: Map<String, String> = emptyMap()): DriverShiftState
    @POST("api/v1/driver/shifts/break/end") suspend fun endBreak(@Body body: Map<String, String> = emptyMap()): DriverShiftState
    @GET("api/v1/driver/shifts/end-check") suspend fun endCheck(): EndCheck
    @POST("api/v1/driver/shifts/finish") suspend fun finish(@Body body: FinishShiftBody): FinishShiftResult

    @GET("api/v1/driver/me") suspend fun me(): DriverMe
    @PUT("api/v1/driver/me/payout-upi") suspend fun setPayoutUpi(@Body body: PayoutUpiBody): DriverMe
    @GET("api/v1/driver/logout-check") suspend fun logoutCheck(): LogoutCheck
    @GET("api/v1/driver/vehicle") suspend fun vehicle(): DriverVehicle
    @PUT("api/v1/driver/vehicle") suspend fun saveVehicle(@Body body: DriverVehicle): DriverVehicle
    @GET("api/v1/driver/documents") suspend fun documents(): DriverDocs
    @POST("api/v1/driver/documents") suspend fun submitDocument(@Body body: SubmitDocBody): DriverDocs

    @GET("api/v1/driver/earnings/week") suspend fun week(@Query("weekStart") weekStart: String? = null): DriverWeek

    @GET("api/v1/driver/returns") suspend fun pickups(): List<DriverPickup>
    @POST("api/v1/driver/returns/{id}/start") suspend fun startPickup(@Path("id") id: String, @Body body: Map<String, String> = emptyMap()): DriverPickup
    @POST("api/v1/driver/returns/{id}/complete") suspend fun completePickup(@Path("id") id: String, @Body body: CompletePickupBody): DriverPickup

    @POST("api/v1/driver/sos") suspend fun sos(@Body body: SosBody): DriverSos
    @POST("api/v1/driver/sos/{id}/location") suspend fun sosLocation(@Path("id") id: String, @Body body: SosLocationBody): DriverSos
    @POST("api/v1/driver/sos/{id}/resolve") suspend fun sosResolve(@Path("id") id: String, @Body body: SosResolveBody): DriverSos

    @POST("api/v1/driver/orders/{id}/fail") suspend fun failDrop(@Path("id") id: String, @Body body: FailDropBody): FailDropResult
    @POST("api/v1/driver/orders/{id}/deliver") suspend fun deliver(@Path("id") id: String, @Body body: DeliverBody): retrofit2.Response<Unit>
    @POST("api/v1/driver/cod-reconcile") suspend fun reconcile(@Body body: ReconcileBody): retrofit2.Response<Unit>
}

@Module
@InstallIn(SingletonComponent::class)
object DriverPlatformApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): DriverPlatformApi = retrofit.create(DriverPlatformApi::class.java)
}

/**
 * The backend error envelope `{"error":{code,message,details}}`, read once. Screens that need
 * a detail (attempts left, when a lock ends, calls still needed) use this instead of userMessage.
 */
data class ServerError(val http: Int, val code: String?, val message: String?, val details: Map<String, Any?>) {
    fun int(key: String): Int? = (details[key] as? Number)?.toInt() ?: details[key]?.toString()?.toDoubleOrNull()?.toInt()
    fun text(key: String): String? = details[key]?.toString()

    companion object {
        private val adapter = Moshi.Builder().build().adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

        fun of(e: Throwable): ServerError? {
            val http = e as? HttpException ?: return null
            val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
            return parse(http.code(), body)
        }

        @Suppress("UNCHECKED_CAST")
        fun parse(http: Int, body: String?): ServerError {
            val root = body?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
            val err = root?.get("error") as? Map<String, Any?>
            return ServerError(http, err?.get("code")?.toString(), err?.get("message")?.toString(),
                (err?.get("details") as? Map<String, Any?>).orEmpty())
        }
    }
}
