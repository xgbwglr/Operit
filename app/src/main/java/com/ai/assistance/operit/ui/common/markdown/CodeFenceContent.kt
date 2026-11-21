package com.ai.assistance.operit.ui.common.markdown

/**
 * Extracts the language and exact body of a Markdown fenced code block without
 * treating nested fences or code that resembles fences as delimiters.
 *
 * The native parser is responsible for identifying the block; this helper only
 * extracts its display content. It is independent of the AI model/provider.
 */
internal object CodeFenceContent {
    data class Parsed(val code: String, val language: String)

    fun parse(content: String): Parsed {
        val normalized = content.replace("\r\n", "\n")
        val lines = normalized.split('\n')
        val first = lines.firstOrNull() ?: return Parsed("", "")
        val openingLine = first.trimStart(' ')
        val indent = first.length - openingLine.length
        val marker = openingLine.firstOrNull() ?: return Parsed(normalized, "")
        if (indent > 3 || (marker != '`' && marker != '~')) {
            return Parsed(normalized, "")
        }
        val markerLength = openingLine.takeWhile { it == marker }.length
        if (markerLength < 3) return Parsed(normalized, "")
        val info = openingLine.substring(markerLength).trim()
        if (marker == '`' && '`' in info) return Parsed(normalized, "")
        val language = info.substringBefore(' ').substringBefore('\t').trim()

        // Streaming content may have an opening fence without a closing fence.
        val lastContentLineIndex = if (lines.lastOrNull() == "") lines.lastIndex - 1 else lines.lastIndex
        val closing = lines.getOrNull(lastContentLineIndex) ?: ""
        val closingBody = closing.trimStart(' ')
        val closingIndent = closing.length - closingBody.length
        val closingRun = closingBody.takeWhile { it == marker }.length
        val closes = lastContentLineIndex > 0 &&
            closingIndent <= 3 &&
            closingRun >= markerLength &&
            closingBody.substring(closingRun).isBlank()

        val body = if (closes) lines.subList(1, lastContentLineIndex) else lines.drop(1)
        return Parsed(body.joinToString("\n"), language)
    }
}
