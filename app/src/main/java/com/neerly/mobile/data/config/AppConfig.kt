package com.neerly.mobile.data.config

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.neerly.mobile.data.api.NeerlyApi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Mirror of backend `GET /api/v1/public/app-config`. */
@JsonClass(generateAdapter = true)
data class AppConfigResponse(
    val minSupportedAndroidVersion: String? = null,
    val latestAndroidVersion: String? = null,
    val maintenance: Boolean = false,
    val maintenanceMessage: String? = null,
    val maintenanceExpectedBack: String? = null,
    /** Admin-controlled, customer-safe business values, keyed like the backend catalogue. */
    val settings: Map<String, Any?> = emptyMap(),
    /** What the server can take today, e.g. `upiAutopay` (false until mandates exist). */
    val features: Map<String, Boolean> = emptyMap()
)

/**
 * The business values the app shows and checks — fees, minimums, cut-offs, support hours,
 * refund promise, top-up limits. They come from the server (admins change them on the
 * Config screen); the defaults here are only what the app assumes before the first fetch
 * or when offline, and match the backend's own defaults.
 */
data class AppConfig(
    val maintenance: Boolean = false,
    val maintenanceMessage: String? = null,
    val minSupportedVersion: String? = null,
    private val settings: Map<String, Any?> = emptyMap(),
    private val features: Map<String, Boolean> = emptyMap()
) {
    /** Door photo before hand-over (admin setting; on unless the server says off). */
    val deliveryPhotoRequired: Boolean get() = settings["delivery.photo_required"]?.toString()?.lowercase() != "false"

    /** UPI autopay for subscriptions — off unless the server says it works. */
    val upiAutopay: Boolean get() = features["upiAutopay"] == true

    private fun decimal(key: String, default: String): BigDecimal =
        settings[key]?.toString()?.toBigDecimalOrNull() ?: BigDecimal(default)
    /** Boondh support line (admin setting `support.phone`); blank = don't offer a call. */
    val supportPhone: String get() = settings["support.phone"]?.toString()?.trim().orEmpty()
    private fun int(key: String, default: Int): Int =
        settings[key]?.toString()?.toBigDecimalOrNull()?.toInt() ?: default
    private fun time(key: String, default: String): LocalTime =
        runCatching { LocalTime.parse(settings[key]?.toString() ?: default) }.getOrDefault(LocalTime.parse(default))

    val deliveryFeeNear get() = decimal("delivery.fee_near", "25")
    val freeDeliveryAbove get() = decimal("delivery.free_above", "400")
    val minOrderValue get() = decimal("orders.min_order_value", "0")
    val vendorAcceptWindowSec get() = int("orders.vendor_accept_window_sec", 180)
    val subscriptionCutoff get() = time("subscriptions.change_cutoff", "21:00")
    val depositForfeitDays get() = int("deposits.forfeit_after_days", 7)
    val complaintFirstReplyHours get() = int("support.complaint_first_response_hours", 24)
    val supportOpens get() = time("support.opens", "06:00")
    val supportCloses get() = time("support.closes", "22:00")
    val refundDaysMin get() = int("support.refund_working_days_min", 3)
    val refundDaysMax get() = int("support.refund_working_days_max", 5)
    val referrerReward get() = decimal("referrals.referrer_reward", "100")
    val refereeReward get() = decimal("referrals.referee_reward", "50")
    val eventMinAdvanceHours get() = int("events.min_advance_hours", 72)
    val topupMin get() = decimal("wallet.topup_min", "10")
    val topupMax get() = decimal("wallet.topup_max", "9999")

    /**
     * The first delivery date a skip or pause can still change: tomorrow until the cut-off
     * the evening before, the day after once it has passed. Same rule the server enforces.
     */
    fun firstChangeableDate(now: ZonedDateTime = ZonedDateTime.now(IST)): LocalDate =
        now.toLocalDate().plusDays(if (now.toLocalTime().isBefore(subscriptionCutoff)) 1 else 2)

    /** "9 PM", "8:30 PM" — for copy that names the cut-off or support hours. */
    fun clock(t: LocalTime): String =
        // Day-period words follow the app's language; whole hours drop the ":00".
        t.format(java.time.format.DateTimeFormatter.ofPattern(if (t.minute == 0) "h a" else "h:mm a",
            com.neerly.mobile.core.util.AppLocale.locale)).replace("am", "AM").replace("pm", "PM")

    companion object {
        val IST: ZoneId = ZoneId.of("Asia/Kolkata")

        fun from(r: AppConfigResponse) = AppConfig(
            maintenance = r.maintenance, maintenanceMessage = r.maintenanceMessage,
            minSupportedVersion = r.minSupportedAndroidVersion, settings = r.settings, features = r.features
        )
    }
}

/** Screens read the current config with `LocalAppConfig.current`. */
val LocalAppConfig = staticCompositionLocalOf { AppConfig() }

/**
 * Fetches the app config at launch and keeps the last good copy on the device, so an
 * offline start still shows the values the server last sent rather than built-in guesses.
 */
@Singleton
class AppConfigRepository @Inject constructor(
    private val api: NeerlyApi,
    moshi: Moshi,
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("boondh_app_config", Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(AppConfigResponse::class.java)
    private val _config = MutableStateFlow(loadCached())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    suspend fun refresh() {
        runCatching { api.appConfig() }.onSuccess { fresh ->
            _config.value = AppConfig.from(fresh)
            prefs.edit().putString(KEY, adapter.toJson(fresh)).apply()
        }
    }

    private fun loadCached(): AppConfig =
        prefs.getString(KEY, null)?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
            ?.let(AppConfig::from) ?: AppConfig()

    private companion object { const val KEY = "last" }
}
