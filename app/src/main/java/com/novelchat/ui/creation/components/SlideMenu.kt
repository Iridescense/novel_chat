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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelchat.data.model.Chapter
import com.novelchat.data.model.Role
import com.novelchat.data.model.Segment

/**
 * 左侧滑菜单（创作台右侧面板）
 * 包含：章节列表、角色管理、节段切换、保存等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideMenu(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    segments: List<Segment>,
    currentSegmentIndex: Int,
    roles: List<Role>,
    currentProtagonistId: Long?,
    hasUnsavedChanges: Boolean,
    novelStatus: String,
    onSwitchChapter: (Int) -> Unit,
    onAddChapter: (String) -> Unit,
    onRenameChapter: (Chapter, String) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onSwitchSegment: (Int) -> Unit,
    onAddSegment: () -> Unit,
    onSegmentProtagonistChange: (Segment, Long?) -> Unit,
    onAddRole: (String, String, String, String) -> Unit,
    onEditRole: (Role) -> Unit,
    onDeleteRole: (Role) -> Unit,
    onSave: () -> Unit,
    onToggleStatus: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            TopAppBar(
                title = { Text("菜单") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    // 保存按钮
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
                // ===== 剧本状态 =====
                item {
                    ListItem(
                        headlineContent = { Text("剧本状态") },
                        trailingContent = {
                            TextButton(onClick = onToggleStatus) {
                                Text(
                                    if (novelStatus == "draft") "标记为完成"
                                    else "变回草稿"
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }

                // ===== 章节列表 =====
                item {
                    ListItem(
                        headlineContent = { Text("章节列表") },
                        trailingContent = {
                            var showAddDialog by remember { mutableStateOf(false) }
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "添加章节")
                            }
                            if (showAddDialog) {
                                var newTitle by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showAddDialog = false },
                                    title = { Text("新建章节") },
                                    text = {
                                        OutlinedTextField(
                                            value = newTitle,
                                            onValueChange = { newTitle = it },
                                            label = { Text("章节名") },
                                            singleLine = true
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            if (newTitle.isNotBlank()) {
                                                onAddChapter(newTitle.trim())
                                                showAddDialog = false
                                            }
                                        }) { Text("确认") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showAddDialog = false }) { Text("取消") }
                                    }
                                )
                            }
                        }
                    )
                }

                items(chapters, key = { it.id }) { chapter ->
                    val index = chapters.indexOf(chapter)
                    val isCurrent = index == currentChapterIndex
                    var showEditDialog by remember { mutableStateOf(false) }

                    ListItem(
                        headlineContent = {
                            Text(
                                chapter.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            if (isCurrent) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            if (isCurrent) {
                                Text("当前", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable { onSwitchChapter(index) }
                    )

                    // 如果当前章节，显示其下的节列表
                    if (isCurrent) {
                        segments.forEach { segment ->
                            val segIndex = segments.indexOf(segment)
                            val isSegCurrent = segIndex == currentSegmentIndex
                            ListItem(
                                headlineContent = {
                                    Text(
                                        segment.title.ifBlank { "节${segIndex + 1}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSegCurrent) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                supportingContent = {
                                    val protoName = roles.find { it.id == segment.protagonistId }?.name
                                    if (protoName != null) {
                                        Text("主角: $protoName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        if (isSegCurrent) Icons.Default.Circle else Icons.Default.CircleOutlined,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isSegCurrent) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.outline
                                    )
                                },
                                modifier = Modifier.clickable { onSwitchSegment(segIndex) }
                            )
                        }

                        // 添加节
                        TextButton(
                            onClick = onAddSegment,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加节", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // 编辑/删除章节
                    if (showEditDialog) {
                        var renameTitle by remember { mutableStateOf(chapter.title) }
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("重命名章节") },
                            text = {
                                OutlinedTextField(
                                    value = renameTitle,
                                    onValueChange = { renameTitle = it },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    onRenameChapter(chapter, renameTitle.trim())
                                    showEditDialog = false
                                }) { Text("确认") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                // ===== 角色管理 =====
                item {
                    ListItem(
                        headlineContent = { Text("角色管理") },
                        trailingContent = {
                            var showAddDialog by remember { mutableStateOf(false) }
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "添加角色")
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
                    )
                }

                items(roles, key = { it.id }) { role ->
                    val isProtagonist = role.id == currentProtagonistId
                    ListItem(
                        headlineContent = { Text(role.name) },
                        supportingContent = {
                            if (isProtagonist) {
                                Text("当前节主角",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        leadingContent = {
                            RoleAvatar(
                                role = role,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEditRole(role) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑",
                                        modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteRole(role) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            // 点击角色设为当前节主角
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
                Spacer(modifier = Modifier.height(12.dp))
                Text("角色颜色", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = color == hex
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(50),
                            color = c,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Box(
                                modifier = Modifier.clickable { color = hex },
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "头像将自动使用首字母，后续可在角色编辑中更换",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), color, "text", name.take(1)) },
                enabled = name.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
