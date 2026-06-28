package com.novelchat.ui.creation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 节(Segment)之间的分割线，可编辑文字
 */
@Composable
fun SegmentDivider(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(title) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = {
                        editText = it
                        onTitleChange(it)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.widthIn(max = 200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { isEditing = false }) {
                    Text("完成", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = title.ifBlank { "● ● ●" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { isEditing = true }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}
