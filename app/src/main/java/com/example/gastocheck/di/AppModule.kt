package com.example.gastocheck.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.AbonoDao
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.HistorialPagoDao
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.SuscripcionDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.gemini.GeminiRepository
import com.example.gastocheck.data.repository.SuscripcionRepository
import com.example.gastocheck.data.repository.SuscripcionRepositoryImpl
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
            .fallbackToDestructiveMigration() // Si cambias la BD, borra y crea de nuevo (útil en desarrollo)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertarCuentaPorDefecto(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Intentamos insertar al abrir por seguridad, si ya existe el IGNORE lo omite
                    insertarCuentaPorDefecto(db)
                }

                private fun insertarCuentaPorDefecto(db: SupportSQLiteDatabase) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // ACTUALIZACIÓN CRÍTICA:
                            // La tabla 'cuentas' ahora tiene campos de crédito.
                            // Debemos incluirlos en el INSERT inicial, estableciéndolos en 0 o false.
                            db.execSQL(
                                """
                                INSERT OR IGNORE INTO cuentas (
                                    id, 
                                    nombre, 
                                    tipo, 
                                    saldoInicial, 
                                    colorHex, 
                                    icono, 
                                    esArchivada,
                                    esCredito,
                                    limiteCredito,
                                    diaCorte,
                                    diaPago,
                                    tasaInteres,
                                    recordatoriosActivos
                                ) VALUES (
                                    1, 
                                    'Efectivo', 
                                    'Efectivo', 
                                    0.0, 
                                    '#00E676', 
                                    'Wallet', 
                                    0,
                                    0,   -- esCredito (false)
                                    0.0, -- limiteCredito
                                    0,   -- diaCorte
                                    0,   -- diaPago
                                    0.0, -- tasaInteres
                                    0    -- recordatoriosActivos (false)
                                )
                                """.trimIndent()
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
            .build()
    }

    // --- DAOs ---

    @Provides
    @Singleton
    fun provideCuentaDao(database: AppDatabase): CuentaDao {
        return database.cuentaDao()
    }

    @Provides
    @Singleton
    fun provideTransaccionDao(database: AppDatabase): TransaccionDao {
        return database.transaccionDao()
    }

    @Provides
    @Singleton
    fun provideMetaDao(database: AppDatabase): MetaDao {
        return database.metaDao()
    }

    @Provides
    @Singleton
    fun provideAbonoDao(database: AppDatabase): AbonoDao {
        return database.abonoDao()
    }

    @Provides
    @Singleton
    fun provideBalanceSnapshotDao(database: AppDatabase): BalanceSnapshotDao {
        return database.balanceSnapshotDao()
    }

    @Provides
    @Singleton
    fun provideSuscripcionDao(database: AppDatabase): SuscripcionDao {
        return database.suscripcionDao()
    }

    @Provides
    @Singleton
    fun provideHistorialPagoDao(database: AppDatabase): HistorialPagoDao {
        return database.historialPagoDao()
    }

    // --- REPOSITORIOS ---

    @Provides
    @Singleton
    fun provideTransaccionRepository(
        transaccionDao: TransaccionDao,
        cuentaDao: CuentaDao,
        balanceSnapshotDao: BalanceSnapshotDao,
        db: AppDatabase
    ): TransaccionRepository {
        return TransaccionRepositoryImpl(transaccionDao, cuentaDao, balanceSnapshotDao, db)
    }

    @Provides
    @Singleton
    fun provideGeminiRepository(): GeminiRepository {
        return GeminiRepository()
    }

    // CORRECCIÓN SOLICITADA:
    // Proveer SuscripcionRepository usando su implementación
    @Provides
    @Singleton
    fun provideSuscripcionRepository(
        suscripcionDao: SuscripcionDao // Hilt inyecta el DAO definido arriba
    ): SuscripcionRepository {
        return SuscripcionRepositoryImpl(suscripcionDao)
    }
}