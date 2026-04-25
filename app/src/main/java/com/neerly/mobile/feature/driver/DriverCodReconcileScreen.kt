package com.neerly.mobile.feature.driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.data.repo.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DriverCodReconcileViewModel @Inject constructor(
    private val repo: DriverRepository
) : ViewModel() {

    fun reconcile(collected: BigDecimal, handedOver: BigDecimal, notes: String?,
                  onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.reconcileCod(collected, handedOver, notes) }
                .onSuccess { onDone() }
                .onFailure { onError(it.message ?: "Failed") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverCodReconcileScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: DriverCodReconcileViewModel = hiltViewModel()
) {
    var collected by remember { mutableStateOf("") }
    var handed by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("End-of-day cash", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.DriverPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(NeerlySpacing.x5),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
        ) {
            Text("Reconcile cash collected today against what you handed over to the vendor.",
                fontSize = 13.sp, color = NeerlyColors.Ink700)

            OutlinedTextField(
                value = collected, onValueChange = { collected = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Collected (₹)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = handed, onValueChange = { handed = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Handed over (₹)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = NeerlyColors.Err, fontSize = 13.sp) }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val c = collected.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val h = handed.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    vm.reconcile(c, h, notes.takeIf { it.isNotBlank() }, onDone) { error = it }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.DriverPrimary)
            ) { Text("Submit", fontWeight = FontWeight.SemiBold) }
        }
    }
}
