package com.example.gastocheck.ui.theme.screens.agregar

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.NotaUtils
import com.example.gastocheck.ui.theme.util.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class AgregarViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ESTADOS DE DATOS ---
    private val _monto = MutableStateFlow("")
    val monto = _monto.asStateFlow()

    private val _descripcion = MutableStateFlow("")
    val descripcion = _descripcion.asStateFlow()

    private val _esIngreso = MutableStateFlow(false)
    val esIngreso = _esIngreso.asStateFlow()

    private val _categoria = MutableStateFlow("Otros")
    val categoria = _categoria.asStateFlow()

    private val _fecha = MutableStateFlow(Date())
    val fecha = _fecha.asStateFlow()

    private val _esMeta = MutableStateFlow(false)
    val esMeta = _esMeta.asStateFlow()

    // --- ESTADOS DE CUENTAS ---
    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cuentaIdSeleccionada = MutableStateFlow(1)
    val cuentaIdSeleccionada = _cuentaIdSeleccionada.asStateFlow()

    // --- ESTADOS VOZ ---
    private val _estadoVoz = MutableStateFlow<EstadoVoz>(EstadoVoz.Inactivo)
    val estadoVoz = _estadoVoz.asStateFlow()

    private val _redireccionTransferencia = Channel<String>()
    val redireccionTransferencia = _redireccionTransferencia.receiveAsFlow()

    private var currentId: Int = -1

    sealed class EstadoVoz {
        object Inactivo : EstadoVoz()
        object Escuchando : EstadoVoz()
        object ProcesandoIA : EstadoVoz()
        data class Exito(val esIngreso: Boolean) : EstadoVoz()
        object Error : EstadoVoz()
    }

    // --- INICIALIZACIÓN ---
    fun inicializar(id: Int, esIngresoDefault: Boolean, vieneDeVoz: Boolean) {
        if (vieneDeVoz) {
            currentId = -1
            return
        }

        if (id != -1) {
            cargarTransaccion(id)
        } else {
            limpiarFormulario()
            _esIngreso.value = esIngresoDefault
        }
    }

    private fun limpiarFormulario() {
        currentId = -1
        _monto.value = ""
        _descripcion.value = ""
        _categoria.value = "Otros"
        _fecha.value = Date()
        _esMeta.value = false
    }

    private fun cargarTransaccion(id: Int) {
        currentId = id
        viewModelScope.launch {
            val transaccion = repository.getTransaccionById(id)
            transaccion?.let {
                _monto.value = it.monto.toString()

                // Limpiamos la nota de detalles técnicos previos si existen
                // (Opcional: puedes dejar la nota tal cual si prefieres)
                val notaLimpia = it.notaCompleta.substringBefore("\n(Conv:")
                _descripcion.value = notaLimpia

                _esIngreso.value = it.esIngreso
                _categoria.value = it.categoria
                _cuentaIdSeleccionada.value = it.cuentaId
                _fecha.value = it.fecha
            }
        }
    }

    // --- SETTERS ---
    fun setCuentaOrigen(id: Int) { _cuentaIdSeleccionada.value = id }
    fun onMontoChange(n: String) { _monto.value = n }
    fun onCategoriaChange(n: String) { _categoria.value = n }
    fun onFechaChange(n: Date) { _fecha.value = n }
    fun reiniciarEstadoVoz() { _estadoVoz.value = EstadoVoz.Inactivo }

    fun onDescripcionChange(nuevoTexto: String) {
        _descripcion.value = nuevoTexto
        detectarCuentaEnTexto(nuevoTexto)
    }

    private fun detectarCuentaEnTexto(texto: String) {
        val listaCuentas = cuentas.value
        if (listaCuentas.isNotEmpty()) {
            val cuentaDetectada = listaCuentas.find {
                texto.contains(it.nombre, ignoreCase = true)
            }
            if (cuentaDetectada != null && cuentaDetectada.id != _cuentaIdSeleccionada.value) {
                _cuentaIdSeleccionada.value = cuentaDetectada.id
            }
        }
    }

    // --- LÓGICA VOZ ---
    fun iniciarEscuchaInteligente() {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.Escuchando
            val recognizer = VoiceRecognizer(context)
            recognizer.escuchar().collect { texto ->
                if (texto.isNotEmpty() && texto != "Error") {
                    procesarTextoHibrido(texto)
                } else {
                    _estadoVoz.value = EstadoVoz.Error
                }
            }
        }
    }

    fun procesarVoz(texto: String) {
        if (texto.isBlank()) return
        procesarTextoHibrido(texto)
    }

    private fun procesarTextoHibrido(texto: String) {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.ProcesandoIA
            try {
                val interpretacion = geminiRepository.interpretarTexto(texto)

                if (interpretacion != null) {
                    if (interpretacion.tipo.equals("TRANSFERENCIA", ignoreCase = true)) {
                        _estadoVoz.value = EstadoVoz.Inactivo
                        _redireccionTransferencia.send(texto)
                        return@launch
                    }

                    _monto.value = interpretacion.monto.toString()
                    _descripcion.value = interpretacion.descripcion
                    _categoria.value = interpretacion.categoria

                    val detectadoEsIngreso = interpretacion.tipo.uppercase() == "INGRESO"
                    _esIngreso.value = detectadoEsIngreso
                    _esMeta.value = texto.contains("meta", ignoreCase = true) || texto.contains("ahorro", ignoreCase = true)

                    var cuentaEncontrada = cuentas.value.find { it.nombre.equals(interpretacion.cuenta_origen, ignoreCase = true) }
                    if (cuentaEncontrada == null) {
                        cuentaEncontrada = cuentas.value.find { texto.contains(it.nombre, ignoreCase = true) }
                    }
                    if (cuentaEncontrada != null) {
                        _cuentaIdSeleccionada.value = cuentaEncontrada.id
                    }
                    _estadoVoz.value = EstadoVoz.Exito(detectadoEsIngreso)
                } else {
                    procesarVozLocal(texto)
                }
            } catch (e: Exception) {
                procesarVozLocal(texto)
            }
        }
    }

    private fun procesarVozLocal(texto: String) {
        val textoLower = texto.lowercase()
        if (textoLower.contains("transfer") || textoLower.contains("pasar a")) {
            viewModelScope.launch {
                _estadoVoz.value = EstadoVoz.Inactivo
                _redireccionTransferencia.send(texto)
            }
            return
        }

        val esIngresoDetectado = textoLower.contains("ingreso") || textoLower.contains("gané")
        _esIngreso.value = esIngresoDetectado
        _monto.value = extraerMontoMaster(texto).toString()
        _descripcion.value = texto
        _estadoVoz.value = EstadoVoz.Exito(esIngresoDetectado)
    }

    private fun extraerMontoMaster(texto: String): Double {
        val textoUnido = texto.replace(Regex("(\\d)\\s+(\\d)"), "$1$2")
        val regex = Regex("[0-9.,]+")
        val matches = regex.findAll(textoUnido)
        var mejorMonto = 0.0
        for (match in matches) {
            val valor = match.value.replace(",", "").toDoubleOrNull() ?: 0.0
            if (valor > mejorMonto) mejorMonto = valor
        }
        return mejorMonto
    }

    // --- GUARDADO CORREGIDO ---
    // Ahora separamos el Resumen (limpio) del Detalle Completo (con conversión)
    fun guardarTransaccion(monedaOrigen: String, onGuardadoExitoso: () -> Unit) {
        val montoOriginal = _monto.value.toDoubleOrNull() ?: 0.0

        if (montoOriginal > 0) {
            viewModelScope.launch {
                var montoFinal = montoOriginal

                // 1. Nota del Usuario (Limpia)
                val notaUsuario = if (_descripcion.value.isBlank()) _categoria.value else _descripcion.value

                // 2. Cálculo de Conversión
                var detalleConversion = ""
                if (monedaOrigen != "MXN") {
                    montoFinal = CurrencyUtils.convertirAMxn(montoOriginal, monedaOrigen)

                    val montoOrigStr = CurrencyUtils.formatCurrency(montoOriginal).replace("$", "")
                    val montoFinalStr = CurrencyUtils.formatCurrency(montoFinal)

                    // Preparamos el texto que SOLO irá a detalles
                    detalleConversion = "\n(Conv: $montoOrigStr $monedaOrigen ≈ $montoFinalStr MXN)"
                }

                // 3. Generamos Resumen SOLO con la nota del usuario (así la lista se ve limpia)
                val notaResumenGenerada = NotaUtils.generarResumen(notaUsuario, _categoria.value)

                // 4. Generamos Nota Completa combinando ambas (para el diálogo de detalles)
                val notaCompletaFinal = notaUsuario + detalleConversion

                val t = TransaccionEntity(
                    id = if (currentId == -1) 0 else currentId,
                    monto = montoFinal,
                    categoria = _categoria.value,
                    notaCompleta = notaCompletaFinal, // Aquí va el detalle técnico
                    notaResumen = notaResumenGenerada, // Aquí va limpio
                    fecha = _fecha.value,
                    esIngreso = _esIngreso.value,
                    cuentaId = _cuentaIdSeleccionada.value
                )
                repository.insertTransaccion(t)
                limpiarFormulario()
                onGuardadoExitoso()
            }
        }
    }
}