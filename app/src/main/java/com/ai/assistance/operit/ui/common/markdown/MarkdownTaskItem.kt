package com.ai.assistance.operit.ui.common.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Accepts GFM task list syntax both with and without list markers stripped by native parsing. */
internal data class MarkdownTaskItem(val checked: Boolean, val label: String) {
    companion object {
        fun parse(content: String): MarkdownTaskItem? {
            val text = content.trim()
                .removePrefix("- ")
                .removePrefix("* ")
                .removePrefix("+ ")
            if (text.length <= 4 || text[0] != '[' || text[2] != ']' || !text[3].isWhitespace()) {
                return null
            }
            val checked = when (text[1]) {
                'x', 'X' -> true
                ' ' -> false
                else -> return null
            }
            val label = text.substring(4).trimStart()
            return if (label.isEmpty() || label.contains('\n')) null else MarkdownTaskItem(checked, label)
        }
    }
}

/**
 * Local, user-controlled task state; it deliberately does not rewrite AI messages.
 * Rendering is identical regardless of provider.
 */
@Composable
internal fun MarkdownTaskItemView(item: MarkdownTaskItem, textColor: Color) {
    var selected by remember(item) { mutableStateOf(item.checked) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(checked = selected, onCheckedChange = { selected = it })
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}
