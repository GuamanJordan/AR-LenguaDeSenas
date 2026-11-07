package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val direction: String, // 'SIGN_TO_TEXT' o 'TEXT_TO_SIGN'
    val inputContent: String,
    val outputContent: String,
    val confidenceScore: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
