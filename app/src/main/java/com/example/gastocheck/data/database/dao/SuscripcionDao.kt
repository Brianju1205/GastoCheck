package com.example.gastocheck.data.database.dao

import androidx.room.*
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuscripcionDao {
    @Query("SELECT * FROM suscripciones ORDER BY fechaPago ASC")
    fun getSuscripciones(): Flow<List<SuscripcionEntity>>

    // IMPORTANTE: Debe devolver Long para obtener el ID recién creado
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuscripcion(suscripcion: SuscripcionEntity): Long
    @Query("SELECT * FROM suscripciones WHERE id = :id")
    suspend fun getSuscripcionById(id: Int): SuscripcionEntity?

    @Delete
    suspend fun deleteSuscripcion(suscripcion: SuscripcionEntity)

    @Update
    suspend fun updateSuscripcion(suscripcion: SuscripcionEntity)

    @Query("DELETE FROM suscripciones WHERE cuentaId = :cuentaId AND (nombre LIKE 'Corte:%' OR nombre LIKE 'Pagar:%')")
    suspend fun eliminarRecordatoriosAutomaticos(cuentaId: Int)
}