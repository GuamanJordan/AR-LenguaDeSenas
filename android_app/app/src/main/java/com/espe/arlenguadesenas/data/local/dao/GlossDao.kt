package com.espe.arlenguadesenas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.espe.arlenguadesenas.data.local.entity.GlossEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossDao {
    @Query("SELECT * FROM glosses ORDER BY spanishTerm ASC")
    fun getAllGlosses(): Flow<List<GlossEntity>>

    @Query("SELECT * FROM glosses WHERE categoryId = :catId ORDER BY spanishTerm ASC")
    fun getGlossesByCategory(catId: Long): Flow<List<GlossEntity>>

    @Query("SELECT * FROM glosses WHERE code = :code LIMIT 1")
    suspend fun getGlossByCode(code: String): GlossEntity?

    @Query("SELECT * FROM glosses WHERE LOWER(spanishTerm) LIKE '%' || LOWER(:query) || '%' OR LOWER(lsecGloss) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchGlosses(query: String): List<GlossEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlosses(glosses: List<GlossEntity>)
}
