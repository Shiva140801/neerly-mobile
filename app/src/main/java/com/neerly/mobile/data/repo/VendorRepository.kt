package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.AcceptOrderRequest
import com.neerly.mobile.data.dto.ComplianceDocResponse
import com.neerly.mobile.data.dto.DispatchOrderRequest
import com.neerly.mobile.data.dto.EarningsSummary
import com.neerly.mobile.data.dto.AddVendorBankAccountRequest
import com.neerly.mobile.data.dto.AddVendorDriverRequest
import com.neerly.mobile.data.dto.AddVendorHolidayRequest
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.dto.RejectOrderRequest
import com.neerly.mobile.data.dto.ReplaceVendorHoursRequest
import com.neerly.mobile.data.dto.SubmitVendorOnboardingRequest
import com.neerly.mobile.data.dto.TogglePauseRequest
import com.neerly.mobile.data.dto.VendorBusinessStatus
import com.neerly.mobile.data.dto.VendorEmergencyCloseRequest
import com.neerly.mobile.data.dto.VendorHolidayRow
import com.neerly.mobile.data.dto.VendorHoursRow
import com.neerly.mobile.data.dto.VendorOrderResponse
import com.neerly.mobile.data.dto.VendorTeamMember
import com.neerly.mobile.data.dto.VendorProductRow
import com.neerly.mobile.data.dto.VendorSubscriptionTodayRow
import com.neerly.mobile.data.dto.VendorTodaySummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorRepository @Inject constructor(private val api: NeerlyApi) {

    suspend fun submitOnboarding(req: SubmitVendorOnboardingRequest) = api.submitVendorOnboarding(req)
    suspend fun me() = api.vendorMe()

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

    suspend fun emergencyClose(hours: Int, reason: String?): VendorBusinessStatus =
        api.vendorEmergencyClose(VendorEmergencyCloseRequest(hours = hours, reason = reason))
    suspend fun reopen(): VendorBusinessStatus = api.vendorReopen()
    suspend fun businessStatus(): VendorBusinessStatus = api.vendorBusinessStatus()

    suspend fun hours(): List<VendorHoursRow> = api.vendorHours()
    suspend fun replaceHours(rows: List<VendorHoursRow>): List<VendorHoursRow> =
        api.replaceVendorHours(ReplaceVendorHoursRequest(rows))

    suspend fun holidays(): List<VendorHolidayRow> = api.vendorHolidays()
    suspend fun addHoliday(date: String, label: String?): VendorHolidayRow =
        api.addVendorHoliday(AddVendorHolidayRequest(date, label))
    suspend fun removeHoliday(date: String) = api.removeVendorHoliday(date)

    suspend fun bank(): VendorBankAccountResponse? = api.vendorBank()
    suspend fun addBank(req: AddVendorBankAccountRequest): VendorBankAccountResponse =
        api.addVendorBank(req)
    suspend fun verifyBank(): VendorBankAccountResponse = api.verifyVendorBank()

    suspend fun team(): List<VendorTeamMember> = api.vendorTeam()
    suspend fun addDriver(phone: String, notes: String?): VendorTeamMember =
        api.addVendorDriver(AddVendorDriverRequest(phone, notes))
    suspend fun removeDriver(driverId: String) = api.removeVendorDriver(driverId)
}
