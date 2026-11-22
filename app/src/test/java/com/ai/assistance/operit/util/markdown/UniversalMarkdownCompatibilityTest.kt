package com.ai.assistance.operit.util.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cross-provider examples: no model-specific prefixes, system prompts, or tags
 * are needed to make these variants render consistently.
 */
class UniversalMarkdownCompatibilityTest {

    @Test
    fun normalizesGfmTableWithoutBoundaryPipes() {
        val source = "名称 | 用途\n--- | ---\nPython | 编程\nC++ | 系统开发"
        val expected = "| 名称 | 用途 |\n| --- | --- |\n| Python | 编程 |\n| C++ | 系统开发 |"
        assertEquals(expected, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun keepsExistingPipeBoundedGfmTable() {
        val source = "| 名称 | 用途 |\n| :--- | ---: |\n| Python | 编程 |"
        assertEquals(source, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun doesNotChangeAProseLineContainingPipes() {
        val source = "Use a | b to denote alternatives.\nNext paragraph."
        assertEquals(source, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun codeFenceContentIsNeverNormalizedAsMarkdownTable() {
        val source = "~~~markdown\n名称 | 用途\n--- | ---\nPython | 编程\n~~~"
        val expected = "```markdown\n名称 | 用途\n--- | ---\nPython | 编程\n```"
        assertEquals(expected, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun tildeFenceUsesLongerBacktickFenceWhenBodyHasBackticks() {
        val source = "~~~text\n```\n~~~"
        val expected = "````text\n```\n````"
        assertEquals(expected, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun incompleteCodeFenceRemainsUnmodified() {
        val source = "~~~python\nprint('hello')"
        assertEquals(source, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun latexBlocksAreNeverConvertedToTables() {
        val source = "$$\na | b\n--- | ---\n$$"
        assertEquals(source, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun preservesCrLfInNormalizedTable() {
        val source = "A | B\r\n--- | ---\r\n1 | 2"
        val expected = "| A | B |\r\n| --- | --- |\r\n| 1 | 2 |"
        assertEquals(expected, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun escapedPipeDoesNotSplitTableCell() {
        val source = "A \\| B | C\n--- | ---\nleft | right"
        val expected = "| A \\| B | C |\n| --- | --- |\n| left | right |"
        assertEquals(expected, UniversalMarkdownCompatibility.normalize(source))
    }

    @Test
    fun ordinaryChineseAndMathTextStaysExactlyTheSame() {
        val source = "## 数学解题\n设 \\(x^2+1=2\\)，则 **x = 1**。"
        assertEquals(source, UniversalMarkdownCompatibility.normalize(source))
    }
}
