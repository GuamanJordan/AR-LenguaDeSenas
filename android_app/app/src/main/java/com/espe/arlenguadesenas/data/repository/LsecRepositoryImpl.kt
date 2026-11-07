package com.espe.arlenguadesenas.data.repository

import com.espe.arlenguadesenas.data.local.dao.GlossDao
import com.espe.arlenguadesenas.data.local.dao.HistoryDao
import com.espe.arlenguadesenas.data.local.entity.GlossEntity
import com.espe.arlenguadesenas.data.local.entity.HistoryEntity
import com.espe.arlenguadesenas.domain.model.Gloss
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LsecRepositoryImpl(
    private val glossDao: GlossDao,
    private val historyDao: HistoryDao
) : LsecRepository {

    override fun getAllGlosses(): Flow<List<Gloss>> {
        return glossDao.getAllGlosses().map { list -> list.map { it.toDomain() } }
    }

    override fun getGlossesByCategory(categoryId: Long): Flow<List<Gloss>> {
        return glossDao.getGlossesByCategory(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override fun getGlossByCode(code: String): Gloss? {
        return kotlinx.coroutines.runBlocking {
            glossDao.getGlossByCode(code)?.toDomain()
        }
    }

    override suspend fun searchGlosses(query: String): List<Gloss> {
        return glossDao.searchGlosses(query).map { it.toDomain() }
    }

    override suspend fun saveInteraction(
        direction: String,
        input: String,
        output: String,
        confidence: Float
    ) {
        historyDao.insertInteraction(
            HistoryEntity(
                direction = direction,
                inputContent = input,
                outputContent = output,
                confidenceScore = confidence
            )
        )
    }

    private fun GlossEntity.toDomain(): Gloss {
        return Gloss(
            id = id,
            categoryId = categoryId,
            code = code,
            spanishTerm = spanishTerm,
            lsecGloss = lsecGloss,
            description = description ?: "",
            isTwoHanded = isTwoHanded,
            isDynamic = isDynamic,
            animationClipName = animationClipName,
            durationMs = durationMs
        )
    }
}
