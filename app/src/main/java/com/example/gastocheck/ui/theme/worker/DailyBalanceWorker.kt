package com.example.gastocheck.ui.theme.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class DailyBalanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cuentaDao: CuentaDao,
    private val balanceSnapshotDao: BalanceSnapshotDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Obtener la suma total de todas las cuentas
            // Si devuelve null (no hay cuentas), asumimos 0.0
            val saldoTotal = cuentaDao.obtenerSaldoTotalGlobal() ?: 0.0

            // 2. Crear la entidad del snapshot
            val snapshot = BalanceSnapshotEntity(
                cuentaId = -1, // ID Global reservado
                saldo = saldoTotal,
                fecha = Date(), // Fecha y hora actual
                motivo = "Cierre diario automático"
            )

            // 3. Guardar en la base de datos
            balanceSnapshotDao.insertSnapshot(snapshot)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // Reintenta si falló por algo temporal
        }
    }
}