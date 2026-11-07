package com.espe.arlenguadesenas.domain.usecase

import com.espe.arlenguadesenas.data.repository.LsecRepository
import com.espe.arlenguadesenas.domain.model.Gloss

class TranslateTextToLsecUseCase(
    private val repository: LsecRepository
) {
    suspend operator fun invoke(inputText: String): List<Gloss> {
        val clean = inputText.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        // 1. Busqueda exacta
        val exactMatches = repository.searchGlosses(clean)
        if (exactMatches.isNotEmpty()) {
            return exactMatches
        }

        // 2. Busqueda por palabras clave individuales
        val words = clean.split(Regex("\\s+"))
        val results = mutableListOf<Gloss>()
        for (w in words) {
            val matches = repository.searchGlosses(w)
            if (matches.isNotEmpty()) {
                results.add(matches.first())
            }
        }
        return results
    }
}
