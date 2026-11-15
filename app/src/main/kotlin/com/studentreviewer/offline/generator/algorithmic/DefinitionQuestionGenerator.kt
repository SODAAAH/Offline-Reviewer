package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedFlashcard
import com.studentreviewer.offline.generator.model.GeneratedQuestion

class DefinitionQuestionGenerator {
    fun generateQuestions(analyzed: AnalyzedContent): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()
        for (def in analyzed.definitions) {
            list.add(
                GeneratedQuestion(
                    stem = "What term is described by: \"${def.definition}\"?",
                    questionType = "IDENTIFICATION",
                    correctAnswer = def.term,
                    options = emptyList(),
                    explanation = "${def.term} is defined as ${def.definition}.",
                    difficultyScore = 1.0f,
                    sourceExcerpt = def.sourceSentence,
                    sourceChunkIndex = def.chunkIndex,
                    pageNumber = def.pageNumber,
                    slideNumber = def.slideNumber,
                    sectionTitle = def.sectionHeader
                )
            )
        }
        return list
    }

    fun generateFlashcards(analyzed: AnalyzedContent): List<GeneratedFlashcard> {
        val list = mutableListOf<GeneratedFlashcard>()
        for (def in analyzed.definitions) {
            list.add(
                GeneratedFlashcard(
                    frontText = "What is ${def.term}?",
                    backText = def.definition,
                    hint = def.sectionHeader,
                    sourceExcerpt = def.sourceSentence
                )
            )
            list.add(
                GeneratedFlashcard(
                    frontText = "What term is defined as: \"${def.definition}\"?",
                    backText = def.term,
                    hint = def.sectionHeader,
                    sourceExcerpt = def.sourceSentence
                )
            )
        }
        return list
    }
}
