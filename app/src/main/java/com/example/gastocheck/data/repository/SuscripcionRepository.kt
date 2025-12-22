package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.entity.SuscripcionEntity
import kotlinx.coroutines.flow.Flow

interface SuscripcionRepository {
    fun getSuscripciones(): Flow<List<SuscripcionEntity>>
    suspend fun getSuscripcionById(id: Int): SuscripcionEntity?
    suspend fun insertSuscripcion(suscripcion: SuscripcionEntity): Long
    suspend fun updateSuscripcion(suscripcion: SuscripcionEntity)
    suspend fun deleteSuscripcion(suscripcion: SuscripcionEntity)
}