package com.example.gastocheck.ui.theme.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val metaDao: MetaDao
) : ViewModel() {

    // Filtro de cuenta (-1 significa "Todas")
    private val _cuentaSeleccionadaId = MutableStateFlow(-1)
    val cuentaSeleccionadaId = _cuentaSeleccionadaId.asStateFlow()

    // Flujo de cuentas disponibles
    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flujo de metas de ahorro
    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transacciones filtradas reactivamente
    // Si cambia el filtro (_cuentaSeleccionadaId), se recargan las transacciones automáticamente
    val transacciones: StateFlow<List<TransaccionEntity>> = _cuentaSeleccionadaId
        .flatMapLatest { id ->
            if (id == -1) repository.getTransaccionesGlobales()
            else repository.getTransaccionesPorCuenta(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saldo Total Calculado Dinámicamente
    // Combina: Lista de Transacciones + Lista de Cuentas + Filtro Actual
    val saldoTotal: StateFlow<Double> = combine(transacciones, cuentas, _cuentaSeleccionadaId) { trs, cts, filtroId ->
        if (filtroId == -1) {
            // Caso GLOBAL: Suma de saldos iniciales de TODAS las cuentas + Ingresos - Gastos
            val saldoInicialTotal = cts.sumOf { it.saldoInicial }
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            saldoInicialTotal + ingresos - gastos
        } else {
            // Caso CUENTA ESPECÍFICA: Saldo inicial de ESA cuenta + Sus Ingresos - Sus Gastos
            val cuenta = cts.find { it.id == filtroId }
            val inicial = cuenta?.saldoInicial ?: 0.0
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            inicial + ingresos - gastos
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- ACCIONES ---

    fun cambiarFiltroCuenta(id: Int) {
        _cuentaSeleccionadaId.value = id
    }

    fun borrarTransaccion(transaccion: TransaccionEntity) {
        viewModelScope.launch {
            repository.deleteTransaccion(transaccion)
        }
    }

    fun borrarCuenta(cuenta: CuentaEntity) {
        // Aquí iría la lógica para borrar cuenta
        // viewModelScope.launch { repository.deleteCuenta(cuenta) }
    }
}