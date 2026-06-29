package com.novelchat.ui.creation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Message
import com.novelchat.ui.creation.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationEditorScreen(
    novelId: Long,
    onBack: () -> Unit,
    onPreview: (Long, Int) -> Unit,
    viewModel: CreationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }

    val novel by viewModel.novel.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val currentSegment by viewModel.currentSegment.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentProtagonist by viewModel.currentProtagonist.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val showSlideMenu by viewModel.showSlideMenu.collectAsState()
    val editingHiddenNote by viewModel.editingHiddenNote.collectAsState()
    val inputSenderType by viewModel.inputSenderType.collectAsState()
    val selectedOtherRoleId by viewModel.selectedOtherRoleId.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<Message?>(null) }

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
                                currentChapter!!.title,
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
                        if (messages.isNotEmpty()) {
                            onPreview(novelId, 0)
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "预览")
                    }
                    IconButton(onClick = { viewModel.save() }) {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(swipeModifier),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (currentSegment != null && currentSegment!!.title.isNotBlank()) {
                        item {
                            SegmentDivider(
                                title = currentSegment!!.title,
                                onTitleChange = { newTitle ->
                                    currentSegment?.let {
                                        viewModel.updateSegmentTitle(it, newTitle)
                                    }
                                }
                            )
                        }
                    }

                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                        val role = message.roleId?.let { id -> roles.find { it.id == id } }
                        val isProtagonist = currentProtagonist?.let { it.id == message.roleId } == true

                        MessageBubble(
                            message = message,
                            role = role,
                            isProtagonist = isProtagonist,
                            onDoubleTap = {
                                actionMessage = message
                            }
                        )
                    }
                }

                BottomInputBar(
                    senderType = inputSenderType,
                    protagonistName = currentProtagonist?.name ?: "未设置",
                    otherRoles = roles.filter { it.id != currentProtagonist?.id },
                    selectedOtherRoleId = selectedOtherRoleId,
                    onSenderTypeChange = { viewModel.setInputSenderType(it) },
                    onSelectedOtherRoleChange = { viewModel.setSelectedOtherRoleId(it) },
                    onSend = { text -> viewModel.sendMessage(text) }
                )
            }

            AnimatedVisibility(
                visible = showSlideMenu,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                SlideMenu(
                    chapters = chapters,
                    currentChapterIndex = viewModel.currentChapterIndex.value,
                    segments = segments,
                    currentSegmentIndex = viewModel.currentSegmentIndex.value,
                    roles = roles,
                    currentProtagonistId = currentProtagonist?.id,
                    hasUnsavedChanges = hasUnsavedChanges,
                    novelStatus = novel?.status ?: "draft",
                    onSwitchChapter = { viewModel.switchChapter(it) },
                    onAddChapter = { viewModel.addChapter(it) },
                    onRenameChapter = { ch, title -> viewModel.renameChapter(ch, title) },
                    onDeleteChapter = { viewModel.deleteChapter(it) },
                    onSwitchSegment = { viewModel.switchSegment(it) },
                    onAddSegment = { viewModel.addSegment() },
                    onSegmentProtagonistChange = { seg, id -> viewModel.setSegmentProtagonist(seg, id) },
                    onAddRole = { name, color, avatarType, avatarValue ->
                        viewModel.addRole(name, color, avatarType, avatarValue)
                    },
                    onEditRole = { },
                    onDeleteRole = { viewModel.deleteRole(it) },
                    onSave = { viewModel.save() },
                    onToggleStatus = { viewModel.toggleNovelStatus() },
                    onClose = { viewModel.closeSlideMenu() }
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改，是否保存？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save()
                    showExitDialog = false
                    onBack()
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        onBack()
                    }) { Text("不保存") }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("取消")
                    }
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
                    Text(
                        "消息内容: ${msg.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                TextButton(onClick = {
                    viewModel.saveHiddenNote(msg.id, noteText.trim())
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEditingHiddenNote() }) { Text("取消") }
            }
        )
    }

    actionMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            title = { Text("消息操作") },
            text = {
                Column {
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            viewModel.startEditingHiddenNote(msg)
                            actionMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("编辑隐藏附注")
                    }
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(msg)
                            actionMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("删除消息", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionMessage = null }) { Text("关闭") }
            }
        )
    }
}
