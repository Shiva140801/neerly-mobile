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
    val containersInStock: Int? = null,
    val photoUrl: String? = null,
    /** Set for a product the shop made itself (no template fits). */
    val custom: ShopCustomInfo? = null,
    /** APPROVED | PENDING_REVIEW | REJECTED — custom products wait for Boondh. */
    val reviewStatus: String = "APPROVED",
    val reviewReason: String? = null
) {
    val name: String get() = if (custom != null) template.displayName
        else listOfNotNull(brand?.takeIf { it.isNotBlank() }, template.displayName).joinToString(" ")
}

/** Backend `CustomProductInfo`: what it is, how it's measured, how it's packed. */
@JsonClass(generateAdapter = true)
data class ShopCustomInfo(
    val category: String,            // WATER | ICE | OTHER
    val kind: String? = null,
    val unit: String,                // L | ML | KG | PCS
    val size: BigDecimal,
    val packaging: String,           // RETURNABLE | SEALED | BAG | TANKER | LOOSE
    val packCount: Int? = null,
    val name: String? = null
)

/** Backend `CreateCustomProductRequest` — POST …/products/custom, PUT …/products/{id}/custom. */
@JsonClass(generateAdapter = true)
data class CustomProductBody(
    val category: String,
    val kind: String? = null,
    val unit: String,
    val size: BigDecimal,
    val packaging: String,
    val packCount: Int? = null,
    val name: String? = null,
    val price: BigDecimal,
    val dailyCapacity: Int,
    val depositAmount: BigDecimal? = null,
    val allowEmptySwap: Boolean = true,
    val photoUrl: String? = null,
    val description: String? = null
)

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

    @POST("api/v1/vendor/catalog/products/custom")
    suspend fun createCustom(@Body body: CustomProductBody): ShopProduct

    @retrofit2.http.PUT("api/v1/vendor/catalog/products/{id}/custom")
    suspend fun updateCustom(@Path("id") id: String, @Body body: CustomProductBody): ShopProduct
}

@Module
@InstallIn(SingletonComponent::class)
object VendorCatalogApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorCatalogApi = retrofit.create(VendorCatalogApi::class.java)
}
