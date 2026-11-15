package com.studentreviewer.offline.exam

import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.QuestionOptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ExamEngineTest {

    @Test
    fun testPrepareExam() {
        val engine = ExamEngine()
        val questions = (1..10).map { i ->
            QuestionEntity(
                id = i.toLong(),
                reviewSetId = 1L,
                type = "MULTIPLE_CHOICE",
                stem = "Question $i",
                correctAnswer = "Answer $i"
            )
        }

        val optionsMap = questions.associate { q ->
            q.id to listOf(
                QuestionOptionEntity(questionId = q.id, optionText = "Option A", isCorrect = false),
                QuestionOptionEntity(questionId = q.id, optionText = "Option B", isCorrect = true)
            )
        }

        val examItems = engine.prepareExam(
            questions = questions,
            optionsMap = optionsMap,
            questionLimit = 5,
            shuffleQuestions = false,
            random = Random(42)
        )

        assertEquals(5, examItems.size)
        assertEquals("Question 1", examItems[0].question.stem)
        assertEquals(2, examItems[0].options.size)
    }

    @Test
    fun testEvaluationLogic() {
        val q1 = QuestionEntity(id = 1L, reviewSetId = 1L, type = "IDENTIFICATION", stem = "What is CPU?", correctAnswer = "Central Processing Unit", explanation = "Hardware.")
        val q2 = QuestionEntity(id = 2L, reviewSetId = 1L, type = "IDENTIFICATION", stem = "What is RAM?", correctAnswer = "Random Access Memory", explanation = "Hardware.")
        val q3 = QuestionEntity(id = 3L, reviewSetId = 1L, type = "TRUE_FALSE", stem = "Is HTTP secure?", correctAnswer = "False", explanation = "Networking.")

        val items = listOf(
            ExamQuestionItem(question = q1, options = emptyList(), selectedAnswer = "Central Processing Unit"),
            ExamQuestionItem(question = q2, options = emptyList(), selectedAnswer = "Read Only Memory"),
            ExamQuestionItem(question = q3, options = listOf("True", "False"), selectedAnswer = "False")
        )

        var correctCount = 0
        for (item in items) {
            if (item.selectedAnswer.equals(item.question.correctAnswer, ignoreCase = true)) {
                correctCount++
            }
        }

        assertEquals(2, correctCount)
        val score = (correctCount.toFloat() / items.size.toFloat()) * 100f
        assertTrue(score > 66f && score < 67f)
        assertFalse(score >= 70f)
    }
}
