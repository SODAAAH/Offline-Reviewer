package com.studentreviewer.offline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.studentreviewer.offline.data.entity.ExamResponseEntity
import com.studentreviewer.offline.data.entity.ExamSessionEntity
import kotlinx.coroutines.flow.Flow

data class ExamSessionWithResponses(
    @androidx.room.Embedded val session: ExamSessionEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "examSessionId"
    )
    val responses: List<ExamResponseEntity>
)

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_sessions ORDER BY startedAt DESC")
    fun getAllExamSessions(): Flow<List<ExamSessionEntity>>

    @Transaction
    @Query("SELECT * FROM exam_sessions WHERE id = :id")
    suspend fun getExamSessionWithResponses(id: Long): ExamSessionWithResponses?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSession(session: ExamSessionEntity): Long

    @Update
    suspend fun updateExamSession(session: ExamSessionEntity)

    @Delete
    suspend fun deleteExamSession(session: ExamSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<ExamResponseEntity>)

    @Update
    suspend fun updateResponse(response: ExamResponseEntity)

    @Query("SELECT AVG(scorePercentage) FROM exam_sessions WHERE isCompleted = 1")
    fun getAverageExamScore(): Flow<Float?>

    @Query("SELECT COUNT(*) FROM exam_sessions WHERE isCompleted = 1")
    fun getCompletedExamCount(): Flow<Int>
}
