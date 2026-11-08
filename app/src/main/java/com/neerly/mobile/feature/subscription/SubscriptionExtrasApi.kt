package com.neerly.mobile.feature.subscription

import com.neerly.mobile.data.dto.ModifySubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionInstanceResponse
import com.neerly.mobile.data.dto.SubscriptionResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Singleton

/**
 * The subscription calls the detail screen needs that `NeerlyApi` doesn't carry yet:
 * the delivery calendar, resume and change. Same Retrofit (auth, request ids) as the rest.
 */
interface SubscriptionExtrasApi {
    @GET("api/v1/customer/subscriptions/{id}/instances")
    suspend fun instances(@Path("id") id: String): List<SubscriptionInstanceResponse>

    @POST("api/v1/customer/subscriptions/{id}/resume")
    suspend fun resume(@Path("id") id: String, @Body body: Map<String, String> = emptyMap()): SubscriptionResponse

    @PATCH("api/v1/customer/subscriptions/{id}")
    suspend fun modify(@Path("id") id: String, @Body body: ModifySubscriptionRequest): SubscriptionResponse
}

@Module
@InstallIn(SingletonComponent::class)
object SubscriptionExtrasModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): SubscriptionExtrasApi = retrofit.create(SubscriptionExtrasApi::class.java)
}
