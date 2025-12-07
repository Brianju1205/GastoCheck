package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetalleCuentaViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: -1

    data class DetalleUiState(
        val cuenta: CuentaEntity? = null,
        val transacciones: List<TransaccionEntity> = emptyList(),
        val saldoActual: Double = 0.0
    )

    val uiState: StateFlow<DetalleUiState> = combine(
        repository.getCuentaByIdFlow(accountId),
        repository.getTransaccionesPorCuenta(accountId)
    ) { cuenta, transacciones ->
        if (cuenta != null) {
            val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }
            val saldoTotal = cuenta.saldoInicial + ingresos - gastos

            DetalleUiState(
                cuenta = cuenta,
                transacciones = transacciones,
                saldoActual = saldoTotal
            )
        } else {
            DetalleUiState()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetalleUiState())

    // --- NUEVO: Borrar cuenta ---
    fun borrarCuentaActual(onSuccess: () -> Unit) {
        val cuenta = uiState.value.cuenta
        if (cuenta != null) {
            viewModelScope.launch {
                repository.deleteCuenta(cuenta)
                onSuccess()
            }
        }
    }
}