package com.example.gastocheck.di

import android.content.Context
import androidx.room.Room
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.MetaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.repository.TransaccionRepository
import com.example.gastocheck.data.repository.TransaccionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.gastocheck.data.gemini.GeminiRepository

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
            .fallbackToDestructiveMigration() // <--- ¡Importante para evitar crashes al cambiar versión!
            .build()
    }

    @Provides
    @Singleton
    fun provideTransaccionDao(database: AppDatabase): TransaccionDao {
        return database.transaccionDao
    }

    // --- NUEVO ---
    @Provides
    @Singleton
    fun provideMetaDao(database: AppDatabase): MetaDao {
        return database.metaDao
    }

    @Provides
    @Singleton
    fun provideTransaccionRepository(dao: TransaccionDao): TransaccionRepository {
        return TransaccionRepositoryImpl(dao)
    }
    @Provides
    @Singleton
    fun provideGeminiRepository(): GeminiRepository {
        return GeminiRepository()
    }
}
