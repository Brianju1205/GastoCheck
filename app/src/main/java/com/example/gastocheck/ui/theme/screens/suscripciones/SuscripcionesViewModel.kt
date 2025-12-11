package com.example.gastocheck.ui.theme.screens.suscripciones

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.HistorialPagoDao
import com.example.gastocheck.data.database.dao.SuscripcionDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.HistorialPagoEntity
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class EstadoSuscripcion { PAGADO, PENDIENTE, ATRASADO, CANCELADO }
enum class FiltroSuscripcion { TODAS, PROXIMAS, ATRASADAS, PAGADAS, CANCELADAS }

data class AdvertenciaState(
    val mensaje: String = "",
    val visible: Boolean = false,
    val esError: Boolean = false
)

@HiltViewModel
class SuscripcionesViewModel @Inject constructor(
    private val suscripcionDao: SuscripcionDao,
    private val historialPagoDao: HistorialPagoDao,
    private val transaccionDao: TransaccionDao,
    private val cuentaDao: CuentaDao,
    private val repository: TransaccionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _filtroActual = MutableStateFlow(FiltroSuscripcion.TODAS)
    val filtroActual = _filtroActual.asStateFlow()

    val suscripcionesRaw = suscripcionDao.getSuscripciones()
    val cuentas = repository.getCuentas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        verificarRenovaciones()
    }

    // --- ACCIÓN: GUARDAR / CREAR / EDITAR ---
    fun guardarSuscripcion(id: Int, nombre: String, monto: Double, fecha: Long, frec: String, icono: String, cta: Int, nota: String, recordatorio: String, hora: String) {
        viewModelScope.launch {
            // Buscamos si existe para preservar el estado si es edición
            val subExistente = suscripcionesRaw.first().find { it.id == id }

            // Si es nueva, el estado será null (para que se calcule automático). Si es edición, mantenemos el actual.
            val estadoInicial = if (id == 0) null else subExistente?.estadoActual

            val nueva = SuscripcionEntity(id, nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora, estadoInicial)

            val idFinal = if (id == 0) {
                // --- ES UNA NUEVA SUSCRIPCIÓN ---
                val newId = suscripcionDao.insertSuscripcion(nueva).toInt()

                // LÓGICA DE COBRO AUTOMÁTICO AL CREAR:
                // Si la fecha configurada es futura (ej. próximo mes), significa que el periodo actual YA se pagó.
                // Registramos ese gasto con fecha de HOY.
                val entidadGuardada = nueva.copy(id = newId)
                if (calcularEstado(entidadGuardada) == EstadoSuscripcion.PAGADO) {
                    registrarGastoYHistorial(entidadGuardada, System.currentTimeMillis())
                }

                newId
            } else {
                // --- ES EDICIÓN ---
                // En edición NO cobramos automáticamente para evitar duplicados accidentales al corregir datos.
                suscripcionDao.updateSuscripcion(nueva)
                id
            }

            programarNotificacion(nueva.copy(id = idFinal))
        }
    }

    // --- ACCIÓN: MARCAR COMO PAGADO (MANUAL) ---
    fun marcarComoPagado(sub: SuscripcionEntity) {
        viewModelScope.launch {
            val fechaPagoReal = System.currentTimeMillis()

            // 1. Registrar el gasto
            registrarGastoYHistorial(sub, fechaPagoReal)

            // 2. Avanzar fecha
            val nuevaFecha = calcularFechaFutura(sub.fechaPago, sub.frecuencia)
            val subActualizada = sub.copy(fechaPago = nuevaFecha, estadoActual = null)

            suscripcionDao.updateSuscripcion(subActualizada)
            programarNotificacion(subActualizada)
        }
    }

    // --- LÓGICA AUTOMÁTICA: RENOVACIONES ---
    private fun verificarRenovaciones() {
        viewModelScope.launch {
            val lista = suscripcionDao.getSuscripciones().first()
            val hoyInicio = getInicioDia(System.currentTimeMillis())

            lista.forEach { sub ->
                val fechaPagoInicio = getInicioDia(sub.fechaPago)

                // Si la fecha ya venció y no está cancelada, renovamos y cobramos.
                // (Nota: Si estaba en PAGADO y venció, es nueva deuda. Si estaba en PENDIENTE y venció, es deuda atrasada,
                //  pero si usamos lógica de renovación auto, asumimos cobro recurrente).
                // Aquí aplicamos la lógica: Si estaba marcada como PAGADA (futuro) y ese futuro ya llegó/pasó -> Nuevo Ciclo.
                if (sub.estadoActual != "CANCELADO" && hoyInicio > fechaPagoInicio) {

                    // 1. Registrar gasto de la renovación
                    registrarGastoYHistorial(sub, sub.fechaPago) // Usamos la fecha teórica de pago

                    // 2. Calcular nueva fecha
                    val nuevaFecha = calcularFechaFutura(sub.fechaPago, sub.frecuencia)

                    // 3. Actualizar
                    val subRenovada = sub.copy(
                        fechaPago = nuevaFecha,
                        estadoActual = null
                    )

                    suscripcionDao.updateSuscripcion(subRenovada)
                    programarNotificacion(subRenovada)
                }
            }
        }
    }

    // --- HELPER CENTRALIZADO PARA REGISTRAR GASTOS ---
    private suspend fun registrarGastoYHistorial(sub: SuscripcionEntity, fechaAplicacion: Long) {
        // 1. Historial
        val nuevoPago = HistorialPagoEntity(
            suscripcionId = sub.id,
            monto = sub.monto,
            fechaPago = fechaAplicacion
        )
        historialPagoDao.insertPago(nuevoPago)

        // 2. Transacción (Gasto)
        val nuevaTransaccion = TransaccionEntity(
            monto = sub.monto,
            categoria = sub.icono, // Icono correcto para la lista de gastos
            notaCompleta = "Pago de suscripción: ${sub.nombre}",
            notaResumen = sub.nombre,
            fecha = Date(fechaAplicacion),
            esIngreso = false,
            cuentaId = sub.cuentaId
        )
        transaccionDao.insertTransaccion(nuevaTransaccion)
    }

    // --- RESTO DE FUNCIONES (Helpers, Filtros, Estados) ---

    fun obtenerHistorial(subId: Int) = historialPagoDao.getHistorial(subId)

    fun deshacerPago(sub: SuscripcionEntity) {
        viewModelScope.launch {
            val fechaAnterior = calcularFechaPasada(sub.fechaPago, sub.frecuencia)
            val subRestaurada = sub.copy(fechaPago = fechaAnterior, estadoActual = null)
            suscripcionDao.updateSuscripcion(subRestaurada)
            programarNotificacion(subRestaurada)
        }
    }

    fun cambiarEstadoManual(sub: SuscripcionEntity, estado: String) {
        viewModelScope.launch {
            suscripcionDao.updateSuscripcion(sub.copy(estadoActual = estado))
        }
    }

    fun borrarSuscripcion(sub: SuscripcionEntity) {
        viewModelScope.launch {
            suscripcionDao.deleteSuscripcion(sub)
            historialPagoDao.deleteHistorial(sub.id)
            WorkManager.getInstance(context).cancelAllWorkByTag("sub_${sub.id}")
        }
    }

    fun calcularEstado(sub: SuscripcionEntity): EstadoSuscripcion {
        if (sub.estadoActual == "CANCELADO") return EstadoSuscripcion.CANCELADO
        val inicioDiaPago = getInicioDia(sub.fechaPago)
        val inicioHoy = getInicioDia(System.currentTimeMillis())
        return when {
            inicioDiaPago > inicioHoy -> EstadoSuscripcion.PAGADO
            inicioDiaPago == inicioHoy -> EstadoSuscripcion.PENDIENTE
            else -> EstadoSuscripcion.ATRASADO
        }
    }

    private val _rotadorMensaje = flow {
        while (true) { emit(true); delay(4000); emit(false); delay(4000) }
    }

    val estadoAdvertencia = combine(suscripcionesRaw, _rotadorMensaje) { lista, mostrarAtrasadas ->
        val activas = lista.filter { it.estadoActual != "CANCELADO" }
        val atrasadas = activas.count { calcularEstado(it) == EstadoSuscripcion.ATRASADO }
        val pendientesHoy = activas.count { calcularEstado(it) == EstadoSuscripcion.PENDIENTE }

        if (atrasadas > 0 && pendientesHoy > 0) {
            if (mostrarAtrasadas) crearAviso(atrasadas, true) else crearAviso(pendientesHoy, false)
        } else if (atrasadas > 0) {
            crearAviso(atrasadas, true)
        } else if (pendientesHoy > 0) {
            crearAviso(pendientesHoy, false)
        } else {
            AdvertenciaState(visible = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdvertenciaState())

    private fun crearAviso(cantidad: Int, esAtrasado: Boolean): AdvertenciaState {
        val texto = if (esAtrasado) {
            if (cantidad == 1) "1 suscripción sigue atrasada" else "$cantidad suscripciones siguen atrasadas"
        } else {
            if (cantidad == 1) "1 suscripción vence hoy" else "$cantidad suscripciones vencen hoy"
        }
        return AdvertenciaState(mensaje = texto, visible = true, esError = esAtrasado)
    }

    val suscripcionesFiltradas = combine(suscripcionesRaw, _filtroActual) { lista, filtro ->
        lista.filter { sub ->
            val estado = calcularEstado(sub)
            when (filtro) {
                FiltroSuscripcion.TODAS -> true
                FiltroSuscripcion.PROXIMAS -> estado == EstadoSuscripcion.PENDIENTE
                FiltroSuscripcion.ATRASADAS -> estado == EstadoSuscripcion.ATRASADO
                FiltroSuscripcion.PAGADAS -> estado == EstadoSuscripcion.PAGADO
                FiltroSuscripcion.CANCELADAS -> estado == EstadoSuscripcion.CANCELADO
            }
        }.sortedBy { sub ->
            if (calcularEstado(sub) == EstadoSuscripcion.PAGADO) calcularProximoPagoProyectado(sub) else sub.fechaPago
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMensual = suscripcionesRaw.map { lista ->
        lista.filter { it.estadoActual != "CANCELADO" }
            .sumOf {
                when (it.frecuencia) {
                    "Semanal" -> it.monto * 4; "Quincenal" -> it.monto * 2; "Anual" -> it.monto / 12; else -> it.monto
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val alertasProximas = suscripcionesRaw.map { lista ->
        val ahora = System.currentTimeMillis()
        val tresDiasMillis = TimeUnit.DAYS.toMillis(3)
        lista.filter { sub ->
            val fechaNotif = calcularFechaRecordatorio(sub)
            val diferencia = fechaNotif - ahora
            val enRango = diferencia > 0 && diferencia <= tresDiasMillis
            val esHoy = calcularEstado(sub) == EstadoSuscripcion.PENDIENTE
            sub.estadoActual != "CANCELADO" && (enRango || esHoy)
        }.sortedBy { it.fechaPago }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cambiarFiltro(nuevo: FiltroSuscripcion) { _filtroActual.value = nuevo }

    private fun programarNotificacion(sub: SuscripcionEntity) {
        val workManager = WorkManager.getInstance(context)
        val tag = "sub_${sub.id}"
        workManager.cancelAllWorkByTag(tag)
        val triggerTime = calcularFechaRecordatorio(sub)
        val delay = triggerTime - System.currentTimeMillis()
        if (delay > 0) {
            val msg = when(sub.recordatorio) { "1 día antes" -> "mañana"; "3 días antes" -> "en 3 días"; "7 días antes" -> "en 7 días"; else -> "pronto" }
            val datos = workDataOf("titulo" to "Pago próximo: ${sub.nombre}", "mensaje" to "Recuerda que pagarás $${sub.monto} $msg", "id" to sub.id)
            val req = OneTimeWorkRequestBuilder<NotificationWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(datos).addTag(tag).build()
            workManager.enqueue(req)
        }
    }

    private fun calcularFechaRecordatorio(sub: SuscripcionEntity): Long {
        val fechaBase = if (calcularEstado(sub) == EstadoSuscripcion.PAGADO) calcularProximoPagoProyectado(sub) else sub.fechaPago
        val diasAntes = when(sub.recordatorio) { "1 día antes" -> 1; "3 días antes" -> 3; "7 días antes" -> 7; else -> 0 }
        val (h, m) = try { sub.horaRecordatorio.split(":").map { it.toInt() } } catch (e: Exception) { listOf(9, 0) }
        return Calendar.getInstance().apply { timeInMillis = fechaBase; add(Calendar.DAY_OF_YEAR, -diasAntes); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }.timeInMillis
    }

    private fun getInicioDia(timeInMillis: Long): Long {
        return Calendar.getInstance().apply { this.timeInMillis = timeInMillis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    }

    private fun calcularTimestampLimite(fechaBase: Long, horaStr: String): Long {
        val (h, m) = try { horaStr.split(":").map { it.toInt() } } catch (e: Exception) { listOf(9, 0) }
        return Calendar.getInstance().apply { timeInMillis = fechaBase; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }.timeInMillis
    }

    private fun calcularFechaFutura(fecha: Long, frec: String): Long = sumarFecha(fecha, frec, 1)
    private fun calcularFechaPasada(fecha: Long, frec: String): Long = sumarFecha(fecha, frec, -1)

    private fun sumarFecha(fecha: Long, frec: String, cantidad: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fecha }
        val sign = if(cantidad > 0) 1 else -1
        when (frec) {
            "Semanal" -> cal.add(Calendar.WEEK_OF_YEAR, 1 * sign); "Quincenal" -> cal.add(Calendar.WEEK_OF_YEAR, 2 * sign); "Mensual" -> cal.add(Calendar.MONTH, 1 * sign); "Anual" -> cal.add(Calendar.YEAR, 1 * sign); else -> cal.add(Calendar.MONTH, 1 * sign)
        }
        return cal.timeInMillis
    }

    fun diasRestantes(fechaPago: Long): Long {
        val hoy = getInicioDia(System.currentTimeMillis())
        val pago = getInicioDia(fechaPago)
        return TimeUnit.MILLISECONDS.toDays(pago - hoy)
    }

    fun calcularProximoPagoProyectado(sub: SuscripcionEntity): Long {
        val hoy = System.currentTimeMillis()
        val cal = Calendar.getInstance(); cal.timeInMillis = sub.fechaPago
        if (cal.timeInMillis >= getInicioDia(hoy)) return cal.timeInMillis
        while (cal.timeInMillis < hoy) {
            when (sub.frecuencia) { "Semanal" -> cal.add(Calendar.WEEK_OF_YEAR, 1); "Quincenal" -> cal.add(Calendar.WEEK_OF_YEAR, 2); "Mensual" -> cal.add(Calendar.MONTH, 1); "Anual" -> cal.add(Calendar.YEAR, 1); else -> cal.add(Calendar.MONTH, 1) }
        }
        return cal.timeInMillis
    }
}