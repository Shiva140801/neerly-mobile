package com.neerly.mobile.feature.auth

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.DevSendOtpRequest
import com.neerly.mobile.data.dto.DevSendOtpResponse
import javax.inject.Inject

/**
 * When the "Resend" link on the code screen may be used (board Otp: "Didn't get it?
 * Resend in 0:23"). A short cooldown after every send stops double taps, and a cap
 * per rolling hour mirrors the server's OTP rate limit (AUTH_OTP_RATE_LIMIT) so the
 * customer sees the wait up front instead of an error after tapping.
 */
class ResendPolicy(
    val cooldownMs: Long = 30_000L,
    val maxPerWindow: Int = 3,
    val windowMs: Long = 60 * 60_000L
) {
    sealed interface Gate {
        data object Open : Gate
        /** Too soon after the last code; whole seconds left, rounded up. */
        data class Cooldown(val secondsLeft: Long) : Gate
        /** The hourly cap is used up; resend opens again at [untilMs]. */
        data class Limited(val untilMs: Long) : Gate
    }

    fun gate(nowMs: Long, lastSentAtMs: Long?, resendsMs: List<Long>): Gate {
        val recent = resendsMs.filter { nowMs - it < windowMs }.sorted()
        if (recent.size >= maxPerWindow) return Gate.Limited(recent.first() + windowMs)
        val last = lastSentAtMs ?: return Gate.Open
        val left = last + cooldownMs - nowMs
        return if (left > 0) Gate.Cooldown((left + 999) / 1000) else Gate.Open
    }
}

/** "0:23" — minutes and zero-padded seconds, as the board shows the countdown. */
fun resendClock(seconds: Long): String = "%d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)

private val OTP_IN_SMS = Regex("(?<!\\d)(\\d{6})(?!\\d)")

/**
 * The 6-digit code inside an SMS the customer let us read (SMS User Consent).
 * Takes the first standalone 6-digit run, so phone numbers and amounts with more
 * digits never match.
 */
fun otpFromSms(message: String?): String? = message?.let { OTP_IN_SMS.find(it)?.groupValues?.get(1) }

/**
 * Asks for a fresh code and keeps the failure, so the screen can show the server's own
 * (localised) message — AuthRepository.sendDevOtp swallows it into null.
 */
class OtpSender @Inject constructor(private val api: NeerlyApi) {
    suspend fun send(phoneE164: String, role: String): Result<DevSendOtpResponse> =
        runCatching { api.devSendOtp(DevSendOtpRequest(phone = phoneE164, role = role)) }
}
