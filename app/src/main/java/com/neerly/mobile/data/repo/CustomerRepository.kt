package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.UserSummary
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin repository wrapping customer-scoped calls. View-models inject this instead
 * of NeerlyApi directly so tests can fake the whole surface with one stub.
 */
@Singleton
class CustomerRepository @Inject constructor(private val api: NeerlyApi) {

    suspend fun me(): UserSummary = api.me()

    // Addresses
    suspend fun addresses(): List<AddressResponse> = api.addresses()
    suspend fun createAddress(req: CreateAddressRequest): AddressResponse = api.createAddress(req)
    suspend fun updateAddress(id: String, req: CreateAddressRequest): AddressResponse = api.updateAddress(id, req)
    suspend fun setPrimary(id: String): AddressResponse = api.setPrimaryAddress(id)
    suspend fun deleteAddress(id: String) = api.deleteAddress(id)

    // Home / browse
    suspend fun vendors(pincode: String): List<VendorCardResponse> = api.vendors(pincode)
    suspend fun vendor(id: String): VendorCardResponse = api.vendor(id)
    suspend fun vendorProducts(vendorId: String): List<ProductResponse> = api.vendorProducts(vendorId)
    suspend fun search(q: String): List<ProductResponse> = api.search(q)

    // Favourites
    suspend fun favouriteIds(): List<String> = api.favouriteIds()
    suspend fun favourite(vendorId: String) = api.favourite(vendorId)
    suspend fun unfavourite(vendorId: String) = api.unfavourite(vendorId)

    // Orders
    suspend fun activeOrders(): List<OrderResponse> =
        api.myOrders(page = 0, size = 5).filter { it.status !in TERMINAL_ORDER_STATES }

    suspend fun myOrders(page: Int = 0, size: Int = 20): List<OrderResponse> = api.myOrders(page, size)
    suspend fun order(id: String): OrderResponse = api.order(id)

    // Wallet
    suspend fun wallet(): WalletResponse = api.wallet()

    private companion object {
        val TERMINAL_ORDER_STATES = setOf("DELIVERED", "CANCELLED", "FAILED")
    }
}
