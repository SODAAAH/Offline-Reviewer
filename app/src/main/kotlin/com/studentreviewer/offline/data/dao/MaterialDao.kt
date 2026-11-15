package com.studentreviewer.offline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.data.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getMaterialsBySubject(subjectId: Long): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getMaterialById(id: Long): MaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity): Long

    @Update
    suspend fun updateMaterial(material: MaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("SELECT * FROM document_chunks WHERE materialId = :materialId ORDER BY chunkIndex ASC")
    fun getChunksForMaterial(materialId: Long): Flow<List<DocumentChunkEntity>>

    @Query("SELECT * FROM document_chunks WHERE materialId = :materialId ORDER BY chunkIndex ASC")
    suspend fun getChunksListForMaterial(materialId: Long): List<DocumentChunkEntity>

    @Query("SELECT COUNT(*) FROM materials")
    fun getMaterialCount(): Flow<Int>
}
