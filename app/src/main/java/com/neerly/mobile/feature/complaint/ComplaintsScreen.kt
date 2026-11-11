package com.neerly.mobile.feature.complaint

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.neerly.mobile.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Board Complaints — "New issue" up top, open problems as framed cards, sorted ones as rows. */
@Composable
fun ComplaintsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit, vm: ComplaintsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    com.neerly.mobile.core.design.OnReturn(vm::load)
    com.neerly.mobile.core.util.OnLiveChange({ it.complaintId != null || it.type?.contains("COMPLAINT") == true }) { vm.load() }
    BoondhPage(stringResource(R.string.acct_cmp_title), onBack = onBack, topAction = {
        androidx.compose.material3.Button(onClick = onNew, shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeerlyColors.Ink900),
            modifier = Modifier.heightIn(min = 48.dp)) {
            androidx.compose.material3.Text(stringResource(R.string.acct_cmp_new_issue), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }) {
        if (s.items.isEmpty()) {
            item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.acct_cmp_empty), stringResource(R.string.acct_cmp_empty_body)) }
            return@BoondhPage
        }
        val (open, closed) = s.items.partition { it.status !in setOf("CLOSED", "WITHDRAWN", "RESOLVED") }
        if (open.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_cmp_open)) }
            items(open, key = { it.id }) { c ->
                val reply = c.messages.lastOrNull { !it.isInternal }?.takeIf { it.authorRole != "CUSTOMER" }
                Column(Modifier.fillMaxWidth()
                    .background(NeerlyColors.Paper, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .border(if (reply != null) 2.dp else 1.dp, if (reply != null) NeerlyColors.CustomerPrimary else NeerlyColors.Ink200,
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpen(c.id) }
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        androidx.compose.material3.Text(c.subject, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        if (reply != null) Box(Modifier.padding(top = 6.dp).size(10.dp).background(NeerlyColors.Err, androidx.compose.foundation.shape.CircleShape))
                    }
                    androidx.compose.material3.Text(stringResource(R.string.acct_cmp_opened, shortDay(c.filedAt) + " " + com.neerly.mobile.feature.notification.clockTime(c.filedAt)),
                        fontSize = 14.sp, color = NeerlyColors.Ink600)
                    reply?.let { m ->
                        androidx.compose.material3.Text(stringResource(R.string.acct_cmp_reply_quote, roleWord(m.authorRole), m.message.take(90)),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Box(Modifier.background(NeerlyColors.WarnSoft, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            androidx.compose.material3.Text(stringResource(R.string.acct_cmp_new_reply).uppercase(), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = NeerlyColors.Warn)
                        }
                    }
                }
            } }
        if (closed.isNotEmpty()) { item { SectionLabel(stringResource(R.string.acct_cmp_sorted), Modifier.padding(top = 8.dp)) }
            items(closed, key = { it.id }) { c -> ListRow(onClick = { onOpen(c.id) }, divider = c != closed.last()) {
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(c.subject, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                    androidx.compose.material3.Text(listOfNotNull(shortDay(c.closedAt ?: c.resolvedAt ?: c.filedAt), c.resolutionNotes?.take(60)).joinToString(" · "),
                        fontSize = 14.sp, color = NeerlyColors.Ink600, maxLines = 2)
                }
                c.awardedAmount?.takeIf { it.signum() > 0 }?.let {
                    androidx.compose.material3.Text(stringResource(R.string.acct_cmp_back, it.rupees()), fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = NeerlyColors.Ok, modifier = Modifier.align(Alignment.Top))
                }
            } } }
    }
}

@Composable
private fun roleWord(role: String): String = when (role) {
    "VENDOR_OWNER", "VENDOR_MANAGER", "VENDOR_STAFF" -> stringResource(R.string.acct_cmp_from_shop)
    else -> stringResource(R.string.acct_cmp_from_boondh)
}
