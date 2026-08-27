package com.neerly.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.auth.AuthRepository
import com.neerly.mobile.data.dto.UserSummary
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val auth: AuthRepository,
    private val api: NeerlyApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.me() }
                .onSuccess { _state.value = ProfileUiState(loading = false, user = it) }
                .onFailure { _state.value = ProfileUiState(loading = false, error = it.message) }
        }
    }

    fun updateLanguage(code: String) {
        viewModelScope.launch {
            runCatching {
                api.updateProfile(com.neerly.mobile.data.dto.UpdateProfileRequest(preferredLanguage = code))
            }.onSuccess { _state.value = _state.value.copy(user = it) }
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.deleteAccount() }
                .onSuccess { auth.logout(); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            onDone()
        }
    }
}

data class ProfileUiState(
    val loading: Boolean = true,
    val user: UserSummary? = null,
    val error: String? = null
)
