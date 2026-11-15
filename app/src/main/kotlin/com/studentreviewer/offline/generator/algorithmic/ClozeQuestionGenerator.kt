package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedQuestion

class ClozeQuestionGenerator {
    fun generateQuestions(analyzed: AnalyzedContent): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()

        for (def in analyzed.definitions) {
            val sentence = def.sourceSentence
            val escapedTerm = Regex.escape(def.term)
            val regex = Regex("""\b$escapedTerm\b""", RegexOption.IGNORE_CASE)

            val maskedSentence = if (regex.containsMatchIn(sentence)) {
                regex.replaceFirst(sentence, "[_____]")
            } else {
                "[_____] is defined as ${def.definition}."
            }

            list.add(
                GeneratedQuestion(
                    stem = "Fill in the blank: $maskedSentence",
                    questionType = "CLOZE",
                    correctAnswer = def.term,
                    options = emptyList(),
                    explanation = "The missing term is '${def.term}'. Full text: ${def.sourceSentence}",
                    difficultyScore = 1.1f,
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
}
