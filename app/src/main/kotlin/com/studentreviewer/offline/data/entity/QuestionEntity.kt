package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ReviewSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewSetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["reviewSetId"]),
        Index(value = ["materialId"]),
        Index(value = ["type"])
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reviewSetId: Long,
    val materialId: Long? = null,
    val chunkId: Long? = null,
    val type: String,
    val stem: String,
    val correctAnswer: String,
    val explanation: String = "",
    val sourceExcerpt: String = "",
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val difficulty: Float = 0.5f,
    val generationEngine: String = "ALGORITHMIC",
    val validationStatus: String = "READY",
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
