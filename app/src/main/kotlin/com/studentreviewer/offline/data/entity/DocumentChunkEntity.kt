package com.studentreviewer.offline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_chunks",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["materialId"]),
        Index(value = ["pageNumber"])
    ]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val materialId: Long,
    val chunkIndex: Int,
    val content: String,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionHeader: String? = null,
    val characterCount: Int = 0
)
