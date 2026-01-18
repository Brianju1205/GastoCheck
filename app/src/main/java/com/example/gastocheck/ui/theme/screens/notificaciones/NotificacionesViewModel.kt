package com.example.gastocheck.ui.theme.screens.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.NotificacionDao
import com.example.gastocheck.data.database.entity.NotificacionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val dao: NotificacionDao
) : ViewModel() {

    // Lista completa de notificaciones (observa cambios en tiempo real)
    val notificaciones: Flow<List<NotificacionEntity>> = dao.getNotificaciones()

    // Conteo de no leídas (Útil para poner un globito rojo o Badge en el icono)
    val conteoNoLeidas: Flow<Int> = dao.getConteoNoLeidas()

    // Marcar todas como leídas (ej. al presionar el botón de "doble check")
    fun marcarTodasLeidas() {
        viewModelScope.launch {
            dao.marcarTodasComoLeidas()
        }
    }

    // Marcar una sola como leída (ej. al hacer click en ella)
    fun marcarUnaComoLeida(id: Int) {
        viewModelScope.launch {
            dao.marcarComoLeida(id)
        }
    }

    // Eliminar una notificación específica (al deslizar o dar click en la X)
    fun eliminar(n: NotificacionEntity) {
        viewModelScope.launch {
            dao.eliminarNotificacion(n)
        }
    }
}