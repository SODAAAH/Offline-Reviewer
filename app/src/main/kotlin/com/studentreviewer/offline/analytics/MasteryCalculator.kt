package com.studentreviewer.offline.analytics

import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.QuestionEntity

enum class MasteryLevel {
    BEGINNER,
    DEVELOPING,
    PROFICIENT,
    MASTERED
}

data class SubjectMastery(
    val subjectId: Long?,
    val subjectName: String,
    val totalItems: Int,
    val masteredItems: Int,
    val masteryPercentage: Float,
    val level: MasteryLevel
)

data class WeakAreaItem(
    val questionId: Long,
    val stem: String,
    val timesAnswered: Int,
    val timesCorrect: Int,
    val accuracy: Float,
    val topic: String
)

data class OverallAnalytics(
    val totalCards: Int,
    val masteredCards: Int,
    val dueTodayCount: Int,
    val overallRetentionRate: Float,
    val overallMasteryLevel: MasteryLevel,
    val weakAreas: List<WeakAreaItem>
)

object MasteryCalculator {
    fun calculateFlashcardMastery(card: FlashcardEntity): Boolean {
        return card.repetitionNumber >= 3 && card.easeFactor >= 2.2f && card.lapsesCount == 0
    }

    fun calculateOverallAnalytics(
        cards: List<FlashcardEntity>,
        questions: List<QuestionEntity>,
        nowTimestamp: Long = System.currentTimeMillis()
    ): OverallAnalytics {
        if (cards.isEmpty() && questions.isEmpty()) {
            return OverallAnalytics(
                totalCards = 0,
                masteredCards = 0,
                dueTodayCount = 0,
                overallRetentionRate = 0f,
                overallMasteryLevel = MasteryLevel.BEGINNER,
                weakAreas = emptyList()
            )
        }

        val totalCards = cards.size
        val masteredCards = cards.count { calculateFlashcardMastery(it) }
        val dueTodayCount = cards.count { it.nextReviewAt <= nowTimestamp }

        val totalLapses = cards.sumOf { it.lapsesCount }
        val totalReps = cards.sumOf { it.repetitionNumber }
        val retentionRate = if (totalReps > 0) {
            ((totalReps - totalLapses).coerceAtLeast(0).toFloat() / totalReps.toFloat()) * 100f
        } else {
            0f
        }

        val masteryPercentage = if (totalCards > 0) {
            (masteredCards.toFloat() / totalCards.toFloat()) * 100f
        } else {
            0f
        }

        val level = when {
            masteryPercentage >= 85f && retentionRate >= 85f -> MasteryLevel.MASTERED
            masteryPercentage >= 65f && retentionRate >= 70f -> MasteryLevel.PROFICIENT
            masteryPercentage >= 35f -> MasteryLevel.DEVELOPING
            else -> MasteryLevel.BEGINNER
        }

        val weakQuestions = questions.filter { q ->
            (q.timesAnswered >= 2 && (q.timesCorrect.toFloat() / q.timesAnswered.toFloat()) < 0.5f)
        }.map { q ->
            val acc = if (q.timesAnswered > 0) (q.timesCorrect.toFloat() / q.timesAnswered.toFloat()) * 100f else 0f
            WeakAreaItem(
                questionId = q.id,
                stem = q.stem,
                timesAnswered = q.timesAnswered,
                timesCorrect = q.timesCorrect,
                accuracy = acc,
                topic = q.explanation.substringBefore('.').takeIf { it.isNotBlank() } ?: "General"
            )
        }

        val lapsedCards = cards.filter { it.lapsesCount >= 2 }.map { c ->
            val totalAttempts = c.repetitionNumber + c.lapsesCount
            val correctAttempts = c.repetitionNumber
            val acc = if (totalAttempts > 0) (correctAttempts.toFloat() / totalAttempts.toFloat()) * 100f else 0f
            WeakAreaItem(
                questionId = c.id,
                stem = c.frontText,
                timesAnswered = totalAttempts,
                timesCorrect = correctAttempts,
                accuracy = acc,
                topic = c.explanation.takeIf { it.isNotBlank() } ?: "Flashcard"
            )
        }

        val combinedWeak = (weakQuestions + lapsedCards).sortedBy { it.accuracy }.take(10)

        return OverallAnalytics(
            totalCards = totalCards,
            masteredCards = masteredCards,
            dueTodayCount = dueTodayCount,
            overallRetentionRate = retentionRate,
            overallMasteryLevel = level,
            weakAreas = combinedWeak
        )
    }
}
