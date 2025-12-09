package com.example.gastocheck.ui.theme.screens.suscripciones

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gastocheck.data.database.dao.SuscripcionDao
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class EstadoSuscripcion { PAGADO, PENDIENTE, ATRASADO, CANCELADO }
enum class FiltroSuscripcion { TODAS, PROXIMAS, ATRASADAS, PAGADAS, CANCELADAS }

@HiltViewModel
class SuscripcionesViewModel @Inject constructor(
    private val suscripcionDao: SuscripcionDao,
    private val repository: TransaccionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _filtroActual = MutableStateFlow(FiltroSuscripcion.TODAS)
    val filtroActual = _filtroActual.asStateFlow()

    val suscripcionesRaw = suscripcionDao.getSuscripciones()
    val cuentas = repository.getCuentas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suscripcionesFiltradas = combine(suscripcionesRaw, _filtroActual) { lista, filtro ->
        lista.filter { sub ->
            val estado = calcularEstado(sub)
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

            val idFinal = if (id == 0) {
                suscripcionDao.insertSuscripcion(nueva).toInt()
            } else {
                suscripcionDao.updateSuscripcion(nueva)
                id
            }

            programarNotificacion(idFinal, nombre, monto, fecha, recordatorio, hora)
        }
    }

    private fun programarNotificacion(id: Int, nombre: String, monto: Double, fechaPago: Long, recordatorio: String, hora: String) {
        val workManager = WorkManager.getInstance(context)
        val tag = "sub_$id"

        workManager.cancelAllWorkByTag(tag)

        val diasAntes = when(recordatorio) {
            "1 día antes" -> 1
            "3 días antes" -> 3
            "7 días antes" -> 7
            else -> 0
        }

        // --- LÓGICA DE MENSAJE PERSONALIZADO ---
        val mensajeTexto = when(recordatorio) {
            "1 día antes" -> "Recuerda que pagarás $$monto mañana"
            "3 días antes" -> "Recuerda que pagarás $$monto dentro de 3 días"
            "7 días antes" -> "Recuerda que pagarás $$monto dentro de 7 días"
            else -> "Recuerda que tienes un pago de $$monto pendiente"
        }
        // ---------------------------------------

        val calendario = Calendar.getInstance().apply { timeInMillis = fechaPago }
        val (h, m) = hora.split(":").map { it.toInt() }

        calendario.add(Calendar.DAY_OF_YEAR, -diasAntes)
        calendario.set(Calendar.HOUR_OF_DAY, h)
        calendario.set(Calendar.MINUTE, m)
        calendario.set(Calendar.SECOND, 0)

        val triggerTime = calendario.timeInMillis
        val ahora = System.currentTimeMillis()
        val delay = triggerTime - ahora

        if (delay > 0) {
            val datos = workDataOf(
                "titulo" to "Pago próximo: $nombre",
                "mensaje" to mensajeTexto, // Usamos el mensaje personalizado aquí
                "id" to id
            )

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(datos)
                .addTag(tag)
                .build()

            workManager.enqueue(workRequest)
        }
    }

    fun cambiarEstadoSuscripcion(sub: SuscripcionEntity, nuevoEstado: String) {
        viewModelScope.launch {
            suscripcionDao.updateSuscripcion(sub.copy(estadoActual = nuevoEstado))
        }
    }

    fun borrarSuscripcion(sub: SuscripcionEntity) {
        viewModelScope.launch {
            suscripcionDao.deleteSuscripcion(sub)
            WorkManager.getInstance(context).cancelAllWorkByTag("sub_${sub.id}")
        }
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