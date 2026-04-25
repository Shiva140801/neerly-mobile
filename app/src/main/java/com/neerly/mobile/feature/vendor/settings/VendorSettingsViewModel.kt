package com.neerly.mobile.feature.vendor.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.data.repo.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorSettingsViewModel @Inject constructor(
    private val repo: VendorRepository
) : ViewModel() {

    fun emergencyClose(reason: String?) {
        viewModelScope.launch { runCatching { repo.emergencyClose(reason) } }
    }

    fun reopen() {
        viewModelScope.launch { runCatching { repo.reopen() } }
    }
}
