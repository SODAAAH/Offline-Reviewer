package com.studentreviewer.offline

import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.MaterialEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.ReviewSetEntity
import com.studentreviewer.offline.data.entity.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppFoundationTest {

    @Test
    fun subjectEntity_hasValidDefaults() {
        val subject = SubjectEntity(name = "Operating Systems")
        assertEquals("Operating Systems", subject.name)
        assertEquals("#00506E", subject.colorHex)
    }

    @Test
    fun materialEntity_tracksMetadata() {
        val material = MaterialEntity(
            subjectId = 1L,
            title = "Lecture 1 Notes",
            fileExtension = "pdf",
            originalFileName = "lecture1.pdf",
            localFilePath = "/data/user/0/com.studentreviewer.offline/files/lecture1.pdf",
            fileSize = 1048576L,
            extractionStatus = "COMPLETED",
            totalPages = 12,
            totalChunks = 45
        )
        assertEquals("Lecture 1 Notes", material.title)
        assertEquals("pdf", material.fileExtension)
        assertEquals(12, material.totalPages)
        assertEquals(45, material.totalChunks)
    }

    @Test
    fun reviewSetEntity_initializesCorrectly() {
        val set = ReviewSetEntity(
            subjectId = 1L,
            title = "OS Midterm Deck",
            questionCount = 50
        )
        assertEquals("OS Midterm Deck", set.title)
        assertEquals(50, set.questionCount)
    }

    @Test
    fun questionEntity_recordsCorrectAnswerAndSource() {
        val question = QuestionEntity(
            reviewSetId = 1L,
            type = "MULTIPLE_CHOICE",
            stem = "What is thrashing in virtual memory?",
            correctAnswer = "Excessive paging activity leading to low CPU utilization",
            explanation = "Thrashing occurs when the system spends more time paging than executing.",
            sourceExcerpt = "Thrashing occurs when memory is overcommitted..."
        )
        assertEquals("MULTIPLE_CHOICE", question.type)
        assertNotNull(question.correctAnswer)
    }

    @Test
    fun flashcardEntity_initializesDefaultSm2Parameters() {
        val flashcard = FlashcardEntity(
            reviewSetId = 1L,
            frontText = "Deadlock",
            backText = "A set of blocked processes each holding a resource and waiting to acquire a resource held by another."
        )
        assertEquals(2.5f, flashcard.easeFactor, 0.001f)
        assertEquals(1, flashcard.intervalDays)
        assertEquals(0, flashcard.repetitionNumber)
    }

    @Test
    fun appDatabaseImpl_existsAndInheritsFromAppDatabase() {
        val implClass = Class.forName("com.studentreviewer.offline.data.database.AppDatabase_Impl")
        assertNotNull(implClass)
        assertEquals(com.studentreviewer.offline.data.database.AppDatabase::class.java, implClass.superclass)
        assertNotNull(implClass.getMethod("subjectDao"))
        assertNotNull(implClass.getMethod("materialDao"))
        assertNotNull(implClass.getMethod("reviewSetDao"))
        assertNotNull(implClass.getMethod("questionDao"))
        assertNotNull(implClass.getMethod("studyDao"))
        assertNotNull(implClass.getMethod("examDao"))
    }
}
