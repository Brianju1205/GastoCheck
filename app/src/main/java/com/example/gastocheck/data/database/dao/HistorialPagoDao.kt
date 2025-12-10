package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gastocheck.data.database.entity.HistorialPagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialPagoDao {
    @Query("SELECT * FROM historial_pagos_suscripcion WHERE suscripcionId = :subId ORDER BY fechaPago DESC")
    fun getHistorial(subId: Int): Flow<List<HistorialPagoEntity>>

    @Insert
    suspend fun insertPago(pago: HistorialPagoEntity)

    @Query("DELETE FROM historial_pagos_suscripcion WHERE suscripcionId = :subId")
    suspend fun deleteHistorial(subId: Int)
}