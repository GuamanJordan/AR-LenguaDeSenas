package com.espe.arlenguadesenas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glosses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["spanishTerm"]),
        Index(value = ["categoryId"])
    ]
)
data class GlossEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val code: String,
    val spanishTerm: String,
    val lsecGloss: String,
    val description: String?,
    val handShape: String?,
    val location: String?,
    val movement: String?,
    val orientation: String?,
    val facialExpression: String?,
    val isTwoHanded: Boolean = false,
    val isDynamic: Boolean = false,
    val animationClipName: String = "idle",
    val durationMs: Int = 1500
)
