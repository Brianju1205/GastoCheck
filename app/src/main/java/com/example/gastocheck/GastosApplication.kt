package com.example.gastocheck

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gastocheck.ui.theme.worker.DailyBalanceWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class GastosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Iniciar el worker de balance diario
        scheduleDailyBalanceWorker(this)
    }

    private fun scheduleDailyBalanceWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Calcular cuánto tiempo falta para las 11:59 PM
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23) // 23 horas (11 PM)
            set(Calendar.MINUTE, 59)      // 59 minutos
            set(Calendar.SECOND, 0)
        }

        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

        // 2. Crear la petición de trabajo periódico (cada 24 horas)
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyBalanceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("daily_balance_tracker")
            .build()

        // 3. Encolar el trabajo (KEEP asegura que no se duplique)
        workManager.enqueueUniquePeriodicWork(
            "DailyBalanceWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}