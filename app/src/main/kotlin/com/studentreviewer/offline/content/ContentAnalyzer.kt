package com.studentreviewer.offline.content

import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.document.util.DocumentTextCleaner

data class ExtractedDefinition(
    val term: String,
    val definition: String,
    val sourceSentence: String,
    val chunkIndex: Int,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionHeader: String? = null
)

data class ExtractedEnumeration(
    val category: String,
    val items: List<String>,
    val sourceSentence: String,
    val chunkIndex: Int,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionHeader: String? = null
)

data class ExtractedProcedure(
    val title: String,
    val steps: List<String>,
    val sourceSentence: String,
    val chunkIndex: Int,
    val pageNumber: Int? = null,
    val slideNumber: Int? = null,
    val sectionHeader: String? = null
)

data class AnalyzedContent(
    val definitions: List<ExtractedDefinition>,
    val enumerations: List<ExtractedEnumeration>,
    val procedures: List<ExtractedProcedure>,
    val allTerms: List<String>
)

class ContentAnalyzer {
    private val boldDefinitionRegex = Regex("""\*\*([^*]{2,60})\*\*\s*[:\-–]\s*(.{10,300})""")
    private val colonDefinitionRegex = Regex("""^([A-Z][a-zA-Z0-9\s\-]{1,40})\s*:\s*(.{15,300})$""", RegexOption.MULTILINE)
    private val verbalDefinitionRegex = Regex("""\b([A-Z][a-zA-Z0-9\s\-]{1,40})\s+(?:is defined as|refers to|means|is an?)\s+(.{15,250}\.?)""", RegexOption.IGNORE_CASE)
    private val enumerationColonRegex = Regex("""(?:The\s+)?([A-Za-z0-9\s\-]{3,50})\s+(?:are|include|consist of|is composed of):\s*([\s\S]+?)(?=\n\n|$)""", RegexOption.IGNORE_CASE)
    private val stepIndicatorRegex = Regex("""(?:Step\s*\d+|First|Second|Third|Next|Then|Finally|Phase\s*\d+)\s*[:\-–]?\s*(.+)""", RegexOption.IGNORE_CASE)

    fun analyze(chunks: List<DocumentChunkEntity>): AnalyzedContent {
        val definitions = mutableListOf<ExtractedDefinition>()
        val enumerations = mutableListOf<ExtractedEnumeration>()
        val procedures = mutableListOf<ExtractedProcedure>()
        val termSet = linkedSetOf<String>()

        for (chunk in chunks) {
            val text = chunk.content

            val boldMatches = boldDefinitionRegex.findAll(text)
            for (match in boldMatches) {
                val term = cleanTerm(match.groupValues[1])
                val def = match.groupValues[2].trim().trimEnd('.')
                if (isValidTermAndDef(term, def)) {
                    definitions.add(
                        ExtractedDefinition(
                            term = term,
                            definition = def,
                            sourceSentence = match.value,
                            chunkIndex = chunk.chunkIndex,
                            pageNumber = chunk.pageNumber,
                            slideNumber = chunk.slideNumber,
                            sectionHeader = chunk.sectionHeader
                        )
                    )
                    termSet.add(term)
                }
            }

            val colonMatches = colonDefinitionRegex.findAll(text)
            for (match in colonMatches) {
                val term = cleanTerm(match.groupValues[1])
                val def = match.groupValues[2].trim().trimEnd('.')
                if (isValidTermAndDef(term, def) && definitions.none { it.term.equals(term, ignoreCase = true) }) {
                    definitions.add(
                        ExtractedDefinition(
                            term = term,
                            definition = def,
                            sourceSentence = match.value,
                            chunkIndex = chunk.chunkIndex,
                            pageNumber = chunk.pageNumber,
                            slideNumber = chunk.slideNumber,
                            sectionHeader = chunk.sectionHeader
                        )
                    )
                    termSet.add(term)
                }
            }

            val sentences = DocumentTextCleaner.splitIntoSentences(text)
            for (sentence in sentences) {
                val verbalMatch = verbalDefinitionRegex.find(sentence)
                if (verbalMatch != null) {
                    val term = cleanTerm(verbalMatch.groupValues[1])
                    val def = verbalMatch.groupValues[2].trim().trimEnd('.')
                    if (isValidTermAndDef(term, def) && definitions.none { it.term.equals(term, ignoreCase = true) }) {
                        definitions.add(
                            ExtractedDefinition(
                                term = term,
                                definition = def,
                                sourceSentence = sentence,
                                chunkIndex = chunk.chunkIndex,
                                pageNumber = chunk.pageNumber,
                                slideNumber = chunk.slideNumber,
                                sectionHeader = chunk.sectionHeader
                            )
                        )
                        termSet.add(term)
                    }
                }
            }

            val enumMatches = enumerationColonRegex.findAll(text)
            for (match in enumMatches) {
                val category = match.groupValues[1].trim()
                val rawItems = match.groupValues[2]
                val items = extractListItems(rawItems)
                if (items.size in 2..8) {
                    enumerations.add(
                        ExtractedEnumeration(
                            category = category,
                            items = items,
                            sourceSentence = match.value,
                            chunkIndex = chunk.chunkIndex,
                            pageNumber = chunk.pageNumber,
                            slideNumber = chunk.slideNumber,
                            sectionHeader = chunk.sectionHeader
                        )
                    )
                    termSet.addAll(items)
                }
            }

            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            val procedureSteps = mutableListOf<String>()
            for (line in lines) {
                val stepMatch = stepIndicatorRegex.find(line)
                if (stepMatch != null) {
                    val stepContent = stepMatch.groupValues[1].trim()
                    if (stepContent.isNotEmpty()) {
                        procedureSteps.add(stepContent)
                    }
                }
            }
            if (procedureSteps.size in 3..7) {
                val title = chunk.sectionHeader ?: "Process"
                procedures.add(
                    ExtractedProcedure(
                        title = title,
                        steps = procedureSteps,
                        sourceSentence = procedureSteps.joinToString("; "),
                        chunkIndex = chunk.chunkIndex,
                        pageNumber = chunk.pageNumber,
                        slideNumber = chunk.slideNumber,
                        sectionHeader = chunk.sectionHeader
                    )
                )
            }
        }

        return AnalyzedContent(
            definitions = definitions,
            enumerations = enumerations,
            procedures = procedures,
            allTerms = termSet.toList()
        )
    }

    private fun cleanTerm(raw: String): String {
        return raw.trim()
            .removePrefix("A ")
            .removePrefix("An ")
            .removePrefix("The ")
            .removePrefix("a ")
            .removePrefix("an ")
            .removePrefix("the ")
            .trim()
    }

    private fun isValidTermAndDef(term: String, def: String): Boolean {
        if (term.length < 2 || term.length > 50) return false
        if (def.length < 10 || def.length > 300) return false
        if (term.count { it == ' ' } > 5) return false
        return true
    }

    private fun extractListItems(raw: String): List<String> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val bulletItems = lines.mapNotNull { line ->
            val cleaned = line.replace(Regex("""^(\d+[\.\)]|[-*•])\s*"""), "").trim()
            if (cleaned.length in 2..80) cleaned else null
        }
        if (bulletItems.size >= 2) return bulletItems

        return raw.split(Regex("""[,;]\s*"""))
            .map { it.trim().trimEnd('.') }
            .map { it.replace(Regex("""^(?:and\s+|or\s+)"""), "") }
            .filter { it.length in 2..80 }
    }
}
