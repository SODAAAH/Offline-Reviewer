package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "materials",
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
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val fileExtension: String,
    val originalFileName: String,
    val localFilePath: String,
    val fileSize: Long,
    val extractionStatus: String,
    val totalPages: Int = 0,
    val totalChunks: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
