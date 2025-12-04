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

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transacciones reactivas al filtro
    val transacciones: StateFlow<List<TransaccionEntity>> = _cuentaSeleccionadaId
        .flatMapLatest { id ->
            if (id == -1) repository.getTransaccionesGlobales()
            else repository.getTransaccionesPorCuenta(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saldo calculado dinámicamente
    val saldoTotal: StateFlow<Double> = combine(transacciones, cuentas, _cuentaSeleccionadaId) { trs, cts, filtroId ->
        if (filtroId == -1) {
            // Saldo Global = Suma de saldos iniciales + Ingresos - Gastos
            val saldoInicialTotal = cts.sumOf { it.saldoInicial }
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            saldoInicialTotal + ingresos - gastos
        } else {
            // Saldo Cuenta Específica
            val cuenta = cts.find { it.id == filtroId }
            val inicial = cuenta?.saldoInicial ?: 0.0
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            inicial + ingresos - gastos
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun cambiarFiltroCuenta(id: Int) {
        _cuentaSeleccionadaId.value = id
    }

    fun borrarTransaccion(transaccion: TransaccionEntity) {
        viewModelScope.launch {
            repository.deleteTransaccion(transaccion)
        }
    }

    fun borrarCuenta(cuenta: CuentaEntity) {
        // Lógica para borrar cuenta (podría requerir confirmar borrado en cascada)
        // Por ahora omitido para brevedad
    }
}
