package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow

interface TransaccionRepository {
    fun getTransacciones(): Flow<List<TransaccionEntity>>

    suspend fun getTransaccionById(id: Int): TransaccionEntity?
    suspend fun insertTransaccion(transaccion: TransaccionEntity)
    suspend fun deleteTransaccion(transaccion: TransaccionEntity)
}