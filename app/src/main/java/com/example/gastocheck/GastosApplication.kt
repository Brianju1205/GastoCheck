package com.example.gastocheck

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gastocheck.ui.theme.worker.DailyBalanceWorker
import com.example.gastocheck.worker.CreditCardWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GastosApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // --- CORRECCIÓN AQUÍ ---
    // En lugar de "override fun getWorkManagerConfiguration() = ...", usamos "override val ..."
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        scheduleDailyBalanceWorker(this)
        programarWorkerCredito()
    }

    private fun scheduleDailyBalanceWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }

        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyBalanceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("daily_balance_tracker")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyBalanceWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    private fun programarWorkerCredito() {
        val workManager = WorkManager.getInstance(this)

        // CAMBIO: Usamos OneTimeWorkRequest (Una sola vez) en lugar de Periodic.
        // Esto tiene prioridad alta y Android lo ejecuta casi de inmediato.
        val workRequest = OneTimeWorkRequestBuilder<CreditCardWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS) // Ejecutar en 10 segundos
            .addTag("credit_test_worker")
            .build()

        // Usamos enqueueUniqueWork (no Periodic)
        // REPLACE asegura que si abres la app de nuevo, se reinicia el contador de 10s
        workManager.enqueueUniqueWork(
            "CreditCardWorkerTest",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}