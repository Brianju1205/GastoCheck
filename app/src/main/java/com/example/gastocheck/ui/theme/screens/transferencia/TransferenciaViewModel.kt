package com.example.gastocheck.ui.theme.screens.transferencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransferenciaViewModel @Inject constructor(
    private val repository: TransaccionRepository
) : ViewModel() {

    // Listado de cuentas disponibles
    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estados del formulario
    private val _origenId = MutableStateFlow<Int>(-1)
    val origenId = _origenId.asStateFlow()

    private val _destinoId = MutableStateFlow<Int>(-1)
    val destinoId = _destinoId.asStateFlow()

    private val _monto = MutableStateFlow("")
    val monto = _monto.asStateFlow()

    private val _nota = MutableStateFlow("")
    val nota = _nota.asStateFlow()

    private val _fecha = MutableStateFlow(Date())
    val fecha = _fecha.asStateFlow()

    fun setOrigen(id: Int) { _origenId.value = id }
    fun setDestino(id: Int) { _destinoId.value = id }
    fun onMontoChange(v: String) { _monto.value = v }
    fun onNotaChange(v: String) { _nota.value = v }
    fun onFechaChange(d: Date) { _fecha.value = d }

    fun realizarTransferencia(onSuccess: () -> Unit) {
        val montoVal = _monto.value.toDoubleOrNull() ?: 0.0
        val origen = _origenId.value
        val destino = _destinoId.value

        if (montoVal > 0 && origen != -1 && destino != -1 && origen != destino) {
            viewModelScope.launch {
                repository.realizarTransferencia(
                    origenId = origen,
                    destinoId = destino,
                    monto = montoVal,
                    fecha = _fecha.value
                )
                onSuccess()
            }
        }
    }
}