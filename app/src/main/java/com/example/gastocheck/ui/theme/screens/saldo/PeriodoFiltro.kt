package com.example.gastocheck.ui.theme.screens.saldo

import com.example.gastocheck.domain.model.Transaccion
import java.time.LocalDate

enum class PeriodoFiltro(val label: String, val dias: Int) {
    HOY("Hoy", 0),          // <--- NUEVO
    SEMANA("7 días", 7),
    MES("30 días", 30),
    TRIMESTRE("3 meses", 90),
    SEMESTRE("6 meses", 180),
    ANIO("Este año", 365),
    TODO("Todo", -1)
}

data class SaldoUiState(
    val saldoActual: Double = 0.0,
    val variacionPorcentaje: Double = 0.0,
    val historialPuntos: List<Pair<LocalDate, Double>> = emptyList(),
    // Nuevo campo para la interactividad (Punto que el usuario está tocando)
    val puntoSeleccionado: Pair<LocalDate, Double>? = null,
    val eventosGrafica: List<Transaccion> = emptyList(),
    val kpiPromedio: Double = 0.0,
    val kpiMaximo: Double = 0.0,
    val kpiMinimo: Double = 0.0,
    val proyeccionFinMes: Double = 0.0,
    val alertaTexto: String? = null,
    val insightIa: String = "Cargando análisis...",
    val periodoSeleccionado: PeriodoFiltro = PeriodoFiltro.SEMANA,
    val isLoading: Boolean = false
)