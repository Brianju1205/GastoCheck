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
import com.example.gastocheck.ui.theme.util.NotaUtils
import com.example.gastocheck.ui.theme.util.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // CORRECCIÓN: Restauramos el estado 'esMeta' que faltaba y causaba el error
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
        _esMeta.value = false // Limpiamos meta también
    }

    private fun cargarTransaccion(id: Int) {
        currentId = id
        viewModelScope.launch {
            val transaccion = repository.getTransaccionById(id)
            transaccion?.let {
                _monto.value = it.monto.toString()
                _descripcion.value = it.notaCompleta
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

    // CORRECCIÓN: Restauramos la función pública 'procesarVoz' que llama ConfirmacionVozScreen
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
                    _monto.value = interpretacion.monto.toString()
                    _descripcion.value = interpretacion.descripcion
                    _categoria.value = interpretacion.categoria

                    val detectadoEsIngreso = interpretacion.tipo.uppercase() == "INGRESO"
                    _esIngreso.value = detectadoEsIngreso

                    // Lógica simple para detectar Meta si la IA no lo devuelve explícitamente
                    _esMeta.value = texto.contains("meta", ignoreCase = true) || texto.contains("ahorro", ignoreCase = true)

                    var cuentaEncontrada = cuentas.value.find {
                        it.nombre.equals(interpretacion.cuenta_origen, ignoreCase = true)
                    }
                    if (cuentaEncontrada == null) {
                        cuentaEncontrada = cuentas.value.find {
                            texto.contains(it.nombre, ignoreCase = true)
                        }
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
        val esIngresoDetectado = textoLower.contains("ingreso") || textoLower.contains("gané")

        // Detectar Meta localmente
        val esMetaDetectada = textoLower.contains("meta") || textoLower.contains("ahorro")

        _esIngreso.value = esIngresoDetectado
        _esMeta.value = esMetaDetectada
        _monto.value = extraerMontoMaster(texto).toString()
        _descripcion.value = texto

        for (cat in CategoriaUtils.listaCategorias) {
            if (textoLower.contains(cat.nombre.lowercase())) {
                _categoria.value = cat.nombre
                break
            }
        }
        detectarCuentaEnTexto(texto)
        _estadoVoz.value = EstadoVoz.Exito(esIngresoDetectado)
    }

    private fun extraerMontoMaster(texto: String): Double {
        val textoUnido = texto.replace(Regex("(\\d)\\s+(\\d)"), "$1$2")
        val regex = Regex("[0-9.,]+")
        val matches = regex.findAll(textoUnido)
        var mejorMonto = 0.0

        for (match in matches) {
            var str = match.value
            if (str.startsWith(".") || str.startsWith(",")) str = str.substring(1)
            if (str.endsWith(".") || str.endsWith(",")) str = str.substring(0, str.length - 1)
            if (str.isEmpty()) continue

            if (!str.contains(".") && !str.contains(",")) {
                val valor = str.toDoubleOrNull() ?: 0.0
                if (valor > mejorMonto) mejorMonto = valor
                continue
            }
            val ultimoPunto = str.lastIndexOf('.')
            val ultimaComa = str.lastIndexOf(',')
            val ultimoSep = max(ultimoPunto, ultimaComa)
            val digitosDespues = str.length - 1 - ultimoSep
            var montoCandidato = 0.0

            if (digitosDespues == 3) {
                val limpio = str.replace(".", "").replace(",", "")
                montoCandidato = limpio.toDoubleOrNull() ?: 0.0
            } else {
                val parteEntera = str.substring(0, ultimoSep).replace(".", "").replace(",", "")
                val parteDecimal = str.substring(ultimoSep + 1)
                montoCandidato = "$parteEntera.$parteDecimal".toDoubleOrNull() ?: 0.0
            }
            if (montoCandidato > mejorMonto) mejorMonto = montoCandidato
        }
        return mejorMonto
    }

    // --- GUARDADO ---
    fun guardarTransaccion(onGuardadoExitoso: () -> Unit) {
        val montoVal = _monto.value.toDoubleOrNull() ?: 0.0
        val notaCompletaFinal = if (_descripcion.value.isBlank()) _categoria.value else _descripcion.value

        // Generamos el resumen
        val notaResumenGenerada = NotaUtils.generarResumen(notaCompletaFinal, _categoria.value)

        if (montoVal > 0) {
            viewModelScope.launch {
                val t = TransaccionEntity(
                    id = if (currentId == -1) 0 else currentId,
                    monto = montoVal,
                    categoria = _categoria.value,
                    notaCompleta = notaCompletaFinal,
                    notaResumen = notaResumenGenerada,
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