package com.studentreviewer.offline.document.util

object DocumentTextCleaner {
    private val dehyphenRegex = Regex("""(\b\w+)-\s*\r?\n\s*(\w+\b)""")
    private val multiSpaceRegex = Regex("""[ \t]+""")
    private val multiNewlineRegex = Regex("""(\r?\n\s*){3,}""")

    fun clean(text: String): String {
        if (text.isBlank()) return ""
        val dehyphenated = dehyphenRegex.replace(text, "$1$2")
        val normalizedSpaces = multiSpaceRegex.replace(dehyphenated, " ")
        val normalizedNewlines = multiNewlineRegex.replace(normalizedSpaces, "\n\n")
        return normalizedNewlines.trim()
    }

    fun splitIntoParagraphs(text: String): List<String> {
        return text.split(Regex("""\r?\n\s*\r?\n"""))
            .map { clean(it) }
            .filter { it.isNotBlank() }
    }

    fun splitIntoSentences(text: String): List<String> {
        val cleaned = clean(text)
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(Regex("""(?<=[.!?])\s+(?=[A-Z0-9])"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
