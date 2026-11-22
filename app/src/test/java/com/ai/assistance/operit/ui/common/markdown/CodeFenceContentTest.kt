package com.ai.assistance.operit.ui.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeFenceContentTest {
    @Test
    fun closedBacktickFenceKeepsBodyAndLanguage() {
        val result = CodeFenceContent.parse("```kotlin\nprintln(1)\n```")
        assertEquals("kotlin", result.language)
        assertEquals("println(1)", result.code)
    }

    @Test
    fun longerFenceAllowsShorterFenceInside() {
        val input = "````markdown\n```\nhello\n````"
        val result = CodeFenceContent.parse(input)
        assertEquals("markdown", result.language)
        assertEquals("```\nhello", result.code)
    }

    @Test
    fun incompleteFenceStillShowsCode() {
        val result = CodeFenceContent.parse("```python\nprint(42)")
        assertEquals("python", result.language)
        assertEquals("print(42)", result.code)
    }

    @Test
    fun tildeCodeFenceSupportsLanguageInfoString() {
        val result = CodeFenceContent.parse("~~~python title=main.py\nprint(1)\n~~~")
        assertEquals("python", result.language)
        assertEquals("print(1)", result.code)
    }

    @Test
    fun leavesUnfencedCodeUnmodified() {
        val result = CodeFenceContent.parse("print(1)\nprint(2)")
        assertEquals("", result.language)
        assertEquals("print(1)\nprint(2)", result.code)
    }

    @Test
    fun keepsInnerBlankLines() {
        val result = CodeFenceContent.parse("```text\na\n\nb\n```")
        assertEquals("a\n\nb", result.code)
    }
}
