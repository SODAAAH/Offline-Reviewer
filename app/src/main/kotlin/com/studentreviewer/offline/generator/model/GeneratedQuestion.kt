package com.studentreviewer.offline.generator.model

data class GeneratedQuestion(
    val stem: String,
    val questionType: String,
    val correctAnswer: String,
    val options: List<String> = emptyList(),
    val explanation: String,
    val difficultyScore: Float = 1.0f,
    val sourceExcerpt: String,
    val sourceChunkIndex: Int,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionTitle: String? = null
)

data class GeneratedFlashcard(
    val frontText: String,
    val backText: String,
    val hint: String? = null,
    val sourceExcerpt: String
)
