package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val description: String?,
    val iconName: String?,
    val displayOrder: Int = 0
)
