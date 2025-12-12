package com.example.gastocheck.ui.theme.screens.saldo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SaldoViewModel @Inject constructor(
    private val balanceDao: BalanceSnapshotDao,
    private val cuentaDao: CuentaDao,
    private val transaccionDao: TransaccionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaldoUiState())
    val uiState: StateFlow<SaldoUiState> = _uiState.asStateFlow()

    init {
        cargarDatos(PeriodoFiltro.SEMANA)
    }

    fun onPeriodoChanged(periodo: PeriodoFiltro) {
        cargarDatos(periodo)
    }

    // Nuevo: Función para actualizar qué punto está tocando el usuario
    fun onPuntoSeleccionado(punto: Pair<LocalDate, Double>?) {
        _uiState.value = _uiState.value.copy(puntoSeleccionado = punto)
    }

    private fun cargarDatos(periodo: PeriodoFiltro) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, periodoSeleccionado = periodo, puntoSeleccionado = null)

            // 1. CALCULAR SALDO REAL ACTUAL
            val sumaIniciales = cuentaDao.obtenerSumaSaldosIniciales() ?: 0.0
            val totalIngresos = transaccionDao.obtenerTotalIngresos()
            val totalGastos = transaccionDao.obtenerTotalGastos()
            val saldoRealActual = sumaIniciales + totalIngresos - totalGastos

            // 2. OBTENER Y PROCESAR HISTORIAL
            val historialEntities: List<BalanceSnapshotEntity> = balanceDao.getHistorialSaldosList()

            // Convertir a pares Fecha-Saldo
            val todosLosPuntos = historialEntities.map { snapshot ->
                val date = snapshot.fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                date to snapshot.saldo
            }

            // --- LÓGICA CLAVE: AGRUPAR POR DÍA (Tomar el último registro de cada día) ---
            val puntosUnicosPorDia = todosLosPuntos
                .groupBy { it.first } // Agrupa por fecha
                .mapValues { entry ->
                    // De cada grupo (día), tomamos el último valor registrado (el más reciente)
                    entry.value.last().second
                }
                .toList() // Convertimos map a list de pairs
                .map { it.first to it.second } // Aseguramos tipo Pair<LocalDate, Double>
                .sortedBy { it.first }

            // Agregar o actualizar el día de HOY con el saldo real calculado
            val listaConHoy = if (puntosUnicosPorDia.none { it.first == LocalDate.now() }) {
                puntosUnicosPorDia + (LocalDate.now() to saldoRealActual)
            } else {
                // Si ya existe hoy, lo reemplazamos con el valor en tiempo real
                puntosUnicosPorDia.filter { it.first != LocalDate.now() } + (LocalDate.now() to saldoRealActual)
            }.sortedBy { it.first }

            // 3. APLICAR FILTRO DE TIEMPO
            val fechaLimite = if (periodo == PeriodoFiltro.TODO) {
                LocalDate.MIN
            } else {
                // Si es HOY (0 dias), restamos 0, o sea mostramos solo hoy.
                // Pero una gráfica de 1 punto es fea, así que para "HOY" mostraremos desde ayer para ver tendencia inmediata
                val diasRestar = if (periodo == PeriodoFiltro.HOY) 1L else periodo.dias.toLong()
                LocalDate.now().minusDays(diasRestar)
            }

            val puntosFiltrados = listaConHoy.filter { !it.first.isBefore(fechaLimite) }

            // 4. CALCULAR KPIs
            if (puntosFiltrados.isNotEmpty()) {
                val startBalance = puntosFiltrados.first().second
                val avg = puntosFiltrados.map { it.second }.average()
                val max = puntosFiltrados.maxOf { it.second }
                val min = puntosFiltrados.minOf { it.second }

                val variacion = if (startBalance != 0.0) {
                    ((saldoRealActual - startBalance) / startBalance) * 100
                } else 0.0

                _uiState.value = _uiState.value.copy(
                    saldoActual = saldoRealActual,
                    historialPuntos = puntosFiltrados,
                    kpiPromedio = avg,
                    kpiMaximo = max,
                    kpiMinimo = min,
                    variacionPorcentaje = variacion,
                    proyeccionFinMes = saldoRealActual, // Simplificado
                    insightIa = if (puntosFiltrados.size < 2) "Poca información para tendencias." else "Datos actualizados.",
                    isLoading = false
                )
            } else {
                // Caso vacío
                _uiState.value = _uiState.value.copy(
                    saldoActual = saldoRealActual,
                    historialPuntos = listOf(LocalDate.now() to saldoRealActual),
                    isLoading = false
                )
            }
        }
    }
}