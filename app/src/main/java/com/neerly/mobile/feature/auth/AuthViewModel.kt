package com.neerly.mobile.feature.auth

import com.neerly.mobile.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.serverErrorCode
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives phone sign-in: send the code from PhoneScreen, check it on OtpScreen.
 *
 * [AuthMode.FIREBASE] (release, or debug with AUTH_MODE=firebase): Firebase Phone Auth
 * sends and checks the SMS, and its ID token is exchanged for our own tokens at
 * /auth/exchange. [AuthMode.DEV]: the local backend's /auth/dev OTP (123456).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val strings: com.neerly.mobile.core.util.Strings,
    private val sender: OtpSender,
    private val mode: AuthMode,
    private val phoneAuth: PhoneAuthGateway
) : ViewModel() {

    val isDevMode: Boolean get() = mode == AuthMode.DEV

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val resendPolicy = ResendPolicy()

    /** Wall clock in ms; tests replace it to walk the countdown without waiting. */
    internal var now: () -> Long = System::currentTimeMillis

    /**
     * The code screen opened: the code was just sent, so the resend countdown starts now.
     * With Firebase, the number may already be proven (instant verification) or get proven
     * while the screen is up (SMS auto-retrieval) — then sign in without a typed code.
     */
    fun otpScreenShown(role: String = "CUSTOMER", onVerified: ((isNewUser: Boolean, roles: List<String>) -> Unit)? = null) {
        if (_state.value.codeSentAt == null) _state.value = _state.value.copy(codeSentAt = now())
        if (mode != AuthMode.FIREBASE || onVerified == null) return
        if (_state.value.roleIntent == null) _state.value = _state.value.copy(roleIntent = role)
        viewModelScope.launch {
            phoneAuth.autoVerified.collect { token ->
                if (token != null && !_state.value.verifying) {
                    phoneAuth.consumeAutoVerified()
                    exchangeAndFinish(token, onVerified)
                }
            }
        }
    }

    fun resendGate(): ResendPolicy.Gate = resendPolicy.gate(now(), _state.value.codeSentAt, _state.value.resends)

    /** "Didn't get it? Resend" — only when the gate is open; the server's own message on failure. */
    fun resendOtp(phoneE164: String, role: String) {
        if (_state.value.resending || resendGate() != ResendPolicy.Gate.Open) return
        _state.value = _state.value.copy(resending = true, error = null, resendNotice = null)
        viewModelScope.launch {
            if (mode == AuthMode.FIREBASE) {
                when (val o = phoneAuth.sendCode(phoneE164, resend = true)) {
                    is SendOutcome.Failed -> _state.value = _state.value.copy(resending = false, error = firebaseMessage(o.error))
                    else -> {
                        val t = now()
                        _state.value = _state.value.copy(resending = false, codeSentAt = t, resends = _state.value.resends + t,
                            resendNotice = strings.get(R.string.core_otp_resent))
                    }
                }
                return@launch
            }
            sender.send(phoneE164, role)
                .onSuccess {
                    val t = now()
                    _state.value = _state.value.copy(resending = false, codeSentAt = t,
                        resends = _state.value.resends + t, hint = it.hint,
                        resendNotice = strings.get(R.string.core_otp_resent))
                }
                .onFailure {
                    _state.value = _state.value.copy(resending = false,
                        error = it.userMessage(context = "resend otp", fallback = strings.get(R.string.core_otp_resend_failed)))
                }
        }
    }

    fun sendOtp(phone10digits: String, role: String = "CUSTOMER", onSent: (phoneE164: String, hint: String) -> Unit) {
        if (!phone10digits.matches(Regex("^[6-9]\\d{9}$"))) {
            _state.value = _state.value.copy(error = strings.get(R.string.cust_auth_bad_phone))
            return
        }
        val phone = "+91$phone10digits"
        _state.value = _state.value.copy(sending = true, error = null, roleIntent = role)
        viewModelScope.launch {
            if (mode == AuthMode.FIREBASE) {
                when (val o = phoneAuth.sendCode(phone, resend = false)) {
                    is SendOutcome.Failed -> _state.value = _state.value.copy(sending = false, error = firebaseMessage(o.error))
                    is SendOutcome.CodeSent -> { _state.value = _state.value.copy(sending = false); onSent(phone, "") }
                    is SendOutcome.Verified -> {
                        // Proven without an SMS: the gateway holds the token and the code
                        // screen signs in with it as soon as it opens.
                        _state.value = _state.value.copy(sending = false); onSent(phone, "")
                    }
                }
                return@launch
            }
            val resp = repo.sendDevOtp(phone, role = role)
            if (resp != null) {
                _state.value = _state.value.copy(sending = false, hint = resp.hint)
                onSent(phone, resp.hint)
            } else {
                _state.value = _state.value.copy(
                    sending = false,
                    error = strings.get(R.string.cust_auth_otp_unavailable)
                )
            }
        }
    }

    fun verifyOtp(phoneE164: String, otp: String, onSuccess: (isNewUser: Boolean, roles: List<String>) -> Unit) {
        if (otp.length != 6 || !otp.all(Char::isDigit)) {
            _state.value = _state.value.copy(error = strings.get(R.string.cust_auth_otp_six))
            return
        }
        val role = _state.value.roleIntent ?: "CUSTOMER"
        _state.value = _state.value.copy(verifying = true, error = null)
        viewModelScope.launch {
            if (mode == AuthMode.FIREBASE) {
                phoneAuth.verifyCode(otp)
                    .onSuccess { exchangeAndFinish(it, onSuccess) }
                    .onFailure { e ->
                        val kind = (e as? PhoneAuthException)?.kind ?: PhoneAuthError.UNAVAILABLE
                        _state.value = _state.value.copy(verifying = false, error = firebaseMessage(kind))
                    }
                return@launch
            }
            val resp = repo.verifyDevOtp(phoneE164, otp, role = role)
            if (resp != null) {
                val roles = resp.grantedRoles
                _state.value = AuthUiState()
                onSuccess(resp.isNewUser, roles)
            } else {
                _state.value = _state.value.copy(
                    verifying = false,
                    error = strings.get(R.string.cust_auth_otp_failed)
                )
            }
        }
    }

    /**
     * Persists the display name typed on the Name step. Navigation is gated on
     * the PATCH succeeding — advancing optimistically is what left every new
     * user called "User".
     */
    fun saveDisplayName(name: String, onSaved: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.length !in 2..50) {
            _state.value = _state.value.copy(error = strings.get(R.string.cust_auth_name_length))
            return
        }
        _state.value = _state.value.copy(savingName = true, error = null)
        viewModelScope.launch {
            runCatching { repo.updateDisplayName(trimmed) }
                .onSuccess {
                    _state.value = _state.value.copy(savingName = false)
                    onSaved()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        savingName = false,
                        error = it.userMessage(
                            context = "save display name",
                            fallback = strings.get(R.string.cust_auth_name_save_failed)
                        )
                    )
                }
        }
    }

    fun switchRole(newRole: String, onDone: () -> Unit) {
        _state.value = _state.value.copy(verifying = true)
        viewModelScope.launch {
            runCatching { repo.switchRole(newRole, remember = false) }
                .onSuccess {
                    _state.value = _state.value.copy(verifying = false)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(verifying = false, error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    /**
     * Firebase ID token → our tokens. DRIVER can't be claimed by a new number (it comes from
     * a shop's invite), so a refusal for that role signs in as a customer instead; the role
     * picker then offers whatever the account really has.
     */
    private suspend fun exchangeAndFinish(idToken: String, onSuccess: (isNewUser: Boolean, roles: List<String>) -> Unit) {
        val role = _state.value.roleIntent ?: "CUSTOMER"
        _state.value = _state.value.copy(verifying = true, error = null)
        val version = com.neerly.mobile.BuildConfig.VERSION_NAME
        runCatching { repo.exchange(idToken, role, appVersion = version) }
            .recoverCatching { e ->
                // Only a 403 can be the role refusal; reading the body uses it up, so other
                // failures go straight to userMessage with their message intact.
                if (role != "CUSTOMER" && (e as? retrofit2.HttpException)?.code() == 403 &&
                    e.serverErrorCode() == "AUTH_ROLE_NOT_GRANTED") repo.exchange(idToken, "CUSTOMER", appVersion = version)
                else throw e
            }
            .onSuccess { resp ->
                _state.value = AuthUiState()
                onSuccess(resp.isNewUser, resp.grantedRoles)
            }
            .onFailure {
                _state.value = _state.value.copy(verifying = false,
                    error = it.userMessage(context = "sign in", fallback = strings.get(R.string.cust_auth_otp_failed)))
            }
    }

    private fun firebaseMessage(kind: PhoneAuthError): String = strings.get(when (kind) {
        PhoneAuthError.INVALID_CODE -> R.string.core_auth_err_wrong_code
        PhoneAuthError.INVALID_PHONE -> R.string.cust_auth_bad_phone
        PhoneAuthError.TOO_MANY_REQUESTS -> R.string.core_auth_err_too_many
        PhoneAuthError.QUOTA -> R.string.core_auth_err_quota
        PhoneAuthError.NETWORK -> R.string.core_auth_err_network
        PhoneAuthError.SESSION_EXPIRED -> R.string.core_auth_err_expired
        PhoneAuthError.UNAVAILABLE -> R.string.core_auth_err_unavailable
    })
}

data class AuthUiState(
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val savingName: Boolean = false,
    val hint: String? = null,
    val error: String? = null,
    val roleIntent: String? = null,
    /** When the current code was sent (start of the resend countdown). */
    val codeSentAt: Long? = null,
    /** Resends in this session, for the hourly cap. */
    val resends: List<Long> = emptyList(),
    val resending: Boolean = false,
    val resendNotice: String? = null
)
