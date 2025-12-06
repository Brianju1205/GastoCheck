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

    // Obtener TODAS (Para el resumen global)
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun getAllTransacciones(): Flow<List<TransaccionEntity>>

    // Obtener POR CUENTA (Para el detalle)
    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC")
    fun getTransaccionesByCuenta(cuentaId: Int): Flow<List<TransaccionEntity>>

    @Query("SELECT * FROM transacciones WHERE id = :id")
    suspend fun getTransaccionById(id: Int): TransaccionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: TransaccionEntity)

    @Delete
    suspend fun deleteTransaccion(transaccion: TransaccionEntity)

    @Query("SELECT * FROM transacciones")
    suspend fun getAllTransaccionesList(): List<TransaccionEntity>

    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId")
    suspend fun getTransaccionesByCuentaList(cuentaId: Int): List<TransaccionEntity>
}
