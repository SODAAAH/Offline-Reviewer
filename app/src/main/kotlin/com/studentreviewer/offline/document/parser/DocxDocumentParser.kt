package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

class DocxDocumentParser : DocumentParser {
    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
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

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            val saxFactory = SAXParserFactory.newInstance()
            saxFactory.isNamespaceAware = false

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlBytes = zip.readBytes()
                    val parser = saxFactory.newSAXParser()

                    var inParagraph = false
                    var inText = false
                    var paragraphText = StringBuilder()
                    var isHeading = false

                    val handler = object : DefaultHandler() {
                        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                            val tag = qName ?: localName ?: ""
                            if (tag.equals("p", ignoreCase = true) || tag.endsWith(":p")) {
                                inParagraph = true
                                paragraphText.setLength(0)
                                isHeading = false
                            } else if (tag.equals("pStyle", ignoreCase = true) || tag.endsWith(":pStyle")) {
                                val styleVal = attributes?.getValue("w:val")
                                    ?: attributes?.getValue("val") ?: ""
                                if (styleVal.contains("Heading", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true)) {
                                    isHeading = true
                                }
                            } else if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                                inText = true
                            }
                        }

                        override fun characters(ch: CharArray, start: Int, length: Int) {
                            if (inText && inParagraph) {
                                paragraphText.append(ch, start, length)
                            }
                        }

                        override fun endElement(uri: String?, localName: String?, qName: String?) {
                            val tag = qName ?: localName ?: ""
                            if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                                inText = false
                            } else if (tag.equals("p", ignoreCase = true) || tag.endsWith(":p")) {
                                inParagraph = false
                                val pStr = paragraphText.toString().trim()
                                if (pStr.isNotEmpty()) {
                                    if (isHeading) {
                                        if (currentBuffer.isNotEmpty()) {
                                            flushBuffer()
                                        }
                                        currentSection = pStr
                                    } else {
                                        if (currentBuffer.isNotEmpty() && currentBuffer.length + pStr.length > 1500) {
                                            flushBuffer()
                                        }
                                        if (currentBuffer.isNotEmpty()) {
                                            currentBuffer.append("\n\n")
                                        }
                                        currentBuffer.append(pStr)
                                    }
                                }
                            }
                        }
                    }

                    parser.parse(ByteArrayInputStream(xmlBytes), handler)
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } catch (_: Exception) {}

        if (currentBuffer.isNotEmpty()) {
            flushBuffer()
        }

        return chunks
    }
}
