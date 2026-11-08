package com.neerly.mobile.data.config

import com.neerly.mobile.data.api.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class AppConfigTest {

    private val moshi = NetworkModule.provideMoshi()

    private fun fixture(): AppConfigResponse {
        val json = requireNotNull(javaClass.classLoader?.getResource("contract/app-config.json")).readText()
        return requireNotNull(moshi.adapter(AppConfigResponse::class.java).fromJson(json))
    }

    @Test
    fun parses_the_real_backend_payload() {
        val cfg = AppConfig.from(fixture())
        assertFalse(cfg.maintenance)
        assertEquals(0, cfg.deliveryFeeNear.compareTo(BigDecimal("25")))
        assertEquals(LocalTime.of(21, 0), cfg.subscriptionCutoff)
        assertEquals(LocalTime.of(6, 0), cfg.supportOpens)
        assertEquals(24, cfg.complaintFirstReplyHours)
        assertEquals(0, cfg.topupMax.compareTo(BigDecimal("9999")))
    }

    @Test
    fun missing_settings_fall_back_to_the_backend_defaults() {
        val cfg = AppConfig()
        assertEquals(LocalTime.of(21, 0), cfg.subscriptionCutoff)
        assertEquals(3, cfg.refundDaysMin)
    }

    @Test
    fun an_admin_change_is_what_the_app_uses() {
        val cfg = AppConfig.from(AppConfigResponse(settings = mapOf(
            "subscriptions.change_cutoff" to "20:30", "delivery.fee_near" to 33.0)))
        assertEquals(LocalTime.of(20, 30), cfg.subscriptionCutoff)
        assertEquals(0, cfg.deliveryFeeNear.compareTo(BigDecimal("33")))
        assertEquals("8:30 PM", cfg.clock(cfg.subscriptionCutoff))
    }

    @Test
    fun first_changeable_date_follows_the_cutoff() {
        val cfg = AppConfig()
        val before = ZonedDateTime.of(2026, 11, 7, 20, 59, 0, 0, AppConfig.IST)
        val after = ZonedDateTime.of(2026, 11, 7, 21, 0, 0, 0, AppConfig.IST)
        assertEquals(LocalDate.of(2026, 11, 8), cfg.firstChangeableDate(before))
        assertEquals(LocalDate.of(2026, 11, 9), cfg.firstChangeableDate(after))
    }

    @Test
    fun support_phone_shows_only_when_admins_set_it() {
        assertEquals("", AppConfig().supportPhone)
        assertEquals("", AppConfig.from(AppConfigResponse(settings = mapOf("support.phone" to " "))).supportPhone)
        assertEquals("+914012345678", AppConfig.from(AppConfigResponse(settings = mapOf("support.phone" to "+914012345678"))).supportPhone)
    }

    @Test
    fun upi_autopay_is_off_unless_the_server_says_so() {
        assertFalse(AppConfig().upiAutopay)
        assertFalse(AppConfig.from(fixture()).upiAutopay)
        org.junit.Assert.assertTrue(AppConfig.from(AppConfigResponse(features = mapOf("upiAutopay" to true))).upiAutopay)
    }
}
