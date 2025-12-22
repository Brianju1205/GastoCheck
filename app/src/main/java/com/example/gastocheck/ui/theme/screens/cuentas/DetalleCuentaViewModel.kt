package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetalleCuentaViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 1. ID REACTIVO: Inicia con el argumento de navegación, pero puede cambiar
    private val _accountId = MutableStateFlow(savedStateHandle.get<Int>("accountId") ?: -1)

    data class DetalleUiState(
        val cuenta: CuentaEntity? = null,
        val transacciones: List<TransaccionEntity> = emptyList(),
        val saldoActual: Double = 0.0 // En Crédito esto representa el DISPONIBLE
    )

    // 2. FUNCIÓN DE ENTRADA: Llamada desde el LaunchedEffect de la vista
    fun inicializar(id: Int) {
        _accountId.value = id
    }

    // 3. FLUJO DE DATOS INTELIGENTE
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetalleUiState> = _accountId.flatMapLatest { id ->
        if (id == -1) {
            flowOf(DetalleUiState())
        } else {
            // Combinamos la Cuenta (por si cambia el nombre/color) y sus Transacciones
            combine(
                repository.getCuentaByIdFlow(id),
                repository.getTransaccionesPorCuenta(id) // Asegúrate que en tu Repo esto devuelve Flow<List<TransaccionEntity>>
            ) { cuenta, transacciones ->
                if (cuenta != null) {
                    val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
                    val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }

                    // FÓRMULA MAESTRA:
                    // Débito: Saldo Inicial + Ingresos - Gastos = Saldo Actual
                    // Crédito: Límite + Pagos - Gastos = Disponible
                    val saldoTotal = cuenta.saldoInicial + ingresos - gastos

                    DetalleUiState(
                        cuenta = cuenta,
                        transacciones = transacciones,
                        saldoActual = saldoTotal
                    )
                } else {
                    DetalleUiState()
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetalleUiState())

    // --- ACCIONES ---

    fun borrarCuentaActual(onSuccess: () -> Unit) {
        val cuenta = uiState.value.cuenta
        if (cuenta != null) {
            viewModelScope.launch {
                // Esto debería borrar en cascada las transacciones si tu BD está configurada así,
                // si no, deberías borrar transacciones primero manualmente.
                repository.deleteCuenta(cuenta)
                onSuccess()
            }
        }
    }

    fun borrarTransaccion(t: TransaccionEntity) {
        viewModelScope.launch {
            if (t.categoria == "Transferencia") {
                // Borrado inteligente de transferencias (origen y destino)
                repository.eliminarTransferenciaCompleta(t.id)
            } else {
                repository.deleteTransaccion(t)
            }
        }
    }
}