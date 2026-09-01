package com.neerly.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the dev-OTP login flow: send-OTP from PhoneScreen, verify-OTP from
 * OtpScreen. Backed by AuthRepository which calls the local-only dev endpoints.
 *
 * In a release build the dev endpoint 404s and AuthRepository returns null —
 * the screens then surface a "use Firebase OTP" path (TODO: wire when Firebase
 * Phone Auth is set up; for now we surface an error).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun sendOtp(phone10digits: String, role: String = "CUSTOMER", onSent: (phoneE164: String, hint: String) -> Unit) {
        if (!phone10digits.matches(Regex("^[6-9]\\d{9}$"))) {
            _state.value = _state.value.copy(error = "Enter a valid 10-digit number starting 6-9")
            return
        }
        val phone = "+91$phone10digits"
        _state.value = _state.value.copy(sending = true, error = null, roleIntent = role)
        viewModelScope.launch {
            val resp = repo.sendDevOtp(phone, role = role)
            if (resp != null) {
                _state.value = _state.value.copy(sending = false, hint = resp.hint)
                onSent(phone, resp.hint)
            } else {
                _state.value = _state.value.copy(
                    sending = false,
                    error = "Dev OTP endpoint not available (release build?). " +
                        "Wire Firebase Phone Auth here."
                )
            }
        }
    }

    fun verifyOtp(phoneE164: String, otp: String, onSuccess: (isNewUser: Boolean, roles: List<String>) -> Unit) {
        if (otp.length != 6 || !otp.all(Char::isDigit)) {
            _state.value = _state.value.copy(error = "OTP must be 6 digits")
            return
        }
        val role = _state.value.roleIntent ?: "CUSTOMER"
        _state.value = _state.value.copy(verifying = true, error = null)
        viewModelScope.launch {
            val resp = repo.verifyDevOtp(phoneE164, otp, role = role)
            if (resp != null) {
                val roles = resp.grantedRoles
                _state.value = AuthUiState()
                onSuccess(resp.isNewUser, roles)
            } else {
                _state.value = _state.value.copy(
                    verifying = false,
                    error = "OTP verification failed. " +
                        "Try the default 123456 or the OTP printed in the backend log."
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
            _state.value = _state.value.copy(error = "Enter a name between 2 and 50 characters")
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
                            fallback = "Couldn't save your name. Please try again."
                        )
                    )
                }
        }
    }

    fun switchRole(newRole: String, onDone: () -> Unit) {
        _state.value = _state.value.copy(verifying = true)
        viewModelScope.launch {
            runCatching { repo.switchRole(newRole) }
                .onSuccess {
                    _state.value = _state.value.copy(verifying = false)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(verifying = false, error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}

data class AuthUiState(
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val savingName: Boolean = false,
    val hint: String? = null,
    val error: String? = null,
    val roleIntent: String? = null
)
