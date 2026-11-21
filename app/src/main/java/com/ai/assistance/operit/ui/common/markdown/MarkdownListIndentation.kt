package com.ai.assistance.operit.ui.common.markdown

/** A conservative indentation depth shared by bullet, ordered and task lists. */
internal object MarkdownListIndentation {
    fun level(text: String): Int {
        var columns = 0
        for (ch in text) {
            when (ch) {
                ' ' -> columns++
                '\t' -> columns += 4
                else -> break
            }
        }
        // Models commonly emit two or four leading spaces per nesting level.
        // Clamp the visual indentation so narrow phone screens stay usable.
        return (columns / 2).coerceIn(0, 4)
    }

    fun stripLeadingIndent(text: String): String = text.trimStart(' ', '\t')
}
