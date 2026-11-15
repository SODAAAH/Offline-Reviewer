package com.studentreviewer.offline.study.scheduler

import kotlin.math.roundToInt

enum class ReviewRating(val quality: Int) {
    AGAIN(0),
    HARD(2),
    GOOD(3),
    EASY(5)
}

data class Sm2Result(
    val repetitionNumber: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val nextReviewAt: Long,
    val isLapsed: Boolean
)

object Sm2SpacedRepetitionScheduler {
    private const val MIN_EASE_FACTOR = 1.3f
    private const val MILLIS_PER_DAY = 86_400_000L

    fun calculateNextReview(
        rating: ReviewRating,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Sm2Result {
        val q = rating.quality

        val newEaseFactor = (currentEaseFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))
            .coerceAtLeast(MIN_EASE_FACTOR)

        val isLapsed = q < 3
        val (newRepetitions, newIntervalDays) = if (isLapsed) {
            Pair(0, 1)
        } else {
            val reps = currentRepetitions + 1
            val interval = when (reps) {
                1 -> 1
                2 -> 6
                else -> (currentIntervalDays * newEaseFactor).roundToInt().coerceAtLeast(1)
            }
            Pair(reps, interval)
        }

        val nextReviewAt = currentTimeMillis + (newIntervalDays * MILLIS_PER_DAY)

        return Sm2Result(
            repetitionNumber = newRepetitions,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            nextReviewAt = nextReviewAt,
            isLapsed = isLapsed
        )
    }
}
