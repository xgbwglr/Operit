package com.ai.assistance.operit.ui.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTaskItemTest {
    @Test fun checkedTask() {
        val parsed = MarkdownTaskItem.parse("- [x] 完成了吗")
        assertEquals("完成了吗", parsed?.label)
        assertTrue(parsed!!.checked)
    }

    @Test fun uncheckedTaskWithoutMarker() {
        val parsed = MarkdownTaskItem.parse("[ ] 编写测试")
        assertEquals("编写测试", parsed?.label)
        assertFalse(parsed!!.checked)
    }

    @Test fun supportsAlternativeListMarkers() {
        assertTrue(MarkdownTaskItem.parse("* [X] Done")!!.checked)
        assertFalse(MarkdownTaskItem.parse("+ [ ] Todo")!!.checked)
    }

    @Test fun leavesRegularListTextAlone() {
        assertNull(MarkdownTaskItem.parse("- Regular item"))
        assertNull(MarkdownTaskItem.parse("- [no] invalid task"))
    }

    @Test fun avoidsRenderingMultiLineItemsAsSingleTask() {
        assertNull(MarkdownTaskItem.parse("[ ] first\nsecond"))
    }
}
