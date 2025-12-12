package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

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

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM transacciones WHERE esIngreso = 1")
    suspend fun obtenerTotalIngresos(): Double

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM transacciones WHERE esIngreso = 0")
    suspend fun obtenerTotalGastos(): Double
    @Query("SELECT * FROM transacciones WHERE fecha = :fecha AND monto = :monto AND categoria = 'Transferencia' AND id != :originalId LIMIT 1")
    suspend fun getTransaccionPareja(fecha: Date, monto: Double, originalId: Int): TransaccionEntity?

    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId")
    suspend fun getTransaccionesByCuentaList(cuentaId: Int): List<TransaccionEntity>
}
