package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_sets",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class ReviewSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val description: String = "",
    val questionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long? = null
)
