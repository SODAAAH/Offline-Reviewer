package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

class PptxDocumentParser : DocumentParser {
    private val slideRegex = Regex("""ppt/slides/slide([0-9]+)\.xml""")

    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
        val chunks = mutableListOf<ExtractedDocumentChunk>()
        var chunkIndex = 0

        val slideEntries = mutableMapOf<Int, ByteArray>()

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                val match = slideRegex.find(entry.name)
                if (match != null) {
                    val slideNum = match.groupValues[1].toIntOrNull() ?: 1
                    slideEntries[slideNum] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            val sortedSlides = slideEntries.toSortedMap()
            val saxFactory = SAXParserFactory.newInstance()
            saxFactory.isNamespaceAware = false

            for ((slideNum, data) in sortedSlides) {
                val parser = saxFactory.newSAXParser()
                val slideText = StringBuilder()
                var currentPara = StringBuilder()
                var inText = false

                val handler = object : DefaultHandler() {
                    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                        val tag = qName ?: localName ?: ""
                        if (tag.equals("p", ignoreCase = true) || tag.endsWith(":p")) {
                            currentPara.setLength(0)
                        } else if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                            inText = true
                        }
                    }

                    override fun characters(ch: CharArray, start: Int, length: Int) {
                        if (inText) {
                            currentPara.append(ch, start, length)
                        }
                    }

                    override fun endElement(uri: String?, localName: String?, qName: String?) {
                        val tag = qName ?: localName ?: ""
                        if (tag.equals("t", ignoreCase = true) || tag.endsWith(":t")) {
                            inText = false
                        } else if (tag.equals("p", ignoreCase = true) || tag.endsWith(":p")) {
                            val pStr = currentPara.toString().trim()
                            if (pStr.isNotEmpty()) {
                                if (slideText.isNotEmpty()) {
                                    slideText.append("\n")
                                }
                                slideText.append(pStr)
                            }
                        }
                    }
                }

                parser.parse(ByteArrayInputStream(data), handler)

                val content = DocumentTextCleaner.clean(slideText.toString())
                if (content.isNotBlank()) {
                    chunks.add(
                        ExtractedDocumentChunk(
                            documentId = documentId,
                            documentName = documentName,
                            chunkIndex = chunkIndex++,
                            content = content,
                            slideNumber = slideNum,
                            sectionHeader = "Slide $slideNum",
                            characterCount = content.length
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return chunks
    }
}
