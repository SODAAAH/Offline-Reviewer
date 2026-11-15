package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_sessions")
data class ExamSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val durationMinutes: Int,
    val scorePercentage: Float = 0f,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val isCompleted: Boolean = false
)
