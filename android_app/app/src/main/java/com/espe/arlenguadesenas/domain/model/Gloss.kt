package com.espe.arlenguadesenas.domain.model

data class Gloss(
    val id: Long,
    val categoryId: Long,
    val code: String,
    val spanishTerm: String,
    val lsecGloss: String,
    val description: String,
    val isTwoHanded: Boolean,
    val isDynamic: Boolean,
    val animationClipName: String,
    val durationMs: Int
)
