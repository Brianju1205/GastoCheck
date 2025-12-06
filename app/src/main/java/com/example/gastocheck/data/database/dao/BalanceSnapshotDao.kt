package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: BalanceSnapshotEntity)

    // Obtenemos los últimos 10 o 20 saldos para no saturar la UI
    @Query("SELECT * FROM balance_snapshots WHERE cuentaId = :cuentaId ORDER BY fecha DESC LIMIT 15")
    fun getHistorialSaldos(cuentaId: Int): Flow<List<BalanceSnapshotEntity>>

    // Para borrar historial si se borra una cuenta
    @Query("DELETE FROM balance_snapshots WHERE cuentaId = :cuentaId")
    suspend fun deleteSnapshotsByCuenta(cuentaId: Int)
}