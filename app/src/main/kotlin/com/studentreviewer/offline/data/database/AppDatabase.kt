package com.studentreviewer.offline.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.studentreviewer.offline.data.dao.ExamDao
import com.studentreviewer.offline.data.dao.MaterialDao
import com.studentreviewer.offline.data.dao.QuestionDao
import com.studentreviewer.offline.data.dao.ReviewSetDao
import com.studentreviewer.offline.data.dao.StudyDao
import com.studentreviewer.offline.data.dao.SubjectDao
import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.data.entity.ExamResponseEntity
import com.studentreviewer.offline.data.entity.ExamSessionEntity
import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.MaterialEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.QuestionOptionEntity
import com.studentreviewer.offline.data.entity.ReviewHistoryEntity
import com.studentreviewer.offline.data.entity.ReviewSetEntity
import com.studentreviewer.offline.data.entity.StudySessionEntity
import com.studentreviewer.offline.data.entity.SubjectEntity

@Database(
    entities = [
        SubjectEntity::class,
        MaterialEntity::class,
        DocumentChunkEntity::class,
        ReviewSetEntity::class,
        QuestionEntity::class,
        QuestionOptionEntity::class,
        FlashcardEntity::class,
        StudySessionEntity::class,
        ReviewHistoryEntity::class,
        ExamSessionEntity::class,
        ExamResponseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun materialDao(): MaterialDao
    abstract fun reviewSetDao(): ReviewSetDao
    abstract fun questionDao(): QuestionDao
    abstract fun studyDao(): StudyDao
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_reviewer.db"
                )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
