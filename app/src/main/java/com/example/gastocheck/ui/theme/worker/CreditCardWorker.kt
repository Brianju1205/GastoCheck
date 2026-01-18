package com.example.gastocheck.worker

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
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.NotificacionDao // <--- NUEVO
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.NotificacionEntity // <--- NUEVO
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.InterestUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class CreditCardWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // 1. EntryPoint actualizado para pedir ambos DAOs
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CreditCardWorkerEntryPoint {
        fun cuentaDao(): CuentaDao
        fun notificacionDao(): NotificacionDao // <--- AGREGADO
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext

        // 2. Obtenemos las dependencias manualmente
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            CreditCardWorkerEntryPoint::class.java
        )

        val cuentaDao = entryPoint.cuentaDao()
        val notificacionDao = entryPoint.notificacionDao() // <--- OBTENEMOS EL DAO DE NOTIFICACIONES

        // 3. Obtener cuentas de crédito
        val cuentas = cuentaDao.getCuentas().first().filter { it.esCredito }
        val hoy = Calendar.getInstance()
        val diaHoy = hoy.get(Calendar.DAY_OF_MONTH)

        // 4. Procesar cada cuenta pasando también el notificacionDao
        cuentas.forEach { cuenta ->
            procesarCuenta(appContext, cuenta, diaHoy, notificacionDao)
        }

        return Result.success()
    }

    private suspend fun procesarCuenta(
        context: Context,
        cuenta: CuentaEntity,
        diaHoy: Int,
        notificacionDao: NotificacionDao // <--- RECIBIMOS EL DAO
    ) {
        val deudaActual = (cuenta.limiteCredito - cuenta.saldoInicial).coerceAtLeast(0.0)

        // --- CASO 1: Día de Corte ---
        if (diaHoy == cuenta.diaCorte) {
            val titulo = "Corte: ${cuenta.nombre}"
            val mensaje = "Hoy cierra tu tarjeta. Deuda al corte: ${CurrencyUtils.formatCurrency(deudaActual)}."

            enviarAlertaGlobal(
                context,
                notificacionDao,
                idSistema = cuenta.id * 100 + 1,
                titulo = titulo,
                mensaje = mensaje,
                esPrioritaria = false,
                tipo = "CREDITO"
            )
        }

        // --- CASO 2: Aviso previo (3 días antes del pago) ---
        if (diaHoy == (cuenta.diaPago - 3) && deudaActual > 0) {
            val titulo = "Pago próximo: ${cuenta.nombre}"
            val mensaje = "Vence en 3 días. Deuda: ${CurrencyUtils.formatCurrency(deudaActual)}."

            enviarAlertaGlobal(
                context,
                notificacionDao,
                idSistema = cuenta.id * 100 + 2,
                titulo = titulo,
                mensaje = mensaje,
                esPrioritaria = true,
                tipo = "CREDITO"
            )
        }

        // --- CASO 3: Día de Pago (CRÍTICO) ---
        if (diaHoy == cuenta.diaPago && deudaActual > 0) {
            val mensajeInteres = InterestUtils.generarMensajeNotificacion(deudaActual, cuenta.tasaInteres)
            val titulo = "¡HOY VENCE!: ${cuenta.nombre}"
            val mensaje = "Debes pagar ${CurrencyUtils.formatCurrency(deudaActual)}. $mensajeInteres"

            enviarAlertaGlobal(
                context,
                notificacionDao,
                idSistema = cuenta.id * 100 + 3,
                titulo = titulo,
                mensaje = mensaje,
                esPrioritaria = true,
                tipo = "CREDITO" // Color rojo en la UI
            )
        }
    }

    // Función auxiliar para enviar al sistema ANDROID y guardar en la BASE DE DATOS
    private suspend fun enviarAlertaGlobal(
        context: Context,
        dao: NotificacionDao,
        idSistema: Int,
        titulo: String,
        mensaje: String,
        esPrioritaria: Boolean,
        tipo: String
    ) {
        // A. Mostrar notificación en el celular (Barra de estado)
        mostrarNotificacionSistema(context, idSistema, titulo, mensaje, esPrioritaria)

        // B. Guardar en el historial de la App (Base de Datos)
        val nuevaNotificacion = NotificacionEntity(
            titulo = titulo,
            mensaje = mensaje,
            fecha = Date(),
            tipo = tipo,
            leida = false,
            iconoRef = "CreditCard" // Referencia para el icono en la lista
        )
        dao.insertNotificacion(nuevaNotificacion)
    }

    private fun mostrarNotificacionSistema(context: Context, id: Int, titulo: String, mensaje: String, esPrioritaria: Boolean) {
        // Verificar permisos (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }

        val channelId = "credito_alerts_v2"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Crédito",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de corte y pago"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(if (esPrioritaria) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        manager.notify(id, builder.build())
    }
}