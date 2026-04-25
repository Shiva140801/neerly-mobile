package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AddressResponse
import com.neerly.mobile.data.dto.AppendComplaintMessageRequest
import com.neerly.mobile.data.dto.CancelSubscriptionRequest
import com.neerly.mobile.data.dto.ComplaintMessageDto
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.dto.CreateAddressRequest
import com.neerly.mobile.data.dto.CreateSubscriptionRequest
import com.neerly.mobile.data.dto.DepositResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.PauseSubscriptionRequest
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.ReturnRequest
import com.neerly.mobile.data.dto.ReturnResponse
import com.neerly.mobile.data.dto.SkipSubscriptionRequest
import com.neerly.mobile.data.dto.SubscriptionResponse
import com.neerly.mobile.data.dto.UpdateProfileRequest
import com.neerly.mobile.data.dto.UserSummary
import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.neerly.mobile.data.dto.WalletTransaction
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
    suspend fun walletTransactions(page: Int = 0, size: Int = 20): List<WalletTransaction> =
        api.walletTransactions(page, size)
    suspend fun walletTopup(req: WalletTopupRequest) = api.walletTopup(req)

    // Subscriptions
    suspend fun subscriptions(): List<SubscriptionResponse> = api.mySubscriptions()
    suspend fun subscription(id: String): SubscriptionResponse = api.subscription(id)
    suspend fun createSubscription(req: CreateSubscriptionRequest) = api.createSubscription(req)
    suspend fun pauseSubscription(id: String, req: PauseSubscriptionRequest) = api.pauseSubscription(id, req)
    suspend fun skipSubscription(id: String, req: SkipSubscriptionRequest) = api.skipSubscription(id, req)
    suspend fun cancelSubscription(id: String, req: CancelSubscriptionRequest) = api.cancelSubscription(id, req)

    // Deposits / Returns
    suspend fun deposits(status: String? = null): List<DepositResponse> = api.myDeposits(status)
    suspend fun scheduleReturn(req: ReturnRequest): ReturnResponse = api.scheduleReturn(req)

    // Complaints
    suspend fun complaint(id: String): ComplaintResponse = api.complaint(id)
    suspend fun appendComplaintMessage(id: String, message: String): ComplaintMessageDto =
        api.appendComplaintMessage(id, AppendComplaintMessageRequest(message = message))
    suspend fun withdrawComplaint(id: String): ComplaintResponse = api.withdrawComplaint(id)

    // Profile
    suspend fun updateProfile(req: UpdateProfileRequest): UserSummary = api.updateProfile(req)
    suspend fun deleteAccount() = api.deleteAccount()

    private companion object {
        val TERMINAL_ORDER_STATES = setOf("DELIVERED", "CANCELLED", "FAILED")
    }
}
