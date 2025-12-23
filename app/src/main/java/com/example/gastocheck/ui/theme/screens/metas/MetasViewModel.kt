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
import java.util.Collections
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val metaDao: MetaDao,
    private val abonoDao: AbonoDao,
    private val repository: TransaccionRepository
) : ViewModel() {

    // Usamos getMetasActivas() para que vengan ordenadas por el campo 'orden'
    // y filtradas por 'esArchivada = 0'
    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetasActivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cuentas: StateFlow<List<CuentaEntity>> = repository.getCuentas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- HISTORIAL DE ABONOS ---
    fun obtenerHistorialAbonos(metaId: Int): Flow<List<AbonoEntity>> {
        return abonoDao.getAbonosPorMeta(metaId)
    }

    // --- REORDENAMIENTO (SUBIR / BAJAR PRIORIDAD) ---
    fun actualizarOrdenMetas(listaReordenada: List<MetaEntity>) {
        viewModelScope.launch {
            // 1. Asignamos el nuevo índice (0, 1, 2...) al campo 'orden'
            val listaConNuevoOrden = listaReordenada.mapIndexed { index, meta ->
                meta.copy(orden = index)
            }
            // 2. Guardamos en bloque en la BD
            metaDao.updateMetas(listaConNuevoOrden)
        }
    }

    // --- GUARDAR (CREAR O EDITAR) ---
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
                // === CREAR NUEVA ===
                // Obtenemos el último orden para ponerla al final
                val maxOrden = metaDao.getMaxOrden() ?: 0

                val nuevaMeta = MetaEntity(
                    id = 0,
                    nombre = nombre,
                    montoObjetivo = objetivo,
                    montoAhorrado = 0.0, // Empieza en 0
                    icono = icono,
                    colorHex = colorHex,
                    fechaLimite = fechaLimite?.time,
                    cuentaId = cuentaId,
                    nota = nota,
                    orden = maxOrden + 1, // Asignamos el siguiente orden
                    esArchivada = false
                )
                metaDao.insertMeta(nuevaMeta)
            } else {
                // === ACTUALIZAR EXISTENTE ===
                // Buscamos la meta actual para conservar su montoAhorrado y orden
                val metaActual = metas.value.find { it.id == id }

                if (metaActual != null) {
                    val metaEditada = metaActual.copy(
                        nombre = nombre,
                        montoObjetivo = objetivo,
                        icono = icono,
                        colorHex = colorHex,
                        fechaLimite = fechaLimite?.time,
                        cuentaId = cuentaId,
                        nota = nota
                        // No tocamos montoAhorrado ni orden aquí
                    )
                    metaDao.updateMeta(metaEditada)
                }
            }
        }
    }

    // --- ABONAR DINERO ---
    fun abonarAMeta(meta: MetaEntity, monto: Double) {
        viewModelScope.launch {
            // 1. Insertar registro en historial de abonos
            val nuevoAbono = AbonoEntity(
                metaId = meta.id,
                monto = monto,
                fecha = System.currentTimeMillis()
            )
            abonoDao.insertAbono(nuevoAbono)

            // 2. Actualizar acumulado en la meta
            val nuevoTotal = meta.montoAhorrado + monto

            // Opcional: Actualizar también la cuenta de origen (restar saldo) si lo deseas
            // repository.actualizarSaldoCuenta(meta.cuentaId, -monto)

            metaDao.updateMonto(meta.id, nuevoTotal)
        }
    }

    // --- EDITAR UN ABONO ---
    fun editarAbono(meta: MetaEntity, abono: AbonoEntity, nuevoMonto: Double) {
        viewModelScope.launch {
            // Calculamos la diferencia para ajustar el total
            val diferencia = nuevoMonto - abono.monto

            // 1. Actualizar el abono
            val abonoActualizado = abono.copy(monto = nuevoMonto)
            abonoDao.updateAbono(abonoActualizado)

            // 2. Actualizar el total de la meta
            val nuevoTotalMeta = meta.montoAhorrado + diferencia
            val totalFinal = if (nuevoTotalMeta < 0) 0.0 else nuevoTotalMeta

            metaDao.updateMonto(meta.id, totalFinal)
        }
    }

    // --- ELIMINAR UN ABONO ---
    fun eliminarAbono(meta: MetaEntity, abono: AbonoEntity) {
        viewModelScope.launch {
            // 1. Eliminar abono
            abonoDao.deleteAbono(abono)

            // 2. Restar el monto al total de la meta
            val nuevoTotal = meta.montoAhorrado - abono.monto
            val totalFinal = if (nuevoTotal < 0) 0.0 else nuevoTotal

            metaDao.updateMonto(meta.id, totalFinal)
        }
    }

    // --- BORRAR META ---
    fun borrarMeta(meta: MetaEntity) {
        viewModelScope.launch {
            // Esto borrará la meta (y los abonos si tienes configurado CASCADE en la BD)
            metaDao.deleteMeta(meta)
        }
    }

    // --- ARCHIVAR META (Opcional, para completadas) ---
    fun toggleArchivarMeta(meta: MetaEntity) {
        viewModelScope.launch {
            metaDao.updateMeta(meta.copy(esArchivada = !meta.esArchivada))
        }
    }
}