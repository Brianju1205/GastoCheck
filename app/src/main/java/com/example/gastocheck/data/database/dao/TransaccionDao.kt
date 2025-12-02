package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaccionDao {

    // Leer todas las transacciones ordenadas por fecha (más recientes primero)
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun getTransacciones(): Flow<List<TransaccionEntity>>

    // Guardar una transacción (si ya existe ID, la reemplaza)
    @Query("SELECT * FROM transacciones WHERE id = :id")
    suspend fun getTransaccionById(id: Int): TransaccionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: TransaccionEntity)

    // Borrar una transacción
    @Delete
    suspend fun deleteTransaccion(transaccion: TransaccionEntity)
}