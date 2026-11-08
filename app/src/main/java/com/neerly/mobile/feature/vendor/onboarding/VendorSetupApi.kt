package com.neerly.mobile.feature.vendor.onboarding

import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.math.BigDecimal
import javax.inject.Singleton

/** First products for a new shop (board V05): the admin's templates, and creating a product from one. */
interface VendorSetupApi {
    @GET("api/v1/vendor/catalog/templates")
    suspend fun templates(): List<ProductTemplate>

    @GET("api/v1/vendor/catalog/products")
    suspend fun products(): List<SetupProduct>

    @POST("api/v1/vendor/catalog/products")
    suspend fun createProduct(@Body body: CreateProductBody): SetupProduct
}

@Module
@InstallIn(SingletonComponent::class)
object VendorSetupApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorSetupApi = retrofit.create(VendorSetupApi::class.java)
}

@JsonClass(generateAdapter = true)
data class ProductTemplate(
    val id: String,
    val categoryCode: String,
    val displayName: String,
    val sizeLabel: String? = null,
    val containerType: String? = null,
    val allowsDeposit: Boolean = false,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class SetupTemplateRef(val categoryCode: String? = null, val displayName: String? = null)

@JsonClass(generateAdapter = true)
data class SetupProduct(val id: String, val template: SetupTemplateRef? = null, val price: BigDecimal? = null)

@JsonClass(generateAdapter = true)
data class CreateProductBody(
    val templateCategoryCode: String,
    val price: BigDecimal,
    val dailyCapacity: Int,
    val allowKeepContainer: Boolean = true,
    val allowTransferAndReturn: Boolean = false,
    val depositAmount: BigDecimal? = null
)
