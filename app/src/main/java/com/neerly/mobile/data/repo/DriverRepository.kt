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
import com.neerly.mobile.data.dto.DriverOffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverRepository @Inject constructor(private val api: NeerlyApi) {

    /** No open shift is a 404 from the server — that is "off duty", not an error. */
    suspend fun currentShift(): DriverShiftResponse? = try {
        api.driverCurrentShift()
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 404) null else throw e
    }

    suspend fun pendingOffer(): DriverOffer? = api.driverTaskOffer().let { r ->
        if (!r.isSuccessful) throw retrofit2.HttpException(r)
        r.body()
    }

    suspend fun acceptOffer(orderId: String) = api.driverAcceptTask(orderId).let { r ->
        if (!r.isSuccessful) throw retrofit2.HttpException(r)
    }

    suspend fun passOffer(orderId: String) = api.driverPassTask(orderId).let { r ->
        if (!r.isSuccessful) throw retrofit2.HttpException(r)
    }

    suspend fun startShift(): DriverShiftResponse = api.driverStartShift(StartShiftRequest())

    suspend fun endShift(shiftId: String, codHandedOver: BigDecimal? = null) =
        api.driverEndShift(shiftId, EndShiftRequest(codCollected = codHandedOver))

    suspend fun assignments(): List<DriverAssignment> = api.driverAssignments()

    suspend fun ping(
        latitude: Double,
        longitude: Double,
        headingDeg: Double? = null,
        speedMps: Double? = null,
        accuracyM: Double? = null,
        batteryPct: Int? = null,
        orderId: String? = null
    ) = api.driverGpsPing(GpsPingRequest(
        latitude = latitude, longitude = longitude, headingDeg = headingDeg,
        speedMps = speedMps, accuracyM = accuracyM, batteryPct = batteryPct, orderId = orderId
    ))

    suspend fun startDelivery(orderId: String): DriverAssignment = api.driverStartDelivery(orderId)
    suspend fun markArrived(orderId: String): DriverAssignment = api.driverMarkArrived(orderId)
    suspend fun complete(
        orderId: String, otp: String, photoS3Key: String, codCollected: BigDecimal?,
        emptiesCollected: Int?, codSettlement: String?
    ) = api.driverCompleteDelivery(
        orderId, CompleteDeliveryRequest(otp, photoS3Key, codCollected, emptiesCollected, codSettlement)
    )

    suspend fun reconcileCod(collected: BigDecimal, handedOver: BigDecimal, notes: String?) =
        api.driverReconcileCod(CodReconcileRequest(collected, handedOver, notes))
}
