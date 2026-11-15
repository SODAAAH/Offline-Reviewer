package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

class XlsxDocumentParser : DocumentParser {
    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
        val chunks = mutableListOf<ExtractedDocumentChunk>()
        var chunkIndex = 0

        val sharedStrings = mutableListOf<String>()
        val sheetBytes = mutableListOf<ByteArray>()

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            val saxFactory = SAXParserFactory.newInstance()
            saxFactory.isNamespaceAware = false

            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val bytes = zip.readBytes()
                    val parser = saxFactory.newSAXParser()
                    var inText = false
                    val currentStr = StringBuilder()

                    val handler = object : DefaultHandler() {
                        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                            val tag = qName ?: localName ?: ""
                            if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                                inText = true
                            }
                        }

                        override fun characters(ch: CharArray, start: Int, length: Int) {
                            if (inText) {
                                currentStr.append(ch, start, length)
                            }
                        }

                        override fun endElement(uri: String?, localName: String?, qName: String?) {
                            val tag = qName ?: localName ?: ""
                            if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                                inText = false
                            } else if (tag.equals("si", ignoreCase = true) || tag.endsWith(":si")) {
                                sharedStrings.add(currentStr.toString().trim())
                                currentStr.setLength(0)
                            }
                        }
                    }

                    parser.parse(ByteArrayInputStream(bytes), handler)
                } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                    sheetBytes.add(zip.readBytes())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            for ((sheetIdx, sBytes) in sheetBytes.withIndex()) {
                val parser = saxFactory.newSAXParser()
                val rows = mutableListOf<List<String>>()
                var currentRow = mutableListOf<String>()
                var inValue = false
                var currentCellType: String? = null
                val currentVal = StringBuilder()

                val handler = object : DefaultHandler() {
                    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                        val tag = qName ?: localName ?: ""
                        if (tag.equals("row", ignoreCase = true) || tag.endsWith(":row")) {
                            currentRow = mutableListOf()
                        } else if (tag.equals("c", ignoreCase = true) || tag.endsWith(":c")) {
                            currentCellType = attributes?.getValue("t")
                            currentVal.setLength(0)
                        } else if (tag.equals("v", ignoreCase = true) || tag.endsWith(":v")) {
                            inValue = true
                        }
                    }

                    override fun characters(ch: CharArray, start: Int, length: Int) {
                        if (inValue) {
                            currentVal.append(ch, start, length)
                        }
                    }

                    override fun endElement(uri: String?, localName: String?, qName: String?) {
                        val tag = qName ?: localName ?: ""
                        if (tag.equals("v", ignoreCase = true) || tag.endsWith(":v")) {
                            inValue = false
                        } else if (tag.equals("c", ignoreCase = true) || tag.endsWith(":c")) {
                            val rawVal = currentVal.toString().trim()
                            val resolvedVal = if (currentCellType == "s") {
                                val idx = rawVal.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) {
                                    sharedStrings[idx]
                                } else {
                                    rawVal
                                }
                            } else {
                                rawVal
                            }
                            if (resolvedVal.isNotEmpty()) {
                                currentRow.add(resolvedVal)
                            }
                        } else if (tag.equals("row", ignoreCase = true) || tag.endsWith(":row")) {
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }

                parser.parse(ByteArrayInputStream(sBytes), handler)

                if (rows.isNotEmpty()) {
                    val sheetContent = StringBuilder()
                    for (row in rows) {
                        if (row.size >= 2) {
                            sheetContent.append(row[0]).append(": ").append(row.subList(1, row.size).joinToString(" | ")).append("\n")
                        } else {
                            sheetContent.append(row.joinToString(" ")).append("\n")
                        }
                    }
                    val content = DocumentTextCleaner.clean(sheetContent.toString())
                    if (content.isNotBlank()) {
                        chunks.add(
                            ExtractedDocumentChunk(
                                documentId = documentId,
                                documentName = documentName,
                                chunkIndex = chunkIndex++,
                                content = content,
                                sectionHeader = "Sheet ${sheetIdx + 1}",
                                characterCount = content.length
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return chunks
    }
}
