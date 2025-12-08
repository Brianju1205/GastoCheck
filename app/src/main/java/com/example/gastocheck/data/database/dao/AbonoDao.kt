package com.example.gastocheck.data.database.dao

import androidx.room.*
import com.example.gastocheck.data.database.entity.AbonoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbonoDao {
    // Obtener abonos de una meta específica, ordenados por fecha (más reciente primero)
    @Query("SELECT * FROM abonos WHERE metaId = :metaId ORDER BY fecha DESC")
    fun getAbonosPorMeta(metaId: Int): Flow<List<AbonoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbono(abono: AbonoEntity)

    @Update
    suspend fun updateAbono(abono: AbonoEntity)

    @Delete
    suspend fun deleteAbono(abono: AbonoEntity)
}