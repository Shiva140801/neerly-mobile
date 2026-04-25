package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AcceptOrderRequest
import com.neerly.mobile.data.dto.ComplianceDocResponse
import com.neerly.mobile.data.dto.DispatchOrderRequest
import com.neerly.mobile.data.dto.EarningsSummary
import com.neerly.mobile.data.dto.RejectOrderRequest
import com.neerly.mobile.data.dto.TogglePauseRequest
import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.dto.VendorProductRow
import com.neerly.mobile.data.dto.VendorSubscriptionTodayRow
import com.neerly.mobile.data.dto.VendorTodaySummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorRepository @Inject constructor(private val api: NeerlyApi) {

    suspend fun today(): VendorTodaySummary = api.vendorTodaySummary()

    suspend fun pendingOrders(): List<VendorOrderResponse> = api.vendorOrders(status = "PLACED")
    suspend fun activeOrders(): List<VendorOrderResponse> = api.vendorOrders(status = "ACTIVE")
    suspend fun completedToday(): List<VendorOrderResponse> = api.vendorOrders(status = "COMPLETED_TODAY")
    suspend fun order(id: String): VendorOrderResponse = api.vendorOrder(id)

    suspend fun accept(id: String) = api.vendorAcceptOrder(id, AcceptOrderRequest())
    suspend fun reject(id: String, reason: String) = api.vendorRejectOrder(id, RejectOrderRequest(reason))
    suspend fun ready(id: String) = api.vendorMarkReady(id)
    suspend fun dispatch(id: String, driverId: String) =
        api.vendorDispatchOrder(id, DispatchOrderRequest(driverId))

    suspend fun catalog(): List<VendorProductRow> = api.vendorCatalog()
    suspend fun pauseProduct(id: String, paused: Boolean): VendorProductRow =
        api.vendorPauseProduct(id, TogglePauseRequest(paused))

    suspend fun earnings(): EarningsSummary = api.vendorEarnings()

    suspend fun complianceDocs(): List<ComplianceDocResponse> = api.vendorComplianceDocs()

    suspend fun subscriptionsToday(): List<VendorSubscriptionTodayRow> = api.vendorSubscriptionsToday()

    suspend fun emergencyClose(reason: String?) = api.vendorEmergencyClose(mapOf("reason" to reason))
    suspend fun reopen() = api.vendorReopen()
}
