package com.studentreviewer.offline.document.parser

object DocumentParserFactory {
    fun getParser(fileName: String, mimeType: String? = null): DocumentParser {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            extension == "md" || extension == "markdown" || mimeType == "text/markdown" -> MarkdownDocumentParser()
            extension == "docx" || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocxDocumentParser()
            extension == "pptx" || mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> PptxDocumentParser()
            extension == "xlsx" || mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> XlsxDocumentParser()
            extension == "pdf" || mimeType == "application/pdf" -> PdfDocumentParser()
            else -> TxtDocumentParser()
        }
    }
}
