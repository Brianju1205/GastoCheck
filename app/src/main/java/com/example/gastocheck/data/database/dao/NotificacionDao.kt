package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gastocheck.data.database.entity.NotificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM notificaciones ORDER BY fecha DESC")
    fun getNotificaciones(): Flow<List<NotificacionEntity>>

    @Query("SELECT COUNT(*) FROM notificaciones WHERE leida = 0")
    fun getConteoNoLeidas(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificacion(notificacion: NotificacionEntity)

    @Query("UPDATE notificaciones SET leida = 1 WHERE id = :id")
    suspend fun marcarComoLeida(id: Int)

    @Query("UPDATE notificaciones SET leida = 1")
    suspend fun marcarTodasComoLeidas()

    @Delete
    suspend fun eliminarNotificacion(notificacion: NotificacionEntity)
}