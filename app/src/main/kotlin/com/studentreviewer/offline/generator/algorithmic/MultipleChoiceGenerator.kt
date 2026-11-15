package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.AnalyzedContent
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import java.util.Random

class MultipleChoiceGenerator {
    fun generateQuestions(analyzed: AnalyzedContent, random: Random = Random(42)): List<GeneratedQuestion> {
        val list = mutableListOf<GeneratedQuestion>()
        val allTerms = analyzed.allTerms.toSet()

        for (def in analyzed.definitions) {
            val distractorPool = allTerms.filter { !it.equals(def.term, ignoreCase = true) }
            if (distractorPool.size >= 3) {
                val chosenDistractors = distractorPool.shuffled(random).take(3)
                val options = (chosenDistractors + def.term).shuffled(random)

                list.add(
                    GeneratedQuestion(
                        stem = "Which of the following matches the description: \"${def.definition}\"?",
                        questionType = "MULTIPLE_CHOICE",
                        correctAnswer = def.term,
                        options = options,
                        explanation = "The correct answer is ${def.term}. ${def.term} is defined as ${def.definition}.",
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

        for (enumItem in analyzed.enumerations) {
            if (enumItem.items.size >= 3) {
                val targetItem = enumItem.items.first()
                val nonMembers = allTerms.filter { term ->
                    enumItem.items.none { it.equals(term, ignoreCase = true) }
                }
                if (nonMembers.size >= 3) {
                    val chosenDistractors = nonMembers.shuffled(random).take(3)
                    val options = (chosenDistractors + targetItem).shuffled(random)

                    list.add(
                        GeneratedQuestion(
                            stem = "Which of the following is associated with ${enumItem.category}?",
                            questionType = "MULTIPLE_CHOICE",
                            correctAnswer = targetItem,
                            options = options,
                            explanation = "${targetItem} is one of the components of ${enumItem.category}: ${enumItem.items.joinToString(", ")}.",
                            difficultyScore = 1.2f,
                            sourceExcerpt = enumItem.sourceSentence,
                            sourceChunkIndex = enumItem.chunkIndex,
                            pageNumber = enumItem.pageNumber,
                            slideNumber = enumItem.slideNumber,
                            sectionTitle = enumItem.sectionHeader
                        )
                    )
                }
            }
        }

        return list
    }
}
