package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gastocheck.data.database.entity.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Query("SELECT * FROM metas")
    fun getMetas(): Flow<List<MetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaEntity)

    @Update
    suspend fun updateMeta(meta: MetaEntity)

    @Delete
    suspend fun deleteMeta(meta: MetaEntity)
}