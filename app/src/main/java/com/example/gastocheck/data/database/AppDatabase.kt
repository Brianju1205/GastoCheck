package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity

@Database(
    entities = [TransaccionEntity::class, MetaEntity::class], // <--- Agregamos MetaEntity
    version = 2 // <--- IMPORTANTE: Subimos la versión a 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val transaccionDao: TransaccionDao
    abstract val metaDao: MetaDao // <--- Agregamos el acceso al DAO
}

