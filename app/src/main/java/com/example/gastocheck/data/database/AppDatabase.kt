package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gastocheck.data.database.Converters
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.dao.VozPendienteDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.database.entity.VozPendienteEntity

@Database(
    entities = [
        TransaccionEntity::class,
        MetaEntity::class,
        VozPendienteEntity::class,
        CuentaEntity::class,
        BalanceSnapshotEntity::class // <--- 1. Asegúrate de que esta entidad esté aquí
    ],
    version = 8, // <--- 2. Asegúrate de que la versión sea 6 (o superior a la que tenías)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun metaDao(): MetaDao
    abstract fun vozPendienteDao(): VozPendienteDao
    abstract fun cuentaDao(): CuentaDao

    // --- 3. ESTA ES LA LÍNEA QUE FALTA Y CAUSA EL ERROR EN APPMODULE ---
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
}