package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reviewSetId: Long?,
    val method: String,
    val itemsStudied: Int = 0,
    val itemsCorrect: Int = 0,
    val durationSeconds: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
