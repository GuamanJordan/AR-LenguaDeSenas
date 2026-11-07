package com.espe.arlenguadesenas.data.repository

import com.espe.arlenguadesenas.domain.model.Gloss
import kotlinx.coroutines.flow.Flow

interface LsecRepository {
    fun getAllGlosses(): Flow<List<Gloss>>
    fun getGlossesByCategory(categoryId: Long): Flow<List<Gloss>>
    suspend fun getGlossByCode(code: String): Gloss?
    suspend fun searchGlosses(query: String): List<Gloss>
    suspend fun saveInteraction(direction: String, input: String, output: String, confidence: Float)
}
