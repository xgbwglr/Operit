package com.ai.assistance.operit.ui.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownListIndentationTest {
    @Test
    fun rootListIsNotIndented() {
        assertEquals(0, MarkdownListIndentation.level("root item"))
        assertEquals(0, MarkdownListIndentation.level("- root item"))
    }

    @Test
    fun nestedListsUseIndentationDepth() {
        assertEquals(1, MarkdownListIndentation.level("  child"))
        assertEquals(2, MarkdownListIndentation.level("    grandchild"))
        assertEquals(3, MarkdownListIndentation.level("      next level"))
    }

    @Test
    fun indentationIsBoundedOnSmallScreens() {
        assertEquals(4, MarkdownListIndentation.level("                deep"))
    }

    @Test
    fun indentationIsRemovedFromInlineChildren() {
        assertEquals("**bold**", MarkdownListIndentation.stripLeadingIndent("    **bold**"))
    }
}
