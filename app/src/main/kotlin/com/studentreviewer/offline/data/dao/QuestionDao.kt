package com.studentreviewer.offline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.QuestionOptionEntity
import kotlinx.coroutines.flow.Flow

data class QuestionWithOptions(
    @androidx.room.Embedded val question: QuestionEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val options: List<QuestionOptionEntity>
)

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun getAllQuestionsWithOptions(): Flow<List<QuestionWithOptions>>

    @Query("SELECT * FROM questions WHERE reviewSetId = :reviewSetId ORDER BY id ASC")
    fun getQuestionsForSet(reviewSetId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM question_options WHERE questionId = :questionId ORDER BY displayOrder ASC")
    suspend fun getOptionsForQuestion(questionId: Long): List<QuestionOptionEntity>

    @Transaction
    @Query("SELECT * FROM questions WHERE reviewSetId = :reviewSetId ORDER BY id ASC")
    fun getQuestionsWithOptionsForSet(reviewSetId: Long): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("SELECT * FROM questions WHERE reviewSetId = :reviewSetId ORDER BY id ASC")
    suspend fun getQuestionsListForSet(reviewSetId: Long): List<QuestionWithOptions>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomQuestionsWithOptions(count: Int): List<QuestionWithOptions>

    @Transaction
    @Query("SELECT * FROM questions WHERE reviewSetId IN (:reviewSetIds) ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomQuestionsForSets(reviewSetIds: List<Long>, count: Int): List<QuestionWithOptions>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<QuestionOptionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("SELECT COUNT(*) FROM questions")
    fun getTotalQuestionCount(): Flow<Int>

    @Query("UPDATE questions SET timesAnswered = timesAnswered + 1, timesCorrect = timesCorrect + :wasCorrect WHERE id = :id")
    suspend fun recordQuestionAttempt(id: Long, wasCorrect: Int)
}
