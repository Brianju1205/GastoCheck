package com.example.gastocheck.ui.theme.screens.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.entity.MetaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetasViewModel @Inject constructor(
    private val metaDao: MetaDao
) : ViewModel() {

    val metas = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun crearMeta(nombre: String, objetivo: Double) {
        viewModelScope.launch {
            metaDao.insertMeta(MetaEntity(nombre = nombre, montoObjetivo = objetivo, montoAhorrado = 0.0))
        }
    }

    fun abonarAMeta(meta: MetaEntity, monto: Double) {
        viewModelScope.launch {
            val nuevaCantidad = meta.montoAhorrado + monto
            // Aseguramos que no supere el objetivo (opcional)
            val cantidadFinal = if (nuevaCantidad > meta.montoObjetivo) meta.montoObjetivo else nuevaCantidad
            metaDao.updateMeta(meta.copy(montoAhorrado = cantidadFinal))
        }
    }

    fun borrarMeta(meta: MetaEntity) {
        viewModelScope.launch { metaDao.deleteMeta(meta) }
    }
}