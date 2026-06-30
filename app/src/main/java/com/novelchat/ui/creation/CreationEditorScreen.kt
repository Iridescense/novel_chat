package com.novelchat.ui.creation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Message
import com.novelchat.data.model.Segment
import com.novelchat.ui.creation.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationEditorScreen(
    novelId: Long,
    chapterId: Long,
    onBack: () -> Unit,
    onPreview: (Long, Int) -> Unit,
    viewModel: CreationViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(novelId, chapterId) {
        viewModel.loadNovel(novelId, chapterId)
    }

    val novel by viewModel.novel.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val currentSegment by viewModel.currentSegment.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val allChapterMessages by viewModel.allChapterMessages.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentProtagonist by viewModel.currentProtagonist.collectAsState()
    val currentSegmentIndex by viewModel.currentSegmentIndex.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val showSlideMenu by viewModel.showSlideMenu.collectAsState()
    val editingHiddenNote by viewModel.editingHiddenNote.collectAsState()
    val inputSenderType by viewModel.inputSenderType.collectAsState()
    val selectedOtherRoleId by viewModel.selectedOtherRoleId.collectAsState()
    val insertAfterId by viewModel.insertAfterId.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }

    BackHandler {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    val swipeModifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = { },
            onHorizontalDrag = { _, dragAmount ->
                if (dragAmount < -50) {
                    viewModel.toggleSlideMenu()
                }
            }
        )
    }

    val chapterDisplay = currentChapter?.let { ch ->
        if (ch.title.startsWith("第") && ch.title.contains("章")) ch.title
        else "第${ch.orderIndex + 1}章 ${ch.title}"
    } ?: "加载中…"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                novel?.title ?: "加载中…",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (currentChapter != null) {
                                Text(
                                    chapterDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) showExitDialog = true
                            else onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (messages.isNotEmpty()) onPreview(novelId, 0)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "预览")
                        }
                        IconButton(onClick = {
                            viewModel.save()
                            scope.launch { snackbarHostState.showSnackbar("已保存 ✓") }
                        }) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "保存",
                                tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { viewModel.toggleSlideMenu() }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val listState = rememberLazyListState()

                    // 构建带分割线的显示列表
                    val displayItems: List<Pair<Segment?, Message?>> = remember(allChapterMessages) {
                        val items = mutableListOf<Pair<Segment?, Message?>>()
                        var lastSegId = -1L
                        for ((seg, msg) in allChapterMessages) {
                            if (seg.id != lastSegId) {
                                items.add(seg to null) // 分割线标记
                                lastSegId = seg.id
                            }
                            items.add(null to msg)
                        }
                        items
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(swipeModifier),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(displayItems, key = { _: Int, item: Pair<Segment?, Message?> ->
                            when {
                                item.first != null -> "seg_${item.first!!.id}"
                                item.second != null -> "msg_${item.second!!.id}_hn${item.second!!.hasHiddenNote}"
                                else -> "empty"
                            }
                        }) { _: Int, item: Pair<Segment?, Message?> ->
                            val seg = item.first
                            val msg = item.second
                            if (seg != null) {
                                SegmentDivider(
                                    title = seg.title.ifBlank { "节${segments.indexOf(seg) + 1}" },
                                    onTitleChange = { newTitle ->
                                        viewModel.updateSegmentTitle(seg, newTitle)
                                    }
                                )
                            } else if (msg != null) {
                                val role = msg.roleId?.let { id -> roles.find { it.id == id } }
                                val isPro = currentProtagonist?.let { it.id == msg.roleId } == true
                                MessageBubble(
                                    message = msg,
                                    role = role,
                                    isProtagonist = isPro,
                                    enableDoubleTap = false,
                                    onTap = {
                                        actionMessage = msg
                                    }
                                )
                            }
                        }
                    }

                    if (insertAfterId != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("插入模式：添加在指定位置下方",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearInsertAfter() }) {
                                    Text("取消", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    BottomInputBar(
                        senderType = inputSenderType,
                        protagonistName = currentProtagonist?.name ?: "未设置",
                        otherRoles = roles.filter { it.id != currentProtagonist?.id },
                        allRoles = roles,
                        selectedOtherRoleId = selectedOtherRoleId,
                        onSenderTypeChange = { viewModel.setInputSenderType(it) },
                        onSelectedOtherRoleChange = { viewModel.setSelectedOtherRoleId(it) },
                        onSetProtagonist = { roleId ->
                            currentSegment?.let { viewModel.setSegmentProtagonist(it, roleId) }
                        },
                        onSend = { text -> viewModel.sendMessage(text) }
                    )
                }
            }
        }

        // 左侧滑菜单（覆盖层）
        if (showSlideMenu) {
            // 半透明背景点击关闭
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { viewModel.closeSlideMenu() }
            )
            AnimatedVisibility(
                visible = showSlideMenu,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                SlideMenu(
                    segments = segments,
                    currentSegmentIndex = currentSegmentIndex,
                    roles = roles,
                    currentProtagonistId = currentProtagonist?.id,
                    hasUnsavedChanges = hasUnsavedChanges,
                    chapterStatus = currentChapter?.status ?: "draft",
                    onSwitchSegment = { viewModel.switchSegment(it) },
                    onAddSegment = { viewModel.addSegment() },
                    onSegmentProtagonistChange = { seg, id -> viewModel.setSegmentProtagonist(seg, id) },
                    onAddRole = { name, color, avatarType, avatarValue ->
                        viewModel.addRole(name, color, avatarType, avatarValue)
                    },
                    onDeleteRole = { viewModel.deleteRole(it) },
                    onDeleteSegment = { viewModel.deleteSegment(it) },
                    onSave = {
                        viewModel.save()
                        scope.launch { snackbarHostState.showSnackbar("已保存 ✓") }
                    },
                    onToggleChapterStatus = { viewModel.toggleChapterStatus() },
                    onClose = { viewModel.closeSlideMenu() }
                )
            }
        }
    }

    // 未保存退出对话框
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改，是否保存？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save {
                        showExitDialog = false
                        onBack()
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.discardChanges()
                        showExitDialog = false
                        onBack()
                    }) { Text("不保存") }
                    TextButton(onClick = { showExitDialog = false }) { Text("取消") }
                }
            }
        )
    }

    editingHiddenNote?.let { msg ->
        var noteText by remember { mutableStateOf(msg.hiddenNote ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.cancelEditingHiddenNote() },
            title = { Text("编辑隐藏附注") },
            text = {
                Column {
                    Text("消息内容: ${msg.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("隐藏描述（长按可见）") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveHiddenNote(msg.id, noteText.trim()) }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { viewModel.cancelEditingHiddenNote() }) { Text("取消") } }
        )
    }

    actionMessage?.let { msg ->
        val isLast = allChapterMessages.lastOrNull()?.second?.id == msg.id
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            title = { Text("消息操作") },
            text = {
                Column {
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { editingMessage = msg; actionMessage = null },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("编辑消息")
                    }
                    TextButton(onClick = { viewModel.startEditingHiddenNote(msg); actionMessage = null },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("编辑隐藏标注")
                    }
                    TextButton(onClick = {
                        viewModel.setInsertAfterId(msg.id)
                        actionMessage = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text(if (isLast) "恢复到底部添加" else "添加消息（该条下方）")
                    }
                    TextButton(onClick = { viewModel.deleteMessage(msg); actionMessage = null },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp)); Text("删除该消息", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { actionMessage = null }) { Text("关闭") } }
        )
    }

    editingMessage?.let { msg ->
        var editText by remember { mutableStateOf(msg.text) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("编辑消息") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateMessage(msg.copy(text = editText.trim()))
                    editingMessage = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("取消") } }
        )
    }
}
