package com.studentreviewer.offline.study

import com.studentreviewer.offline.data.dao.StudyDao
import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.ReviewHistoryEntity
import com.studentreviewer.offline.data.entity.StudySessionEntity
import com.studentreviewer.offline.study.scheduler.ReviewRating
import com.studentreviewer.offline.study.scheduler.Sm2SpacedRepetitionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudySessionManager(
    private val studyDao: StudyDao
) {
    suspend fun startSession(reviewSetId: Long?, method: String): Long = withContext(Dispatchers.IO) {
        val session = StudySessionEntity(
            reviewSetId = reviewSetId,
            method = method,
            startedAt = System.currentTimeMillis()
        )
        studyDao.insertStudySession(session)
    }

    suspend fun recordReview(
        sessionId: Long,
        flashcard: FlashcardEntity,
        rating: ReviewRating
    ): FlashcardEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sm2 = Sm2SpacedRepetitionScheduler.calculateNextReview(
            rating = rating,
            currentRepetitions = flashcard.repetitionNumber,
            currentIntervalDays = flashcard.intervalDays,
            currentEaseFactor = flashcard.easeFactor,
            currentTimeMillis = now
        )

        val updatedFlashcard = flashcard.copy(
            repetitionNumber = sm2.repetitionNumber,
            intervalDays = sm2.intervalDays,
            easeFactor = sm2.easeFactor,
            nextReviewAt = sm2.nextReviewAt,
            lastReviewedAt = now,
            lapsesCount = if (sm2.isLapsed) flashcard.lapsesCount + 1 else flashcard.lapsesCount
        )

        studyDao.updateFlashcard(updatedFlashcard)

        val history = ReviewHistoryEntity(
            flashcardId = flashcard.id,
            qualityScore = rating.quality,
            scheduledIntervalDays = flashcard.intervalDays,
            actualIntervalDays = sm2.intervalDays,
            newEaseFactor = sm2.easeFactor,
            reviewedAt = now
        )
        studyDao.insertReviewHistory(history)

        updatedFlashcard
    }

    suspend fun finishSession(
        session: StudySessionEntity,
        totalItems: Int,
        correctItems: Int,
        durationSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val completedSession = session.copy(
            itemsStudied = totalItems,
            itemsCorrect = correctItems,
            durationSeconds = durationSeconds,
            completedAt = System.currentTimeMillis()
        )
        studyDao.updateStudySession(completedSession)
    }
}
