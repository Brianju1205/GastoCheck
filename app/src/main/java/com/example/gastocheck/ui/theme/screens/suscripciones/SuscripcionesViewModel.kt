package com.example.gastocheck.ui.theme.screens.suscripciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.SuscripcionDao
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class EstadoSuscripcion { PAGADO, PENDIENTE, ATRASADO, CANCELADO }

// 1. ACTUALIZAMOS EL ENUM DE FILTROS
enum class FiltroSuscripcion { TODAS, PROXIMAS, ATRASADAS, PAGADAS, CANCELADAS }

@HiltViewModel
class SuscripcionesViewModel @Inject constructor(
    private val suscripcionDao: SuscripcionDao,
    private val repository: TransaccionRepository
) : ViewModel() {

    private val _filtroActual = MutableStateFlow(FiltroSuscripcion.TODAS)
    val filtroActual = _filtroActual.asStateFlow()

    val suscripcionesRaw = suscripcionDao.getSuscripciones()
    val cuentas = repository.getCuentas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suscripcionesFiltradas = combine(suscripcionesRaw, _filtroActual) { lista, filtro ->
        lista.filter { sub ->
            val estado = calcularEstado(sub)
            // 2. LÓGICA PARA LOS NUEVOS FILTROS
            when (filtro) {
                FiltroSuscripcion.TODAS -> true
                FiltroSuscripcion.PROXIMAS -> estado == EstadoSuscripcion.PENDIENTE && diasRestantes(sub.fechaPago) <= 7
                FiltroSuscripcion.ATRASADAS -> estado == EstadoSuscripcion.ATRASADO
                FiltroSuscripcion.PAGADAS -> estado == EstadoSuscripcion.PAGADO
                FiltroSuscripcion.CANCELADAS -> estado == EstadoSuscripcion.CANCELADO
            }
        }.sortedBy { it.fechaPago }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMensual = suscripcionesRaw.map { lista ->
        lista.filter { it.estadoActual != "CANCELADO" }
            .sumOf { if (it.frecuencia == "Anual") it.monto / 12 else it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val alertasProximas = suscripcionesRaw.map { lista ->
        lista.filter {
            val estado = calcularEstado(it)
            estado == EstadoSuscripcion.PENDIENTE && diasRestantes(it.fechaPago) in 0..5
        }.sortedBy { it.fechaPago }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cambiarFiltro(nuevoFiltro: FiltroSuscripcion) {
        _filtroActual.value = nuevoFiltro
    }

    fun guardarSuscripcion(id: Int, nombre: String, monto: Double, fecha: Long, frec: String, icono: String, cta: Int, nota: String, recordatorio: String, hora: String) {
        viewModelScope.launch {
            val subExistente = suscripcionesRaw.first().find { it.id == id }
            val estado = subExistente?.estadoActual
            val nueva = SuscripcionEntity(id, nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora, estado)
            if (id == 0) suscripcionDao.insertSuscripcion(nueva) else suscripcionDao.updateSuscripcion(nueva)
        }
    }

    fun cambiarEstadoSuscripcion(sub: SuscripcionEntity, nuevoEstado: String) {
        viewModelScope.launch {
            suscripcionDao.updateSuscripcion(sub.copy(estadoActual = nuevoEstado))
        }
    }

    fun borrarSuscripcion(sub: SuscripcionEntity) {
        viewModelScope.launch { suscripcionDao.deleteSuscripcion(sub) }
    }

    fun calcularEstado(sub: SuscripcionEntity): EstadoSuscripcion {
        when (sub.estadoActual) {
            "CANCELADO" -> return EstadoSuscripcion.CANCELADO
            "PAGADO" -> return EstadoSuscripcion.PAGADO
            "PENDIENTE" -> return EstadoSuscripcion.PENDIENTE
        }
        val dias = diasRestantes(sub.fechaPago)
        return if (dias < 0) EstadoSuscripcion.ATRASADO else EstadoSuscripcion.PENDIENTE
    }

    fun diasRestantes(fechaPago: Long): Long {
        val hoy = System.currentTimeMillis()
        val diff = fechaPago - hoy
        return TimeUnit.MILLISECONDS.toDays(diff)
    }
}