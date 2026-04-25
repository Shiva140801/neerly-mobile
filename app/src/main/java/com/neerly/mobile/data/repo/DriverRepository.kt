package com.neerly.mobile.data.repo

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.CodReconcileRequest
import com.neerly.mobile.data.dto.CompleteDeliveryRequest
import com.neerly.mobile.data.dto.DriverAssignment
import com.neerly.mobile.data.dto.DriverShiftResponse
import com.neerly.mobile.data.dto.EndShiftRequest
import com.neerly.mobile.data.dto.GpsPingRequest
import com.neerly.mobile.data.dto.StartShiftRequest
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverRepository @Inject constructor(private val api: NeerlyApi) {

    suspend fun currentShift(): DriverShiftResponse? = api.driverCurrentShift()

    suspend fun startShift(): DriverShiftResponse = api.driverStartShift(StartShiftRequest())

    suspend fun endShift(shiftId: String, codHandedOver: BigDecimal? = null) =
        api.driverEndShift(shiftId, EndShiftRequest(codHandedOver = codHandedOver))

    suspend fun assignments(): List<DriverAssignment> = api.driverAssignments()

    suspend fun ping(lat: Double, lng: Double, accuracy: Double?, orderId: String? = null) =
        api.driverGpsPing(GpsPingRequest(lat, lng, accuracy, orderId, Instant.now().toString()))

    suspend fun startDelivery(orderId: String): DriverAssignment = api.driverStartDelivery(orderId)
    suspend fun markArrived(orderId: String): DriverAssignment = api.driverMarkArrived(orderId)
    suspend fun complete(orderId: String, otp: String, photoS3Key: String, codCollected: BigDecimal?) =
        api.driverCompleteDelivery(orderId, CompleteDeliveryRequest(otp, photoS3Key, codCollected))

    suspend fun reconcileCod(collected: BigDecimal, handedOver: BigDecimal, notes: String?) =
        api.driverReconcileCod(CodReconcileRequest(collected, handedOver, notes))
}
