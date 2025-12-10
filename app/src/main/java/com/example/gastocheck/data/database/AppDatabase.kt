package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// Imports de DAOs
import com.example.gastocheck.data.database.dao.AbonoDao
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.HistorialPagoDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.SuscripcionDao // <--- Correcto
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.dao.VozPendienteDao
// Imports de Entidades
import com.example.gastocheck.data.database.entity.AbonoEntity
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.SuscripcionEntity // <--- ESTE FALTABA
import com.example.gastocheck.data.database.entity.HistorialPagoEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.database.entity.VozPendienteEntity

@Database(
    entities = [
        TransaccionEntity::class,
        MetaEntity::class,
        VozPendienteEntity::class,
        CuentaEntity::class,
        BalanceSnapshotEntity::class,
        AbonoEntity::class,
        SuscripcionEntity::class,
        HistorialPagoEntity::class
    ],
    version = 15, // <--- Versión actualizada
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun metaDao(): MetaDao
    abstract fun vozPendienteDao(): VozPendienteDao
    abstract fun cuentaDao(): CuentaDao
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
    abstract fun abonoDao(): AbonoDao

    // Nuevo DAO para suscripciones
    abstract fun suscripcionDao(): SuscripcionDao
    abstract fun historialPagoDao(): HistorialPagoDao
}