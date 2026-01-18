package com.example.gastocheck.ui.theme.screens.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.AbonoDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.entity.AbonoEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// Enum para los filtros
enum class FiltroMeta { TODOS, PROGRESO, CUMPLIDAS, VENCIDAS, PAUSADAS }

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val metaDao: MetaDao,
    private val abonoDao: AbonoDao,
    private val repository: TransaccionRepository
) : ViewModel() {

    // 1. FUENTE DE DATOS
    private val _metasTodas = metaDao.getMetasActivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filtroActual = MutableStateFlow(FiltroMeta.PROGRESO)
    val filtroActual = _filtroActual.asStateFlow()

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. LÓGICA DE FILTRADO Y ORDENAMIENTO
    val metasFiltradas: StateFlow<List<MetaEntity>> = combine(_metasTodas, _filtroActual) { lista, filtro ->
        val hoy = System.currentTimeMillis()

        lista.filter { meta ->
            val estaCumplida = meta.montoAhorrado >= meta.montoObjetivo
            val estaVencida = meta.fechaLimite != null && meta.fechaLimite < hoy && !estaCumplida && !meta.esPausada

            when (filtro) {
                FiltroMeta.TODOS -> true
                FiltroMeta.PROGRESO -> !estaCumplida && !estaVencida && !meta.esPausada
                FiltroMeta.CUMPLIDAS -> estaCumplida
                FiltroMeta.VENCIDAS -> estaVencida
                FiltroMeta.PAUSADAS -> meta.esPausada && !estaCumplida
            }
        }
            // ORDENAMIENTO: Por prioridad manual (orden) en listas activas, por ID en historial
            .sortedBy {
                if (filtro == FiltroMeta.PROGRESO || filtro == FiltroMeta.TODOS) it.orden else it.id
            }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- ACCIONES UI ---

    fun cambiarFiltro(nuevoFiltro: FiltroMeta) {
        _filtroActual.value = nuevoFiltro
    }

    /**
     * ✅ FUNCIÓN MAESTRA DE REORDENAMIENTO
     * Esta función recibe la lista tal cual quedó visualmente en la pantalla
     * y actualiza los índices en la base de datos de un solo golpe.
     */
    fun onReorder(fromIndex: Int, toIndex: Int, listaYaOrdenada: List<MetaEntity>) {
        viewModelScope.launch {
            // Recorremos la lista visual y le asignamos números consecutivos (0, 1, 2...)
            val listaConIndicesNuevos = listaYaOrdenada.mapIndexed { index, meta ->
                meta.copy(orden = index)
            }
            // Guardamos el lote completo en la BD
            metaDao.updateMetas(listaConIndicesNuevos)
        }
    }

    // --- LÓGICA DE NEGOCIO (CRUD) ---

    fun guardarMeta(
        id: Int = 0,
        nombre: String,
        objetivo: Double,
        icono: String,
        colorHex: String,
        fechaLimite: Date?,
        cuentaId: Int,
        nota: String
    ) {
        viewModelScope.launch {
            if (id == 0) {
                // Al crear nueva, la ponemos al final de la lista
                val maxOrden = metaDao.getMaxOrden() ?: 0
                val nuevaMeta = MetaEntity(
                    id = 0,
                    nombre = nombre,
                    montoObjetivo = objetivo,
                    montoAhorrado = 0.0,
                    icono = icono,
                    colorHex = colorHex,
                    fechaLimite = fechaLimite?.time,
                    cuentaId = cuentaId,
                    nota = nota,
                    orden = maxOrden + 1,
                    esArchivada = false,
                    esPausada = false
                )
                metaDao.insertMeta(nuevaMeta)
            } else {
                val metaActual = _metasTodas.value.find { it.id == id }
                if (metaActual != null) {
                    val metaEditada = metaActual.copy(
                        nombre = nombre,
                        montoObjetivo = objetivo,
                        icono = icono,
                        colorHex = colorHex,
                        fechaLimite = fechaLimite?.time,
                        cuentaId = cuentaId,
                        nota = nota
                    )
                    metaDao.updateMeta(metaEditada)
                }
            }
        }
    }

    fun togglePausaMeta(meta: MetaEntity) {
        viewModelScope.launch {
            metaDao.updateMeta(meta.copy(esPausada = !meta.esPausada))
        }
    }

    fun borrarMeta(meta: MetaEntity) {
        viewModelScope.launch {
            metaDao.deleteMeta(meta)
        }
    }

    // --- ABONOS ---

    fun obtenerHistorialAbonos(metaId: Int): Flow<List<AbonoEntity>> {
        return abonoDao.getAbonosPorMeta(metaId)
    }

    fun abonarAMeta(meta: MetaEntity, monto: Double) {
        viewModelScope.launch {
            val nuevoAbono = AbonoEntity(
                metaId = meta.id,
                monto = monto,
                fecha = System.currentTimeMillis()
            )
            abonoDao.insertAbono(nuevoAbono)

            val nuevoTotal = meta.montoAhorrado + monto
            metaDao.updateMonto(meta.id, nuevoTotal)
        }
    }

    fun editarAbono(meta: MetaEntity, abono: AbonoEntity, nuevoMonto: Double) {
        viewModelScope.launch {
            val diferencia = nuevoMonto - abono.monto
            val abonoActualizado = abono.copy(monto = nuevoMonto)
            abonoDao.updateAbono(abonoActualizado)

            val nuevoTotalMeta = meta.montoAhorrado + diferencia
            val totalFinal = if (nuevoTotalMeta < 0) 0.0 else nuevoTotalMeta

            metaDao.updateMonto(meta.id, totalFinal)
        }
    }
}