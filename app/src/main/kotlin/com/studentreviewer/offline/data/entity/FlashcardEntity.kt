package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = ReviewSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewSetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["reviewSetId"]),
        Index(value = ["questionId"]),
        Index(value = ["nextReviewAt"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reviewSetId: Long,
    val questionId: Long? = null,
    val frontText: String,
    val backText: String,
    val explanation: String = "",
    val sourceReference: String = "",
    val repetitionNumber: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val lapsesCount: Int = 0
)
