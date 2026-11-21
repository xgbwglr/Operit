package com.ai.assistance.operit.util.stream.plugins

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NestedListPluginTest {
    @Test
    fun indentedUnorderedListIsDetected() {
        val plugin = StreamMarkdownUnorderedListPlugin(includeMarker = false)
        plugin.initPlugin()
        val emissions = mutableListOf<Boolean>()
        "  - child".forEachIndexed { i, c ->
            emissions.add(plugin.processChar(c, atStartOfLine = (i == 0)))
        }
        assertEquals(PluginState.PROCESSING, plugin.state)
        // Indentation stays in the output, while the marker is omitted.
        assertTrue(emissions[0])
        assertTrue(emissions[1])
        assertEquals(false, emissions[2])
        assertEquals(false, emissions[3])
    }

    @Test
    fun indentedOrderedListIsDetected() {
        val plugin = StreamMarkdownOrderedListPlugin(includeMarker = true)
        plugin.initPlugin()
        "    12. example".forEachIndexed { i, c ->
            plugin.processChar(c, atStartOfLine = (i == 0))
        }
        assertEquals(PluginState.PROCESSING, plugin.state)
    }

    @Test
    fun regularIndentedParagraphIsNotList() {
        val plugin = StreamMarkdownUnorderedListPlugin(includeMarker = false)
        plugin.initPlugin()
        "  plain paragraph".forEachIndexed { i, c ->
            plugin.processChar(c, atStartOfLine = (i == 0))
        }
        assertEquals(PluginState.IDLE, plugin.state)
    }
}
