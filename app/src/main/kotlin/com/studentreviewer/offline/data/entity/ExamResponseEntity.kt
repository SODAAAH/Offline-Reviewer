package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_responses",
    foreignKeys = [
        ForeignKey(
            entity = ExamSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["examSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["examSessionId"]),
        Index(value = ["questionId"])
    ]
)
data class ExamResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examSessionId: Long,
    val questionId: Long,
    val questionStem: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val isMarkedForReview: Boolean = false,
    val timeSpentSeconds: Int = 0
)
