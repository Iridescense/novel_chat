package com.novelchat.ui.creation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    allRoles: List<Role>,
    selectedOtherRoleId: Long?,
    onSenderTypeChange: (SenderType) -> Unit,
    onSelectedOtherRoleChange: (Long?) -> Unit,
    onSetProtagonist: (Long) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var showSenderMenu by remember { mutableStateOf(false) }
    var showProtagonistDialog by remember { mutableStateOf(false) }
    var showOtherRoles by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 发送者按钮
                    Box {
                        when (senderType) {
                            SenderType.PROTAGONIST -> {
                                TextButton(onClick = { showProtagonistDialog = true }) {
                                    Icon(Icons.Default.Star, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("主角: $protagonistName",
                                        style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            SenderType.OTHER -> {
                                TextButton(onClick = { showOtherRoles = !showOtherRoles }) {
                                    Icon(Icons.Default.Person, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    val otherName = otherRoles.find { it.id == selectedOtherRoleId }?.name
                                    Text("其他人: ${otherName ?: "选择"}",
                                        style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            SenderType.NARRATOR -> {
                                TextButton(onClick = {
                                    showSenderMenu = true
                                }) {
                                    Icon(Icons.Default.TextSnippet, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("旁白", style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // 切换发送者类型的菜单
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
                            DropdownMenuItem(
                                text = { Text("其他人") },
                                onClick = {
                                    onSenderTypeChange(SenderType.OTHER)
                                    showSenderMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
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

                    // 其他人展开列表
                    if (showOtherRoles && senderType == SenderType.OTHER) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column {
                                otherRoles.forEach { role ->
                                    val isSelected = role.id == selectedOtherRoleId
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                onSelectedOtherRoleChange(role.id)
                                                onSenderTypeChange(SenderType.OTHER)
                                                showOtherRoles = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RoleAvatar(role, Modifier.size(24.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(role.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 格式按钮（预留）
                IconButton(onClick = { }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "格式")
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
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
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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

    // 主角设置对话框
    if (showProtagonistDialog) {
        AlertDialog(
            onDismissRequest = { showProtagonistDialog = false },
            title = { Text("设置当前节主角") },
            text = {
                Column {
                    Text("请选择谁担任本节的主角：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    allRoles.forEach { role ->
                        val isCurrent = role.name == protagonistName
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetProtagonist(role.id)
                                    onSenderTypeChange(SenderType.PROTAGONIST)
                                    showProtagonistDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoleAvatar(role, Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(role.name, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                if (isCurrent) {
                                    Text("当前主角", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtagonistDialog = false }) { Text("关闭") }
            }
        )
    }
}
