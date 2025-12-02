package com.example.gastocheck.ui.theme.screens.agregar

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
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
    private val _esMeta = MutableStateFlow(false)
    val esMeta = _esMeta.asStateFlow()

    // --- ESTADOS DE UI (VOZ) ---
    private val _estadoVoz = MutableStateFlow<EstadoVoz>(EstadoVoz.Inactivo)
    val estadoVoz = _estadoVoz.asStateFlow()

    private var currentId: Int = -1

    sealed class EstadoVoz {
        object Inactivo : EstadoVoz()
        object Escuchando : EstadoVoz()
        object ProcesandoIA : EstadoVoz()
        data class Exito(val texto: String) : EstadoVoz()
        object Error : EstadoVoz()
    }

    init {
        val id = savedStateHandle.get<Int>("id") ?: -1
        val esIngresoInicial = savedStateHandle.get<Boolean>("esIngreso") ?: false

        if (id != -1) {
            cargarTransaccion(id)
        } else {
            _esIngreso.value = esIngresoInicial
        }
    }

    private fun cargarTransaccion(id: Int) {
        currentId = id
        viewModelScope.launch {
            val transaccion = repository.getTransaccionById(id)
            transaccion?.let {
                _monto.value = it.monto.toString()
                _descripcion.value = it.descripcion
                _esIngreso.value = it.esIngreso
                _categoria.value = it.categoria
            }
        }
    }

    // =====================================================================
    // LÓGICA DE VOZ
    // =====================================================================

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

    private fun procesarTextoHibrido(texto: String) {
        viewModelScope.launch {
            _estadoVoz.value = EstadoVoz.ProcesandoIA
            try {
                // INTENTO 1: Usar Gemini
                val interpretacion = geminiRepository.interpretarTexto(texto)
                if (interpretacion != null) {
                    aplicarDatosInterpretados(
                        interpretacion.tipo, interpretacion.monto,
                        interpretacion.categoria, interpretacion.descripcion
                    )
                } else {
                    // FALLBACK: Si Gemini devuelve null (ej. error interno)
                    procesarVozLocal(texto)
                }
            } catch (e: Exception) {
                // FALLBACK: Si hay excepción de red (OFFLINE)
                procesarVozLocal(texto)
            }
            _estadoVoz.value = EstadoVoz.Exito(texto)
        }
    }

    fun procesarVoz(texto: String) {
        procesarTextoHibrido(texto)
    }

    private fun aplicarDatosInterpretados(tipo: String, monto: Double, cat: String, desc: String) {
        _monto.value = monto.toString()
        _descripcion.value = desc
        _categoria.value = cat
        when (tipo.uppercase()) {
            "INGRESO" -> { _esIngreso.value = true; _esMeta.value = false }
            "META" -> { _esIngreso.value = false; _esMeta.value = true }
            else -> { _esIngreso.value = false; _esMeta.value = false }
        }
    }

    // --- LÓGICA LOCAL (OFFLINE) ---
    private fun procesarVozLocal(texto: String) {
        val textoLower = texto.lowercase(Locale.getDefault())

        // 1. Tipo
        var tipoDetectado = "GASTO"
        if (textoLower.contains("meta") || textoLower.contains("ahorro")) tipoDetectado = "META"
        else if (textoLower.contains("ingreso") || textoLower.contains("gané") || textoLower.contains("recibí")) tipoDetectado = "INGRESO"

        // 2. Categoría
        var catDetectada = "Otros"
        for (cat in CategoriaUtils.listaCategorias) {
            if (textoLower.contains(cat.nombre.lowercase())) {
                catDetectada = cat.nombre
                break
            }
        }
        if (tipoDetectado == "META") catDetectada = "Ahorro"

        // 3. Monto (Lógica Master)
        val montoEncontrado = extraerMontoMaster(texto)

        // 4. Descripción
        var descEncontrada = ""
        val ignorar = listOf("gasto", "ingreso", "meta", "pesos", "dólares", "en", "de", "el", "la", "un", "una", "para", "$")
        val palabras = texto.split(" ")

        for (palabra in palabras) {
            val pLimpia = palabra.replace(Regex("[^0-9]"), "")
            val esMonto = try {
                pLimpia.isNotEmpty() && montoEncontrado.toString().replace(".0","").startsWith(pLimpia)
            } catch(e:Exception) { false }

            if (!esMonto && !ignorar.contains(palabra.lowercase()) && !palabra.equals(catDetectada, true)) {
                descEncontrada += "$palabra "
            }
        }

        // Aplicar
        _monto.value = if (montoEncontrado > 0) montoEncontrado.toString() else ""
        _descripcion.value = descEncontrada.trim().ifEmpty { catDetectada }
        _categoria.value = catDetectada

        when (tipoDetectado) {
            "INGRESO" -> { _esIngreso.value = true; _esMeta.value = false }
            "META" -> { _esIngreso.value = false; _esMeta.value = true }
            else -> { _esIngreso.value = false; _esMeta.value = false }
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

    fun reiniciarEstadoVoz() { _estadoVoz.value = EstadoVoz.Inactivo }
    fun onMontoChange(nuevo: String) { _monto.value = nuevo }
    fun onDescripcionChange(nuevo: String) { _descripcion.value = nuevo }
    fun onEsIngresoChange(nuevo: Boolean) { _esIngreso.value = nuevo }
    fun onCategoriaChange(nuevo: String) { _categoria.value = nuevo }

    fun guardarTransaccion(onGuardadoExitoso: () -> Unit) {
        val montoDouble = _monto.value.toDoubleOrNull() ?: 0.0
        val descFinal = if (_descripcion.value.isBlank()) _categoria.value else _descripcion.value
        if (montoDouble > 0) {
            val transaccion = TransaccionEntity(
                id = if (currentId == -1) 0 else currentId,
                monto = montoDouble,
                categoria = _categoria.value,
                descripcion = descFinal,
                fecha = Date(),
                esIngreso = _esIngreso.value
            )
            viewModelScope.launch {
                repository.insertTransaccion(transaccion)
                onGuardadoExitoso()
            }
        }
    }
}
