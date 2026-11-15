package com.studentreviewer.offline.document.model

data class ExtractedDocumentChunk(
    val documentId: Long = 0,
    val documentName: String = "",
    val chunkIndex: Int,
    val content: String,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionHeader: String? = null,
    val characterCount: Int = content.length
)
