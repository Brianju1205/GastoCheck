package com.example.gastocheck.ui.theme.screens.transferencia

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.ImageUtils
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
class TransferenciaViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
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

    // --- NUEVO: ESTADO FOTOS (LISTA) ---
    private val _fotos = MutableStateFlow<List<String>>(emptyList())
    val fotos = _fotos.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _estadoVoz = MutableStateFlow<EstadoVoz>(EstadoVoz.Inactivo)
    val estadoVoz = _estadoVoz.asStateFlow()

    sealed class EstadoVoz {
        object Inactivo : EstadoVoz()
        object Escuchando : EstadoVoz()
        object ProcesandoIA : EstadoVoz()
        object Exito : EstadoVoz()
        object Error : EstadoVoz()
    }

    private var transaccionIdEdicion: Int = -1

    // SETTERS
    fun setOrigen(id: Int) { _origenId.value = id }
    fun setDestino(id: Int) { _destinoId.value = id }
    fun onMontoChange(v: String) { _monto.value = v }
    fun onNotaChange(v: String) { _nota.value = v }
    fun onFechaChange(d: Date) { _fecha.value = d }

    // --- GESTIÓN DE FOTOS ---
    fun onFotoCapturada(uriString: String) {
        viewModelScope.launch {
            val uri = Uri.parse(uriString)
            // 1. COPIAMOS la imagen al almacenamiento interno (para que sea persistente)
            val pathPermanente = ImageUtils.guardarImagenEnInterno(context, uri)

            if (pathPermanente != null) {
                // 2. AÑADIMOS a la lista existente
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

    fun reiniciarEstadoVoz() { _estadoVoz.value = EstadoVoz.Inactivo }

    // --- INICIALIZACIÓN (Edición o Voz Externa) ---
    fun inicializar(id: Int = -1, textoVoz: String? = null) {
        if (textoVoz != null) {
            // Si viene texto de voz desde Home, lo procesamos
            procesarTextoHibrido(textoVoz)
            return
        }

        if (id != -1) {
            transaccionIdEdicion = id
            viewModelScope.launch {
                val t = repository.getTransaccionById(id)
                if (t != null) {
                    _monto.value = t.monto.toString()
                    _fecha.value = t.fecha
                    val tPareja = repository.getTransaccionPareja(t)
                    if (t.esIngreso) {
                        _destinoId.value = t.cuentaId
                        _origenId.value = tPareja?.cuentaId ?: -1
                    } else {
                        _origenId.value = t.cuentaId
                        _destinoId.value = tPareja?.cuentaId ?: -1
                    }

                    // --- LIMPIEZA DE NOTA ---
                    val nombrePareja = tPareja?.let { p -> cuentas.value.find { it.id == p.cuentaId }?.nombre } ?: ""
                    var notaLimpia = t.notaCompleta
                        .replace("Transferencia a $nombrePareja", "")
                        .replace("Recibido de $nombrePareja", "")
                        .trim()

                    // Correcciones de formato de nota
                    if (notaLimpia.startsWith(".")) {
                        notaLimpia = notaLimpia.substring(1).trim()
                    }
                    if (notaLimpia.contains("\n(Conv:")) {
                        notaLimpia = notaLimpia.substringBefore("\n(Conv:").trim()
                    }

                    _nota.value = notaLimpia

                    // Cargar Lista de Fotos
                    _fotos.value = t.fotos
                }
            }
        } else {
            limpiarFormulario()
        }
    }

    private fun limpiarFormulario() {
        transaccionIdEdicion = -1
        _monto.value = ""
        _nota.value = ""
        _fecha.value = Date()
        _origenId.value = -1
        _destinoId.value = -1
        _fotos.value = emptyList() // Limpiar lista
    }

    // --- LÓGICA DE VOZ (IA) ---

    fun iniciarEscuchaInteligente() {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.Escuchando
            val recognizer = VoiceRecognizer(context)
            recognizer.escuchar().collect { texto ->
                if (texto.isNotEmpty() && texto != "Error") {
                    procesarTextoHibrido(texto)
                } else {
                    _estadoVoz.value = EstadoVoz.Error
                    sendError("No se escuchó nada")
                }
            }
        }
    }

    fun procesarTextoExterno(texto: String) {
        procesarTextoHibrido(texto)
    }

    private fun procesarTextoHibrido(texto: String) {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.ProcesandoIA
            try {
                val interpretacion = geminiRepository.interpretarTexto(texto)

                if (interpretacion != null) {
                    if (interpretacion.monto > 0) {
                        _monto.value = interpretacion.monto.toString()
                    }
                    if (interpretacion.descripcion.isNotEmpty()) {
                        _nota.value = interpretacion.descripcion
                    } else {
                        _nota.value = texto
                    }

                    val listaCuentas = cuentas.value
                    if (!interpretacion.cuenta_origen.isNullOrBlank()) {
                        val cuentaEnc = buscarCuentaPorNombre(listaCuentas, interpretacion.cuenta_origen)
                        if (cuentaEnc != null) _origenId.value = cuentaEnc.id
                    }
                    if (!interpretacion.cuenta_destino.isNullOrBlank()) {
                        val cuentaEnc = buscarCuentaPorNombre(listaCuentas, interpretacion.cuenta_destino)
                        if (cuentaEnc != null) _destinoId.value = cuentaEnc.id
                    }

                    if (_origenId.value == -1 || _destinoId.value == -1) {
                        detectarCuentasLocalmente(texto, listaCuentas)
                    }
                    _estadoVoz.value = EstadoVoz.Exito
                } else {
                    procesarVozLocal(texto)
                }
            } catch (e: Exception) {
                procesarVozLocal(texto)
            }
        }
    }

    private fun procesarVozLocal(texto: String) {
        val montoDetectado = extraerMontoMaster(texto)
        if (montoDetectado > 0) {
            _monto.value = montoDetectado.toString()
        }
        _nota.value = texto
        detectarCuentasLocalmente(texto, cuentas.value)
        _estadoVoz.value = EstadoVoz.Exito
    }

    private fun detectarCuentasLocalmente(texto: String, listaCuentas: List<CuentaEntity>) {
        val cuentasEncontradas = listaCuentas.filter { cuenta ->
            texto.contains(cuenta.nombre, ignoreCase = true)
        }
        if (cuentasEncontradas.isNotEmpty()) {
            if (_origenId.value == -1) _origenId.value = cuentasEncontradas[0].id
            if (cuentasEncontradas.size > 1 && _destinoId.value == -1) {
                if (cuentasEncontradas[1].id != _origenId.value) _destinoId.value = cuentasEncontradas[1].id
            }
        }
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

    private fun buscarCuentaPorNombre(lista: List<CuentaEntity>, nombreBusqueda: String): CuentaEntity? {
        return lista.find {
            it.nombre.contains(nombreBusqueda, ignoreCase = true) ||
                    nombreBusqueda.contains(it.nombre, ignoreCase = true)
        }
    }

    // --- GUARDAR (CON CONVERSIÓN Y FOTOS) ---
    fun realizarTransferencia(monedaOrigen: String, onSuccess: () -> Unit) {
        val montoVal = _monto.value.toDoubleOrNull() ?: 0.0
        val origen = _origenId.value
        val destino = _destinoId.value

        if (montoVal <= 0) { sendError("Ingresa un monto válido"); return }
        if (origen == -1) { sendError("Selecciona una cuenta de origen"); return }
        if (destino == -1) { sendError("Selecciona una cuenta de destino"); return }
        if (origen == destino) { sendError("Las cuentas deben ser diferentes"); return }

        viewModelScope.launch {
            try {
                if (transaccionIdEdicion != -1) {
                    repository.eliminarTransferenciaCompleta(transaccionIdEdicion)
                }

                // --- LÓGICA DE CONVERSIÓN ---
                var montoFinal = montoVal
                val notaUsuario = _nota.value.trim()
                var detalleTecnico: String? = null

                if (monedaOrigen != "MXN") {
                    montoFinal = CurrencyUtils.convertirAMxn(montoVal, monedaOrigen)

                    val montoOrigStr = CurrencyUtils.formatCurrency(montoVal).replace("$", "")
                    val montoFinalStr = CurrencyUtils.formatCurrency(montoFinal)

                    detalleTecnico = "(Conv: $montoOrigStr $monedaOrigen ≈ $montoFinalStr MXN)"
                }

                // --- GUARDADO CON LISTA DE FOTOS ---
                repository.realizarTransferencia(
                    origenId = origen,
                    destinoId = destino,
                    monto = montoFinal,
                    notaUsuario = notaUsuario,
                    detalleTecnico = detalleTecnico,
                    fotos = _fotos.value, // <--- Enviamos la lista
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