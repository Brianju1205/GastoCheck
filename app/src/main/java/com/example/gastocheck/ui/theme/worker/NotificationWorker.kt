package com.example.gastocheck.ui.theme.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gastocheck.MainActivity
import com.example.gastocheck.R
import com.example.gastocheck.data.database.dao.NotificacionDao // <--- Importante
import com.example.gastocheck.data.database.entity.NotificacionEntity // <--- Importante
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Date
import kotlin.random.Random

// CAMBIO 1: Usamos CoroutineWorker para poder usar la Base de Datos (suspend functions)
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // CAMBIO 2: EntryPoint para obtener el DAO sin problemas de inyección
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationWorkerEntryPoint {
        fun notificacionDao(): NotificacionDao
    }

    override suspend fun doWork(): Result {
        // Obtenemos los datos que enviaste al programar el worker
        val titulo = inputData.getString("titulo") ?: "Recordatorio"
        val mensaje = inputData.getString("mensaje") ?: "Tienes un pendiente."
        val idNotificacion = inputData.getInt("id", Random.nextInt())

        // Obtenemos el tipo (si no se envía, asumimos que es una SUSCRIPCIÓN)
        val tipo = inputData.getString("tipo") ?: "SUSCRIPCION"

        // 1. Mostrar la notificación visual en el teléfono
        mostrarNotificacionSistema(titulo, mensaje, idNotificacion)

        // 2. GUARDAR EN LA BASE DE DATOS (Para que salga en tu historial)
        guardarEnHistorial(titulo, mensaje, tipo)

        return Result.success()
    }

    private suspend fun guardarEnHistorial(titulo: String, mensaje: String, tipo: String) {
        val appContext = applicationContext

        // Inyección manual segura
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            NotificationWorkerEntryPoint::class.java
        )
        val dao = entryPoint.notificacionDao()

        // Crear la entidad
        val nuevaNotificacion = NotificacionEntity(
            titulo = titulo,
            mensaje = mensaje,
            fecha = Date(),
            tipo = tipo, // "SUSCRIPCION", "META", etc.
            leida = false,
            iconoRef = if (tipo == "SUSCRIPCION") "EventRepeat" else "Notifications"
        )

        // Insertar
        dao.insertNotificacion(nuevaNotificacion)
    }

    private fun mostrarNotificacionSistema(titulo: String, mensaje: String, id: Int) {
        val context = applicationContext
        val channelId = "suscripciones_channel"
        val channelName = "Recordatorios de Suscripciones"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje)) // Para textos largos
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}