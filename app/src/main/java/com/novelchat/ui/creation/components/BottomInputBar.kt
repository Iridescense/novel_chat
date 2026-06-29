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
    var showProtagonistDialog by remember { mutableStateOf(false) }
    var showOtherRoles by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // ===== 四个功能按钮行 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 按钮1：主角
                Button(
                    onClick = {
                        onSenderTypeChange(SenderType.PROTAGONIST)
                        showProtagonistDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (senderType == SenderType.PROTAGONIST)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(protagonistName, style = MaterialTheme.typography.labelSmall)
                }

                // 按钮2：其他人
                Button(
                    onClick = {
                        onSenderTypeChange(SenderType.OTHER)
                        showOtherRoles = !showOtherRoles
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (senderType == SenderType.OTHER)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    val otherName = otherRoles.find { it.id == selectedOtherRoleId }?.name
                    Text(otherName ?: "其他人", style = MaterialTheme.typography.labelSmall)
                }

                // 按钮3：旁白
                Button(
                    onClick = { onSenderTypeChange(SenderType.NARRATOR) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (senderType == SenderType.NARRATOR)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.TextSnippet, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("旁白", style = MaterialTheme.typography.labelSmall)
                }

                // 按钮4：格式（预留）
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.FormatBold, contentDescription = "格式", modifier = Modifier.size(14.dp))
                }
            }

            // 其他人展开列表
            if (showOtherRoles && otherRoles.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectedOtherRoleChange(role.id)
                                        onSenderTypeChange(SenderType.OTHER)
                                        showOtherRoles = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RoleAvatar(role, Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(role.name, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // ===== 输入框 + 发送按钮 =====
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                    Icon(Icons.Default.Send, contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary)
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
                    Text("选择谁担任本节主角：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    allRoles.forEach { role ->
                        val isCurrent = role.name == protagonistName
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSetProtagonist(role.id)
                                onSenderTypeChange(SenderType.PROTAGONIST)
                                showProtagonistDialog = false
                            }.padding(vertical = 8.dp, horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoleAvatar(role, Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(role.name, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                if (isCurrent) Text("当前主角", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProtagonistDialog = false }) { Text("关闭") } }
        )
    }
}
