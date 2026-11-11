package com.neerly.mobile.feature.vendor.catalog

import com.neerly.mobile.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.*
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.feature.driver.PhotoUploadResult
import com.neerly.mobile.feature.driver.PhotoUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CustomProductUi(
    val form: CustomProductForm = CustomProductForm(),
    /** Editing this product; null when making a new one. */
    val editingId: String? = null,
    val saving: Boolean = false,
    val photoBusy: Boolean = false,
    val photoError: String? = null,
    val saved: ShopProduct? = null,
    val error: String? = null
)

@HiltViewModel
class VendorCustomProductViewModel @Inject constructor(
    private val api: VendorCatalogApi,
    private val uploader: PhotoUploader,
    private val strings: com.neerly.mobile.core.util.Strings
) : ViewModel() {
    private val _state = MutableStateFlow(CustomProductUi())
    val state = _state.asStateFlow()

    fun edit(p: ShopProduct) {
        if (_state.value.editingId == p.id) return
        CustomProductForm.from(p)?.let { _state.value = CustomProductUi(form = it, editingId = p.id) }
    }

    fun update(f: CustomProductForm.() -> CustomProductForm) {
        _state.value = _state.value.copy(form = _state.value.form.f(), error = null)
    }

    fun newPhotoFile(): File = uploader.newPhotoFile()

    fun photoTaken(file: File, ok: Boolean) {
        if (!ok) return
        _state.value = _state.value.copy(photoBusy = true, photoError = null)
        viewModelScope.launch {
            when (val r = uploader.upload(file, scope = "VENDOR_PRODUCTS", kind = "product-photo")) {
                is PhotoUploadResult.Uploaded -> _state.value = _state.value.copy(photoBusy = false, form = _state.value.form.copy(photoKey = r.s3Key))
                else -> _state.value = _state.value.copy(photoBusy = false, photoError = strings.get(R.string.vend_cp_photo_failed))
            }
        }
    }

    fun save() {
        val s = _state.value
        val body = s.form.body() ?: return
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { s.editingId?.let { api.updateCustom(it, body) } ?: api.createCustom(body) }
                .onSuccess { _state.value = _state.value.copy(saving = false, saved = it) }
                .onFailure { _state.value = _state.value.copy(saving = false,
                    error = it.userMessage("vendor custom product", strings.get(R.string.vend_cp_err_save))) }
        }
    }
}

/**
 * "Make your own product" — for what no Boondh template covers (a 1 kg bag of ice, a
 * 500 ml × 24 case). Guided top to bottom: what it is, kind, measured in, size, packed
 * as, then price. Board VAddProduct / VProductNew styling.
 */
