package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import java.util.Random

class SequencingQuestionGenerator {
    fun generateQuestions(analyzed: AnalyzedContent, random: Random = Random(42)): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()

        for (procedure in analyzed.procedures) {
            val steps = procedure.steps
            if (steps.size in 3..6) {
                val shuffledIndices = steps.indices.toList().shuffled(random)
                val letters = listOf("A", "B", "C", "D", "E", "F")

                val stemBuilder = StringBuilder("Arrange the steps for '${procedure.title}' in the correct chronological order:\n\n")
                shuffledIndices.forEachIndexed { i, origIndex ->
                    val letter = letters.getOrElse(i) { "${i + 1}" }
                    stemBuilder.append("$letter. ${steps[origIndex]}\n")
                }

                val correctOrder = steps.indices.map { origIndex ->
                    val displayIndex = shuffledIndices.indexOf(origIndex)
                    letters.getOrElse(displayIndex) { "${displayIndex + 1}" }
                }.joinToString(" -> ")

                list.add(
                    GeneratedQuestion(
                        stem = stemBuilder.toString().trim(),
                        questionType = "SEQUENCING",
                        correctAnswer = correctOrder,
                        options = shuffledIndices.map { steps[it] },
                        explanation = "The correct procedure order is: $correctOrder\n(${procedure.steps.joinToString(" -> ")})",
                        difficultyScore = 1.3f,
                        sourceExcerpt = procedure.sourceSentence,
                        sourceChunkIndex = procedure.chunkIndex,
                        pageNumber = procedure.pageNumber,
                        slideNumber = procedure.slideNumber,
                        sectionTitle = procedure.sectionHeader
                    )
                )
            }
        }

        return list
    }
}
