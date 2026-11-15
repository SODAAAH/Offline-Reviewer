package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import java.io.InputStream

interface DocumentParser {
    fun parse(inputStream: InputStream, documentId: Long = 0, documentName: String = ""): List<ExtractedDocumentChunk>
}
