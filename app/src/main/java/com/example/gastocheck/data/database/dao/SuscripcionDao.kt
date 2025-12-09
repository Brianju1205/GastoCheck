package com.example.gastocheck.data.database.dao

import androidx.room.*
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuscripcionDao {
    @Query("SELECT * FROM suscripciones ORDER BY fechaPago ASC")
    fun getSuscripciones(): Flow<List<SuscripcionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuscripcion(suscripcion: SuscripcionEntity)

    @Delete
    suspend fun deleteSuscripcion(suscripcion: SuscripcionEntity)

    @Update
    suspend fun updateSuscripcion(suscripcion: SuscripcionEntity)
}