package com.studentreviewer.offline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.ReviewHistoryEntity
import com.studentreviewer.offline.data.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM flashcards WHERE reviewSetId = :reviewSetId ORDER BY id ASC")
    fun getFlashcardsForSet(reviewSetId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :nowTimestamp ORDER BY nextReviewAt ASC")
    fun getDueFlashcards(nowTimestamp: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :nowTimestamp ORDER BY nextReviewAt ASC")
    suspend fun getDueFlashcardsList(nowTimestamp: Long): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE nextReviewAt <= :nowTimestamp")
    fun getDueFlashcardCount(nowTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Update
    suspend fun updateStudySession(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC LIMIT 20")
    fun getRecentStudySessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewHistory(history: ReviewHistoryEntity): Long
}
