package com.example.gastocheck.ui.theme.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.gastocheck.MainActivity
import com.example.gastocheck.R
import kotlin.random.Random

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: "Recordatorio de Pago"
        val mensaje = inputData.getString("mensaje") ?: "Tienes una suscripción pendiente."
        val idNotificacion = inputData.getInt("id", Random.nextInt())

        mostrarNotificacion(titulo, mensaje, idNotificacion)

        return Result.success()
    }

    private fun mostrarNotificacion(titulo: String, mensaje: String, id: Int) {
        val context = applicationContext
        val channelId = "suscripciones_channel"
        val channelName = "Recordatorios de Suscripciones"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // CORRECCIÓN AQUÍ: Usar Build.VERSION_CODES.O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordar pagos de servicios"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este icono exista
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}