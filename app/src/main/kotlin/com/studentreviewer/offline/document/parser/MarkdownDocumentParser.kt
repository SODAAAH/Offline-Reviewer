package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class MarkdownDocumentParser : DocumentParser {
    private val headingRegex = Regex("""^(#{1,6})\s+(.+)$""")

    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val chunks = mutableListOf<ExtractedDocumentChunk>()
        var chunkIndex = 0
        var currentSection: String? = null
        var currentBuffer = StringBuilder()

        fun flushBuffer() {
            val content = DocumentTextCleaner.clean(currentBuffer.toString())
            if (content.isNotBlank()) {
                chunks.add(
                    ExtractedDocumentChunk(
                        documentId = documentId,
                        documentName = documentName,
                        chunkIndex = chunkIndex++,
                        content = content,
                        sectionHeader = currentSection,
                        characterCount = content.length
                    )
                )
            }
            currentBuffer = StringBuilder()
        }

        reader.forEachLine { rawLine ->
            val line = rawLine.trimEnd()
            val headingMatch = headingRegex.find(line.trim())
            if (headingMatch != null) {
                if (currentBuffer.isNotEmpty()) {
                    flushBuffer()
                }
                currentSection = headingMatch.groupValues[2].trim()
            } else {
                if (currentBuffer.isNotEmpty() && currentBuffer.length + line.length > 1500 && line.isBlank()) {
                    flushBuffer()
                } else {
                    if (currentBuffer.isNotEmpty()) {
                        currentBuffer.append("\n")
                    }
                    currentBuffer.append(line)
                }
            }
        }

        if (currentBuffer.isNotEmpty()) {
            flushBuffer()
        }

        return chunks
    }
}
