package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import java.util.Random

class MatchingQuestionGenerator {
    fun generateQuestions(analyzed: AnalyzedContent, random: Random = Random(42)): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()
        val defs = analyzed.definitions
        if (defs.size < 4) return emptyList()

        val batch = defs.shuffled(random).take(5)
        val terms = batch.map { it.term }
        val definitions = batch.map { it.definition }
        val shuffledDefinitions = definitions.shuffled(random)

        val stemBuilder = StringBuilder("Match each term with its corresponding description:\n\nTerms:\n")
        terms.forEachIndexed { i, term ->
            stemBuilder.append("${i + 1}. $term\n")
        }
        stemBuilder.append("\nDescriptions:\n")
        val letters = listOf("A", "B", "C", "D", "E")
        shuffledDefinitions.forEachIndexed { i, def ->
            val letter = letters.getOrElse(i) { "${i + 1}" }
            stemBuilder.append("$letter. $def\n")
        }

        val answerMapping = terms.mapIndexed { i, term ->
            val origDef = batch.first { it.term == term }.definition
            val defIndex = shuffledDefinitions.indexOf(origDef)
            val letter = letters.getOrElse(defIndex) { "${defIndex + 1}" }
            "${i + 1}-$letter"
        }.joinToString(", ")

        list.add(
            GeneratedQuestion(
                stem = stemBuilder.toString().trim(),
                questionType = "MATCHING",
                correctAnswer = answerMapping,
                options = letters.take(batch.size),
                explanation = "Correct pairings: $answerMapping",
                difficultyScore = 1.4f,
                sourceExcerpt = batch.joinToString("\n") { "${it.term}: ${it.definition}" },
                sourceChunkIndex = batch.first().chunkIndex,
                pageNumber = batch.first().pageNumber,
                slideNumber = batch.first().slideNumber,
                sectionTitle = batch.first().sectionHeader
            )
        )

        return list
    }
}
