package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gastocheck.data.database.dao.*
import com.example.gastocheck.data.database.entity.*

@Database(
    entities = [
        TransaccionEntity::class,
        MetaEntity::class,
        VozPendienteEntity::class,
        CuentaEntity::class,
        BalanceSnapshotEntity::class,
        AbonoEntity::class,
        SuscripcionEntity::class,
        HistorialPagoEntity::class,
        NotificacionEntity::class // <--- ¡AGREGA ESTA LÍNEA!
    ],
    version = 19, // <--- Sube la versión a 19 por si acaso (ya que hiciste cambios fallidos en la 18)
    exportSchema = false
)
@TypeConverters(Converters::class) // Asegúrate de que este converter exista, o usa DateConverter::class si es lo que usabas antes
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun metaDao(): MetaDao
    abstract fun vozPendienteDao(): VozPendienteDao
    abstract fun cuentaDao(): CuentaDao
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
    abstract fun abonoDao(): AbonoDao
    abstract fun suscripcionDao(): SuscripcionDao
    abstract fun historialPagoDao(): HistorialPagoDao
    abstract fun notificacionDao(): NotificacionDao
}