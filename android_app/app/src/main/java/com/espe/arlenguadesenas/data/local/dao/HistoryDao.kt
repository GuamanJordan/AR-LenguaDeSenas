package com.espe.arlenguadesenas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.espe.arlenguadesenas.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insertInteraction(interaction: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
