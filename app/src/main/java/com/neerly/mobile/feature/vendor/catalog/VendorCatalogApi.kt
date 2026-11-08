package com.neerly.mobile.feature.vendor.catalog

import com.squareup.moshi.JsonClass
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
import java.math.BigDecimal
import javax.inject.Singleton

/** Mirrors backend `ProductResponse` (the fields the shop screens use). */
@JsonClass(generateAdapter = true)
data class ShopProduct(
    val id: String,
    val template: ShopTemplate,
    val brand: String? = null,
    val price: BigDecimal,
    val description: String? = null,
    val dailyCapacity: Int,
    val status: String,                       // AVAILABLE / OUT_OF_STOCK / PAUSED
    val allowKeepContainer: Boolean = false,
    val allowTransferAndReturn: Boolean = false,
    val depositAmount: BigDecimal? = null,
    val retentionHours: Int? = null,
    val pausedUntil: String? = null,          // YYYY-MM-DD
    val soldThisMonth: Long? = null,
    val containersInStock: Int? = null
) {
    val name: String get() = listOfNotNull(brand?.takeIf { it.isNotBlank() }, template.displayName).joinToString(" ")
}

/** Backend `ProductTemplateResponse`. */
@JsonClass(generateAdapter = true)
data class ShopTemplate(
    val id: String,
    val categoryCode: String,
    val displayName: String,
    val sizeLabel: String? = null,
    val containerType: String? = null,
    val waterType: String? = null,
    val allowsDeposit: Boolean = false
)

/** PATCH body — only non-null fields change. Backend `UpdateProductRequest`. */
@JsonClass(generateAdapter = true)
data class ProductPatch(
    val price: BigDecimal? = null,
    val dailyCapacity: Int? = null,
    val depositAmount: BigDecimal? = null,
    val retentionHours: Int? = null,
    val status: String? = null,
    val pausedUntil: String? = null
)

@JsonClass(generateAdapter = true)
data class PauseBody(val paused: Boolean, val pausedUntil: String? = null)

/** Backend `CreateProductRequest` — products always start from an admin template. */
@JsonClass(generateAdapter = true)
data class NewProduct(
    val templateCategoryCode: String,
    val brand: String? = null,
    val price: BigDecimal,
    val dailyCapacity: Int,
    val allowKeepContainer: Boolean,
    val allowTransferAndReturn: Boolean,
    val depositAmount: BigDecimal? = null,
    val retentionHours: Int? = null
)

/** The shop's catalogue calls. `NeerlyApi` keeps the older slim list; these carry the full product. */
interface VendorCatalogApi {
    @GET("api/v1/vendor/catalog/products")
    suspend fun products(): List<ShopProduct>

    @GET("api/v1/vendor/catalog/templates")
    suspend fun templates(): List<ShopTemplate>

    @PATCH("api/v1/vendor/catalog/products/{id}")
    suspend fun update(@Path("id") id: String, @Body body: ProductPatch): ShopProduct

    @POST("api/v1/vendor/catalog/products/{id}/pause")
    suspend fun pause(@Path("id") id: String, @Body body: PauseBody): ShopProduct

    @POST("api/v1/vendor/catalog/products")
    suspend fun create(@Body body: NewProduct): ShopProduct
}

@Module
@InstallIn(SingletonComponent::class)
object VendorCatalogApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorCatalogApi = retrofit.create(VendorCatalogApi::class.java)
}
