package com.example.gastocheck.ui.theme.screens.historial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val repository: TransaccionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Recibimos el ID de la cuenta desde la navegación (si es -1 es Global)
    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: -1

    // Obtenemos el flujo completo de historial sin límites
    val historialCompleto: StateFlow<List<BalanceSnapshotEntity>> = repository.getHistorialSaldos(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}