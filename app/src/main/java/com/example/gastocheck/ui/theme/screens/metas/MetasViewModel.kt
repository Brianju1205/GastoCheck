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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val metaDao: MetaDao,
    private val abonoDao: AbonoDao, // Inyectamos el nuevo DAO
    private val repository: TransaccionRepository
) : ViewModel() {

    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Función para obtener el historial de una meta en tiempo real
    fun obtenerHistorialAbonos(metaId: Int): Flow<List<AbonoEntity>> {
        return abonoDao.getAbonosPorMeta(metaId)
    }

    fun guardarMeta(
        id: Int = 0,
        nombre: String,
        objetivo: Double,
        icono: String,
        fechaLimite: Date?,
        cuentaId: Int,
        nota: String
    ) {
        viewModelScope.launch {
            val montoActual = if (id != 0) {
                metas.value.find { it.id == id }?.montoAhorrado ?: 0.0
            } else {
                0.0
            }

            val nuevaMeta = MetaEntity(
                id = id,
                nombre = nombre,
                montoObjetivo = objetivo,
                montoAhorrado = montoActual,
                icono = icono,
                fechaLimite = fechaLimite?.time,
                cuentaId = cuentaId,
                nota = nota
            )
            metaDao.insertMeta(nuevaMeta)
        }
    }

    fun abonarAMeta(meta: MetaEntity, monto: Double) {
        viewModelScope.launch {
            // 1. Guardar en el historial
            val nuevoAbono = AbonoEntity(
                metaId = meta.id,
                monto = monto,
                fecha = System.currentTimeMillis()
            )
            abonoDao.insertAbono(nuevoAbono)

            // 2. Actualizar el total de la meta
            val nuevoTotal = meta.montoAhorrado + monto
            metaDao.updateMonto(meta.id, nuevoTotal)
        }
    }

    fun editarAbono(meta: MetaEntity, abono: AbonoEntity, nuevoMonto: Double) {
        viewModelScope.launch {
            // Calculamos la diferencia para ajustar el total de la meta
            val diferencia = nuevoMonto - abono.monto

            // 1. Actualizamos el abono individual
            val abonoActualizado = abono.copy(monto = nuevoMonto)
            abonoDao.updateAbono(abonoActualizado)

            // 2. Ajustamos el total de la meta
            val nuevoTotalMeta = meta.montoAhorrado + diferencia
            // Evitamos negativos por seguridad
            val totalFinal = if (nuevoTotalMeta < 0) 0.0 else nuevoTotalMeta

            metaDao.updateMonto(meta.id, totalFinal)
        }
    }

    fun borrarMeta(meta: MetaEntity) {
        viewModelScope.launch { metaDao.deleteMeta(meta) }
    }
}