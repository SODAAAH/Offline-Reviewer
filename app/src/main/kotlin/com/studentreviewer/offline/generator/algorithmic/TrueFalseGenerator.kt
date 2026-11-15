package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import java.util.Random

class TrueFalseGenerator {
    fun generateQuestions(analyzed: AnalyzedContent, random: Random = Random(42)): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()
        val allTerms = analyzed.allTerms

        for ((index, def) in analyzed.definitions.withIndex()) {
            if (index % 2 == 0) {
                list.add(
                    GeneratedQuestion(
                        stem = "True or False: ${def.term} refers to ${def.definition}.",
                        questionType = "TRUE_FALSE",
                        correctAnswer = "True",
                        options = listOf("True", "False"),
                        explanation = "True. According to the reference material, ${def.term} is defined as ${def.definition}.",
                        difficultyScore = 0.8f,
                        sourceExcerpt = def.sourceSentence,
                        sourceChunkIndex = def.chunkIndex,
                        pageNumber = def.pageNumber,
                        slideNumber = def.slideNumber,
                        sectionTitle = def.sectionHeader
                    )
                )
            } else {
                val alternateTerms = allTerms.filter { !it.equals(def.term, ignoreCase = true) }
                if (alternateTerms.isNotEmpty()) {
                    val mutatedTerm = alternateTerms.shuffled(random).first()
                    list.add(
                        GeneratedQuestion(
                            stem = "True or False: $mutatedTerm refers to ${def.definition}.",
                            questionType = "TRUE_FALSE",
                            correctAnswer = "False",
                            options = listOf("True", "False"),
                            explanation = "False. It is ${def.term} that refers to ${def.definition}, not $mutatedTerm.",
                            difficultyScore = 1.0f,
                            sourceExcerpt = def.sourceSentence,
                            sourceChunkIndex = def.chunkIndex,
                            pageNumber = def.pageNumber,
                            slideNumber = def.slideNumber,
                            sectionTitle = def.sectionHeader
                        )
                    )
                }
            }
        }

        return list
    }
}
