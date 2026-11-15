package com.studentreviewer.offline.analytics

import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryCalculatorTest {

    @Test
    fun testFlashcardMastery() {
        val masteredCard = FlashcardEntity(
            id = 1L,
            reviewSetId = 1L,
            frontText = "Front",
            backText = "Back",
            repetitionNumber = 4,
            easeFactor = 2.5f,
            lapsesCount = 0
        )
        val lapsedCard = FlashcardEntity(
            id = 2L,
            reviewSetId = 1L,
            frontText = "Front",
            backText = "Back",
            repetitionNumber = 4,
            easeFactor = 2.5f,
            lapsesCount = 1
        )
        val newCard = FlashcardEntity(
            id = 3L,
            reviewSetId = 1L,
            frontText = "Front",
            backText = "Back",
            repetitionNumber = 1,
            easeFactor = 2.5f,
            lapsesCount = 0
        )

        assertTrue(MasteryCalculator.calculateFlashcardMastery(masteredCard))
        assertFalse(MasteryCalculator.calculateFlashcardMastery(lapsedCard))
        assertFalse(MasteryCalculator.calculateFlashcardMastery(newCard))
    }

    @Test
    fun testOverallAnalytics() {
        val cards = listOf(
            FlashcardEntity(id = 1L, reviewSetId = 1L, frontText = "F1", backText = "B1", repetitionNumber = 4, easeFactor = 2.5f, lapsesCount = 0),
            FlashcardEntity(id = 2L, reviewSetId = 1L, frontText = "F2", backText = "B2", repetitionNumber = 3, easeFactor = 2.3f, lapsesCount = 0),
            FlashcardEntity(id = 3L, reviewSetId = 1L, frontText = "F3", backText = "B3", repetitionNumber = 1, easeFactor = 2.0f, lapsesCount = 3)
        )

        val questions = listOf(
            QuestionEntity(id = 10L, reviewSetId = 1L, type = "MCQ", stem = "Hard Question", correctAnswer = "A", timesAnswered = 4, timesCorrect = 1)
        )

        val analytics = MasteryCalculator.calculateOverallAnalytics(cards, questions)

        assertEquals(3, analytics.totalCards)
        assertEquals(2, analytics.masteredCards)
        assertTrue(analytics.weakAreas.any { it.questionId == 10L })
        assertTrue(analytics.weakAreas.any { it.questionId == 3L })
    }
}
