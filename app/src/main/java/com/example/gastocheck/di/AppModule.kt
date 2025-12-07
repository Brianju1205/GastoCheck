package com.example.gastocheck.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
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
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                // Se ejecuta solo cuando se crea la base de datos por primera vez
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertarCuentaPorDefecto(db)
                }

                // Se ejecuta cada vez que se abre (útil para asegurar que exista si se borró manualmente)
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Descomenta la siguiente línea si quieres forzar que se cree si no existe al abrir
                    // insertarCuentaPorDefecto(db)
                }

                private fun insertarCuentaPorDefecto(db: SupportSQLiteDatabase) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // --- ACTUALIZACIÓN CLAVE ---
                            // Agregamos el campo 'icono' en la inserción SQL para que coincida con la nueva tabla
                            db.execSQL(
                                "INSERT OR IGNORE INTO cuentas (id, nombre, tipo, saldoInicial, colorHex, icono, esArchivada) " +
                                        "VALUES (1, 'Efectivo', 'Efectivo', 0.0, '#00E676', 'Wallet', 0)"
                            )
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
    fun provideBalanceSnapshotDao(database: AppDatabase): BalanceSnapshotDao {
        return database.balanceSnapshotDao()
    }

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
}