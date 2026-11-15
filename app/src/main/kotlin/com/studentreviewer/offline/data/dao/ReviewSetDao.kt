package com.studentreviewer.offline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentreviewer.offline.data.entity.ReviewSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewSetDao {
    @Query("SELECT * FROM review_sets ORDER BY createdAt DESC")
    fun getAllReviewSets(): Flow<List<ReviewSetEntity>>

    @Query("SELECT * FROM review_sets WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getReviewSetsBySubject(subjectId: Long): Flow<List<ReviewSetEntity>>

    @Query("SELECT * FROM review_sets WHERE id = :id")
    suspend fun getReviewSetById(id: Long): ReviewSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewSet(reviewSet: ReviewSetEntity): Long

    @Update
    suspend fun updateReviewSet(reviewSet: ReviewSetEntity)

    @Delete
    suspend fun deleteReviewSet(reviewSet: ReviewSetEntity)

    @Query("SELECT COUNT(*) FROM review_sets")
    fun getReviewSetCount(): Flow<Int>
}
