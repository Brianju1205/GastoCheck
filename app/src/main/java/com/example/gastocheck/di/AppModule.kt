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
            // 1. IMPORTANTE: Permite a Room destruir la BD vieja si cambia la versión
            // Esto soluciona el crash cuando cambias columnas o tablas
            .fallbackToDestructiveMigration()

            // 2. CALLBACK: Inserta datos iniciales obligatorios
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertarCuentaPorDefecto(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Verificamos al abrir por si acaso se borró manualmente
                    insertarCuentaPorDefecto(db)
                }

                private fun insertarCuentaPorDefecto(db: SupportSQLiteDatabase) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Insertamos la cuenta "Efectivo" (ID 1) si no existe.
                            // Esto evita errores de Foreign Key al guardar transacciones.
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
        return database.cuentaDao() // CORREGIDO: Se agregaron paréntesis ()
    }

    @Provides
    @Singleton
    fun provideTransaccionDao(database: AppDatabase): TransaccionDao {
        return database.transaccionDao() // CORREGIDO: Se agregaron paréntesis ()
    }

    @Provides
    @Singleton
    fun provideMetaDao(database: AppDatabase): MetaDao {
        return database.metaDao() // CORREGIDO: Se agregaron paréntesis ()
    }

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