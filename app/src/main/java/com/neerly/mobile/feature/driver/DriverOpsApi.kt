package com.neerly.mobile.feature.driver

import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal
import javax.inject.Singleton

/** `GET /driver/earnings/today` — verified against :8081. */
@JsonClass(generateAdapter = true)
data class DriverToday(
    val earned: BigDecimal = BigDecimal.ZERO,
    val drops: Int = 0,
    val cashInHand: BigDecimal = BigDecimal.ZERO,
    /** DOff: "Yesterday · 15 drops · ₹610". */
    val yesterdayEarned: BigDecimal = BigDecimal.ZERO,
    val yesterdayDrops: Int = 0
)

/** `GET /driver/earnings/order/{id}` (backend DriverDropEarningResponse). 404 until the drop is recorded. */
@JsonClass(generateAdapter = true)
data class DriverDropEarning(
    val orderId: String,
    val deliveryFee: BigDecimal = BigDecimal.ZERO,
    val bonus: BigDecimal = BigDecimal.ZERO,
    val bonusReason: String? = null,
    val codCollected: BigDecimal = BigDecimal.ZERO,
    val onTime: Boolean = true,
    val earnedAt: String? = null,
    val todayEarned: BigDecimal = BigDecimal.ZERO,
    val todayDrops: Int = 0
) {
    val total: BigDecimal get() = deliveryFee + bonus
}

/** A shop this driver drives for (`GET /driver/vendors`, also inside `GET /driver/me`). */
@JsonClass(generateAdapter = true)
data class DriverShop(
    val id: String,
    val name: String = "Your shop",
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val todayOpens: String? = null,
    val todayCloses: String? = null
) {
    val shopId: String get() = id
    val shopName: String get() = name
}

/** Driver calls the shared [com.neerly.mobile.data.api.NeerlyApi] doesn't carry. */
interface DriverOpsApi {
    @GET("api/v1/driver/vendors")
    suspend fun vendors(): List<DriverShop>

    @POST("api/v1/driver/shifts/start/{vendorId}")
    suspend fun startShiftFor(@Path("vendorId") vendorId: String, @Body body: Map<String, String> = emptyMap()):
        com.neerly.mobile.data.dto.DriverShiftResponse

    @GET("api/v1/driver/earnings/today")
    suspend fun today(): DriverToday

    @GET("api/v1/driver/earnings/order/{id}")
    suspend fun dropEarning(@Path("id") orderId: String): DriverDropEarning
}

@Module
@InstallIn(SingletonComponent::class)
object DriverOpsApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): DriverOpsApi = retrofit.create(DriverOpsApi::class.java)
}
