package com.example.gastocheck.ui.theme.screens.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    private val metaDao: MetaDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val transacciones: StateFlow<List<TransaccionEntity>> = repository.getTransacciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metas: StateFlow<List<MetaEntity>> = metaDao.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SALDO INICIAL PERSISTENTE ---
    private val prefs: SharedPreferences = context.getSharedPreferences("gastocheck_prefs", Context.MODE_PRIVATE)

    // Leemos el saldo guardado, o 0.0 por defecto
    private val _saldoManual = MutableStateFlow(prefs.getFloat("saldo_inicial", 0f).toDouble())
    val saldoManual = _saldoManual.asStateFlow()

    fun actualizarSaldoManual(nuevoSaldo: Double) {
        _saldoManual.value = nuevoSaldo
        // Guardamos el nuevo saldo
        prefs.edit().putFloat("saldo_inicial", nuevoSaldo.toFloat()).apply()
    }

    fun borrarTransaccion(transaccion: TransaccionEntity) {
        viewModelScope.launch {
            repository.deleteTransaccion(transaccion)
        }
    }
}

