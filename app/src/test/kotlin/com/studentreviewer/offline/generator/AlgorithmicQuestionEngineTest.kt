package com.studentreviewer.offline.generator

import com.studentreviewer.offline.content.ContentAnalyzer
import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.generator.algorithmic.AlgorithmicQuestionEngine
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import com.studentreviewer.offline.generator.validation.QuestionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmicQuestionEngineTest {

    @Test
    fun testContentAnalyzer() {
        val analyzer = ContentAnalyzer()
        val text = """
            **Cache**: Fast temporary storage used to speed up data access.
            RAM: Primary volatile memory used by running programs.
            A Compiler is defined as a program that translates source code into machine executable binary.
            The CPU components are: Arithmetic Logic Unit, Control Unit, Registers.
            First, fetch the instruction from memory.
            Second, decode the instruction.
            Third, execute the operation.
        """.trimIndent()

        val chunk = DocumentChunkEntity(
            id = 1L,
            materialId = 1L,
            chunkIndex = 0,
            content = text,
            sectionHeader = "Computer Architecture"
        )

        val analyzed = analyzer.analyze(listOf(chunk))
        assertTrue(analyzed.definitions.any { it.term.equals("Cache", ignoreCase = true) })
        assertTrue(analyzed.definitions.any { it.term.equals("RAM", ignoreCase = true) })
        assertTrue(analyzed.definitions.any { it.term.equals("Compiler", ignoreCase = true) })
        assertTrue(analyzed.enumerations.any { it.category.contains("CPU components", ignoreCase = true) })
        assertTrue(analyzed.procedures.isNotEmpty())
    }

    @Test
    fun testQuestionValidator() {
        val validator = QuestionValidator()
        val q1 = GeneratedQuestion(
            stem = "What term describes fast temporary storage?",
            questionType = "IDENTIFICATION",
            correctAnswer = "Cache",
            explanation = "Cache is fast storage",
            sourceExcerpt = "Cache is fast temporary storage",
            sourceChunkIndex = 0
        )
        val qDuplicate = GeneratedQuestion(
            stem = "What term describes fast temporary storage?",
            questionType = "IDENTIFICATION",
            correctAnswer = "Cache",
            explanation = "Cache is fast storage",
            sourceExcerpt = "Cache is fast temporary storage",
            sourceChunkIndex = 0
        )
        val qDistinct = GeneratedQuestion(
            stem = "Which device translates source code to binary?",
            questionType = "IDENTIFICATION",
            correctAnswer = "Compiler",
            explanation = "A compiler translates code",
            sourceExcerpt = "A compiler translates code",
            sourceChunkIndex = 0
        )

        assertFalse(validator.isValid(qDuplicate, listOf(q1)))
        assertTrue(validator.isValid(qDistinct, listOf(q1)))
    }

    @Test
    fun testAlgorithmicQuestionEngine() {
        val engine = AlgorithmicQuestionEngine()
        val text = """
            **Stack**: A linear data structure that follows the Last In First Out principle.
            **Queue**: A linear data structure that follows the First In First Out principle.
            **Array**: A collection of elements identified by index or key stored contiguously.
            **Tree**: A hierarchical data structure consisting of nodes connected by edges.
            **Graph**: A non-linear data structure consisting of vertices and edges.
            
            The types of trees are: Binary Tree, AVL Tree, Red-Black Tree, B-Tree.
            
            Step 1: Traverse the left subtree.
            Step 2: Visit the root node.
            Step 3: Traverse the right subtree.
        """.trimIndent()

        val chunk = DocumentChunkEntity(
            id = 1L,
            materialId = 1L,
            chunkIndex = 0,
            content = text,
            sectionHeader = "Data Structures"
        )

        val result = engine.generate(listOf(chunk))

        assertTrue(result.questions.isNotEmpty())
        assertTrue(result.flashcards.isNotEmpty())

        val mcqs = result.questions.filter { it.questionType == "MULTIPLE_CHOICE" }
        assertTrue(mcqs.isNotEmpty())
        for (mcq in mcqs) {
            assertEquals(4, mcq.options.size)
            assertTrue(mcq.options.contains(mcq.correctAnswer))
            assertEquals(4, mcq.options.toSet().size)
        }

        val trueFalse = result.questions.filter { it.questionType == "TRUE_FALSE" }
        assertTrue(trueFalse.isNotEmpty())

        val identifications = result.questions.filter { it.questionType == "IDENTIFICATION" }
        assertTrue(identifications.isNotEmpty())

        val cloze = result.questions.filter { it.questionType == "CLOZE" }
        assertTrue(cloze.isNotEmpty())
        for (c in cloze) {
            assertTrue(c.stem.contains("[_____]"))
        }

        val sequencing = result.questions.filter { it.questionType == "SEQUENCING" }
        assertTrue(sequencing.isNotEmpty())
    }
}
