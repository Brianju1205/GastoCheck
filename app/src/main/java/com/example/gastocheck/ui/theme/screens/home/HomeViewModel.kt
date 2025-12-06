package com.example.gastocheck.ui.theme.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.ui.theme.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Agregamos RANGO al enum
enum class FiltroTiempo { DIA, SEMANA, MES, ANIO, RANGO }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val metaDao: MetaDao
) : ViewModel() {

    private val _cuentaSeleccionadaId = MutableStateFlow(-1)
    val cuentaSeleccionadaId = _cuentaSeleccionadaId.asStateFlow()

    private val _filtroTiempo = MutableStateFlow(FiltroTiempo.MES)
    val filtroTiempo = _filtroTiempo.asStateFlow()

    // Estado para guardar el rango personalizado (Inicio, Fin) en milisegundos
    private val _rangoFechas = MutableStateFlow<Pair<Long, Long>?>(null)
    val rangoFechas = _rangoFechas.asStateFlow()

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val transaccionesBase: Flow<List<TransaccionEntity>> = _cuentaSeleccionadaId
        .flatMapLatest { id ->
            if (id == -1) repository.getTransaccionesGlobales()
            else repository.getTransaccionesPorCuenta(id)
        }

    // LISTA FILTRADA (Gráficas y Lista)
    val transaccionesFiltradas: StateFlow<List<TransaccionEntity>> = combine(
        transaccionesBase,
        _filtroTiempo,
        _rangoFechas
    ) { lista, filtro, rango ->
        lista.filter { t ->
            when (filtro) {
                FiltroTiempo.DIA -> DateUtils.esHoy(t.fecha)
                FiltroTiempo.SEMANA -> DateUtils.esEstaSemana(t.fecha)
                FiltroTiempo.MES -> DateUtils.esEsteMes(t.fecha)
                FiltroTiempo.ANIO -> DateUtils.esEsteAnio(t.fecha)
                FiltroTiempo.RANGO -> {
                    if (rango != null) DateUtils.esEnRango(t.fecha, rango.first, rango.second)
                    else true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SALDO TOTAL (Siempre muestra el real, no se filtra)
    val saldoTotal: StateFlow<Double> = combine(transaccionesBase, cuentas, _cuentaSeleccionadaId) { trs, cts, filtroId ->
        if (filtroId == -1) {
            val saldoInicialTotal = cts.sumOf { it.saldoInicial }
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            saldoInicialTotal + ingresos - gastos
        } else {
            val cuenta = cts.find { it.id == filtroId }
            val inicial = cuenta?.saldoInicial ?: 0.0
            val ingresos = trs.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = trs.filter { !it.esIngreso }.sumOf { it.monto }
            inicial + ingresos - gastos
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // HISTORIAL DE SALDOS (Ahora VINCULADO a los filtros)
    val historialSaldos: StateFlow<List<BalanceSnapshotEntity>> = combine(
        _cuentaSeleccionadaId.flatMapLatest { id -> repository.getHistorialSaldos(id) },
        _filtroTiempo,
        _rangoFechas
    ) { historial, filtro, rango ->
        historial.filter { snapshot ->
            when (filtro) {
                FiltroTiempo.DIA -> DateUtils.esHoy(snapshot.fecha)
                FiltroTiempo.SEMANA -> DateUtils.esEstaSemana(snapshot.fecha)
                FiltroTiempo.MES -> DateUtils.esEsteMes(snapshot.fecha)
                FiltroTiempo.ANIO -> DateUtils.esEsteAnio(snapshot.fecha)
                FiltroTiempo.RANGO -> {
                    if (rango != null) DateUtils.esEnRango(snapshot.fecha, rango.first, rango.second)
                    else true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cambiarFiltroCuenta(id: Int) {
        _cuentaSeleccionadaId.value = id
    }

    fun cambiarFiltroTiempo(filtro: FiltroTiempo) {
        _filtroTiempo.value = filtro
        // Si cambiamos a un filtro estándar, limpiamos el rango personalizado
        if (filtro != FiltroTiempo.RANGO) {
            _rangoFechas.value = null
        }
    }

    // Nueva función para establecer rango personalizado
    fun establecerRangoFechas(inicio: Long, fin: Long) {
        _rangoFechas.value = Pair(inicio, fin)
        _filtroTiempo.value = FiltroTiempo.RANGO
    }

    fun borrarTransaccion(transaccion: TransaccionEntity) {
        viewModelScope.launch {
            repository.deleteTransaccion(transaccion)
        }
    }
}