package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class TxtDocumentParser : DocumentParser {
    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
        val bytes = inputStream.readBytes()
        val text = try {
            val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
            reader.readText()
        } catch (_: Exception) {
            val fallbackReader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1))
            fallbackReader.readText()
        }

        val paragraphs = DocumentTextCleaner.splitIntoParagraphs(text)
        val chunks = mutableListOf<ExtractedDocumentChunk>()
        var chunkIndex = 0
        var currentChunk = StringBuilder()

        for (para in paragraphs) {
            if (currentChunk.isNotEmpty() && currentChunk.length + para.length > 1500) {
                val content = currentChunk.toString().trim()
                if (content.isNotBlank()) {
                    chunks.add(
                        ExtractedDocumentChunk(
                            documentId = documentId,
                            documentName = documentName,
                            chunkIndex = chunkIndex++,
                            content = content,
                            characterCount = content.length
                        )
                    )
                }
                currentChunk = StringBuilder()
            }
            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(para)
        }

        if (currentChunk.isNotEmpty()) {
            val content = currentChunk.toString().trim()
            if (content.isNotBlank()) {
                chunks.add(
                    ExtractedDocumentChunk(
                        documentId = documentId,
                        documentName = documentName,
                        chunkIndex = chunkIndex,
                        content = content,
                        characterCount = content.length
                    )
                )
            }
        }

        return chunks
    }
}
