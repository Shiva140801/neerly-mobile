package com.neerly.mobile.data.api

import com.neerly.mobile.data.auth.AuthInterceptor
import com.neerly.mobile.data.auth.TokenAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Emulator → host loopback. Device builds should override this via a
     * flavor or build-config field.
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        // Money fields are BigDecimal across the whole contract and Moshi ships
        // no adapter for it — without this every amount-carrying DTO fails to
        // build a converter. Must be registered before the reflective factory.
        .add(BigDecimal::class.java, BigDecimalAdapter.nullSafe())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .authenticator(tokenAuthenticator)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton
    fun provideNeerlyApi(retrofit: Retrofit): NeerlyApi = retrofit.create(NeerlyApi::class.java)
}
