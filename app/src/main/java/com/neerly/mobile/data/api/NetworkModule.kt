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
    /** Per build type (app/build.gradle.kts): emulator host in debug, the real API in release. */
    private val BASE_URL: String = com.neerly.mobile.BuildConfig.API_BASE_URL

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
        tokenAuthenticator: TokenAuthenticator,
        @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        // Server copy (error messages, notices) in the app's language — the backend's
        // AcceptHeaderLocaleResolver picks messages_te / messages_hi.
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("Accept-Language", com.neerly.mobile.core.util.AppLocale.current(appContext)).build())
        }
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
