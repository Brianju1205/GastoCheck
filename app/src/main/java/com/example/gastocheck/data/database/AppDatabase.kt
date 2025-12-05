package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.dao.VozPendienteDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.database.entity.VozPendienteEntity
// Borramos el import de DateConverter porque Converters está en este mismo paquete

@Database(
    entities = [
        TransaccionEntity::class,
        MetaEntity::class,
        VozPendienteEntity::class,
        CuentaEntity::class
    ],
    version = 5,
    exportSchema = false
)
// AQUÍ ESTÁ EL CAMBIO: Usamos tu clase 'Converters'
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun metaDao(): MetaDao
    abstract fun vozPendienteDao(): VozPendienteDao
    abstract fun cuentaDao(): CuentaDao
}