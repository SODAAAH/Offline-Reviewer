package com.studentreviewer.offline.backup

import com.studentreviewer.offline.data.database.AppDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val database: AppDatabase
) {
    suspend fun exportBackup(outputStream: OutputStream): Result<String> = withContext(Dispatchers.IO) {
        try {
            val zos = ZipOutputStream(outputStream)

            val subjects = database.subjectDao().getAllSubjects().first()
            val materials = database.materialDao().getAllMaterials().first()
            val reviewSets = database.reviewSetDao().getAllReviewSets().first()
            val examSessions = database.examDao().getAllExamSessions().first()

            val allChunks = mutableListOf<DocumentChunkEntity>()
            for (m in materials) {
                allChunks.addAll(database.materialDao().getChunksListForMaterial(m.id))
            }

            val allQuestions = mutableListOf<QuestionEntity>()
            val allOptions = mutableListOf<QuestionOptionEntity>()
            val allCards = mutableListOf<FlashcardEntity>()
            for (rs in reviewSets) {
                val qList = database.questionDao().getQuestionsForSet(rs.id).first()
                allQuestions.addAll(qList)
                for (q in qList) {
                    allOptions.addAll(database.questionDao().getOptionsForQuestion(q.id))
                }
                allCards.addAll(database.studyDao().getFlashcardsForSet(rs.id).first())
            }

            val manifestObj = JSONObject()
            manifestObj.put("version", 1)
            manifestObj.put("exportTime", System.currentTimeMillis())
            manifestObj.put("subjectCount", subjects.size)
            manifestObj.put("materialCount", materials.size)
            manifestObj.put("reviewSetCount", reviewSets.size)

            writeZipEntry(zos, "manifest.json", manifestObj.toString())

            val subjectsJson = JSONArray()
            for (s in subjects) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("description", s.description)
                obj.put("colorHex", s.colorHex)
                subjectsJson.put(obj)
            }
            writeZipEntry(zos, "subjects.json", subjectsJson.toString())

            val materialsJson = JSONArray()
            for (m in materials) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("subjectId", m.subjectId ?: JSONObject.NULL)
                obj.put("title", m.title)
                obj.put("fileExtension", m.fileExtension)
                obj.put("originalFileName", m.originalFileName)
                obj.put("localFilePath", m.localFilePath)
                obj.put("fileSize", m.fileSize)
                obj.put("extractionStatus", m.extractionStatus)
                obj.put("totalPages", m.totalPages)
                obj.put("totalChunks", m.totalChunks)
                materialsJson.put(obj)
            }
            writeZipEntry(zos, "materials.json", materialsJson.toString())

            val reviewSetsJson = JSONArray()
            for (r in reviewSets) {
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("subjectId", r.subjectId ?: JSONObject.NULL)
                obj.put("title", r.title)
                obj.put("description", r.description)
                obj.put("questionCount", r.questionCount)
                reviewSetsJson.put(obj)
            }
            writeZipEntry(zos, "review_sets.json", reviewSetsJson.toString())

            val flashcardsJson = JSONArray()
            for (f in allCards) {
                val obj = JSONObject()
                obj.put("id", f.id)
                obj.put("reviewSetId", f.reviewSetId)
                obj.put("frontText", f.frontText)
                obj.put("backText", f.backText)
                obj.put("repetitionNumber", f.repetitionNumber)
                obj.put("intervalDays", f.intervalDays)
                obj.put("easeFactor", f.easeFactor.toDouble())
                obj.put("nextReviewAt", f.nextReviewAt)
                obj.put("lapsesCount", f.lapsesCount)
                flashcardsJson.put(obj)
            }
            writeZipEntry(zos, "flashcards.json", flashcardsJson.toString())

            zos.close()
            Result.success("Export completed successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            val entries = mutableMapOf<String, String>()

            while (entry != null) {
                val content = zis.readBytes().toString(StandardCharsets.UTF_8)
                entries[entry.name] = content
                zis.closeEntry()
                entry = zis.nextEntry
            }

            if (!entries.containsKey("manifest.json")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup: manifest.json missing"))
            }

            var importedCount = 0

            database.runInTransaction {
                val subjectsJsonStr = entries["subjects.json"]
                if (subjectsJsonStr != null) {
                    val arr = JSONArray(subjectsJsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = SubjectEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            colorHex = obj.optString("colorHex", "#2563EB")
                        )
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT OR REPLACE INTO subjects (id, name, description, colorHex, createdAt) VALUES (?, ?, ?, ?, ?)",
                            arrayOf<Any?>(entity.id, entity.name, entity.description, entity.colorHex, System.currentTimeMillis())
                        )
                        importedCount++
                    }
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, data: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(data.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }
}
