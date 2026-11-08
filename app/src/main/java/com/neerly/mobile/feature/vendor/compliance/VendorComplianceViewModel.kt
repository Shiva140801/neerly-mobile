package com.neerly.mobile.feature.vendor.compliance

import com.neerly.mobile.R
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the owner is filling in for one document before sending it. */
data class DocDraft(
    val kind: DocKind,
    val number: String = "",
    val issued: String = "",
    val expires: String = "",
    val file: Uri? = null,
    val fileName: String? = null
) {
    /** What's still wrong, as a string id; null when it can be sent. */
    @get:androidx.annotation.StringRes
    val problem: Int? get() = when {
        file == null -> R.string.vend_dp_file
        kind.type == "FSSAI_LICENSE" && !number.matches(Regex("^\\d{14}$")) -> R.string.vend_dp_fssai
        kind.type == "PAN" && !number.uppercase().matches(Regex("^[A-Z]{5}\\d{4}[A-Z]$")) -> R.string.vend_dp_pan
        kind.type == "GSTIN" && !number.uppercase().matches(Regex("^\\d{2}[A-Z]{5}\\d{4}[A-Z][0-9A-Z]{2}$")) -> R.string.vend_dp_gstin
        kind.hasExpiry && !isDate(expires) -> R.string.vend_dp_expiry
        issued.isNotBlank() && !isDate(issued) -> R.string.vend_dp_dates
        else -> null
    }
    private fun isDate(s: String) = runCatching { java.time.LocalDate.parse(s.trim()) }.isSuccess
}

data class VendorComplianceUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val docs: List<DocRow> = emptyList(),
    val draft: DocDraft? = null,
    val sending: Boolean = false,
    val sendError: String? = null,
    val sent: String? = null
) {
    /** The latest submission per type — older rejected copies don't count once replaced. */
    val latest: Map<String, DocRow> get() = docs.groupBy { it.docType }.mapValues { (_, v) -> v.maxBy { it.uploadedAt } }
    val toFix: List<DocRow> get() = latest.values.filter { it.status == "REJECTED" }
    val toRenew: List<DocRow> get() = latest.values.filter { it.status in setOf("APPROVED", "EXPIRED") && it.renewSoon }
    val missing: List<DocKind> get() = SHOP_DOCS.filter { it.required && it.type !in latest }
}

@HiltViewModel
class VendorComplianceViewModel @Inject constructor(
    private val api: VendorDocsApi,
    private val uploader: DocUploader,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {

    private val _state = MutableStateFlow(VendorComplianceUiState())
    val state: StateFlow<VendorComplianceUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.docs.isEmpty(), error = null)
        viewModelScope.launch {
            runCatching { api.docs() }
                .onSuccess { _state.value = _state.value.copy(loading = false, docs = it) }
                .onFailure { _state.value = _state.value.copy(loading = false,
                    error = it.userMessage("vendor compliance", strings.get(R.string.vend_err_load_docs))) }
        }
    }

    /** Start (or redo) one document — prefilled with what we already have, except the file. */
    fun edit(type: String) {
        val kind = SHOP_DOCS.firstOrNull { it.type == type }
            ?: DocKind(type, R.string.vend_document, null, required = false, hasNumber = true, hasExpiry = false)
        val prev = _state.value.latest[type]
        // A renewal is a new licence: keep the number field empty-ish only if it was rejected.
        val renewing = prev != null && prev.status != "REJECTED"
        _state.value = _state.value.copy(
            draft = DocDraft(kind, number = if (renewing) "" else prev?.docNumber.orEmpty()),
            sendError = null, sent = null)
    }

    fun update(transform: DocDraft.() -> DocDraft) {
        _state.value.draft?.let { _state.value = _state.value.copy(draft = it.transform(), sendError = null) }
    }

    fun cancel() { _state.value = _state.value.copy(draft = null, sendError = null) }
    fun dismissSent() { _state.value = _state.value.copy(sent = null) }

    fun send() {
        val d = _state.value.draft ?: return
        d.problem?.let { _state.value = _state.value.copy(sendError = strings.get(it)); return }
        if (_state.value.sending) return
        _state.value = _state.value.copy(sending = true, sendError = null)
        viewModelScope.launch {
            runCatching {
                val key = uploader.upload(d.file!!, d.kind.type)
                api.submit(SubmitDocBody(
                    docType = d.kind.type,
                    docNumber = d.number.trim().uppercase().ifBlank { null },
                    s3Key = key,
                    issuedDate = d.issued.trim().ifBlank { null },
                    expiresDate = d.expires.trim().ifBlank { null }
                ))
            }.onSuccess { row ->
                _state.value = _state.value.copy(sending = false, draft = null, docs = _state.value.docs + row,
                    sent = strings.get(R.string.vend_doc_sent, strings.get(d.kind.label)))
            }.onFailure {
                _state.value = _state.value.copy(sending = false, sendError = when (it) {
                    is DocUploader.UploadUnavailable -> strings.get(R.string.vend_upload_unavailable)
                    is DocUploader.FileProblem -> strings.get(it.res)
                    is IllegalStateException -> it.message
                    else -> it.userMessage("vendor compliance submit", strings.get(R.string.vend_err_send_doc))
                })
            }
        }
    }
}
