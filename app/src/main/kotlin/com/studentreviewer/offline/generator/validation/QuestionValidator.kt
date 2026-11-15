package com.studentreviewer.offline.generator.validation

import com.studentreviewer.offline.generator.model.GeneratedQuestion

class QuestionValidator {
    private val stopWords = setOf(
        "a", "an", "the", "is", "are", "was", "were", "what", "which", "who", "where",
        "when", "why", "how", "of", "in", "to", "for", "with", "on", "at", "from",
        "by", "described", "refers", "defined", "following", "term", "statement"
    )

    fun calculateJaccardSimilarity(text1: String, text2: String): Double {
        val tokens1 = tokenize(text1)
        val tokens2 = tokenize(text2)
        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() && it !in stopWords }
            .toSet()
    }

    fun isValid(candidate: GeneratedQuestion, existingQuestions: List<GeneratedQuestion>): Boolean {
        if (candidate.stem.isBlank() || candidate.correctAnswer.isBlank()) return false
        if (candidate.sourceExcerpt.isBlank()) return false

        for (existing in existingQuestions) {
            val similarity = calculateJaccardSimilarity(candidate.stem, existing.stem)
            if (similarity > 0.75) {
                return false
            }
            if (candidate.correctAnswer.equals(existing.correctAnswer, ignoreCase = true) &&
                candidate.questionType == existing.questionType &&
                similarity > 0.50
            ) {
                return false
            }
        }

        if (candidate.questionType == "MULTIPLE_CHOICE") {
            if (candidate.options.size != 4) return false
            val uniqueOptions = candidate.options.map { it.trim().lowercase() }.toSet()
            if (uniqueOptions.size != 4) return false
            if (candidate.options.none { it.equals(candidate.correctAnswer, ignoreCase = true) }) return false
        }

        return true
    }
}
