package com.example.gastocheck.ui.theme.screens.saldo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.gemini.AnalisisCache // <--- NUEVO
import com.example.gastocheck.data.gemini.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SaldoViewModel @Inject constructor(
    private val balanceDao: BalanceSnapshotDao,
    private val cuentaDao: CuentaDao,
    private val transaccionDao: TransaccionDao,
    private val geminiRepository: GeminiRepository,
    private val analisisCache: AnalisisCache // <--- INYECTAMOS EL CACHÉ
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaldoUiState())
    val uiState: StateFlow<SaldoUiState> = _uiState.asStateFlow()

    private var iaEnProceso = false

    init {
        cargarDatos(PeriodoFiltro.SEMANA)
    }

    fun onPeriodoChanged(periodo: PeriodoFiltro) {
        cargarDatos(periodo)
    }

    fun onPuntoSeleccionado(punto: Pair<LocalDate, Double>?) {
        _uiState.value = _uiState.value.copy(puntoSeleccionado = punto)
    }

    // Función para forzar recarga (por ejemplo, con un botón de refrescar)
    fun forzarAnalisis() {
        analisisCache.limpiarCache()
        cargarDatos(_uiState.value.periodoSeleccionado)
    }

    private fun cargarDatos(periodo: PeriodoFiltro) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                periodoSeleccionado = periodo,
                puntoSeleccionado = null
            )

            // 1. Obtener Datos Reales
            val sumaIniciales = cuentaDao.obtenerSumaSaldosIniciales() ?: 0.0
            val totalIngresos = transaccionDao.obtenerTotalIngresos()
            val totalGastos = transaccionDao.obtenerTotalGastos()
            val saldoRealActual = sumaIniciales + totalIngresos - totalGastos

            val historialEntities = balanceDao.getHistorialSaldosList()

            // 2. Procesar Gráfica
            val puntos = historialEntities
                .map { it.fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() to it.saldo }
                .groupBy { it.first }
                .mapValues { it.value.last().second }
                .toList()
                .sortedBy { it.first }
                .toMutableList()

            if (puntos.none { it.first == LocalDate.now() }) {
                puntos.add(LocalDate.now() to saldoRealActual)
            }

            // 3. Filtros y KPIs
            val fechaLimite = when (periodo) {
                PeriodoFiltro.HOY -> LocalDate.now().minusDays(1)
                PeriodoFiltro.TODO -> LocalDate.MIN
                else -> LocalDate.now().minusDays(periodo.dias.toLong())
            }
            val puntosFiltrados = puntos.filter { !it.first.isBefore(fechaLimite) }

            val startBalance = puntosFiltrados.firstOrNull()?.second ?: 0.0
            val variacion = if (startBalance != 0.0) ((saldoRealActual - startBalance) / startBalance) * 100 else 0.0
            val avg = if (puntosFiltrados.isNotEmpty()) puntosFiltrados.map { it.second }.average() else 0.0
            val max = puntosFiltrados.maxOfOrNull { it.second } ?: 0.0
            val min = puntosFiltrados.minOfOrNull { it.second } ?: 0.0

            // 4. Actualizar UI (Datos base)
            _uiState.value = _uiState.value.copy(
                saldoActual = saldoRealActual,
                historialPuntos = puntosFiltrados,
                kpiPromedio = avg,
                kpiMaximo = max,
                kpiMinimo = min,
                variacionPorcentaje = variacion,
                isLoading = false
            )

            // 5. INTELIGENCIA ARTIFICIAL CON CACHÉ INTELIGENTE
            if (historialEntities.size >= 2 && !iaEnProceso) {
                // Generamos una "huella" única de los datos actuales
                val firmaActual = generarFirmaDatos(historialEntities.size, saldoRealActual, totalIngresos, totalGastos)

                // PREGUNTAMOS AL CACHÉ: ¿Ya analizamos esto antes?
                val analisisGuardado = withContext(Dispatchers.IO) {
                    analisisCache.obtenerAnalisisCacheado(firmaActual)
                }

                if (analisisGuardado != null) {
                    Log.d("CACHE_IA", "¡Hit de Caché! Usando análisis guardado (Sin llamar a API)")
                    _uiState.value = _uiState.value.copy(
                        proyeccionFinMes = analisisGuardado.proyeccionFinMes,
                        insightIa = analisisGuardado.insightPrincipal,
                        alertaTexto = analisisGuardado.patrones.firstOrNull(),
                        recomendacion = analisisGuardado.recomendacion
                    )
                } else {
                    // Si no coincide o es nuevo, llamamos a la API
                    Log.d("CACHE_IA", "Datos nuevos detectados. Llamando a API...")
                    _uiState.value = _uiState.value.copy(insightIa = "Analizando nuevos movimientos...")
                    iaEnProceso = true
                    analizarConGemini(historialEntities, firmaActual) // Pasamos la firma para guardar después
                    iaEnProceso = false
                }
            } else if (historialEntities.size < 2) {
                _uiState.value = _uiState.value.copy(insightIa = "Registra más movimientos para activar el análisis.")
            }
        }
    }

    private suspend fun analizarConGemini(historial: List<BalanceSnapshotEntity>, firmaParaGuardar: String) {
        try {
            val transacciones = transaccionDao.getAllTransaccionesList()
            val analisis = geminiRepository.analizarFinanzas(historial, transacciones)

            if (analisis != null) {
                // ¡ÉXITO! Guardamos en caché para la próxima
                withContext(Dispatchers.IO) {
                    analisisCache.guardarAnalisis(firmaParaGuardar, analisis)
                }

                _uiState.value = _uiState.value.copy(
                    proyeccionFinMes = analisis.proyeccionFinMes,
                    insightIa = analisis.insightPrincipal,
                    alertaTexto = analisis.patrones.firstOrNull(),
                    recomendacion = analisis.recomendacion
                )
            } else {
                _uiState.value = _uiState.value.copy(insightIa = "No se pudo actualizar el análisis.")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(insightIa = "Sin conexión.")
        }
    }

    // Crea un string único que cambia si los datos cambian
    private fun generarFirmaDatos(countHistorial: Int, saldo: Double, ingresos: Double, gastos: Double): String {
        val fechaHoy = LocalDate.now().toString()
        // La firma es: "2023-12-12|H:50|S:1500.0|I:5000|G:3500"
        // Si cambia la fecha, o entra dinero, o gastas -> La firma cambia -> Se activa la API
        return "$fechaHoy|H:$countHistorial|S:$saldo|I:$ingresos|G:$gastos"
    }
}