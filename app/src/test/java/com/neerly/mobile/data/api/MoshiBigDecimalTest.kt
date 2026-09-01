package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.VendorCardResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTopupRequest
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Guards the P0 regression where Moshi had no `BigDecimal` adapter: every DTO
 * carrying a money field failed at *adapter build* time with "Unable to create
 * converter for …", which Retrofit throws before any request is made. That took
 * out vendors, wallet, orders and subscriptions — the whole commercial surface.
 */
class MoshiBigDecimalTest {

    private val moshi = NetworkModule.provideMoshi()

    @Test
    fun vendorListAdapter_buildsWithoutThrowing() {
        val type = Types.newParameterizedType(List::class.java, VendorCardResponse::class.java)
        val adapter = moshi.adapter<List<VendorCardResponse>>(type)

        val json = """
            [{"id":"v1","businessName":"Sri Ganesh Water Supply","tier":"TIER_1",
              "status":"ACTIVE","businessCity":"Hyderabad","businessPincode":"500032",
              "avgRating":4.35,"totalOrders":128,"fssaiNumber":null}]
        """.trimIndent()

        val vendors = adapter.fromJson(json)!!
        assertEquals("Sri Ganesh Water Supply", vendors.single().businessName)
        assertEquals(BigDecimal("4.35"), vendors.single().avgRating)
    }

    @Test
    fun nullableBigDecimal_roundTripsAsNull() {
        val type = Types.newParameterizedType(List::class.java, VendorCardResponse::class.java)
        val json = """
            [{"id":"v1","businessName":"New Vendor","tier":"TIER_2","status":"ACTIVE",
              "businessCity":"Hyderabad","businessPincode":"500032",
              "avgRating":null,"totalOrders":0,"fssaiNumber":null}]
        """.trimIndent()

        val vendors = moshi.adapter<List<VendorCardResponse>>(type).fromJson(json)!!
        assertEquals(null, vendors.single().avgRating)
    }

    @Test
    fun amounts_keepScaleAndPrecision_neverViaDouble() {
        val adapter = moshi.adapter(WalletResponse::class.java)
        // Wire names as the backend actually sends them.
        val json = """
            {"balance":1234.50,"holdAmount":0.00,
             "available":0.1000000000000000055511151231257827,
             "currency":"INR","status":"ACTIVE"}
        """.trimIndent()

        val wallet = adapter.fromJson(json)!!
        assertEquals(BigDecimal("1234.50"), wallet.balance)
        assertEquals(BigDecimal("0.00"), wallet.heldAmount)
        // A Double round-trip would have collapsed this to 0.1.
        assertEquals(
            BigDecimal("0.1000000000000000055511151231257827"),
            wallet.availableAmount
        )
    }

    @Test
    fun amounts_serialiseAsJsonNumbers() {
        val body = moshi.adapter(WalletTopupRequest::class.java)
            .toJson(WalletTopupRequest(BigDecimal("500.00"), method = "UPI"))

        // Unquoted — the backend contract types these as numbers, not strings.
        assertEquals("""{"amount":500.00,"method":"UPI"}""", body)
    }
}
