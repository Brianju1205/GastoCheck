package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gastocheck.data.database.Converters
import com.example.gastocheck.data.database.dao.AbonoDao // <--- IMPORTANTE
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.dao.VozPendienteDao
import com.example.gastocheck.data.database.entity.AbonoEntity // <--- IMPORTANTE
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
        BalanceSnapshotEntity::class,
        AbonoEntity::class // <--- 1. AGREGADO: La nueva tabla para el historial
    ],
    version = 10, // <--- 2. ACTUALIZADO: Subimos la versión de 9 a 10
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun metaDao(): MetaDao
    abstract fun vozPendienteDao(): VozPendienteDao
    abstract fun cuentaDao(): CuentaDao
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao

    // --- 3. AGREGADO: El DAO para poder guardar y leer los abonos
    abstract fun abonoDao(): AbonoDao
}