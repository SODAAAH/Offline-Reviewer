package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_history",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["flashcardId"]),
        Index(value = ["reviewedAt"])
    ]
)
data class ReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val flashcardId: Long,
    val qualityScore: Int,
    val scheduledIntervalDays: Int,
    val actualIntervalDays: Int,
    val newEaseFactor: Float,
    val reviewedAt: Long = System.currentTimeMillis()
)
