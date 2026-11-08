package com.neerly.mobile.feature.complaint

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.ComplaintResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplaintsUiState(val loading: Boolean = true, val items: List<ComplaintResponse> = emptyList(), val error: String? = null)

@HiltViewModel
class ComplaintsViewModel @Inject constructor(private val api: NeerlyApi, private val strings: com.neerly.mobile.core.util.Strings) : ViewModel() {
    private val _s = MutableStateFlow(ComplaintsUiState()); val state = _s.asStateFlow()
    init { load() }
    fun load() { viewModelScope.launch {
        _s.value = _s.value.copy(loading = true, error = null)
        runCatching { api.myComplaints() }.onSuccess { _s.value = ComplaintsUiState(false, it) }
            .onFailure { _s.value = ComplaintsUiState(false, error = it.userMessage("complaints", R.string.acct_cmp_load_failed, strings)) }
    } }
}

/** Boondh board 49 — open problems first, then the ones already sorted. */
@Composable
fun ComplaintsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit, vm: ComplaintsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::load)
    BoondhPage(stringResource(R.string.acct_cmp_title), onBack = onBack, footer = { SecondaryButton(stringResource(R.string.acct_cmp_new), onNew) }) {
        if (s.items.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.acct_cmp_empty), stringResource(R.string.acct_cmp_empty_body)) }
            return@BoondhPage
        }
        val (open, closed) = s.items.partition { it.status !in setOf("CLOSED", "WITHDRAWN", "RESOLVED") }
        if (open.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_cmp_open)) }
            items(open, key = { it.id }) { c -> BoondhCard(onClick = { onOpen(c.id) }) {
                InfoRow(c.subject, stringResource(R.string.acct_cmp_opened, shortDay(c.filedAt)))
                c.messages.lastOrNull { !it.isInternal && it.authorRole != "CUSTOMER" }?.let { Pill(stringResource(R.string.acct_cmp_new_reply), Tone.Info) }
            } } }
        if (closed.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_cmp_sorted)) }
            items(closed, key = { it.id }) { c -> BoondhCard(onClick = { onOpen(c.id) }) {
                InfoRow(c.subject, listOfNotNull(shortDay(c.closedAt ?: c.resolvedAt ?: c.filedAt),
                    c.awardedAmount?.takeIf { it.signum() > 0 }?.let { stringResource(R.string.acct_cmp_back, it.rupees()) }).joinToString(" · "))
            } } }
    }
}
