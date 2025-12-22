package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gastocheck.data.database.entity.CuentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CuentaDao {
    @Query("SELECT * FROM cuentas WHERE esArchivada = 0")
    fun getCuentas(): Flow<List<CuentaEntity>>

    @Query("SELECT * FROM cuentas WHERE id = :id")
    suspend fun getCuentaById(id: Int): CuentaEntity?

    @Query("SELECT * FROM cuentas")
    suspend fun getCuentasList(): List<CuentaEntity>
    // --- NUEVO: ESTE ES EL QUE NECESITA EL VIEWMODEL ---

    @Query("SELECT * FROM cuentas WHERE id = :id")
    fun getCuentaByIdFlow(id: Int): Flow<CuentaEntity?>

    // En com.example.gastocheck.data.database.dao.CuentaDao

    @Query("SELECT SUM(saldoInicial) FROM cuentas")
    suspend fun obtenerSaldoTotalGlobal(): Double?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuenta(cuenta: CuentaEntity): Long

    @Query("SELECT COALESCE(SUM(saldoInicial), 0.0) FROM cuentas")
    suspend fun obtenerSumaSaldosIniciales(): Double?
    @Update
    suspend fun updateCuenta(cuenta: CuentaEntity)

    @Delete
    suspend fun deleteCuenta(cuenta: CuentaEntity)
}