package com.studentreviewer.offline.study

import com.studentreviewer.offline.study.scheduler.ReviewRating
import com.studentreviewer.offline.study.scheduler.Sm2SpacedRepetitionScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    @Test
    fun testFirstSuccessfulReview() {
        val now = 1700000000000L
        val result = Sm2SpacedRepetitionScheduler.calculateNextReview(
            rating = ReviewRating.GOOD,
            currentRepetitions = 0,
            currentIntervalDays = 1,
            currentEaseFactor = 2.5f,
            currentTimeMillis = now
        )

        assertEquals(1, result.repetitionNumber)
        assertEquals(1, result.intervalDays)
        assertFalse(result.isLapsed)
        assertEquals(now + 86_400_000L, result.nextReviewAt)
    }

    @Test
    fun testSecondSuccessfulReview() {
        val now = 1700000000000L
        val result = Sm2SpacedRepetitionScheduler.calculateNextReview(
            rating = ReviewRating.GOOD,
            currentRepetitions = 1,
            currentIntervalDays = 1,
            currentEaseFactor = 2.5f,
            currentTimeMillis = now
        )

        assertEquals(2, result.repetitionNumber)
        assertEquals(6, result.intervalDays)
        assertFalse(result.isLapsed)
        assertEquals(now + 6 * 86_400_000L, result.nextReviewAt)
    }

    @Test
    fun testSubsequentEasyReviewIntervalExpansion() {
        val now = 1700000000000L
        val result = Sm2SpacedRepetitionScheduler.calculateNextReview(
            rating = ReviewRating.EASY,
            currentRepetitions = 2,
            currentIntervalDays = 6,
            currentEaseFactor = 2.5f,
            currentTimeMillis = now
        )

        assertEquals(3, result.repetitionNumber)
        assertTrue(result.intervalDays >= 15)
        assertTrue(result.easeFactor > 2.5f)
        assertFalse(result.isLapsed)
    }

    @Test
    fun testLapseReset() {
        val now = 1700000000000L
        val result = Sm2SpacedRepetitionScheduler.calculateNextReview(
            rating = ReviewRating.AGAIN,
            currentRepetitions = 5,
            currentIntervalDays = 30,
            currentEaseFactor = 2.5f,
            currentTimeMillis = now
        )

        assertEquals(0, result.repetitionNumber)
        assertEquals(1, result.intervalDays)
        assertTrue(result.isLapsed)
        assertTrue(result.easeFactor < 2.5f)
    }

    @Test
    fun testEaseFactorFloor() {
        var ef = 1.35f
        for (i in 1..10) {
            val result = Sm2SpacedRepetitionScheduler.calculateNextReview(
                rating = ReviewRating.AGAIN,
                currentRepetitions = 0,
                currentIntervalDays = 1,
                currentEaseFactor = ef
            )
            ef = result.easeFactor
        }
        assertEquals(1.3f, ef, 0.001f)
    }
}
