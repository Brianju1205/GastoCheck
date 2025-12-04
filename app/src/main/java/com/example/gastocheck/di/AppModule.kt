package com.example.gastocheck.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.data.repository.TransaccionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gastos_database"
        )
            // 1. ELIMINAMOS .addMigrations(...) para que no intente migrar manualmente.

            // 2. ACTIVAMOS MIGRACIÓN DESTRUCTIVA
            // Si cambias la versión en AppDatabase.kt, borrará la BD y la creará de cero.
            .fallbackToDestructiveMigration()

            // 3. CALLBACK DE SEGURIDAD (CRÍTICO)
            // Esto asegura que siempre exista la cuenta "Efectivo" (ID 1)
            // para que no explote la app al guardar un gasto.
            .addCallback(object : RoomDatabase.Callback() {
                // Se ejecuta cuando se crea la base de datos desde cero (instalación nueva o tras migración destructiva)
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertarCuentaPorDefecto(db)
                }

                // Se ejecuta cada vez que se abre la app (por si acaso la cuenta se borró)
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    insertarCuentaPorDefecto(db)
                }

                private fun insertarCuentaPorDefecto(db: SupportSQLiteDatabase) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // SQL directo para mayor seguridad y velocidad
                            db.execSQL("INSERT OR IGNORE INTO cuentas (id, nombre, tipo, saldoInicial, colorHex, esArchivada) VALUES (1, 'Efectivo', 'Efectivo', 0.0, '#00E676', 0)")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideCuentaDao(database: AppDatabase): CuentaDao {
        return database.cuentaDao
    }

    @Provides
    @Singleton
    fun provideTransaccionDao(database: AppDatabase): TransaccionDao {
        return database.transaccionDao
    }

    @Provides
    @Singleton
    fun provideMetaDao(database: AppDatabase): MetaDao {
        return database.metaDao
    }

    // Repositorio actualizado para recibir los 2 DAOs y la DB
    @Provides
    @Singleton
    fun provideTransaccionRepository(
        transaccionDao: TransaccionDao,
        cuentaDao: CuentaDao,
        db: AppDatabase
    ): TransaccionRepository {
        return TransaccionRepositoryImpl(transaccionDao, cuentaDao, db)
    }

    @Provides
    @Singleton
    fun provideGeminiRepository(): GeminiRepository {
        return GeminiRepository()
    }
}