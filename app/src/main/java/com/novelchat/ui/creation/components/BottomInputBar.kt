package com.novelchat.ui.creation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novelchat.data.model.Role
import com.novelchat.ui.creation.SenderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomInputBar(
    senderType: SenderType,
    protagonistName: String,
    otherRoles: List<Role>,
    selectedOtherRoleId: Long?,
    onSenderTypeChange: (SenderType) -> Unit,
    onSelectedOtherRoleChange: (Long?) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var showSenderMenu by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 发送者选择行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 发送者按钮
                Box {
                    val senderLabel = when (senderType) {
                        SenderType.PROTAGONIST -> "主角: $protagonistName"
                        SenderType.OTHER -> {
                            val otherName = otherRoles.find { it.id == selectedOtherRoleId }?.name
                            otherName?.let { "其他人: $it" } ?: "其他人"
                        }
                        SenderType.NARRATOR -> "旁白"
                    }

                    TextButton(onClick = { showSenderMenu = true }) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(senderLabel, style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showSenderMenu,
                        onDismissRequest = { showSenderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("主角: $protagonistName") },
                            onClick = {
                                onSenderTypeChange(SenderType.PROTAGONIST)
                                showSenderMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                        )
                        otherRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text("其他人: ${role.name}") },
                                onClick = {
                                    onSenderTypeChange(SenderType.OTHER)
                                    onSelectedOtherRoleChange(role.id)
                                    showSenderMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("旁白") },
                            onClick = {
                                onSenderTypeChange(SenderType.NARRATOR)
                                showSenderMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) }
                        )
                    }
                }

                // 格式按钮
                Box {
                    IconButton(onClick = { showFormatMenu = true }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "格式")
                    }
                    DropdownMenu(
                        expanded = showFormatMenu,
                        onDismissRequest = { showFormatMenu = false }
                    ) {
                        Text("格式功能（后续扩展）", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp))
                        Text("加粗 / 斜体 / 下划线 / 颜色 / 字号 / 表情",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // 输入框 + 发送按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息…") },
                    textStyle = TextStyle(fontSize = 16.sp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = text.isNotBlank(),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