@Composable
fun VendorCustomProductScreen(
    onBack: () -> Unit,
    onDone: (ShopProduct) -> Unit,
    editing: ShopProduct? = null,
    vm: VendorCustomProductViewModel = hiltViewModel()
) {
    LaunchedEffect(editing?.id) { editing?.let(vm::edit) }
    val s by vm.state.collectAsState()
    LaunchedEffect(s.saved) { s.saved?.let(onDone) }
    val f = s.form
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val camera = com.neerly.mobile.feature.driver.rememberCameraCapture { ok -> pendingPhoto?.let { vm.photoTaken(it, ok) } }

    BoondhPage(
        title = stringResource(if (s.editingId != null) R.string.vend_cp_edit_title else R.string.vend_cp_title),
        onBack = onBack, backLabel = stringResource(R.string.vend_types),
        subtitle = stringResource(R.string.vend_cp_sub),
        footer = {
            s.error?.let { Notice(it, Tone.Bad) }
            if (f.returnable && !f.swap && f.depositValue == null) Notice(stringResource(R.string.vend_cp_returnable_need), Tone.Warn)
            PrimaryButton(stringResource(when { s.saving -> R.string.vend_cp_saving; s.editingId != null -> R.string.vend_cp_save_edit; else -> R.string.vend_cp_save }),
                vm::save, enabled = f.valid && !s.saving, color = NeerlyColors.VendorPrimary)
        }
    ) {
        // 1 · What is it?
        item {
            Step(stringResource(R.string.vend_cp_q_what)) {
                Chips(CustomCategory.entries, f.category, { stringResource(categoryLabel(it)) }) { c -> vm.update { withCategory(c) } }
                if (f.category == CustomCategory.OTHER) Text(stringResource(R.string.vend_cp_other_hint), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
        val c = f.category ?: return@BoondhPage
        // 2 · Kind
        item {
            Step(stringResource(R.string.vend_cp_q_kind)) {
                when (c) {
                    CustomCategory.WATER -> Chips(CustomProductForm.WATER_KINDS, f.kind, { stringResource(kindLabel(it)) }) { k -> vm.update { copy(kind = k) } }
                    CustomCategory.ICE -> Chips(CustomProductForm.ICE_KINDS, f.kind, { stringResource(kindLabel(it)) }) { k -> vm.update { copy(kind = k) } }
                    CustomCategory.OTHER -> OutlinedTextField(f.kind.orEmpty(), { v -> vm.update { copy(kind = v.take(24)) } },
                        label = { Text(stringResource(R.string.vend_cp_other_name)) }, singleLine = true,
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        // 3 · Measured in, 4 · Size
        item {
            Step(stringResource(R.string.vend_cp_q_unit)) {
                Chips(CustomProductForm.units(c), f.unit, { stringResource(unitLabel(it)) }) { u -> vm.update { withUnit(u) } }
                f.unit?.let { u ->
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(f.size, { v -> vm.update { copy(size = v.filter { it.isDigit() || it == '.' }.take(7)) } },
                        label = { Text(stringResource(R.string.vend_cp_q_size)) },
                        suffix = { Text(CustomProductForm.unitLabel(u, f.sizeValue ?: java.math.BigDecimal.TEN)) },
                        isError = f.size.isNotBlank() && !f.sizeOk, singleLine = true,
                        supportingText = { f.sizeRange?.let { r -> Text(stringResource(R.string.vend_cp_size_hint,
                            CustomProductForm.plain(r.start), CustomProductForm.plain(r.endInclusive), CustomProductForm.unitLabel(u, java.math.BigDecimal.TEN))) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        // 5 · Packed as
        item {
            Step(stringResource(R.string.vend_cp_q_pack)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomProductForm.packagings(c, f.unit).forEach { p ->
                        FilterChip(selected = f.packaging == p, onClick = { vm.update { copy(packaging = p) } },
                            label = { Text(stringResource(packLabel(p)), fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp))
                    }
                }
                if (f.packaging == Packaging.SEALED) {
                    Text(stringResource(R.string.vend_cp_pack_of), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                    Stepper(f.packCount, stringResource(R.string.vend_cp_in_case)) { by ->
                        vm.update { copy(packCount = (packCount + by).coerceIn(1, 200)) } }
                }
                if (f.packaging != null && !f.returnable) Text(stringResource(R.string.vend_cp_one_way), fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
        // 6 · Name, price, how many a day
        item {
            Step(stringResource(R.string.vend_cp_q_price)) {
                val suggested = f.suggestedName
                OutlinedTextField(f.name.ifBlank { suggested.orEmpty() }, { v -> vm.update { copy(name = v.take(120)) } },
                    label = { Text(stringResource(R.string.vend_cp_name)) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                if (f.name.isNotBlank() && suggested != null && f.name != suggested) {
                    TextButton(onClick = { vm.update { copy(name = "") } }, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.vend_cp_use_suggested, suggested), color = NeerlyColors.VendorPrimary, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(f.price, { v -> vm.update { copy(price = v.filter { it.isDigit() || it == '.' }.take(8)) } },
                    label = { Text(stringResource(R.string.vend_cp_price)) }, prefix = { Text("₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.vend_cp_per_day), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                Stepper(f.dailyCapacity, stringResource(R.string.vend_cp_packs_a_day)) { by ->
                    vm.update { copy(dailyCapacity = (dailyCapacity + by).coerceIn(1, 9999)) } }
            }
        }
        // Returnable only: swap and/or deposit
        if (f.returnable) item {
            Step(stringResource(R.string.vend_how_take_jar)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.vend_swap_empty), Modifier.weight(1f), fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Switch(f.swap, { v -> vm.update { copy(swap = v) } })
                }
                OutlinedTextField(f.deposit, { v -> vm.update { copy(deposit = v.filter { it.isDigit() || it == '.' }.take(8)) } },
                    label = { Text(stringResource(R.string.vend_cp_deposit)) }, prefix = { Text("₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            }
        }
        // Photo (optional)
        item {
            Step(stringResource(R.string.vend_cp_photo)) {
                when {
                    s.photoBusy -> Text(stringResource(R.string.vend_cp_photo_sending), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    f.photoKey != null -> Text(stringResource(R.string.vend_cp_photo_saved), fontSize = 14.sp, color = NeerlyColors.Ok, fontWeight = FontWeight.SemiBold)
                }
                s.photoError?.let { Notice(it, Tone.Bad) }
                SecondaryButton(stringResource(if (f.photoKey != null) R.string.vend_cp_photo_again else R.string.vend_cp_photo_add),
                    { val file = vm.newPhotoFile(); pendingPhoto = file; camera(file) }, enabled = !s.photoBusy, color = NeerlyColors.VendorPrimary)
            }
        }
        item { Notice(stringResource(R.string.vend_cp_review_note), Tone.Info) }
    }
}

@Composable
private fun Step(title: String, content: @Composable ColumnScope.() -> Unit) {
    BoondhCard {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T?, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o), fontSize = 14.sp) },
                modifier = Modifier.heightIn(min = 44.dp))
        }
    }
}

private fun categoryLabel(c: CustomCategory) = when (c) {
    CustomCategory.WATER -> R.string.vend_cp_water; CustomCategory.ICE -> R.string.vend_cp_ice; CustomCategory.OTHER -> R.string.vend_cp_other
}

private fun kindLabel(k: String) = when (k) {
    "MINERAL" -> R.string.vend_cp_k_mineral; "CHILLED" -> R.string.vend_cp_k_chilled; "ALKALINE" -> R.string.vend_cp_k_alkaline
    "CUBES" -> R.string.vend_cp_k_cubes; "CRUSHED" -> R.string.vend_cp_k_crushed; "BLOCK" -> R.string.vend_cp_k_block
    else -> R.string.vend_cp_k_drinking
}

private fun unitLabel(u: CustomUnit) = when (u) {
    CustomUnit.L -> R.string.vend_cp_u_l; CustomUnit.ML -> R.string.vend_cp_u_ml; CustomUnit.KG -> R.string.vend_cp_u_kg; CustomUnit.PCS -> R.string.vend_cp_u_pcs
}

private fun packLabel(p: Packaging) = when (p) {
    Packaging.RETURNABLE -> R.string.vend_cp_p_returnable; Packaging.SEALED -> R.string.vend_cp_p_sealed; Packaging.BAG -> R.string.vend_cp_p_bag
    Packaging.TANKER -> R.string.vend_cp_p_tanker; Packaging.LOOSE -> R.string.vend_cp_p_loose
}
