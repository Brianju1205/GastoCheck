package com.example.gastocheck.ui.theme.screens.agregar

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.ocr.OcrService
import com.example.gastocheck.data.repository.CuentaRepository
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.ImageUtils
import com.example.gastocheck.ui.theme.util.NotaUtils
import com.example.gastocheck.ui.theme.util.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AgregarViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val geminiRepository: GeminiRepository,
    private val cuentaRepository: CuentaRepository,
    private val ocrService: OcrService,
    // REFACTORIZACIÓN: Eliminado SuscripcionRepository.
    // Las alertas de crédito ahora son manejadas por el CreditCardWorker.
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ESTADOS DE DATOS (Se mantienen igual) ---
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

    private val _fotos = MutableStateFlow<List<String>>(emptyList())
    val fotos = _fotos.asStateFlow()

    // --- ESTADOS DE CUENTAS ---
    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cuentaIdSeleccionada = MutableStateFlow(1)
    val cuentaIdSeleccionada = _cuentaIdSeleccionada.asStateFlow()

    // --- ESTADOS VOZ / PROCESAMIENTO ---
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
        _fotos.value = emptyList()
    }

    private fun cargarTransaccion(id: Int) {
        currentId = id
        viewModelScope.launch {
            val transaccion = repository.getTransaccionById(id)
            transaccion?.let {
                _monto.value = it.monto.toString()
                val notaLimpia = it.notaCompleta.substringBefore("\n(Conv:")
                _descripcion.value = notaLimpia
                _esIngreso.value = it.esIngreso
                _categoria.value = it.categoria
                _cuentaIdSeleccionada.value = it.cuentaId
                _fecha.value = it.fecha
                _fotos.value = it.fotos
            }
        }
    }

    // --- SETTERS ---
    fun setCuentaOrigen(id: Int) { _cuentaIdSeleccionada.value = id }
    fun onMontoChange(n: String) { _monto.value = n }
    fun onCategoriaChange(n: String) { _categoria.value = n }
    fun onFechaChange(n: Date) { _fecha.value = n }
    fun reiniciarEstadoVoz() { _estadoVoz.value = EstadoVoz.Inactivo }

    // --- GESTIÓN DE FOTOS ---
    fun onFotoCapturada(uriString: String) {
        viewModelScope.launch {
            val uri = Uri.parse(uriString)
            val pathPermanente = ImageUtils.guardarImagenEnInterno(context, uri)
            if (pathPermanente != null) {
                val listaActual = _fotos.value.toMutableList()
                listaActual.add(pathPermanente)
                _fotos.value = listaActual
            }
        }
    }

    fun eliminarFoto(path: String) {
        val listaActual = _fotos.value.toMutableList()
        listaActual.remove(path)
        _fotos.value = listaActual
    }

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

    // --- LÓGICA ESCANEO DE RECIBOS ---
    fun escanearRecibo(uri: Uri) {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.ProcesandoIA
            onFotoCapturada(uri.toString())

            try {
                val textoCrudo = ocrService.procesarImagen(uri)

                if (textoCrudo.isNotBlank()) {
                    val datos = geminiRepository.analizarTextoRecibo(textoCrudo)

                    if (datos != null) {
                        val montoActual = _monto.value.toDoubleOrNull() ?: 0.0
                        val nuevoMonto = montoActual + datos.monto
                        _monto.value = nuevoMonto.toString()

                        if (_categoria.value == "Otros") {
                            _categoria.value = datos.categoria
                        }

                        val notaActual = _descripcion.value
                        val nuevaNota = if (notaActual.isBlank()) datos.descripcion else "$notaActual + ${datos.descripcion}"
                        _descripcion.value = nuevaNota

                        if (datos.fecha != null) {
                            _fecha.value = datos.fecha
                        }
                        _estadoVoz.value = EstadoVoz.Inactivo
                    } else {
                        if (_descripcion.value.isBlank()) {
                            _descripcion.value = "Texto detectado: ${textoCrudo.take(100)}..."
                        }
                        _estadoVoz.value = EstadoVoz.Error
                    }
                } else {
                    _estadoVoz.value = EstadoVoz.Error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _estadoVoz.value = EstadoVoz.Error
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

    // --- GUARDADO DE TRANSACCIÓN REFACTORIZADO ---
    fun guardarTransaccion(monedaOrigen: String, onGuardadoExitoso: () -> Unit) {
        val montoOriginal = _monto.value.toDoubleOrNull() ?: 0.0

        if (montoOriginal > 0) {
            viewModelScope.launch {
                var montoFinal = montoOriginal

                val notaUsuario = if (_descripcion.value.isBlank()) _categoria.value else _descripcion.value
                var detalleConversion = ""
                if (monedaOrigen != "MXN") {
                    montoFinal = CurrencyUtils.convertirAMxn(montoOriginal, monedaOrigen)
                    val montoOrigStr = CurrencyUtils.formatCurrency(montoOriginal).replace("$", "")
                    val montoFinalStr = CurrencyUtils.formatCurrency(montoFinal)
                    detalleConversion = "\n(Conv: $montoOrigStr $monedaOrigen ≈ $montoFinalStr MXN)"
                }
                val notaResumenGenerada = NotaUtils.generarResumen(notaUsuario, _categoria.value)
                val notaCompletaFinal = notaUsuario + detalleConversion

                // 1. OBTENER LA CUENTA ACTUAL
                val cuenta = cuentaRepository.getCuentaById(_cuentaIdSeleccionada.value)

                if (cuenta != null) {
                    // 2. CALCULAR NUEVO SALDO
                    // Esta lógica es fundamental. Aunque aquí no manejamos recordatorios,
                    // el Worker de Crédito necesita leer el saldo actualizado para calcular la deuda.
                    val nuevoSaldo = if (_esIngreso.value) {
                        cuenta.saldoInicial + montoFinal
                    } else {
                        cuenta.saldoInicial - montoFinal
                    }

                    // 3. ACTUALIZAR CUENTA EN BD
                    val cuentaActualizada = cuenta.copy(saldoInicial = nuevoSaldo)
                    cuentaRepository.updateCuenta(cuentaActualizada)

                    // 4. GUARDAR TRANSACCIÓN
                    val t = TransaccionEntity(
                        id = if (currentId == -1) 0 else currentId,
                        monto = montoFinal,
                        categoria = _categoria.value,
                        notaCompleta = notaCompletaFinal,
                        notaResumen = notaResumenGenerada,
                        fecha = _fecha.value,
                        esIngreso = _esIngreso.value,
                        cuentaId = _cuentaIdSeleccionada.value,
                        fotos = _fotos.value
                    )
                    repository.insertTransaccion(t)

                    // REFACTORIZACIÓN COMPLETA:
                    // Se ha eliminado el bloque que actualizaba recordatorios/suscripciones aquí.
                    // La lógica ahora es desacoplada: El Worker leerá el nuevo saldo de la cuenta
                    // en su próxima ejecución diaria y generará las alertas pertinentes.
                }

                limpiarFormulario()
                onGuardadoExitoso()
            }
        }
    }
}