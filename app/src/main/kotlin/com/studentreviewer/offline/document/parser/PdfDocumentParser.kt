package com.studentreviewer.offline.document.parser

import com.studentreviewer.offline.document.model.ExtractedDocumentChunk
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class PdfDocumentParser : DocumentParser {
    private val textBlockRegex = Regex("""BT\b(.*?)ET\b""", RegexOption.DOT_MATCHES_ALL)
    private val parenthesesStringRegex = Regex("""\(((?:\\.|[^()\\])*)\)""")

    override fun parse(
        inputStream: InputStream,
        documentId: Long,
        documentName: String
    ): List<ExtractedDocumentChunk> {
        val chunks = mutableListOf<ExtractedDocumentChunk>()
        var chunkIndex = 0

        try {
            val bytes = inputStream.readBytes()
            val streams = extractStreams(bytes)

            var pageNumber = 1
            for (streamData in streams) {
                val streamText = String(streamData, StandardCharsets.ISO_8859_1)
                if (streamText.contains("BT") && streamText.contains("ET")) {
                    val extractedPageText = extractTextFromStream(streamText)
                    val cleaned = DocumentTextCleaner.clean(extractedPageText)
                    if (cleaned.isNotBlank()) {
                        chunks.add(
                            ExtractedDocumentChunk(
                                documentId = documentId,
                                documentName = documentName,
                                chunkIndex = chunkIndex++,
                                content = cleaned,
                                pageNumber = pageNumber++,
                                sectionHeader = "Page $pageNumber",
                                characterCount = cleaned.length
                            )
                        )
                    }
                }
            }

            if (chunks.isEmpty()) {
                val rawText = String(bytes, StandardCharsets.ISO_8859_1)
                val fallbackText = extractTextFromStream(rawText)
                val cleanedFallback = DocumentTextCleaner.clean(fallbackText)
                if (cleanedFallback.isNotBlank()) {
                    chunks.add(
                        ExtractedDocumentChunk(
                            documentId = documentId,
                            documentName = documentName,
                            chunkIndex = chunkIndex,
                            content = cleanedFallback,
                            pageNumber = 1,
                            characterCount = cleanedFallback.length
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return chunks
    }

    private fun extractStreams(bytes: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var index = 0
        val streamMarker = "stream".toByteArray(StandardCharsets.ISO_8859_1)
        val endStreamMarker = "endstream".toByteArray(StandardCharsets.ISO_8859_1)

        while (index < bytes.size) {
            val streamStart = indexOfBytes(bytes, streamMarker, index)
            if (streamStart == -1) break

            var dataStart = streamStart + streamMarker.size
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val streamEnd = indexOfBytes(bytes, endStreamMarker, dataStart)
            if (streamEnd == -1) break

            var dataEnd = streamEnd
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\n'.code.toByte()) dataEnd--
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\r'.code.toByte()) dataEnd--

            val streamLen = dataEnd - dataStart
            if (streamLen > 0) {
                val headerStart = (streamStart - 200).coerceAtLeast(0)
                val headerBytes = bytes.copyOfRange(headerStart, streamStart)
                val headerStr = String(headerBytes, StandardCharsets.ISO_8859_1)
                val isFlate = headerStr.contains("/FlateDecode")

                val streamBytes = bytes.copyOfRange(dataStart, dataEnd)
                val decompressed = if (isFlate) {
                    decompressFlate(streamBytes)
                } else {
                    streamBytes
                }
                if (decompressed != null) {
                    result.add(decompressed)
                }
            }

            index = streamEnd + endStreamMarker.size
        }

        return result
    }

    private fun decompressFlate(data: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(data)
            val outputStream = ByteArrayOutputStream(data.size * 2)
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            outputStream.toByteArray()
        } catch (_: Exception) {
            try {
                val iis = InflaterInputStream(ByteArrayInputStream(data))
                iis.readBytes()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty()) return 0
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun extractTextFromStream(streamText: String): String {
        val result = StringBuilder()
        val blocks = textBlockRegex.findAll(streamText)
        for (block in blocks) {
            val content = block.groupValues[1]
            val stringMatches = parenthesesStringRegex.findAll(content)
            for (sm in stringMatches) {
                val raw = sm.groupValues[1]
                val decoded = decodePdfString(raw)
                result.append(decoded).append(" ")
            }
        }
        return result.toString()
    }

    private fun decodePdfString(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '\\' && i + 1 < str.length) {
                when (val next = str[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000c'); i += 2 }
                    '(' -> { sb.append('('); i += 2 }
                    ')' -> { sb.append(')'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    in '0'..'7' -> {
                        var octal = "" + next
                        var k = i + 2
                        while (k < str.length && k < i + 4 && str[k] in '0'..'7') {
                            octal += str[k]
                            k++
                        }
                        val charCode = octal.toIntOrNull(8) ?: 32
                        sb.append(charCode.toChar())
                        i = k
                    }
                    else -> {
                        sb.append(next)
                        i += 2
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
