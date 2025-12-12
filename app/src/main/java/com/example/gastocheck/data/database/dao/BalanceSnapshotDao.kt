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

    // Para la gráfica (Flow)
    @Query("SELECT * FROM balance_snapshots WHERE cuentaId = :cuentaId ORDER BY fecha DESC LIMIT 15")
    fun getHistorialSaldos(cuentaId: Int): Flow<List<BalanceSnapshotEntity>>

    // --- ESTA ES LA QUE FALTABA Y CAUSA EL ERROR ---
    @Query("SELECT * FROM balance_snapshots WHERE cuentaId = -1 ORDER BY fecha ASC")
    suspend fun getHistorialSaldosList(): List<BalanceSnapshotEntity>

    @Query("DELETE FROM balance_snapshots WHERE cuentaId = :cuentaId")
    suspend fun deleteSnapshotsByCuenta(cuentaId: Int)
}