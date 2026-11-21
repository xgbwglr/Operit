package com.ai.assistance.operit.util.markdown

/**
 * Lossless, provider-independent compatibility pass for widely used Markdown
 * variants that the native streaming Markdown splitter does not yet recognize.
 *
 * This runs only for completed messages. Live messages retain the existing native
 * parser so characters continue to appear without a line-buffering delay.
 *
 * The original ChatMessage.content is never modified.
 */
internal object UniversalMarkdownCompatibility {
    private val separatorCell = Regex(":?-{3,}:?")

    private data class Fence(
        val marker: Char,
        val length: Int,
        val indent: String,
        val suffix: String,
    )

    fun normalize(markdown: String): String {
        if ('|' !in markdown && "~~~" !in markdown) return markdown

        val lines = markdown.split('\n')
        val result = ArrayList<String>(lines.size)
        var position = 0
        var currentFence: Fence? = null
        var mathClose: String? = null

        while (position < lines.size) {
            val original = lines[position]
            val line = original.removeSuffix("\r")
            val stripped = line.trim()

            if (currentFence != null) {
                result.add(original)
                if (isFenceClosing(line, currentFence)) currentFence = null
                position++
                continue
            }

            if (mathClose != null) {
                result.add(original)
                if (stripped.contains(mathClose)) mathClose = null
                position++
                continue
            }

            val opener = parseFence(line)
            if (opener != null) {
                if (opener.marker == '~') {
                    // Converting a tilde fence to an equal-length backtick fence
                    // is unsafe when the code itself contains backticks. Choose
                    // a longer fence that cannot occur inside the code instead.
                    var closingIndex = position + 1
                    while (closingIndex < lines.size &&
                        !isFenceClosing(lines[closingIndex].removeSuffix("\r"), opener)
                    ) {
                        closingIndex++
                    }

                    if (closingIndex < lines.size) {
                        val codeLines = lines.subList(position + 1, closingIndex)
                        val largestRun = codeLines.maxOfOrNull {
                            largestBacktickRun(it)
                        } ?: 0
                        val backticks = "\u0060".repeat(maxOf(3, largestRun + 1))
                        val openingEnding = if (original.endsWith("\r")) "\r" else ""
                        val closingRaw = lines[closingIndex]
                        val closingLine = closingRaw.removeSuffix("\r")
                        val closingIndent = closingLine.takeWhile { it == ' ' }
                        val closingEnding = if (closingRaw.endsWith("\r")) "\r" else ""

                        result.add(opener.indent + backticks + opener.suffix + openingEnding)
                        result.addAll(codeLines)
                        result.add(closingIndent + backticks + closingEnding)
                        position = closingIndex + 1
                        continue
                    }
                }

                // Keep incomplete fences unchanged (especially useful when the
                // model has not finished its response yet).
                currentFence = opener
                result.add(original)
                position++
                continue
            }

            val mathDelimiter = when {
                stripped.startsWith("$$") -> "$$"
                stripped.startsWith("\\[") -> "\\]"
                else -> null
            }
            if (mathDelimiter != null) {
                val closingAlreadyPresent = when (mathDelimiter) {
                    "$$" -> stripped.indexOf("$$", 2) >= 0
                    else -> stripped.contains("\\]")
                }
                if (!closingAlreadyPresent) mathClose = mathDelimiter
                result.add(original)
                position++
                continue
            }

            if (position + 1 < lines.size &&
                indentationWidth(line) <= 3 &&
                indentationWidth(lines[position + 1]) <= 3
            ) {
                val headerCells = splitCells(line)
                val delimiterCells = splitCells(lines[position + 1])
                if (headerCells != null &&
                    delimiterCells != null &&
                    headerCells.size == delimiterCells.size &&
                    delimiterCells.all { separatorCell.matches(it.trim()) }
                ) {
                    // GFM accepts 'a | b' with no leading/trailing pipes. The
                    // native table splitter only recognizes pipe-led rows.
                    var rowIndex = position
                    while (rowIndex < lines.size) {
                        val cells = splitCells(lines[rowIndex])
                        if (cells == null ||
                            cells.size != headerCells.size ||
                            indentationWidth(lines[rowIndex]) > 3
                        ) break
                        result.add(withBoundaryPipes(lines[rowIndex]))
                        rowIndex++
                    }
                    position = rowIndex
                    continue
                }
            }

            result.add(original)
            position++
        }

        return result.joinToString("\n")
    }

    private fun indentationWidth(line: String): Int =
        line.takeWhile { it == ' ' }.length

    private fun parseFence(line: String): Fence? {
        val indent = line.takeWhile { it == ' ' }
        if (indent.length > 3) return null
        val body = line.substring(indent.length)
        val marker = body.firstOrNull() ?: return null
        if (marker != '~' && marker != '\u0060') return null
        val count = body.takeWhile { it == marker }.length
        if (count < 3) return null
        val suffix = body.substring(count)
        // CommonMark does not allow backticks in a backtick fence's info string.
        if (marker == '\u0060' && '\u0060' in suffix) return null
        return Fence(marker, count, indent, suffix)
    }

    private fun isFenceClosing(line: String, fence: Fence): Boolean {
        val body = line.trimStart(' ')
        val indent = line.length - body.length
        if (indent > 3 || body.firstOrNull() != fence.marker) return false
        val count = body.takeWhile { it == fence.marker }.length
        return count >= fence.length && body.substring(count).isBlank()
    }

    private fun largestBacktickRun(line: String): Int {
        var maximum = 0
        var current = 0
        for (c in line) {
            if (c == '\u0060') {
                current++
                maximum = maxOf(maximum, current)
            } else {
                current = 0
            }
        }
        return maximum
    }

    private fun splitCells(line: String): List<String>? {
        var content = line.trim()
        if (content.isEmpty() || !content.contains('|')) return null
        if (content.startsWith("|")) content = content.drop(1)
        if (content.endsWith("|") && !isEscaped(content, content.lastIndex)) {
            content = content.dropLast(1)
        }

        val cells = ArrayList<String>()
        var start = 0
        for (i in content.indices) {
            if (content[i] == '|' && !isEscaped(content, i)) {
                cells.add(content.substring(start, i))
                start = i + 1
            }
        }
        cells.add(content.substring(start))
        return cells.takeIf { it.size >= 2 }
    }

    private fun isEscaped(s: String, index: Int): Boolean {
        var slashCount = 0
        var pos = index - 1
        while (pos >= 0 && s[pos] == '\\') {
            slashCount++
            pos--
        }
        return slashCount % 2 != 0
    }

    private fun withBoundaryPipes(raw: String): String {
        val ending = if (raw.endsWith("\r")) "\r" else ""
        val line = raw.removeSuffix("\r")
        val indent = line.takeWhile { it == ' ' }
        var body = line.substring(indent.length).trimEnd()
        if (!body.startsWith("|")) body = "| " + body
        if (!body.endsWith("|") || isEscaped(body, body.lastIndex)) body += " |"
        return indent + body + ending
    }
}
