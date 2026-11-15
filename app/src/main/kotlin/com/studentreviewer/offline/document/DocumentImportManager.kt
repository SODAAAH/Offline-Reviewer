package com.studentreviewer.offline.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.studentreviewer.offline.data.dao.MaterialDao
import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.data.entity.MaterialEntity
import com.studentreviewer.offline.document.parser.DocumentParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentImportManager(
    private val context: Context,
    private val materialDao: MaterialDao
) {
    suspend fun importDocument(uri: Uri, subjectId: Long?): Result<MaterialEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryFileName(uri) ?: "imported_document_${System.currentTimeMillis()}"
            val mimeType = context.contentResolver.getType(uri)
            val extension = fileName.substringAfterLast('.', "").lowercase()

            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val localFile = File(docsDir, "${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read input stream from URI"))

            val initialMaterial = MaterialEntity(
                subjectId = subjectId,
                title = fileName.substringBeforeLast('.'),
                fileExtension = extension,
                originalFileName = fileName,
                localFilePath = localFile.absolutePath,
                fileSize = localFile.length(),
                extractionStatus = "PROCESSING",
                createdAt = System.currentTimeMillis()
            )

            val insertedId = materialDao.insertMaterial(initialMaterial)

            val parser = DocumentParserFactory.getParser(fileName, mimeType)
            val chunks = localFile.inputStream().use { stream ->
                parser.parse(stream, insertedId, fileName)
            }

            val maxPage = chunks.mapNotNull { it.pageNumber ?: it.slideNumber }.maxOrNull() ?: 1

            val entities = chunks.map { chunk ->
                DocumentChunkEntity(
                    materialId = insertedId,
                    chunkIndex = chunk.chunkIndex,
                    content = chunk.content,
                    pageNumber = chunk.pageNumber,
                    slideNumber = chunk.slideNumber,
                    sectionHeader = chunk.sectionHeader,
                    characterCount = chunk.characterCount
                )
            }

            materialDao.insertChunks(entities)

            val completedMaterial = initialMaterial.copy(
                id = insertedId,
                extractionStatus = "COMPLETED",
                totalPages = maxPage,
                totalChunks = entities.size
            )
            materialDao.updateMaterial(completedMaterial)

            Result.success(completedMaterial)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}
