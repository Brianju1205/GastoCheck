package com.example.gastocheck.ui.theme.screens.transferencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransferenciaViewModel @Inject constructor(
    private val repository: TransaccionRepository
) : ViewModel() {

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Canal para enviar mensajes de error a la UI (Toasts)
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var transaccionIdEdicion: Int = -1

    fun setOrigen(id: Int) { _origenId.value = id }
    fun setDestino(id: Int) { _destinoId.value = id }
    fun onMontoChange(v: String) { _monto.value = v }
    fun onNotaChange(v: String) { _nota.value = v }
    fun onFechaChange(d: Date) { _fecha.value = d }

    // --- CARGAR DATOS MEJORADO ---
    fun inicializar(id: Int) {
        if (id != -1) {
            transaccionIdEdicion = id
            viewModelScope.launch {
                val t = repository.getTransaccionById(id)
                if (t != null) {
                    _monto.value = t.monto.toString()
                    _fecha.value = t.fecha

                    // Buscamos la transacción pareja para saber la otra cuenta
                    val tPareja = repository.getTransaccionPareja(t)

                    if (t.esIngreso) {
                        // Si editamos el ingreso, esta cuenta es Destino
                        _destinoId.value = t.cuentaId
                        _origenId.value = tPareja?.cuentaId ?: -1
                    } else {
                        // Si editamos el gasto, esta cuenta es Origen
                        _origenId.value = t.cuentaId
                        _destinoId.value = tPareja?.cuentaId ?: -1
                    }

                    // Limpiamos la nota del texto automático
                    val nombrePareja = tPareja?.let { p -> cuentas.value.find { it.id == p.cuentaId }?.nombre } ?: ""
                    var notaLimpia = t.notaCompleta
                        .replace("Transferencia a $nombrePareja", "")
                        .replace("Recibido de $nombrePareja", "")
                        .trim()

                    _nota.value = notaLimpia
                }
            }
        } else {
            transaccionIdEdicion = -1
            _monto.value = ""
            _nota.value = ""
            _fecha.value = Date()
            _origenId.value = -1
            _destinoId.value = -1
        }
    }

    fun realizarTransferencia(onSuccess: () -> Unit) {
        val montoVal = _monto.value.toDoubleOrNull() ?: 0.0
        val origen = _origenId.value
        val destino = _destinoId.value

        // VALIDACIONES CON MENSAJES
        if (montoVal <= 0) {
            sendError("Ingresa un monto válido")
            return
        }
        if (origen == -1) {
            sendError("Selecciona una cuenta de origen")
            return
        }
        if (destino == -1) {
            sendError("Selecciona una cuenta de destino")
            return
        }
        if (origen == destino) {
            sendError("Las cuentas deben ser diferentes")
            return
        }

        viewModelScope.launch {
            try {
                if (transaccionIdEdicion != -1) {
                    repository.eliminarTransferenciaCompleta(transaccionIdEdicion)
                }
                repository.realizarTransferencia(
                    origenId = origen,
                    destinoId = destino,
                    monto = montoVal,
                    fecha = _fecha.value
                )
                onSuccess()
            } catch (e: Exception) {
                sendError("Error al guardar: ${e.message}")
            }
        }
    }

    private fun sendError(msg: String) {
        viewModelScope.launch { _uiEvent.send(msg) }
    }
}