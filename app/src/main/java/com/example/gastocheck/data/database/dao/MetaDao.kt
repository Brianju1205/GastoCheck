package com.example.gastocheck.data.database.dao

import androidx.room.*
import com.example.gastocheck.data.database.entity.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {

    // --- PARA LA PANTALLA DE METAS (Trae todo lo activo) ---
    @Query("SELECT * FROM metas WHERE esArchivada = 0 ORDER BY orden ASC")
    fun getMetasActivas(): Flow<List<MetaEntity>>

    // --- PARA EL HOME (REQUERIMIENTO 1) ---
    // Busca la meta más importante (orden ASC) que NO esté completada y NO esté archivada.
    // Así, si la primera de la lista ya se llenó, el Home mostrará la segunda.
    @Query("""
        SELECT * FROM metas 
        WHERE esArchivada = 0 
        AND montoAhorrado < montoObjetivo 
        ORDER BY orden ASC 
        LIMIT 1
    """)
    fun getMetaPrioritariaHome(): Flow<MetaEntity?>

    // --- PARA GUARDAR EL REORDENAMIENTO ---
    @Update
    suspend fun updateMetas(metas: List<MetaEntity>)

    // ... (Tus otros inserts/updates) ...
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaEntity): Long

    @Update
    suspend fun updateMeta(meta: MetaEntity)

    @Delete
    suspend fun deleteMeta(meta: MetaEntity)

    @Query("SELECT MAX(orden) FROM metas")
    suspend fun getMaxOrden(): Int?

    // --- (OPCIONAL) TU MÉTODO ANTERIOR ---
    // Puedes dejarlo si lo usas en alguna parte específica, pero 'updateMeta' ya hace esto.
    @Query("UPDATE metas SET montoAhorrado = :nuevoMonto WHERE id = :id")
    suspend fun updateMonto(id: Int, nuevoMonto: Double)
}