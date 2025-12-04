package com.example.gastocheck.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.dao.VozPendienteDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.database.entity.VozPendienteEntity

@Database(
    entities = [
        TransaccionEntity::class,
        MetaEntity::class,
        VozPendienteEntity::class,
        CuentaEntity::class // Nueva tabla
    ],
    version = 4, // Subimos versión
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val transaccionDao: TransaccionDao
    abstract val metaDao: MetaDao
    abstract val vozPendienteDao: VozPendienteDao
    abstract val cuentaDao: CuentaDao

    companion object {
        // Migración manual para preservar datos
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Crear tabla Cuentas
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cuentas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `nombre` TEXT NOT NULL, 
                        `tipo` TEXT NOT NULL, 
                        `saldoInicial` REAL NOT NULL, 
                        `colorHex` TEXT NOT NULL DEFAULT '#00E676', 
                        `esArchivada` INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // 2. Insertar cuenta por defecto "Efectivo" con ID 1
                database.execSQL("""
                    INSERT INTO cuentas (id, nombre, tipo, saldoInicial, colorHex) 
                    VALUES (1, 'Efectivo', 'Efectivo', 0.0, '#00E676')
                """)

                // 3. Crear tabla temporal de transacciones con la nueva columna
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transacciones_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `monto` REAL NOT NULL, 
                        `categoria` TEXT NOT NULL, 
                        `descripcion` TEXT NOT NULL, 
                        `fecha` INTEGER NOT NULL, 
                        `esIngreso` INTEGER NOT NULL,
                        `cuentaId` INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(`cuentaId`) REFERENCES `cuentas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)

                // 4. Copiar datos viejos a la nueva tabla (asignando cuentaId = 1)
                database.execSQL("""
                    INSERT INTO transacciones_new (id, monto, categoria, descripcion, fecha, esIngreso, cuentaId)
                    SELECT id, monto, categoria, descripcion, fecha, esIngreso, 1 FROM transacciones
                """)

                // 5. Borrar tabla vieja y renombrar la nueva
                database.execSQL("DROP TABLE transacciones")
                database.execSQL("ALTER TABLE transacciones_new RENAME TO transacciones")

                // 6. Crear índice
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transacciones_cuentaId` ON `transacciones` (`cuentaId`)")
            }
        }
    }
}
