package com.novelchat.ui.creation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelchat.data.model.Role
import com.novelchat.data.model.Segment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideMenu(
    segments: List<Segment>,
    currentSegmentIndex: Int,
    roles: List<Role>,
    currentProtagonistId: Long?,
    hasUnsavedChanges: Boolean,
    novelStatus: String,
    onSwitchSegment: (Int) -> Unit,
    onAddSegment: () -> Unit,
    onSegmentProtagonistChange: (Segment, Long?) -> Unit,
    onAddRole: (String, String, String, String) -> Unit,
    onDeleteRole: (Role) -> Unit,
    onSave: () -> Unit,
    onToggleStatus: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("菜单") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "保存",
                            tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 剧本状态
                item {
                    ListItem(
                        headlineContent = { Text("剧本状态") },
                        trailingContent = {
                            TextButton(onClick = onToggleStatus) {
                                Text(if (novelStatus == "draft") "标记为完成" else "变回草稿")
                            }
                        }
                    )
                    HorizontalDivider()
                }

                // ===== 节列表 =====
                item {
                    ListItem(
                        headlineContent = { Text("节列表") },
                        trailingContent = {
                            IconButton(onClick = onAddSegment) {
                                Icon(Icons.Default.Add, contentDescription = "添加节")
                            }
                        }
                    )
                }

                items(segments, key = { it.id }) { segment ->
                    val segIndex = segments.indexOf(segment)
                    val isCurrent = segIndex == currentSegmentIndex
                    ListItem(
                        headlineContent = {
                            Text(
                                segment.title.ifBlank { "节${segIndex + 1}" },
                                style = if (isCurrent) MaterialTheme.typography.bodyLarge
                                        else MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            val protoName = roles.find { it.id == segment.protagonistId }?.name
                            Text("主角: ${protoName ?: "未设置"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        },
                        leadingContent = {
                            Icon(
                                if (isCurrent) Icons.Default.Circle else Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.clickable { onSwitchSegment(segIndex) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                // ===== 角色管理 =====
                item {
                    ListItem(
                        headlineContent = { Text("角色管理") },
                        trailingContent = {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "添加角色")
                            }
                        }
                    )
                }

                items(roles, key = { it.id }) { role ->
                    val isProtagonist = role.id == currentProtagonistId
                    ListItem(
                        headlineContent = { Text(role.name) },
                        supportingContent = {
                            if (isProtagonist) Text("当前节主角",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        },
                        leadingContent = {
                            RoleAvatar(role = role, modifier = Modifier.size(28.dp))
                        },
                        trailingContent = {
                            IconButton(onClick = { onDeleteRole(role) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clickable {
                            val currentSeg = segments.getOrNull(currentSegmentIndex)
                            if (currentSeg != null) {
                                onSegmentProtagonistChange(
                                    currentSeg,
                                    if (isProtagonist) null else role.id
                                )
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoleDialog(
            onConfirm = { name, color, avatarType, avatarValue ->
                onAddRole(name, color, avatarType, avatarValue)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddRoleDialog(
    onConfirm: (name: String, color: String, avatarType: String, avatarValue: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#4ECDC4") }

    val colorOptions = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加角色") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("角色颜色", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    colorOptions.forEach { hex ->
                        val isSelected = color == hex
                        val c = androidx.compose.ui.graphics.Color(
                            android.graphics.Color.parseColor(hex))
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(50),
                            color = c,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp,
                                    MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Box(modifier = Modifier.clickable { color = hex },
                                contentAlignment = Alignment.Center) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), color, "text", name.take(1)) },
                enabled = name.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
