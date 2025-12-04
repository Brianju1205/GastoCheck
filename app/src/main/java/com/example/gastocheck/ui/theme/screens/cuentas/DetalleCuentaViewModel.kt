package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetalleCuentaViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: -1

    // Estado combinado para la UI
    data class DetalleUiState(
        val cuenta: CuentaEntity? = null,
        val transacciones: List<TransaccionEntity> = emptyList(),
        val saldoActual: Double = 0.0,
        val presupuestoDiario: Double = 120.00 // Dato Dummy por ahora como en la imagen
    )

    val uiState: StateFlow<DetalleUiState> = combine(
        repository.getCuentaByIdFlow(accountId), // Necesitas asegurarte que este método exista en Repo o usar getCuentas y filtrar
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
}